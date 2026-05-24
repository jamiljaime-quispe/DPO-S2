package Presentation.Controllers;

import Presentation.Views.MainMenuView;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Root controller for the main menu.
 * Routes button clicks to the appropriate sub-controllers (admin, parking, booking, statistics).
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
     *
     * @param view main menu window
     */
    public MainController(MainMenuView view) {
        this.view = view;
        initListeners();
    }

    /** Wires the main menu buttons to their controllers. */
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

    /** Opens the regular-user parking entry flow. */
    private void handleParkingEntry() {
        if (hasParkingController()) {
            showVehicleEntryDialog();
        }
    }

    /** Opens the regular-user parking exit flow. */
    private void handleParkingExit() {
        if (hasParkingController()) {
            showVehicleExitDialog();
        }
    }

    /** Sets the active menu mode: admin or regular user. */
    public void setMode(int mode) {
        setCurrentMode(mode);
        if (isRegularUserMode() && hasParkingController()) {
            refreshExitButtonState();
        }
    }

    /** Sets the authentication controller used for logout and account deletion. */
    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }

    /** Sets the parking controller used by parking buttons. */
    public void setParkingController(ParkingController parkingController) {
        this.parkingController = parkingController;
        if (isRegularUserMode() && hasParkingController()) {
            refreshExitButtonState();
        }
    }

    /** Sets the statistics controller used by the chart button. */
    public void setStatisticsController(StatisticsController statisticsController) {
        this.statisticsController = statisticsController;
    }

    /** Sets the admin parking controller. */
    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    /** Sets the booking controller. */
    public void setSlotBookingController(AdminSlotBookingController slotBookingController) {
        this.slotBookingController = slotBookingController;
    }

    /** Clears every controller and view that may still hold data from the logged-out user. */
    public void clearSessionState() {
        clearCurrentMode();
        clearStatisticsSessionState();
        clearParkingSessionState();
        clearAdminSessionState();
        clearBookingSessionState();
        clearMainViewSessionState();
    }

    /** Adds a listener to the admin parking management action. */
    private void addEntryExitListener(ActionListener listener) {
        view.addEntryExitListener(listener);
    }

    /** Adds a listener to the booking management action. */
    private void addReservationListener(ActionListener listener) {
        view.addReservationListener(listener);
    }

    /** Adds a listener to the parking entry action. */
    private void addParkingEntryListener(ActionListener listener) {
        view.addParkingEntryListener(listener);
    }

    /** Adds a listener to the parking exit action. */
    private void addParkingExitListener(ActionListener listener) {
        view.addParkingExitListener(listener);
    }

    /** Adds a listener to the occupancy chart action. */
    private void addOccupancyChartListener(ActionListener listener) {
        view.addOccupancyChartListener(listener);
    }

    /** Adds a listener to the current parking status action. */
    private void addStatusListener(ActionListener listener) {
        view.addStatusListener(listener);
    }

    /** Adds a listener to the occupancy chart back action. */
    private void addOccupancyChartBackListener(ActionListener listener) {
        view.addOccupancyChartBackListener(listener);
    }

    /** Adds a listener to the logout action. */
    private void addLogoutListener(ActionListener listener) {
        view.addLogoutListener(listener);
    }

    /** Adds a listener to the delete-account action. */
    private void addDeleteAccountListener(ActionListener listener) {
        view.addDeleteAccountListener(listener);
    }

    /** Adds a mouse listener to the parking slots table. */
    private void addParkingSlotsTableMouseListener(MouseListener listener) {
        view.addParkingSlotsTableMouseListener(listener);
    }

    /** Adds a listener to the parking slots back action. */
    private void addParkingSlotsBackListener(ActionListener listener) {
        view.addParkingSlotsBackListener(listener);
    }

    /** Opens admin parking management when the current mode is admin. */
    private void openAdminParkingManagement() {
        if (isAdminMode() && hasAdminController()) {
            showAdminParkingManagementView();
        }
    }

    /** Opens booking management for the current mode. */
    private void openBookingManagement() {
        if (hasSlotBookingController()) {
            showSlotBookingView();
        }
    }

    /** Opens the occupancy chart and starts tracking. */
    private void openOccupancyChart() {
        showOccupancyChartView();
        if (hasStatisticsController()) {
            startStatisticsTracking();
        }
    }

    /** Opens the current parking status table. */
    private void openCurrentParkingStatus() {
        if (hasParkingController()) {
            rebuildParkingSlotsPanel();
            loadParkingStatus();
        }
    }

    /** Opens details for a clicked space when the current mode is admin. */
    private void openSpaceDetailsIfAdminClicked(MouseEvent e) {
        if (!isAdminMode() || !hasParkingController()) return;

        String code = getParkingSpaceCodeAtPoint(e);
        if (code != null) {
            showSpaceDetails(code);
        }
    }

    /**
     * Handles a click on the parking status table.
     *
     * @param event mouse click event
     */
    void handleParkingSlotsTableClick(MouseEvent event) {
        openSpaceDetailsIfAdminClicked(event);
    }

    /** Restores the main menu and stops chart tracking. */
    private void returnToMainMenu() {
        resetDisplayedContent();
        if (hasStatisticsController()) {
            stopStatisticsTracking();
        }
    }

    /** Logs the user out when they confirm. */
    private void logoutIfConfirmed() {
        if (isLogoutConfirmed() && hasAuthController()) {
            logout();
        }
    }

    /** Starts account deletion when the auth controller exists. */
    private void deleteAccountIfPossible() {
        if (hasAuthController()) {
            handleDeleteAccount();
        }
    }

    /** Saves the current mode in memory. */
    private void setCurrentMode(int mode) {
        currentMode = mode;
    }

    /** Checks whether the menu is in regular-user mode. */
    private boolean isRegularUserMode() {
        return currentMode == 2;
    }

    /** Checks whether the menu is in admin mode. */
    private boolean isAdminMode() {
        return currentMode == 1;
    }

    /** Checks whether the parking controller exists. */
    private boolean hasParkingController() {
        return parkingController != null;
    }

    /** Checks whether the admin controller exists. */
    private boolean hasAdminController() {
        return adminController != null;
    }

    /** Checks whether the slot booking controller exists. */
    private boolean hasSlotBookingController() {
        return slotBookingController != null;
    }

    /** Checks whether the statistics controller exists. */
    private boolean hasStatisticsController() {
        return statisticsController != null;
    }

    /** Checks whether the authentication controller exists. */
    private boolean hasAuthController() {
        return authController != null;
    }

    /** Shows the admin parking management view. */
    private void showAdminParkingManagementView() {
        adminController.showView();
    }

    /** Shows the slot booking view with the current mode. */
    private void showSlotBookingView() {
        slotBookingController.showView(currentMode);
    }

    /** Shows the occupancy chart view. */
    private void showOccupancyChartView() {
        view.showOccupancyChart();
    }

    /** Starts chart tracking through the statistics controller. */
    private void startStatisticsTracking() {
        statisticsController.startTracking();
    }

    /** Rebuilds the current parking status panel. */
    private void rebuildParkingSlotsPanel() {
        view.rebuildParkingSlotsPanel();
    }

    /** Loads parking status through the parking controller. */
    private void loadParkingStatus() {
        parkingController.loadParkingStatus();
    }

    /** Gets the parking space code at the clicked point. */
    private String getParkingSpaceCodeAtPoint(MouseEvent e) {
        return view.getParkingSpaceCodeAtPoint(e.getPoint());
    }

    /** Shows parking-space details through the parking controller. */
    private void showSpaceDetails(String code) {
        parkingController.showSpaceDetails(code);
    }

    /** Resets the main menu content. */
    private void resetDisplayedContent() {
        view.resetDisplayedContent();
    }

    /** Stops chart tracking through the statistics controller. */
    private void stopStatisticsTracking() {
        statisticsController.stopTracking();
    }

    /** Checks whether logout was confirmed in the view. */
    private boolean isLogoutConfirmed() {
        return view.confirmLogout();
    }

    /** Logs out through the authentication controller. */
    private void logout() {
        authController.logout();
    }

    /** Starts account deletion through the authentication controller. */
    private void handleDeleteAccount() {
        authController.handleDeleteAccount();
    }

    /** Opens the parking entry dialog through the parking controller. */
    private void showVehicleEntryDialog() {
        parkingController.showVehicleEntryDialog();
    }

    /** Opens the parking exit dialog through the parking controller. */
    private void showVehicleExitDialog() {
        parkingController.showVehicleExitDialog();
    }

    /** Refreshes the exit button through the parking controller. */
    private void refreshExitButtonState() {
        parkingController.refreshExitButtonState();
    }

    /** Clears chart timers and chart-related session state. */
    private void clearStatisticsSessionState() {
        if (statisticsController != null) {
            statisticsController.clearSessionState();
        }
    }

    /** Clears parking status, exit-button state, and details dialogs. */
    private void clearParkingSessionState() {
        if (parkingController != null) {
            parkingController.clearSessionState();
        }
    }

    /** Clears admin parking management tables and dialogs. */
    private void clearAdminSessionState() {
        if (adminController != null) {
            adminController.clearSessionState();
        }
    }

    /** Clears booking tables, selected plates, and booking dialogs. */
    private void clearBookingSessionState() {
        if (slotBookingController != null) {
            slotBookingController.clearSessionState();
        }
    }

    /** Clears the main menu text and table state. */
    private void clearMainViewSessionState() {
        view.clearSessionViewState();
    }

    /** Resets the menu mode so the controller keeps no role from the previous session. */
    private void clearCurrentMode() {
        currentMode = 0;
    }
}
