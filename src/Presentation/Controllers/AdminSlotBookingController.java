package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.ReservationService;
import Business.Services.UserService;
import Presentation.Views.AdminSlotBookingManagementView;
import Presentation.Views.SlotBookingActions;

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
public class AdminSlotBookingController implements SlotBookingActions {
    private static final int ADMIN_MODE = 1;
    private static final int USER_MODE = 2;

    private AdminSlotBookingManagementView bookingView;
    private ParkingService parkingService;
    private AdminService adminService;
    private ReservationService reservationService;
    private UserService userService;
    private Runnable logoutAction;
    private int currentMode = ADMIN_MODE;
    private volatile int bookingsLoadId;
    private String currentUserBookingPlate;
    private VehicleType currentUserBookingType;

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
        setViewController();
        setViewLogoutListener();
    }

    /** Sets the action used when the user logs out from this dialog. */
    public void setLogoutAction(Runnable logoutAction) {
        this.logoutAction = logoutAction;
    }

    /**
     * Configures the view for the given mode and makes it visible.
     *
     * @param mode ADMIN_MODE (1) or USER_MODE (2)
     */
    public void showView(int mode) {
        currentMode = mode;
        currentUserBookingPlate = null;
        currentUserBookingType = null;
        setBookingViewMode(mode);
        setUserBookingVehicle(currentUserBookingPlate, currentUserBookingType);
        clearBookingsTable();
        loadBookings();
        showBookingView();
    }

    /**
     * Reloads the view if it is currently visible.
     * Safe to call from any thread.
     */
    public void refreshIfVisible() {
        if (!isBookingViewVisible()) return;

        if (isEventDispatchThread()) {
            loadBookings();
        } else {
            runOnEventDispatchThread(() -> loadBookings());
        }
    }

    /**
     * Loads all parking spaces and highlights the user's own bookings in USER_MODE.
     * Uses a generation counter to discard results from stale background tasks.
     */
    public void loadBookings() {
        bookingsLoadId++;
        int loadId = bookingsLoadId;
        setLoading(true);

        int modeForLoad = currentMode;
        int userId = getCurrentUserId();
        VehicleType typeForLoad = currentUserBookingType;

        new SwingWorker<Set<String>, BookingRow>() {
            /** Loads booking rows away from the EDT. */
            @Override
            protected Set<String> doInBackground() {
                BookingLoadData loadData = loadBookingRows(modeForLoad, userId, typeForLoad);
                for (BookingRow row : loadData.getRows()) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    if (loadId == bookingsLoadId) {
                        publish(row);
                    }
                }
                return loadData.getLoadedCodes();
            }

            /** Adds loaded booking rows to the table on the EDT. */
            @Override
            protected void process(List<BookingRow> chunks) {
                if (loadId != bookingsLoadId) return;

                for (BookingRow row : chunks) {
                    addBookingToTable(row.getSpace(), row.isUserBooking());
                }
            }

            /** Finishes refreshing the booking tables. */
            @Override
            protected void done() {
                if (loadId != bookingsLoadId) return;

                try {
                    Set<String> loadedCodes = get();
                    finishBookingsLoad(loadedCodes, modeForLoad, userId);
                } catch (InterruptedException | ExecutionException e) {
                    showError("Failed to load slot bookings: " + e.getMessage());
                } finally {
                    setLoading(false);
                }
            }
        }.execute();
    }

    /** Loads and orders booking rows for one refresh. */
    private BookingLoadData loadBookingRows(int modeForLoad, int userId, VehicleType typeForLoad) {
        List<ParkingSpace> spaces = loadSpacesForBookingMode(modeForLoad, typeForLoad);
        List<Reservation> userReservations = loadUserReservationsForMode(modeForLoad, userId);
        Set<String> userBookingCodes = collectUserBookingCodes(userReservations);
        List<ParkingSpace> orderedSpaces = orderSpacesForDisplay(spaces, userBookingCodes, userReservations);
        return new BookingLoadData(
                collectLoadedCodes(orderedSpaces),
                buildBookingRows(orderedSpaces, userBookingCodes));
    }

    /** Loads the spaces that should be shown for the current booking mode. */
    private List<ParkingSpace> loadSpacesForBookingMode(int modeForLoad, VehicleType typeForLoad) {
        if (modeForLoad == USER_MODE && typeForLoad != null) {
            return findAvailableSpaces(typeForLoad);
        }
        return loadAllSpaces();
    }

    /** Loads reservations only when the current refresh belongs to a regular user. */
    private List<Reservation> loadUserReservationsForMode(int modeForLoad, int userId) {
        if (modeForLoad == USER_MODE && userId > 0) {
            return getReservationsByUser(userId);
        }
        return new ArrayList<>();
    }

    /** Collects active reservation space codes for the current user. */
    private Set<String> collectUserBookingCodes(List<Reservation> userReservations) {
        Set<String> userBookingCodes = new HashSet<>();
        for (Reservation reservation : userReservations) {
            if (reservation.isActive() && reservation.getParkingSpace() != null) {
                userBookingCodes.add(reservation.getParkingSpace().getId());
            }
        }
        return userBookingCodes;
    }

    /** Orders own reservations first, then every other visible space. */
    private List<ParkingSpace> orderSpacesForDisplay(List<ParkingSpace> spaces, Set<String> userBookingCodes,
                                                     List<Reservation> userReservations) {
        List<ParkingSpace> orderedSpaces = new ArrayList<>();
        addUserReservationSpaces(orderedSpaces, spaces, userReservations);
        addRemainingSpaces(orderedSpaces, spaces, userBookingCodes);
        return orderedSpaces;
    }

    /** Adds the current user's active reservation spaces at the top of the table. */
    private void addUserReservationSpaces(List<ParkingSpace> orderedSpaces, List<ParkingSpace> spaces,
                                          List<Reservation> userReservations) {
        for (Reservation reservation : userReservations) {
            if (reservation.isActive() && reservation.getParkingSpace() != null) {
                ParkingSpace matchingSpace = findSpaceByCode(spaces, reservation.getParkingSpace().getId());
                orderedSpaces.add(matchingSpace != null ? matchingSpace : reservation.getParkingSpace());
            }
        }
    }

    /** Clears booking state when the active user session ends. */
    public void clearSessionState() {
        bookingsLoadId++;
        clearCurrentMode();
        currentUserBookingPlate = null;
        currentUserBookingType = null;
        clearBookingViewSessionState();
    }

    /** Adds every non-user-reservation space after the user's own bookings. */
    private void addRemainingSpaces(List<ParkingSpace> orderedSpaces, List<ParkingSpace> spaces,
                                    Set<String> userBookingCodes) {
        for (ParkingSpace space : spaces) {
            if (!userBookingCodes.contains(space.getId())) {
                orderedSpaces.add(space);
            }
        }
    }

    /** Collects the parking space codes loaded by the worker. */
    private Set<String> collectLoadedCodes(List<ParkingSpace> orderedSpaces) {
        Set<String> loadedCodes = new HashSet<>();
        for (ParkingSpace space : orderedSpaces) {
            loadedCodes.add(space.getId());
        }
        return loadedCodes;
    }

    /** Builds the table row objects published by the worker. */
    private List<BookingRow> buildBookingRows(List<ParkingSpace> orderedSpaces, Set<String> userBookingCodes) {
        List<BookingRow> rows = new ArrayList<>();
        for (ParkingSpace space : orderedSpaces) {
            rows.add(new BookingRow(space, userBookingCodes.contains(space.getId())));
        }
        return rows;
    }

    /** Applies the final state of a booking table refresh. */
    private void finishBookingsLoad(Set<String> loadedCodes, int modeForLoad, int userId) {
        removeBookingSpacesNotIn(loadedCodes);
        closeActiveBookingDialogIfTargetUnavailable();
        closeActiveCancelDialogIfTargetUnavailable();
        if (modeForLoad == USER_MODE && userId > 0) {
            updateReservationsTable(collectActiveReservations(userId));
        }
    }

    /** Gets only active reservations for the current user's reservation tab. */
    private List<Reservation> collectActiveReservations(int userId) {
        List<Reservation> active = new ArrayList<>();
        for (Reservation reservation : getReservationsByUser(userId)) {
            if (reservation.isActive()) {
                active.add(reservation);
            }
        }
        return active;
    }

    /**
     * Stores the vehicle chosen by a regular user before showing compatible spaces.
     *
     * @param plate license plate chosen for the new booking
     * @param type  vehicle type chosen for the new booking
     */
    public void prepareUserBooking(String plate, VehicleType type) {
        currentUserBookingPlate = normalizePlate(plate);
        currentUserBookingType = type;
        setUserBookingVehicle(currentUserBookingPlate, currentUserBookingType);
        showSlotBookingsTab();
        clearBookingsTable();
        loadBookings();
    }

    /**
     * Creates a reservation for the given plate, vehicle type, and space code.
     *
     * @param plate     license plate
     * @param type      vehicle type
     * @param spaceCode target parking space code
     */
    public void createBooking(String plate, VehicleType type, String spaceCode) {
        String bookingPlate = currentMode == USER_MODE && currentUserBookingPlate != null
                ? currentUserBookingPlate
                : normalizePlate(plate);
        VehicleType bookingType = currentMode == USER_MODE && currentUserBookingType != null
                ? currentUserBookingType
                : type;

        if (bookingPlate == null || bookingPlate.isBlank()
                || bookingType == null
                || spaceCode == null || spaceCode.isBlank()) {
            showError("License plate and space code cannot be empty.");
            return;
        }

        setLoading(true);
        new SwingWorker<Reservation, Void>() {
            /** Creates the booking away from the EDT. */
            @Override
            protected Reservation doInBackground() throws Exception {
                return createReservation(
                        getCurrentUserId(),
                        bookingPlate,
                        bookingType,
                        spaceCode);
            }

            /** Updates the booking view after creation finishes. */
            @Override
            protected void done() {
                try {
                    Reservation reservation = get();
                    if (reservation != null) {
                        showInfo("Booking created for space \"" + spaceCode
                                + "\" and plate \"" + bookingPlate + "\".");
                        loadBookings();
                    } else {
                        setLoading(false);
                        showError("Booking could not be created.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    setLoading(false);
                    Throwable cause = e.getCause();
                    showError(cause != null ? cause.getMessage() : "Failed to create booking.");
                }
            }
        }.execute();
    }

    /** Finds a parking space inside a loaded list by code. */
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
            showError("License plate and space code cannot be empty.");
            return;
        }

        setLoading(true);
        new SwingWorker<Reservation, Void>() {
            /** Reassigns the booking away from the EDT. */
            @Override
            protected Reservation doInBackground() throws Exception {
                return reassignReservation(normalizePlate(plate), spaceCode);
            }

            /** Updates the booking view after reassignment finishes. */
            @Override
            protected void done() {
                try {
                    Reservation reservation = get();
                    if (reservation != null) {
                        showInfo("Booking reassigned from space \"" + originalSpaceCode
                                + "\" to space \"" + spaceCode + "\".");
                        loadBookings();
                    } else {
                        setLoading(false);
                        showError("Booking could not be reassigned.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    setLoading(false);
                    Throwable cause = e.getCause();
                    showError(cause != null ? cause.getMessage() : "Failed to reassign booking.");
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
            showError("Cannot cancel a booking without a license plate.");
            return;
        }

        setLoading(true);
        new SwingWorker<Boolean, Void>() {
            /** Cancels the booking away from the EDT. */
            @Override
            protected Boolean doInBackground() throws Exception {
                if (currentMode == USER_MODE) {
                    return cancelReservationByPlateForUser(
                            getCurrentUserId(),
                            normalizePlate(plate));
                }
                return cancelReservationByPlateAsAdmin(normalizePlate(plate));
            }

            /** Updates the booking view after cancellation finishes. */
            @Override
            protected void done() {
                try {
                    boolean cancelled = get();
                    if (cancelled) {
                        if (currentMode == USER_MODE) {
                            showInfo("Your booking for space \"" + spaceCode + "\" has been cancelled.");
                        } else {
                            showInfo("Booking for space \"" + spaceCode
                                    + "\" has been cancelled. The space may still show occupied if a vehicle is parked there.");
                        }
                        loadBookings();
                    } else {
                        setLoading(false);
                        showError("No active booking found for license plate \"" + plate + "\".");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    setLoading(false);
                    Throwable cause = e.getCause();
                    showError(cause != null ? cause.getMessage() : "Failed to cancel booking.");
                }
            }
        }.execute();
    }

    /** Normalizes a license plate entered by the user. */
    private String normalizePlate(String plate) {
        if (plate == null) return "";
        return plate.trim().toUpperCase();
    }

    /** Sets this controller on the booking view. */
    private void setViewController() {
        bookingView.setActions(this);
    }

    /** Connects the booking dialog logout button to this controller. */
    private void setViewLogoutListener() {
        bookingView.addLogoutListener(e -> logoutIfConfirmed());
    }

    /** Logs out from the booking dialog after confirmation. */
    private void logoutIfConfirmed() {
        if (logoutAction != null && bookingView.confirmLogout()) {
            bookingView.dispose();
            logoutAction.run();
        }
    }

    /** Sets the booking view mode. */
    private void setBookingViewMode(int mode) {
        bookingView.setMode(mode);
    }

    /** Sets the selected user booking vehicle on the view. */
    private void setUserBookingVehicle(String plate, VehicleType type) {
        bookingView.setUserBookingVehicle(plate, type);
    }

    /** Clears the bookings table. */
    private void clearBookingsTable() {
        bookingView.clearBookingsTable();
    }

    /** Shows the booking view. */
    private void showBookingView() {
        bookingView.setVisible(true);
    }

    /** Clears user-related data from the booking view. */
    private void clearBookingViewSessionState() {
        bookingView.clearSessionViewState();
    }

    /** Resets the mode so this controller keeps no role from the previous session. */
    private void clearCurrentMode() {
        currentMode = ADMIN_MODE;
    }

    /** Checks whether the booking view is visible. */
    private boolean isBookingViewVisible() {
        return bookingView != null && bookingView.isVisible();
    }

    /** Checks whether the current thread is the EDT. */
    private boolean isEventDispatchThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    /** Runs a task on the EDT. */
    private void runOnEventDispatchThread(Runnable task) {
        SwingUtilities.invokeLater(task);
    }

    /** Sets the booking view loading state. */
    private void setLoading(boolean loading) {
        bookingView.setLoading(loading);
    }

    /** Gets the current logged-in user ID. */
    private int getCurrentUserId() {
        return userService.getLastLoggedInUserId();
    }

    /** Finds available spaces through the reservation service. */
    private List<ParkingSpace> findAvailableSpaces(VehicleType type) {
        return reservationService.getAvailableSpaces(type);
    }

    /** Loads every parking space through the parking service. */
    private List<ParkingSpace> loadAllSpaces() {
        return parkingService.getAllSpaces();
    }

    /** Gets reservations belonging to a user. */
    private List<Reservation> getReservationsByUser(int userId) {
        return reservationService.getReservationsByUser(userId);
    }

    /** Adds one booking row to the view. */
    private void addBookingToTable(ParkingSpace space, boolean userBooking) {
        bookingView.addBookingToTable(space, userBooking);
    }

    /** Removes booking rows not present in the latest load. */
    private void removeBookingSpacesNotIn(Set<String> loadedCodes) {
        bookingView.removeBookingSpacesNotIn(loadedCodes);
    }

    /** Closes a booking dialog if its target is no longer available. */
    private void closeActiveBookingDialogIfTargetUnavailable() {
        bookingView.closeActiveBookingDialogIfTargetUnavailable();
    }

    /** Closes a cancellation dialog if its target is no longer valid. */
    private void closeActiveCancelDialogIfTargetUnavailable() {
        bookingView.closeActiveCancelDialogIfTargetUnavailable();
    }

    /** Updates the reservation table. */
    private void updateReservationsTable(List<Reservation> reservations) {
        bookingView.updateReservationsTable(reservations);
    }

    /** Shows an error in the booking view. */
    private void showError(String message) {
        bookingView.showError(message);
    }

    /** Shows an informational message in the booking view. */
    private void showInfo(String message) {
        bookingView.showInfo(message);
    }

    /** Shows the slot bookings tab. */
    private void showSlotBookingsTab() {
        bookingView.showSlotBookingsTab();
    }

    /** Creates a reservation through the reservation service. */
    private Reservation createReservation(int userId, String plate, VehicleType type, String spaceCode) {
        return reservationService.createReservation(userId, plate, type, spaceCode);
    }

    /** Reassigns a reservation through the reservation service. */
    private Reservation reassignReservation(String plate, String spaceCode) {
        return reservationService.reassignReservation(plate, spaceCode);
    }

    /** Cancels a user reservation through the reservation service. */
    private boolean cancelReservationByPlateForUser(int userId, String plate) {
        return reservationService.cancelReservationByPlateForUser(userId, plate);
    }

    /** Cancels a reservation through the admin service. */
    private boolean cancelReservationByPlateAsAdmin(String plate) {
        return adminService.cancelReservationByPlate(plate);
    }
}
