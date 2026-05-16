import Business.Entities.OccupancyTracker;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.StatisticsService;
import Persistence.DatabaseManager;
import Persistence.IMPL.OccupancyDAOImpl;
import Persistence.IMPL.ParkingSpaceDAOImpl;
import Persistence.IMPL.ReservationDAOImpl;
import Persistence.IMPL.UserDAOImpl;
import Persistence.IMPL.VehicleDAOImpl;
import Presentation.Controllers.AdminController;
import Presentation.Controllers.AuthController;
import Presentation.Controllers.MainController;
import Presentation.Controllers.ParkingController;
import Presentation.Controllers.StatisticsController;
import Presentation.Controllers.UserService;
import Presentation.Views.ParkingSpaceDetailsView;
import Presentation.Views.ParkingStatusView;
import Presentation.Views.AdminParkingManagementView;
import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.SignupView;
import java.util.LinkedList;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {

            // 1. Initialize core services
            DatabaseManager db = new DatabaseManager("localhost", 3306, "parking_db", "root", "");
            UserService userService = new UserService(db);

            // 2. Initialize Views
            LoginView loginView = new LoginView();            SignupView signupView = new SignupView();

            // 3. Initialize Auth Controller
            AuthController controller = new AuthController(loginView, userService);

            // 4. Wire the Auth Controller to both the Login and Signup Views
            loginView.authenControllerSetter(controller);
            signupView.setController(controller);

            controller.setSignupView(signupView);

            // 5. Initialize View Layouts
            loginView.initComponents();            signupView.initComponents();

            // 6. Main Menu Logic
            MainMenuView mainMenuView = new MainMenuView();
            mainMenuView.initComponents();

            MainController mainController = new MainController(mainMenuView);
            mainMenuView.setController(mainController);

            // 7. Link Auth Controller to Main Menu (reservationService wired below for login notifications)

            // 8. Give the MainController access to the AuthController
            mainController.setAuthController(controller);

            // 9. Wire up the occupancy chart
            OccupancyDAOImpl occupancyDAO       = new OccupancyDAOImpl(db);
            ParkingSpaceDAOImpl parkingSpaceDAO = new ParkingSpaceDAOImpl(db);
            mainController.setParkingSpaceDAO(parkingSpaceDAO);
            OccupancyTracker tracker            = new OccupancyTracker(new LinkedList<>(), 60);
            StatisticsService statsService      = new StatisticsService(tracker, parkingSpaceDAO, occupancyDAO);
            StatisticsController statsCtrl      = new StatisticsController(mainMenuView.getOccupancyChartView(), statsService);
            mainController.setStatisticsController(statsCtrl);

            // 10. Wire up the admin parking management
            VehicleDAOImpl vehicleDAO           = new VehicleDAOImpl(db);
            ReservationDAOImpl reservationDAO   = new ReservationDAOImpl(db);
            UserDAOImpl userDAO                 = new UserDAOImpl(db);
            ParkingService parkingService       = new ParkingService(parkingSpaceDAO, vehicleDAO, reservationDAO);
            AdminService adminService           = new AdminService(parkingService, reservationDAO);
            AdminParkingManagementView adminView = new AdminParkingManagementView(mainMenuView);
            AdminController adminController     = new AdminController(adminView, parkingService);
            mainController.setAdminController(adminController);

            Business.Services.ReservationService reservationBusinessService =
                    new Business.Services.ReservationService(reservationDAO, parkingSpaceDAO, vehicleDAO, userDAO);

            controller.setMainMenuController(mainController, mainMenuView, reservationBusinessService);

            javax.swing.JDialog statusDialog = new javax.swing.JDialog(mainMenuView, "Parking status", false);
            String[] statusCols = { "Code", "Floor", "Status", "Reservation", "License plate" };
            javax.swing.table.DefaultTableModel statusTableModel = new javax.swing.table.DefaultTableModel(statusCols, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            javax.swing.JTable statusTable = new javax.swing.JTable(statusTableModel);
            javax.swing.JButton statusRefreshBtn = new javax.swing.JButton("Refresh");
            javax.swing.JButton statusBackBtn = new javax.swing.JButton("Close");
            javax.swing.JPanel statusPanel = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
            statusPanel.add(new javax.swing.JScrollPane(statusTable), java.awt.BorderLayout.CENTER);
            javax.swing.JPanel statusSouth = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
            statusSouth.add(statusRefreshBtn);
            statusSouth.add(statusBackBtn);
            statusPanel.add(statusSouth, java.awt.BorderLayout.SOUTH);
            statusDialog.setContentPane(statusPanel);
            statusDialog.setSize(620, 400);
            statusDialog.setLocationRelativeTo(mainMenuView);
            ParkingStatusView parkingStatusView = new ParkingStatusView(statusDialog, statusTable,
                    statusRefreshBtn, statusBackBtn, null);

            javax.swing.JDialog spaceDetailsDialog = new javax.swing.JDialog(mainMenuView, "Parking space details", false);
            javax.swing.JPanel detailsPanel = new javax.swing.JPanel();
            detailsPanel.setLayout(new java.awt.GridLayout(0, 2, 8, 8));
            detailsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
            javax.swing.JLabel codeLabel = new javax.swing.JLabel();
            javax.swing.JLabel floorLabel = new javax.swing.JLabel();
            javax.swing.JLabel typeLabel = new javax.swing.JLabel();
            javax.swing.JLabel reservedUserLabel = new javax.swing.JLabel();
            detailsPanel.add(new javax.swing.JLabel("Code:"));
            detailsPanel.add(codeLabel);
            detailsPanel.add(new javax.swing.JLabel("Floor:"));
            detailsPanel.add(floorLabel);
            detailsPanel.add(new javax.swing.JLabel("Vehicle type:"));
            detailsPanel.add(typeLabel);
            detailsPanel.add(new javax.swing.JLabel("Reserved by:"));
            detailsPanel.add(reservedUserLabel);
            javax.swing.JButton cancelResBtn = new javax.swing.JButton("Cancel reservation");
            javax.swing.JButton detailsBackBtn = new javax.swing.JButton("Back");
            javax.swing.JPanel detailsButtons = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
            detailsButtons.add(cancelResBtn);
            detailsButtons.add(detailsBackBtn);
            javax.swing.JPanel detailsRoot = new javax.swing.JPanel(new java.awt.BorderLayout());
            detailsRoot.add(detailsPanel, java.awt.BorderLayout.CENTER);
            detailsRoot.add(detailsButtons, java.awt.BorderLayout.SOUTH);
            spaceDetailsDialog.setContentPane(detailsRoot);
            spaceDetailsDialog.setSize(420, 220);
            spaceDetailsDialog.setLocationRelativeTo(mainMenuView);
            ParkingSpaceDetailsView parkingSpaceDetailsView = new ParkingSpaceDetailsView(spaceDetailsDialog,
                    codeLabel, floorLabel, typeLabel, reservedUserLabel, cancelResBtn, detailsBackBtn, null);

            ParkingController parkingController = new ParkingController(parkingStatusView, null, parkingService,
                    adminService, parkingSpaceDetailsView);
            mainController.setParkingController(parkingController);

            javax.swing.JTextField resPlateField = new javax.swing.JTextField(12);
            javax.swing.JComboBox<Business.Entities.VehicleType> resTypeCombo =
                    new javax.swing.JComboBox<>(Business.Entities.VehicleType.values());
            javax.swing.DefaultListModel<String> resListModel = new javax.swing.DefaultListModel<>();
            javax.swing.JList<String> resSpacesList = new javax.swing.JList<>(resListModel);
            javax.swing.JButton resReserveBtn = new javax.swing.JButton("Reserve");
            javax.swing.JButton resBookCloseBtn = new javax.swing.JButton("Close");
            Presentation.Views.ReservationView reservationView =
                    new Presentation.Views.ReservationView(mainMenuView, resPlateField, resTypeCombo, resSpacesList,
                            resReserveBtn, resBookCloseBtn, null);

            String[] resCols = { "License plate", "Space type", "Reserved on" };
            javax.swing.table.DefaultTableModel resTableModel = new javax.swing.table.DefaultTableModel(resCols, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            javax.swing.JTable resTable = new javax.swing.JTable(resTableModel);
            javax.swing.JButton resCancelBtn = new javax.swing.JButton("Cancel selected");
            javax.swing.JButton resMgmtCloseBtn = new javax.swing.JButton("Close");
            Presentation.Views.ReservationManagementView reservationMgmtView =
                    new Presentation.Views.ReservationManagementView(mainMenuView, resTable, resCancelBtn,
                            resMgmtCloseBtn, null);

            Presentation.Controllers.ReservationController reservationController =
                    new Presentation.Controllers.ReservationController(reservationView, reservationMgmtView,
                            reservationBusinessService);
            reservationView.setController(reservationController);
            reservationMgmtView.setController(reservationController);

            resTypeCombo.addActionListener(ev -> reservationController
                    .searchAvailableSpaces((Business.Entities.VehicleType) resTypeCombo.getSelectedItem()));
            resReserveBtn.addActionListener(ev -> reservationController.reserveSpace(reservationView.getVehiclePlate(),
                    reservationView.getVehicleType(), reservationView.getSelectedSpace()));
            resCancelBtn.addActionListener(ev -> reservationController
                    .cancelReservation(reservationMgmtView.getSelectedReservationPlate()));

            javax.swing.JTabbedPane reservationTabs = new javax.swing.JTabbedPane();
            javax.swing.JPanel bookPanel = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
            javax.swing.JPanel bookNorth = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            bookNorth.add(new javax.swing.JLabel("License plate:"));
            bookNorth.add(resPlateField);
            bookNorth.add(new javax.swing.JLabel("Vehicle type:"));
            bookNorth.add(resTypeCombo);
            bookPanel.add(bookNorth, java.awt.BorderLayout.NORTH);
            bookPanel.add(new javax.swing.JScrollPane(resSpacesList), java.awt.BorderLayout.CENTER);
            javax.swing.JPanel bookSouth = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
            bookSouth.add(resReserveBtn);
            bookSouth.add(resBookCloseBtn);
            bookPanel.add(bookSouth, java.awt.BorderLayout.SOUTH);
            reservationTabs.addTab("Book space", bookPanel);

            javax.swing.JPanel mgmtPanel = new javax.swing.JPanel(new java.awt.BorderLayout(8, 8));
            mgmtPanel.add(new javax.swing.JScrollPane(resTable), java.awt.BorderLayout.CENTER);
            javax.swing.JPanel mgmtSouth = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
            mgmtSouth.add(resCancelBtn);
            mgmtSouth.add(resMgmtCloseBtn);
            mgmtPanel.add(mgmtSouth, java.awt.BorderLayout.SOUTH);
            reservationTabs.addTab("My reservations", mgmtPanel);

            javax.swing.JDialog reservationDialog = new javax.swing.JDialog(mainMenuView, "Reservations", false);
            reservationDialog.setContentPane(reservationTabs);
            reservationDialog.setSize(560, 420);
            reservationDialog.setLocationRelativeTo(mainMenuView);

            resBookCloseBtn.addActionListener(ev -> reservationDialog.setVisible(false));
            resMgmtCloseBtn.addActionListener(ev -> reservationDialog.setVisible(false));

            mainController.setReservationShell(reservationDialog, reservationController, userService);
        });
    }
}
