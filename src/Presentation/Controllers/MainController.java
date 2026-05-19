package Presentation.Controllers;

import Presentation.Views.MainMenuView;
import static Presentation.Views.MainMenuView.MODE_ADMIN;
import static Presentation.Views.MainMenuView.MODE_CLIENT;
import javax.swing.JOptionPane;
import Business.Entities.ParkingSpace;
import Persistence.ParkingSpaceDAO;
// WARNING: BE CAREFUL IN NOT BREAKING LAYER ARCHITECTURE

import javax.swing.SwingWorker;
import java.util.List;

public class MainController {
    private MainMenuView view;
    private AuthController authController;
    private StatisticsController statisticsController;
    private ParkingSpaceDAO parkingSpaceDAO;
    private AdminController adminController;
    private ParkingController parkingController;
    private int currentMode;
    private javax.swing.JDialog reservationDialog;
    private Presentation.Controllers.ReservationController reservationController;
    private UserService reservationUserService;

    public MainController(MainMenuView view) {
        this.view = view;
        initListeners();
    }

    private void initListeners() {
        // 1. Manage Slots (admin only)
        view.getEntryExitButton().addActionListener(e -> {
            if (currentMode == MODE_ADMIN && adminController != null) {
                adminController.showView();
            }
        });

        // 2. Manage Bookings (regular users only)
        view.getReservationButton().addActionListener(e -> {
            if (currentMode == MODE_CLIENT) {
                openReservationDialog();
            }
        });

        // 3. Occupancy Chart
        view.getOccupancyChartButton().addActionListener(e -> {
            view.showOccupancyChart();
            if (statisticsController != null)
                statisticsController.startTracking();
        });

        // 4. Current Status (embedded table on main menu only)
        view.getStatusButton().addActionListener(e -> {
            view.clearParkingSlotsTable();
            view.showParkingSlotsTable();
            executeParkingSpace();
        });

        // 5. Logout
        view.getLogoutButton().addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(view,
                    "Are you sure you want to log out?",
                    "Logout",
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

    void executeParkingSpace() {
        view.clearParkingSlotsTable();
        SwingWorker<Void, ParkingSpace> worker = new SwingWorker<Void, ParkingSpace>() {
            @Override
            protected Void doInBackground() {
                List<ParkingSpace> spaces = parkingSpaceDAO.findAll();

                for (ParkingSpace space : spaces) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    publish(space);
                }

                return null;
            }

            @Override
            protected void process(List<ParkingSpace> chunks) {
                for (ParkingSpace space : chunks) {
                    view.addParkingSpaceToTable(space);
                }
            }

            @Override
            protected void done() {
                System.out.println("Parking spaces loaded successfully.");
            }
        };

        worker.execute();
    }

    private void openReservationDialog() {
        if (reservationDialog == null || reservationController == null || reservationUserService == null)
            return;
        reservationController.loadUserReservations(reservationUserService.getLastLoggedInUserId());
        reservationDialog.setVisible(true);
    }

    public void setMode(int mode) {
        this.currentMode = mode;
    }

    public void setAuthController(AuthController authController) {
        this.authController = authController;
    }

    public void setParkingSpaceDAO(ParkingSpaceDAO dao) {
        this.parkingSpaceDAO = dao;
    }

    public void setStatisticsController(StatisticsController statisticsController) {
        this.statisticsController = statisticsController;
    }

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    public void setParkingController(ParkingController parkingController) {
        this.parkingController = parkingController;
        if (parkingController != null) {
            parkingController.mainController = this;
        }
        if (view.getParkingSlotsTable() != null) {
            view.getParkingSlotsTable().getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && currentMode == MODE_ADMIN && parkingController != null) {
                    int row = view.getParkingSlotsTable().getSelectedRow();
                    if (row >= 0) {
                        Object code = view.getParkingSlotsTable().getValueAt(row, 0);
                        if (code != null) {
                            parkingController.showSpaceDetails(code.toString());
                        }
                    }
                }
            });
        }
    }

    /**
     * Wires the reservation dialog shown from the main menu (regular users only).
     */
    public void setReservationShell(javax.swing.JDialog dialog,
                                    Presentation.Controllers.ReservationController reservationController,
                                    UserService userService) {
        this.reservationDialog = dialog;
        this.reservationController = reservationController;
        this.reservationUserService = userService;
    }

    public void resetSession() {
        if (reservationDialog != null) {
            reservationDialog.setVisible(false);
        }
    }
}
