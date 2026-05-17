package Presentation.Controllers;

import Presentation.Views.MainMenuView;
import javax.swing.JOptionPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
// WARNING: BE CAREFUL IN NOT BREAKING LAYER ARCHITECTURE

public class MainController {
    private MainMenuView view;
    private AuthController authController;
    private StatisticsController statisticsController;
    private ParkingController parkingController;
    private AdminController adminController;
    private AdminSlotBookingController slotBookingController;
    private int currentMode;

    public MainController(MainMenuView view) {
        this.view = view;
        initListeners();
    }

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
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMode != 1 || parkingController == null) return;

                String code = view.getParkingSpaceCodeAtPoint(e.getPoint());
                if (code != null) {
                    parkingController.showSpaceDetails(code);
                }
            }
        });

        // 5. Logout
        view.getLogoutButton().addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(view,
                    "Are you sure you want to log out?",
                    "Log out",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
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

    private void handleParkingEntry() {
        if (parkingController != null) {
            parkingController.showVehicleEntryDialog();
        }
    }

    private void handleParkingExit() {
        if (parkingController != null) {
            parkingController.showVehicleExitDialog();
        }
    }

    public void setMode(int mode) {
        this.currentMode = mode;
        if (currentMode == 2 && parkingController != null) {
            parkingController.refreshExitButtonState();
        }
    }

    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }

    public void setParkingController(ParkingController parkingController) {
        this.parkingController = parkingController;
        if (currentMode == 2 && parkingController != null) {
            parkingController.refreshExitButtonState();
        }
    }

    public void setStatisticsController(StatisticsController statisticsController) {
        this.statisticsController = statisticsController;
    }

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    public void setSlotBookingController(AdminSlotBookingController slotBookingController) {
        this.slotBookingController = slotBookingController;
    }
}
