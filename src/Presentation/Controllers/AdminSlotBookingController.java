package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.ReservationService;
import Business.Services.UserService;
import Presentation.Views.AdminSlotBookingManagementView;

import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Controller for the slot booking management view.
 * Supports both admin mode (manage all bookings) and user mode (manage own bookings).
 */
public class AdminSlotBookingController {
    private static final int ADMIN_MODE = 1;
    private static final int USER_MODE = 2;

    private AdminSlotBookingManagementView bookingView;
    private ParkingService parkingService;
    private AdminService adminService;
    private ReservationService reservationService;
    private UserService userService;
    private int currentMode = ADMIN_MODE;
    private volatile int bookingsLoadId;

    private static class BookingRow {
        private ParkingSpace space;
        private boolean userBooking;

        private BookingRow(ParkingSpace space, boolean userBooking) {
            this.space = space;
            this.userBooking = userBooking;
        }
    }

    /**
     * Constructs the controller and wires it to the given view.
     *
     * @param bookingView        the slot booking management view
     * @param parkingService     the parking service
     * @param adminService       the admin service
     * @param reservationService the reservation service
     * @param userService        the user service
     */
    public AdminSlotBookingController(AdminSlotBookingManagementView bookingView, ParkingService parkingService,
                                      AdminService adminService, ReservationService reservationService,
                                      UserService userService) {
        this.bookingView = bookingView;
        this.parkingService = parkingService;
        this.adminService = adminService;
        this.reservationService = reservationService;
        this.userService = userService;
        bookingView.setController(this);
    }

    /**
     * Configures the view for the given mode and makes it visible.
     *
     * @param mode ADMIN_MODE (1) or USER_MODE (2)
     */
    public void showView(int mode) {
        currentMode = mode;
        bookingView.setMode(mode);
        bookingView.clearBookingsTable();
        loadBookings();
        bookingView.setVisible(true);
    }

    /**
     * Reloads the view if it is currently visible.
     * Safe to call from any thread.
     */
    public void refreshIfVisible() {
        if (bookingView == null || !bookingView.isVisible()) return;

        if (SwingUtilities.isEventDispatchThread()) {
            loadBookings();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    loadBookings();
                }
            });
        }
    }

    /**
     * Loads all parking spaces and highlights the user's own bookings in USER_MODE.
     * Uses a generation counter to discard results from stale background tasks.
     */
    public void loadBookings() {
        bookingsLoadId++;
        int loadId = bookingsLoadId;

        bookingView.setLoading(true);

        int modeForLoad = currentMode;
        int userId = userService.getLastLoggedInUserId();

        new SwingWorker<Set<String>, BookingRow>() {
            @Override
            protected Set<String> doInBackground() {
                List<ParkingSpace> spaces = parkingService.getAllSpaces();
                Set<String> userBookingCodes = new HashSet<>();
                List<ParkingSpace> orderedSpaces = new ArrayList<>();
                Set<String> loadedCodes = new HashSet<>();

                if (modeForLoad == USER_MODE && userId > 0) {
                    List<Reservation> userReservations = reservationService.getReservationsByUser(userId);
                    for (Reservation reservation : userReservations) {
                        if (!reservation.isActive()) {
                            continue;
                        }
                        if (reservation.getParkingSpace() != null) {
                            String code = reservation.getParkingSpace().getId();
                            userBookingCodes.add(code);

                            ParkingSpace matchingSpace = findSpaceByCode(spaces, code);
                            if (matchingSpace != null) {
                                orderedSpaces.add(matchingSpace);
                            } else {
                                orderedSpaces.add(reservation.getParkingSpace());
                            }
                        }
                    }
                }

                for (ParkingSpace space : spaces) {
                    if (!userBookingCodes.contains(space.getId())) {
                        orderedSpaces.add(space);
                    }
                }

                for (ParkingSpace space : orderedSpaces) {
                    loadedCodes.add(space.getId());
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    if (loadId == bookingsLoadId) {
                        publish(new BookingRow(space, userBookingCodes.contains(space.getId())));
                    }
                }

                return loadedCodes;
            }

            @Override
            protected void process(List<BookingRow> chunks) {
                if (loadId != bookingsLoadId) return;

                for (BookingRow row : chunks) {
                    bookingView.addBookingToTable(row.space, row.userBooking);
                }
            }

            @Override
            protected void done() {
                if (loadId != bookingsLoadId) return;

                try {
                    Set<String> loadedCodes = get();
                    bookingView.removeBookingSpacesNotIn(loadedCodes);
                    bookingView.closeActiveBookingDialogIfTargetUnavailable();
                    bookingView.closeActiveCancelDialogIfTargetUnavailable();
                    if (modeForLoad == USER_MODE && userId > 0) {
                        bookingView.updateReservationsTable(
                                reservationService.getReservationsByUser(userId));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    bookingView.showError("Failed to load slot bookings: " + e.getMessage());
                } finally {
                    bookingView.setLoading(false);
                }
            }
        }.execute();
    }

    /**
     * Creates a reservation for the given plate, vehicle type, and space code.
     *
     * @param plate     license plate
     * @param type      vehicle type
     * @param spaceCode target parking space code
     */
    public void createBooking(String plate, VehicleType type, String spaceCode) {
        if (plate == null || plate.isBlank() || spaceCode == null || spaceCode.isBlank()) {
            bookingView.showError("License plate and space code cannot be empty.");
            return;
        }

        bookingView.setLoading(true);
        new SwingWorker<Reservation, Void>() {
            @Override
            protected Reservation doInBackground() throws Exception {
                return reservationService.createReservation(
                        userService.getLastLoggedInUserId(),
                        normalizePlate(plate),
                        type,
                        spaceCode);
            }

            @Override
            protected void done() {
                try {
                    Reservation reservation = get();
                    if (reservation != null) {
                        bookingView.showInfo("Booking created for space \"" + spaceCode
                                + "\" and plate \"" + normalizePlate(plate) + "\".");
                        loadBookings();
                    } else {
                        bookingView.setLoading(false);
                        bookingView.showError("Booking could not be created.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    bookingView.setLoading(false);
                    Throwable cause = e.getCause();
                    bookingView.showError(cause != null ? cause.getMessage() : "Failed to create booking.");
                }
            }
        }.execute();
    }

    private ParkingSpace findSpaceByCode(List<ParkingSpace> spaces, String code) {
        for (ParkingSpace space : spaces) {
            if (space.getId().equals(code)) {
                return space;
            }
        }
        return null;
    }

    /**
     * Reassigns an existing reservation identified by plate to a different space.
     *
     * @param originalSpaceCode the original space code (for feedback messages)
     * @param plate             the license plate on the reservation
     * @param type              the vehicle type
     * @param spaceCode         the target space code
     */
    public void editBooking(String originalSpaceCode, String plate, VehicleType type, String spaceCode) {
        if (plate == null || plate.isBlank() || spaceCode == null || spaceCode.isBlank()) {
            bookingView.showError("License plate and space code cannot be empty.");
            return;
        }

        bookingView.setLoading(true);
        new SwingWorker<Reservation, Void>() {
            @Override
            protected Reservation doInBackground() throws Exception {
                return reservationService.reassignReservation(normalizePlate(plate), spaceCode);
            }

            @Override
            protected void done() {
                try {
                    Reservation reservation = get();
                    if (reservation != null) {
                        bookingView.showInfo("Booking reassigned from space \"" + originalSpaceCode
                                + "\" to space \"" + spaceCode + "\".");
                        loadBookings();
                    } else {
                        bookingView.setLoading(false);
                        bookingView.showError("Booking could not be reassigned.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    bookingView.setLoading(false);
                    Throwable cause = e.getCause();
                    bookingView.showError(cause != null ? cause.getMessage() : "Failed to reassign booking.");
                }
            }
        }.execute();
    }

    /**
     * Cancels the reservation for the given space and plate.
     * In USER_MODE only the current user's own reservation is cancelled.
     *
     * @param spaceCode the space code (for feedback messages)
     * @param plate     the license plate on the reservation
     */
    public void deleteBooking(String spaceCode, String plate) {
        if (plate == null || plate.isBlank()) {
            bookingView.showError("Cannot cancel a booking without a license plate.");
            return;
        }

        bookingView.setLoading(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (currentMode == USER_MODE) {
                    return reservationService.cancelReservationByPlateForUser(
                            userService.getLastLoggedInUserId(),
                            normalizePlate(plate));
                }
                return adminService.cancelReservationByPlate(normalizePlate(plate));
            }

            @Override
            protected void done() {
                try {
                    boolean cancelled = get();
                    if (cancelled) {
                        if (currentMode == USER_MODE) {
                            bookingView.showInfo("Your booking for space \"" + spaceCode + "\" has been cancelled.");
                        } else {
                            bookingView.showInfo("Booking for space \"" + spaceCode
                                    + "\" has been cancelled. The space may still show occupied if a vehicle is parked there.");
                        }
                        loadBookings();
                    } else {
                        bookingView.setLoading(false);
                        bookingView.showError("No active booking found for license plate \"" + plate + "\".");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    bookingView.setLoading(false);
                    Throwable cause = e.getCause();
                    bookingView.showError(cause != null ? cause.getMessage() : "Failed to cancel booking.");
                }
            }
        }.execute();
    }

    private String normalizePlate(String plate) {
        return plate.trim().toUpperCase();
    }
}
