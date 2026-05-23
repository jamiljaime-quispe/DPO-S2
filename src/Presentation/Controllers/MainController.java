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
        view.getEntryExitButton().addActionListener(e -> {
            if (currentMode == 1 && adminController != null) {
                adminController.showView();
            }
        });

        // 2. Manage Bookings
        view.getReservationButton().addActionListener(e -> {
            if (slotBookingController != null) {
                slotBookingController.showView(currentMode);
            }
        });

        view.getParkingEntryButton().addActionListener(e -> {
            if (currentMode == 2) {
                handleParkingEntry();
            }
        });

        view.getParkingExitButton().addActionListener(e -> {
            if (currentMode == 2) {
                handleParkingExit();
            }
        });

        // 3. Occupancy Chart
        view.getOccupancyChartButton().addActionListener(e -> {
            view.showOccupancyChart();
            if (statisticsController != null)
                statisticsController.startTracking();
        });

        // 4. Current Status
        view.getStatusButton().addActionListener(e -> {
            if (parkingController != null) {
                view.rebuildParkingSlotsPanel();
                parkingController.loadParkingStatus();
            }
        });

        view.addParkingSlotsTableMouseListener(new MouseAdapter() {
            /** Opens admin details when an admin clicks a parking row. */
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMode != 1 || parkingController == null) return;

                String code = view.getParkingSpaceCodeAtPoint(e.getPoint());
                if (code != null) {
                    parkingController.showSpaceDetails(code);
                }
            }
        });

        java.awt.event.ActionListener backToMain = e -> {
            view.resetDisplayedContent();
            if (statisticsController != null) {
                statisticsController.stopTracking();
            }
        };
        view.getBackToMenuButton().addActionListener(backToMain);
        view.addParkingSlotsBackListener(backToMain);

        // 5. Logout
        view.getLogoutButton().addActionListener(e -> {
            if (view.confirmLogout()) {
                if (authController != null) {
                    authController.logout();
                }
            }
        });

        // 6. Delete Account
        view.getDeleteAccountButton().addActionListener(e -> {
            if (authController != null) {
                authController.handleDeleteAccount();
            }
        });
    }

    /** Opens the regular-user parking entry flow. */
    private void handleParkingEntry() {
        if (parkingController != null) {
            parkingController.showVehicleEntryDialog();
        }
    }

    /** Opens the regular-user parking exit flow. */
    private void handleParkingExit() {
        if (parkingController != null) {
            parkingController.showVehicleExitDialog();
        }
    }

    /** Sets the active menu mode: admin or regular user. */
    public void setMode(int mode) {
        this.currentMode = mode;
        if (currentMode == 2 && parkingController != null) {
            parkingController.refreshExitButtonState();
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
            parkingController.refreshExitButtonState();
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
}
