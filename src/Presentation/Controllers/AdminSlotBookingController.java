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
 * Controller for the slot booking management view. Supports both admin mode (manage all bookings) and user
 * mode (manage own bookings).
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
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
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param bookingView the slot booking management view
     * @param parkingService the parking service
     * @param adminService the admin service
     * @param reservationService the reservation service
     * @param userService the user service
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

    /**
     * Sets the action used when the user logs out from this dialog.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param logoutAction logout action used by this operation
     */
    public void setLogoutAction(Runnable logoutAction) {
        this.logoutAction = logoutAction;
    }

    /**
     * Configures the view for the given mode and makes it visible.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
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
     * Reloads the view if it is currently visible. Safe to call from any thread.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
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
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     */
    public void loadBookings() {
        bookingsLoadId++;
        int loadId = bookingsLoadId;
        setLoading(true);

        int modeForLoad = currentMode;
        int userId = getCurrentUserId();
        VehicleType typeForLoad = currentUserBookingType;

        new SwingWorker<Set<String>, BookingRow>() {
            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the set of values found for the operation
             */
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

            /**
             * Applies worker updates on the Swing screen thread.
             * <p>
             * This receives the values published by the worker on the Swing screen thread, which makes it
             * safe to add rows or refresh visible components little by little.
             * </p>
             *
             * @param chunks chunks used by this operation
             */
            @Override
            protected void process(List<BookingRow> chunks) {
                if (loadId != bookingsLoadId) return;

                for (BookingRow row : chunks) {
                    addBookingToTable(row.getSpace(), row.isUserBooking());
                }
            }

            /**
             * Finishes the worker task on the Swing screen thread.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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

    /**
     * Loads booking rows.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     *
     * @param modeForLoad mode for load used by this operation
     * @param userId identifier of the user involved in the operation
     * @param typeForLoad type for load used by this operation
     * @return the loaded booking rows
     */
    private BookingLoadData loadBookingRows(int modeForLoad, int userId, VehicleType typeForLoad) {
        List<ParkingSpace> spaces = loadSpacesForBookingMode(modeForLoad, typeForLoad);
        List<Reservation> userReservations = loadUserReservationsForMode(modeForLoad, userId);
        Set<String> userBookingCodes = collectUserBookingCodes(userReservations);
        List<ParkingSpace> orderedSpaces = orderSpacesForDisplay(spaces, userBookingCodes, userReservations);
        return new BookingLoadData(
                collectLoadedCodes(orderedSpaces),
                buildBookingRows(orderedSpaces, userBookingCodes));
    }

    /**
     * Loads spaces for booking mode.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     *
     * @param modeForLoad mode for load used by this operation
     * @param typeForLoad type for load used by this operation
     * @return the loaded spaces for booking mode
     */
    private List<ParkingSpace> loadSpacesForBookingMode(int modeForLoad, VehicleType typeForLoad) {
        if (modeForLoad == USER_MODE && typeForLoad != null) {
            return findAvailableSpaces(typeForLoad);
        }
        return loadAllSpaces();
    }

    /**
     * Loads user reservations for mode.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     *
     * @param modeForLoad mode for load used by this operation
     * @param userId identifier of the user involved in the operation
     * @return the loaded user reservations for mode
     */
    private List<Reservation> loadUserReservationsForMode(int modeForLoad, int userId) {
        if (modeForLoad == USER_MODE && userId > 0) {
            return getReservationsByUser(userId);
        }
        return new ArrayList<>();
    }

    /**
     * Handles collect user booking codes.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param userReservations user reservations used by this operation
     * @return the set of values found for the operation
     */
    private Set<String> collectUserBookingCodes(List<Reservation> userReservations) {
        Set<String> userBookingCodes = new HashSet<>();
        for (Reservation reservation : userReservations) {
            if (reservation.isActive() && reservation.getParkingSpace() != null) {
                userBookingCodes.add(reservation.getParkingSpace().getId());
            }
        }
        return userBookingCodes;
    }

    /**
     * Handles order spaces for display.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param spaces spaces used by this operation
     * @param userBookingCodes user booking codes used by this operation
     * @param userReservations user reservations used by this operation
     * @return the list of values found for the operation
     */
    private List<ParkingSpace> orderSpacesForDisplay(List<ParkingSpace> spaces, Set<String> userBookingCodes,
                                                     List<Reservation> userReservations) {
        List<ParkingSpace> orderedSpaces = new ArrayList<>();
        addUserReservationSpaces(orderedSpaces, spaces, userReservations);
        addRemainingSpaces(orderedSpaces, spaces, userBookingCodes);
        return orderedSpaces;
    }

    /**
     * Adds user reservation spaces.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param orderedSpaces ordered spaces used by this operation
     * @param spaces spaces used by this operation
     * @param userReservations user reservations used by this operation
     */
    private void addUserReservationSpaces(List<ParkingSpace> orderedSpaces, List<ParkingSpace> spaces,
                                          List<Reservation> userReservations) {
        for (Reservation reservation : userReservations) {
            if (reservation.isActive() && reservation.getParkingSpace() != null) {
                ParkingSpace matchingSpace = findSpaceByCode(spaces, reservation.getParkingSpace().getId());
                orderedSpaces.add(matchingSpace != null ? matchingSpace : reservation.getParkingSpace());
            }
        }
    }

    /**
     * Handles clear session state.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    public void clearSessionState() {
        bookingsLoadId++;
        clearCurrentMode();
        currentUserBookingPlate = null;
        currentUserBookingType = null;
        clearBookingViewSessionState();
    }

    /**
     * Adds remaining spaces.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param orderedSpaces ordered spaces used by this operation
     * @param spaces spaces used by this operation
     * @param userBookingCodes user booking codes used by this operation
     */
    private void addRemainingSpaces(List<ParkingSpace> orderedSpaces, List<ParkingSpace> spaces,
                                    Set<String> userBookingCodes) {
        for (ParkingSpace space : spaces) {
            if (!userBookingCodes.contains(space.getId())) {
                orderedSpaces.add(space);
            }
        }
    }

    /**
     * Collects the parking space codes loaded by the worker.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param orderedSpaces ordered spaces used by this operation
     * @return the set of values found for the operation
     */
    private Set<String> collectLoadedCodes(List<ParkingSpace> orderedSpaces) {
        Set<String> loadedCodes = new HashSet<>();
        for (ParkingSpace space : orderedSpaces) {
            loadedCodes.add(space.getId());
        }
        return loadedCodes;
    }

    /**
     * Builds the table row objects published by the worker.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param orderedSpaces ordered spaces used by this operation
     * @param userBookingCodes user booking codes used by this operation
     * @return the built booking rows
     */
    private List<BookingRow> buildBookingRows(List<ParkingSpace> orderedSpaces, Set<String> userBookingCodes) {
        List<BookingRow> rows = new ArrayList<>();
        for (ParkingSpace space : orderedSpaces) {
            rows.add(new BookingRow(space, userBookingCodes.contains(space.getId())));
        }
        return rows;
    }

    /**
     * Handles finish bookings load.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param loadedCodes loaded codes used by this operation
     * @param modeForLoad mode for load used by this operation
     * @param userId identifier of the user involved in the operation
     */
    private void finishBookingsLoad(Set<String> loadedCodes, int modeForLoad, int userId) {
        removeBookingSpacesNotIn(loadedCodes);
        closeActiveBookingDialogIfTargetUnavailable();
        closeActiveCancelDialogIfTargetUnavailable();
        if (modeForLoad == USER_MODE && userId > 0) {
            updateReservationsTable(collectActiveReservations(userId));
        }
    }

    /**
     * Gets only active reservations for the current user's reservation tab.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param userId identifier of the user involved in the operation
     * @return the list of values found for the operation
     */
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
     * Handles prepare user booking.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param plate license plate chosen for the new booking
     * @param type vehicle type chosen for the new booking
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
     * Creates booking.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param plate license plate
     * @param type vehicle type
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
            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the result of the operation
             * @throws Exception if the operation cannot be completed correctly
             */
            @Override
            protected Reservation doInBackground() throws Exception {
                return createReservation(
                        getCurrentUserId(),
                        bookingPlate,
                        bookingType,
                        spaceCode);
            }

            /**
             * Updates the booking view after creation finishes.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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

    /**
     * Finds space by code.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param spaces spaces used by this operation
     * @param code parking space code involved in the operation
     * @return the matching space by code, or null when it is not found
     */
    private ParkingSpace findSpaceByCode(List<ParkingSpace> spaces, String code) {
        for (ParkingSpace space : spaces) {
            if (space.getId().equals(code)) {
                return space;
            }
        }
        return null;
    }

    /**
     * Handles edit booking.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param originalSpaceCode the original space code (for feedback messages)
     * @param plate the license plate on the reservation
     * @param type the vehicle type
     * @param spaceCode the target space code
     */
    public void editBooking(String originalSpaceCode, String plate, VehicleType type, String spaceCode) {
        if (plate == null || plate.isBlank() || spaceCode == null || spaceCode.isBlank()) {
            showError("License plate and space code cannot be empty.");
            return;
        }

        setLoading(true);
        new SwingWorker<Reservation, Void>() {
            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the result of the operation
             * @throws Exception if the operation cannot be completed correctly
             */
            @Override
            protected Reservation doInBackground() throws Exception {
                return reassignReservation(normalizePlate(plate), spaceCode);
            }

            /**
             * Updates the booking view after reassignment finishes.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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
     * Cancels the reservation for the given space and plate. In USER_MODE only the current user's own
     * reservation is cancelled.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param spaceCode the space code (for feedback messages)
     * @param plate the license plate on the reservation
     */
    public void deleteBooking(String spaceCode, String plate) {
        if (plate == null || plate.isBlank()) {
            showError("Cannot cancel a booking without a license plate.");
            return;
        }

        setLoading(true);
        new SwingWorker<Boolean, Void>() {
            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the result of the operation
             * @throws Exception if the operation cannot be completed correctly
             */
            @Override
            protected Boolean doInBackground() throws Exception {
                if (currentMode == USER_MODE) {
                    return cancelReservationByPlateForUser(
                            getCurrentUserId(),
                            normalizePlate(plate));
                }
                return cancelReservationByPlateAsAdmin(normalizePlate(plate));
            }

            /**
             * Updates the booking view after cancellation finishes.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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

    /**
     * Handles normalize plate.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param plate license plate involved in the operation
     * @return the result of the operation
     */
    private String normalizePlate(String plate) {
        if (plate == null) return "";
        return plate.trim().toUpperCase();
    }

    /**
     * Sets this controller on the booking view.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     */
    private void setViewController() {
        bookingView.setActions(this);
    }

    /**
     * Connects the booking dialog logout button to this controller.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     */
    private void setViewLogoutListener() {
        bookingView.addLogoutListener(e -> logoutIfConfirmed());
    }

    /**
     * Logs out from the booking dialog after confirmation.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void logoutIfConfirmed() {
        if (logoutAction != null && bookingView.confirmLogout()) {
            bookingView.dispose();
            logoutAction.run();
        }
    }

    /**
     * Sets the booking view mode.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param mode mode used by this operation
     */
    private void setBookingViewMode(int mode) {
        bookingView.setMode(mode);
    }

    /**
     * Sets the selected user booking vehicle on the view.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param plate license plate involved in the operation
     * @param type vehicle type involved in the operation
     */
    private void setUserBookingVehicle(String plate, VehicleType type) {
        bookingView.setUserBookingVehicle(plate, type);
    }

    /**
     * Handles clear bookings table.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearBookingsTable() {
        bookingView.clearBookingsTable();
    }

    /**
     * Shows the booking view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showBookingView() {
        bookingView.setVisible(true);
    }

    /**
     * Clears user-related data from the booking view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearBookingViewSessionState() {
        bookingView.clearSessionViewState();
    }

    /**
     * Handles clear current mode.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearCurrentMode() {
        currentMode = ADMIN_MODE;
    }

    /**
     * Checks whether the booking view is visible.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean isBookingViewVisible() {
        return bookingView != null && bookingView.isVisible();
    }

    /**
     * Checks whether event dispatch thread.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean isEventDispatchThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    /**
     * Handles run on event dispatch thread.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param task task used by this operation
     */
    private void runOnEventDispatchThread(Runnable task) {
        SwingUtilities.invokeLater(task);
    }

    /**
     * Sets the booking view loading state.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param loading true while the screen is waiting for an operation to finish
     */
    private void setLoading(boolean loading) {
        bookingView.setLoading(loading);
    }

    /**
     * Gets the current logged-in user ID.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current current user ID
     */
    private int getCurrentUserId() {
        return userService.getLastLoggedInUserId();
    }

    /**
     * Finds available spaces.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param type vehicle type involved in the operation
     * @return the matching available spaces, or null when it is not found
     */
    private List<ParkingSpace> findAvailableSpaces(VehicleType type) {
        return reservationService.getAvailableSpaces(type);
    }

    /**
     * Loads all spaces.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     *
     * @return the loaded all spaces
     */
    private List<ParkingSpace> loadAllSpaces() {
        return parkingService.getAllSpaces();
    }

    /**
     * Gets reservations belonging to a user.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @param userId identifier of the user involved in the operation
     * @return the current reservations by user
     */
    private List<Reservation> getReservationsByUser(int userId) {
        return reservationService.getReservationsByUser(userId);
    }

    /**
     * Adds one booking row to the view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param space space used by this operation
     * @param userBooking user booking used by this operation
     */
    private void addBookingToTable(ParkingSpace space, boolean userBooking) {
        bookingView.addBookingToTable(space, userBooking);
    }

    /**
     * Handles remove booking spaces not in.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param loadedCodes loaded codes used by this operation
     */
    private void removeBookingSpacesNotIn(Set<String> loadedCodes) {
        bookingView.removeBookingSpacesNotIn(loadedCodes);
    }

    /**
     * Closes a booking dialog if its target is no longer available.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void closeActiveBookingDialogIfTargetUnavailable() {
        bookingView.closeActiveBookingDialogIfTargetUnavailable();
    }

    /**
     * Closes a cancellation dialog if its target is no longer valid.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void closeActiveCancelDialogIfTargetUnavailable() {
        bookingView.closeActiveCancelDialogIfTargetUnavailable();
    }

    /**
     * Updates reservations table.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param reservations reservations used by this operation
     */
    private void updateReservationsTable(List<Reservation> reservations) {
        bookingView.updateReservationsTable(reservations);
    }

    /**
     * Shows an error in the booking view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     *
     * @param message message shown to the user or written to the log
     */
    private void showError(String message) {
        bookingView.showError(message);
    }

    /**
     * Shows an informational message in the booking view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     *
     * @param message message shown to the user or written to the log
     */
    private void showInfo(String message) {
        bookingView.showInfo(message);
    }

    /**
     * Shows slot bookings tab.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showSlotBookingsTab() {
        bookingView.showSlotBookingsTab();
    }

    /**
     * Creates reservation.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param userId identifier of the user involved in the operation
     * @param plate license plate involved in the operation
     * @param type vehicle type involved in the operation
     * @param spaceCode parking space code involved in the operation
     * @return the created reservation
     */
    private Reservation createReservation(int userId, String plate, VehicleType type, String spaceCode) {
        return reservationService.createReservation(userId, plate, type, spaceCode);
    }

    /**
     * Handles reassign reservation.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param plate license plate involved in the operation
     * @param spaceCode parking space code involved in the operation
     * @return the result of the operation
     */
    private Reservation reassignReservation(String plate, String spaceCode) {
        return reservationService.reassignReservation(plate, spaceCode);
    }

    /**
     * Handles cancel reservation by plate for user.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param userId identifier of the user involved in the operation
     * @param plate license plate involved in the operation
     * @return true when the condition is met, false otherwise
     */
    private boolean cancelReservationByPlateForUser(int userId, String plate) {
        return reservationService.cancelReservationByPlateForUser(userId, plate);
    }

    /**
     * Handles cancel reservation by plate as admin.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param plate license plate involved in the operation
     * @return true when the condition is met, false otherwise
     */
    private boolean cancelReservationByPlateAsAdmin(String plate) {
        return adminService.cancelReservationByPlate(plate);
    }
}
