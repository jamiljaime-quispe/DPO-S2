package Presentation.Controllers;

import Presentation.Views.MainMenuView;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
// WARNING: BE CAREFUL IN NOT BREAKING LAYER ARCHITECTURE

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
        getEntryExitButton().addActionListener(e -> openAdminParkingManagement());

        // 2. Manage Bookings
        getReservationButton().addActionListener(e -> openBookingManagement());

        getParkingEntryButton().addActionListener(e -> {
            if (currentMode == 2) {
                handleParkingEntry();
            }
        });

        getParkingExitButton().addActionListener(e -> {
            if (currentMode == 2) {
                handleParkingExit();
            }
        });

        // 3. Occupancy Chart
        getOccupancyChartButton().addActionListener(e -> openOccupancyChart());

        // 4. Current Status
        getStatusButton().addActionListener(e -> openCurrentParkingStatus());

        addParkingSlotsTableMouseListener(new MouseAdapter() {
            /** Opens admin details when an admin clicks a parking row. */
            @Override
            public void mouseClicked(MouseEvent e) {
                openSpaceDetailsIfAdminClicked(e);
            }
        });

        java.awt.event.ActionListener backToMain = e -> {
            returnToMainMenu();
        };
        getBackToMenuButton().addActionListener(backToMain);
        addParkingSlotsBackListener(backToMain);

        // 5. Logout
        getLogoutButton().addActionListener(e -> logoutIfConfirmed());

        // 6. Delete Account
        getDeleteAccountButton().addActionListener(e -> deleteAccountIfPossible());
    }

    /** Opens the regular-user parking entry flow. */
    private void handleParkingEntry() {
        if (parkingController != null) {
            showVehicleEntryDialog();
        }
    }

    /** Opens the regular-user parking exit flow. */
    private void handleParkingExit() {
        if (parkingController != null) {
            showVehicleExitDialog();
        }
    }

    /** Sets the active menu mode: admin or regular user. */
    public void setMode(int mode) {
        this.currentMode = mode;
        if (currentMode == 2 && parkingController != null) {
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
        if (currentMode == 2 && parkingController != null) {
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

    /** Gets the manage-slots button from the view. */
    private javax.swing.JButton getEntryExitButton() {
        return view.getEntryExitButton();
    }

    /** Gets the booking-management button from the view. */
    private javax.swing.JButton getReservationButton() {
        return view.getReservationButton();
    }

    /** Gets the parking-entry button from the view. */
    private javax.swing.JButton getParkingEntryButton() {
        return view.getParkingEntryButton();
    }

    /** Gets the parking-exit button from the view. */
    private javax.swing.JButton getParkingExitButton() {
        return view.getParkingExitButton();
    }

    /** Gets the occupancy-chart button from the view. */
    private javax.swing.JButton getOccupancyChartButton() {
        return view.getOccupancyChartButton();
    }

    /** Gets the current-status button from the view. */
    private javax.swing.JButton getStatusButton() {
        return view.getStatusButton();
    }

    /** Gets the back-to-menu button from the view. */
    private javax.swing.JButton getBackToMenuButton() {
        return view.getBackToMenuButton();
    }

    /** Gets the logout button from the view. */
    private javax.swing.JButton getLogoutButton() {
        return view.getLogoutButton();
    }

    /** Gets the delete-account button from the view. */
    private javax.swing.JButton getDeleteAccountButton() {
        return view.getDeleteAccountButton();
    }

    /** Adds a mouse listener to the parking slots table. */
    private void addParkingSlotsTableMouseListener(MouseAdapter listener) {
        view.addParkingSlotsTableMouseListener(listener);
    }

    /** Adds a listener to the parking slots back action. */
    private void addParkingSlotsBackListener(java.awt.event.ActionListener listener) {
        view.addParkingSlotsBackListener(listener);
    }

    /** Opens admin parking management when the current mode is admin. */
    private void openAdminParkingManagement() {
        if (currentMode == 1 && adminController != null) {
            adminController.showView();
        }
    }

    /** Opens booking management for the current mode. */
    private void openBookingManagement() {
        if (slotBookingController != null) {
            slotBookingController.showView(currentMode);
        }
    }

    /** Opens the occupancy chart and starts tracking. */
    private void openOccupancyChart() {
        view.showOccupancyChart();
        if (statisticsController != null) {
            statisticsController.startTracking();
        }
    }

    /** Opens the current parking status table. */
    private void openCurrentParkingStatus() {
        if (parkingController != null) {
            view.rebuildParkingSlotsPanel();
            parkingController.loadParkingStatus();
        }
    }

    /** Opens details for a clicked space when the current mode is admin. */
    private void openSpaceDetailsIfAdminClicked(MouseEvent e) {
        if (currentMode != 1 || parkingController == null) return;

        String code = view.getParkingSpaceCodeAtPoint(e.getPoint());
        if (code != null) {
            parkingController.showSpaceDetails(code);
        }
    }

    /** Restores the main menu and stops chart tracking. */
    private void returnToMainMenu() {
        view.resetDisplayedContent();
        if (statisticsController != null) {
            statisticsController.stopTracking();
        }
    }

    /** Logs the user out when they confirm. */
    private void logoutIfConfirmed() {
        if (view.confirmLogout() && authController != null) {
            authController.logout();
        }
    }

    /** Starts account deletion when the auth controller exists. */
    private void deleteAccountIfPossible() {
        if (authController != null) {
            authController.handleDeleteAccount();
        }
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
}
