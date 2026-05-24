package Presentation.Controllers;

import Presentation.Views.MainMenuView;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Root controller for the main menu. Routes button clicks to the appropriate sub-controllers (admin,
 * parking, booking, statistics).
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
 */
public class MainController {
    private MainMenuView view;
    private AuthController authController;
    private StatisticsController statisticsController;
    private ParkingController parkingController;
    private AdminController adminController;
    private AdminSlotBookingController slotBookingController;
    private int currentMode;

    /**
     * Constructs the main menu controller.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param view main menu window
     */
    public MainController(MainMenuView view) {
        this.view = view;
        initListeners();
    }

    /**
     * Handles init listeners.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void initListeners() {
        // 1. Manage Slots (admin only)
        addEntryExitListener(e -> openAdminParkingManagement());

        // 2. Manage Bookings
        addReservationListener(e -> openBookingManagement());

        addParkingEntryListener(e -> {
            if (currentMode == 2) {
                handleParkingEntry();
            }
        });

        addParkingExitListener(e -> {
            if (currentMode == 2) {
                handleParkingExit();
            }
        });

        // 3. Occupancy Chart
        addOccupancyChartListener(e -> openOccupancyChart());

        // 4. Current Status
        addStatusListener(e -> openCurrentParkingStatus());

        addParkingSlotsTableMouseListener(new ParkingSlotsClickListener(this));

        java.awt.event.ActionListener backToMain = e -> {
            returnToMainMenu();
        };
        addOccupancyChartBackListener(backToMain);
        addParkingSlotsBackListener(backToMain);

        // 5. Logout
        addLogoutListener(e -> logoutIfConfirmed());

        // 6. Delete Account
        addDeleteAccountListener(e -> deleteAccountIfPossible());
    }

    /**
     * Handles parking entry.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     */
    private void handleParkingEntry() {
        if (hasParkingController()) {
            showVehicleEntryDialog();
        }
    }

    /**
     * Handles parking exit.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     */
    private void handleParkingExit() {
        if (hasParkingController()) {
            showVehicleExitDialog();
        }
    }

    /**
     * Sets the active menu mode: admin or regular user.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param mode mode used by this operation
     */
    public void setMode(int mode) {
        setCurrentMode(mode);
        if (isRegularUserMode() && hasParkingController()) {
            refreshExitButtonState();
        }
    }

    /**
     * Sets the authentication controller used for logout and account deletion.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param authController auth controller that coordinates the related screen action
     */
    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }

    /**
     * Sets the parking controller used by parking buttons.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param parkingController parking controller that coordinates the related screen action
     */
    public void setParkingController(ParkingController parkingController) {
        this.parkingController = parkingController;
        if (isRegularUserMode() && hasParkingController()) {
            refreshExitButtonState();
        }
    }

    /**
     * Sets the statistics controller used by the chart button.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param statisticsController statistics controller that coordinates the related screen action
     */
    public void setStatisticsController(StatisticsController statisticsController) {
        this.statisticsController = statisticsController;
    }

    /**
     * Sets the admin parking controller.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param adminController admin controller that coordinates the related screen action
     */
    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    /**
     * Sets the booking controller.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param slotBookingController slot booking controller that coordinates the related screen action
     */
    public void setSlotBookingController(AdminSlotBookingController slotBookingController) {
        this.slotBookingController = slotBookingController;
    }

    /**
     * Clears every controller and view that may still hold data from the logged-out user.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    public void clearSessionState() {
        clearCurrentMode();
        clearStatisticsSessionState();
        clearParkingSessionState();
        clearAdminSessionState();
        clearBookingSessionState();
        clearMainViewSessionState();
    }

    /**
     * Adds entry exit listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addEntryExitListener(ActionListener listener) {
        view.addEntryExitListener(listener);
    }

    /**
     * Adds reservation listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addReservationListener(ActionListener listener) {
        view.addReservationListener(listener);
    }

    /**
     * Adds parking entry listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addParkingEntryListener(ActionListener listener) {
        view.addParkingEntryListener(listener);
    }

    /**
     * Adds parking exit listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addParkingExitListener(ActionListener listener) {
        view.addParkingExitListener(listener);
    }

    /**
     * Adds occupancy chart listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addOccupancyChartListener(ActionListener listener) {
        view.addOccupancyChartListener(listener);
    }

    /**
     * Adds status listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addStatusListener(ActionListener listener) {
        view.addStatusListener(listener);
    }

    /**
     * Adds occupancy chart back listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addOccupancyChartBackListener(ActionListener listener) {
        view.addOccupancyChartBackListener(listener);
    }

    /**
     * Adds logout listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addLogoutListener(ActionListener listener) {
        view.addLogoutListener(listener);
    }

    /**
     * Adds delete account listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addDeleteAccountListener(ActionListener listener) {
        view.addDeleteAccountListener(listener);
    }

    /**
     * Adds parking slots table mouse listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addParkingSlotsTableMouseListener(MouseListener listener) {
        view.addParkingSlotsTableMouseListener(listener);
    }

    /**
     * Adds parking slots back listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action that will run when the related event happens
     */
    private void addParkingSlotsBackListener(ActionListener listener) {
        view.addParkingSlotsBackListener(listener);
    }

    /**
     * Handles open admin parking management.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void openAdminParkingManagement() {
        if (isAdminMode() && hasAdminController()) {
            showAdminParkingManagementView();
        }
    }

    /**
     * Handles open booking management.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void openBookingManagement() {
        if (hasSlotBookingController()) {
            showSlotBookingView();
        }
    }

    /**
     * Handles open occupancy chart.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void openOccupancyChart() {
        showOccupancyChartView();
        if (hasStatisticsController()) {
            startStatisticsTracking();
        }
    }

    /**
     * Handles open current parking status.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void openCurrentParkingStatus() {
        if (hasParkingController()) {
            rebuildParkingSlotsPanel();
            loadParkingStatus();
        }
    }

    /**
     * Handles open space details if admin clicked.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param e e used by this operation
     */
    private void openSpaceDetailsIfAdminClicked(MouseEvent e) {
        if (!isAdminMode() || !hasParkingController()) return;

        String code = getParkingSpaceCodeAtPoint(e);
        if (code != null) {
            showSpaceDetails(code);
        }
    }

    /**
     * Handles parking slots table click.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     *
     * @param event mouse click event
     */
    void handleParkingSlotsTableClick(MouseEvent event) {
        openSpaceDetailsIfAdminClicked(event);
    }

    /**
     * Handles return to main menu.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void returnToMainMenu() {
        resetDisplayedContent();
        if (hasStatisticsController()) {
            stopStatisticsTracking();
        }
    }

    /**
     * Handles logout if confirmed.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void logoutIfConfirmed() {
        if (isLogoutConfirmed() && hasAuthController()) {
            logout();
        }
    }

    /**
     * Deletes account if possible.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void deleteAccountIfPossible() {
        if (hasAuthController()) {
            handleDeleteAccount();
        }
    }

    /**
     * Sets the current mode.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param mode mode used by this operation
     */
    private void setCurrentMode(int mode) {
        currentMode = mode;
    }

    /**
     * Checks whether regular user mode.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean isRegularUserMode() {
        return currentMode == 2;
    }

    /**
     * Checks whether admin mode.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean isAdminMode() {
        return currentMode == 1;
    }

    /**
     * Checks whether parking controller exists.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean hasParkingController() {
        return parkingController != null;
    }

    /**
     * Checks whether admin controller exists.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean hasAdminController() {
        return adminController != null;
    }

    /**
     * Checks whether slot booking controller exists.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean hasSlotBookingController() {
        return slotBookingController != null;
    }

    /**
     * Checks whether statistics controller exists.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean hasStatisticsController() {
        return statisticsController != null;
    }

    /**
     * Checks whether auth controller exists.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean hasAuthController() {
        return authController != null;
    }

    /**
     * Shows the admin parking management view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showAdminParkingManagementView() {
        adminController.showView();
    }

    /**
     * Shows the slot booking view with the current mode.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showSlotBookingView() {
        slotBookingController.showView(currentMode);
    }

    /**
     * Shows the occupancy chart view.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showOccupancyChartView() {
        view.showOccupancyChart();
    }

    /**
     * Handles start statistics tracking.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void startStatisticsTracking() {
        statisticsController.startTracking();
    }

    /**
     * Handles rebuild parking slots panel.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void rebuildParkingSlotsPanel() {
        view.rebuildParkingSlotsPanel();
    }

    /**
     * Loads parking status.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     */
    private void loadParkingStatus() {
        parkingController.loadParkingStatus();
    }

    /**
     * Gets the parking space code at the clicked point.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @param e e used by this operation
     * @return the current parking space code at point
     */
    private String getParkingSpaceCodeAtPoint(MouseEvent e) {
        return view.getParkingSpaceCodeAtPoint(e.getPoint());
    }

    /**
     * Shows space details.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     *
     * @param code parking space code involved in the operation
     */
    private void showSpaceDetails(String code) {
        parkingController.showSpaceDetails(code);
    }

    /**
     * Handles reset displayed content.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void resetDisplayedContent() {
        view.resetDisplayedContent();
    }

    /**
     * Handles stop statistics tracking.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void stopStatisticsTracking() {
        statisticsController.stopTracking();
    }

    /**
     * Checks whether logout was confirmed in the view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean isLogoutConfirmed() {
        return view.confirmLogout();
    }

    /**
     * Handles logout.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void logout() {
        authController.logout();
    }

    /**
     * Handles delete account.
     * <p>
     * This method is called from a user action, gathers what the screen needs, and passes the real work to
     * the service layer.
     * </p>
     */
    private void handleDeleteAccount() {
        authController.handleDeleteAccount();
    }

    /**
     * Opens the parking entry dialog through the parking controller.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showVehicleEntryDialog() {
        parkingController.showVehicleEntryDialog();
    }

    /**
     * Opens the parking exit dialog through the parking controller.
     * <p>
     * This method prepares the information needed for a dialog and lets the view handle the actual Swing
     * display.
     * </p>
     */
    private void showVehicleExitDialog() {
        parkingController.showVehicleExitDialog();
    }

    /**
     * Handles refresh exit button state.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     */
    private void refreshExitButtonState() {
        parkingController.refreshExitButtonState();
    }

    /**
     * Handles clear statistics session state.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearStatisticsSessionState() {
        if (statisticsController != null) {
            statisticsController.clearSessionState();
        }
    }

    /**
     * Clears parking status, exit-button state, and details dialogs.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearParkingSessionState() {
        if (parkingController != null) {
            parkingController.clearSessionState();
        }
    }

    /**
     * Clears admin parking management tables and dialogs.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearAdminSessionState() {
        if (adminController != null) {
            adminController.clearSessionState();
        }
    }

    /**
     * Clears booking tables, selected plates, and booking dialogs.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearBookingSessionState() {
        if (slotBookingController != null) {
            slotBookingController.clearSessionState();
        }
    }

    /**
     * Handles clear main view session state.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearMainViewSessionState() {
        view.clearSessionViewState();
    }

    /**
     * Handles clear current mode.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void clearCurrentMode() {
        currentMode = 0;
    }
}
