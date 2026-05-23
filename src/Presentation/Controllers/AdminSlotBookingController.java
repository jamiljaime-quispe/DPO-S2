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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminSlotBookingController {
    private static final int ADMIN_MODE = 1;
    private static final int USER_MODE = 2;
    private static final int BACKGROUND_TEST_DELAY_MS = 300;
    private static final int ROW_LOAD_DELAY_MS = 100;
    private static final DateTimeFormatter RESERVATION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    public void showView(int mode) {
        currentMode = mode;
        bookingView.setMode(mode);
        bookingView.clearBookingsTable();
        loadBookings();
        bookingView.setVisible(true);
    }

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
                    delayRowLoad();
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
                } catch (Exception e) {
                    bookingView.showError("Failed to load slot bookings: " + e.getMessage());
                } finally {
                    bookingView.setLoading(false);
                }
            }
        }.execute();
    }

    public void createBooking(String plate, VehicleType type, String spaceCode) {
        if (plate == null || plate.isBlank() || spaceCode == null || spaceCode.isBlank()) {
            bookingView.showError("License plate and space code cannot be empty.");
            return;
        }

        bookingView.setLoading(true);
        new SwingWorker<Reservation, Void>() {
            private String errorMessage;

            @Override
            protected Reservation doInBackground() {
                try {
                    simulateDatabaseDelay();
                    return reservationService.createReservation(
                            userService.getLastLoggedInUserId(),
                            normalizePlate(plate),
                            type,
                            spaceCode);
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return null;
                }
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
                        bookingView.showError(errorMessage != null
                                ? errorMessage
                                : "Booking could not be created.");
                    }
                } catch (Exception e) {
                    bookingView.setLoading(false);
                    bookingView.showError("Failed to create booking: " + e.getMessage());
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

    public void editBooking(String originalSpaceCode, String plate, VehicleType type, String spaceCode) {
        if (plate == null || plate.isBlank() || spaceCode == null || spaceCode.isBlank()) {
            bookingView.showError("License plate and space code cannot be empty.");
            return;
        }

        bookingView.setLoading(true);
        new SwingWorker<Reservation, Void>() {
            private String errorMessage;

            @Override
            protected Reservation doInBackground() {
                try {
                    simulateDatabaseDelay();
                    return reservationService.reassignReservation(normalizePlate(plate), spaceCode);
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return null;
                }
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
                        bookingView.showError(errorMessage != null
                                ? errorMessage
                                : "Booking could not be reassigned.");
                    }
                } catch (Exception e) {
                    bookingView.setLoading(false);
                    bookingView.showError("Failed to reassign booking: " + e.getMessage());
                }
            }
        }.execute();
    }

    public void deleteBooking(String spaceCode, String plate) {
        if (plate == null || plate.isBlank()) {
            bookingView.showError("Cannot cancel a booking without a license plate.");
            return;
        }

        bookingView.setLoading(true);
        new SwingWorker<Boolean, Void>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    simulateDatabaseDelay();
                    if (currentMode == USER_MODE) {
                        return reservationService.cancelReservationByPlateForUser(
                                userService.getLastLoggedInUserId(),
                                normalizePlate(plate));
                    }
                    return adminService.cancelReservationByPlate(normalizePlate(plate));
                } catch (Exception e) {
                    errorMessage = "Failed to cancel booking: " + e.getMessage();
                    return false;
                }
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
                        bookingView.showError(errorMessage != null
                                ? errorMessage
                                : "No active booking found for license plate \"" + plate + "\".");
                    }
                } catch (Exception e) {
                    bookingView.setLoading(false);
                    bookingView.showError("Failed to cancel booking: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void delayRowLoad() {
        // Row-by-row display delay disabled because automatic refreshes should be fast.
        // try {
        //     Thread.sleep(ROW_LOAD_DELAY_MS);
        // } catch (InterruptedException e) {
        //     Thread.currentThread().interrupt();
        // }
    }

    private void simulateDatabaseDelay() {
        try {
            Thread.sleep(BACKGROUND_TEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String normalizePlate(String plate) {
        return plate.trim().toUpperCase();
    }
}
