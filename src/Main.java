import Business.Entities.Config;
import Business.Entities.OccupancyTracker;
import Business.Services.AdminService;
import Business.Services.ConfigService;
import Business.Services.ParkingService;
import Business.Services.ReservationService;
import Business.Services.SimulationService;
import Business.Services.StatisticsService;
import Business.Services.UserService;
import Persistence.DatabaseManager;
import Persistence.IMPL.OccupancyDAOImpl;
import Persistence.IMPL.ParkingSpaceDAOImpl;
import Persistence.IMPL.ReservationDAOImpl;
import Persistence.IMPL.UserDAOImpl;
import Persistence.IMPL.VehicleDAOImpl;
import Presentation.Controllers.AdminController;
import Presentation.Controllers.AdminSlotBookingController;
import Presentation.Controllers.AuthController;
import Presentation.Controllers.MainController;
import Presentation.Controllers.ParkingController;
import Presentation.Controllers.StatisticsController;
import Presentation.Views.AdminParkingManagementView;
import Presentation.Views.AdminSlotBookingManagementView;
import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.SignupView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class    Main {
    public static void main(String[] args) {
        // Load config before entering the EDT (reads config.json from project root)
        ConfigService configService = new ConfigService(new Config());

        javax.swing.SwingUtilities.invokeLater(() -> {

            // 1. Database
            DatabaseManager db = new DatabaseManager(
                    configService.getConfig().getDbIP(),
                    configService.getConfig().getDbPort(),
                    configService.getConfig().getDbName(),
                    configService.getConfig().getDbUser(),
                    configService.getConfig().getDbPassword());

            // 2. DAOs
            UserDAOImpl userDAO                     = new UserDAOImpl(db);
            VehicleDAOImpl vehicleDAO               = new VehicleDAOImpl(db);
            ParkingSpaceDAOImpl parkingSpaceDAO      = new ParkingSpaceDAOImpl(db);
            ReservationDAOImpl reservationDAO        = new ReservationDAOImpl(db);
            OccupancyDAOImpl occupancyDAO            = new OccupancyDAOImpl(db);

            // 3. Services
            UserService userService                  = new UserService(userDAO, vehicleDAO);
            ParkingService parkingService            = new ParkingService(parkingSpaceDAO, vehicleDAO, reservationDAO);
            ReservationService reservationService    = new ReservationService(reservationDAO, parkingSpaceDAO, vehicleDAO);
            AdminService adminService                = new AdminService(parkingService, reservationDAO);
            OccupancyTracker tracker                 = new OccupancyTracker(new LinkedList<>(), 60);
            StatisticsService statsService           = new StatisticsService(tracker, parkingSpaceDAO, occupancyDAO);
            // The bot needs ParkingService to change parking status, Config to know the delay,
            // Random to make decisions, and a list to remember simulated parked vehicles.
            SimulationService simService             = new SimulationService(parkingService, configService.getConfig(), new Random(), new ArrayList<>());

            // 4. Views
            LoginView loginView         = new LoginView();
            SignupView signupView       = new SignupView();
            MainMenuView mainMenuView   = new MainMenuView();

            // 5. Auth Controller
            AuthController authController = new AuthController(loginView, userService);
            loginView.authenControllerSetter(authController);
            signupView.setController(authController);
            authController.setSignupView(signupView);
            authController.setConfigService(configService);
            authController.setReservationService(reservationService);

            // 6. Init view layouts (LoginView.initComponents calls setVisible(true))
            loginView.initComponents();
            signupView.initComponents();
            mainMenuView.initComponents();

            // 7. Main Menu
            MainController mainController = new MainController(mainMenuView);
            mainMenuView.setController(mainController);
            authController.setMainMenuController(mainController, mainMenuView);
            mainController.setAuthController(authController);

            // 8. Statistics
            StatisticsController statsCtrl = new StatisticsController(mainMenuView.getOccupancyChartView(), statsService);
            mainController.setStatisticsController(statsCtrl);

            // 9. Parking
            ParkingController parkingController = new ParkingController(null, null, parkingService);
            parkingController.setMainMenuView(mainMenuView);
            parkingController.setUserService(userService);
            parkingController.setAdminService(adminService);
            mainController.setParkingController(parkingController);
            simService.setParkingStatusChangeListener(parkingController);

            // 10. Admin parking management
            AdminParkingManagementView adminView    = new AdminParkingManagementView(mainMenuView);
            AdminController adminController         = new AdminController(adminView, parkingService);
            adminController.setAdminService(adminService);
            parkingController.setAdminController(adminController);
            mainController.setAdminController(adminController);

            // 11. Slot booking management
            AdminSlotBookingManagementView bookingView = new AdminSlotBookingManagementView(mainMenuView);
            AdminSlotBookingController bookingController = new AdminSlotBookingController(
                    bookingView, parkingService, adminService, reservationService, userService);
            parkingController.setSlotBookingController(bookingController);
            mainController.setSlotBookingController(bookingController);

            // 12. Simulation — startSimulation() sets running=true and stores thread ref for clean interrupt
            // This starts the independent background thread that repeatedly runs SimulationService.run().
            simService.startSimulation();
            mainMenuView.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    // This asks the background simulation thread to stop when the main window closes.
                    simService.stopSimulation();
                }
            });
        });
    }
}
