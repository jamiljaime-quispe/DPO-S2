import Persistence.DatabaseManager;
import Persistence.IMPL.OccupancyDAOImpl;
import Persistence.IMPL.ParkingSpaceDAOImpl;
import Persistence.IMPL.ReservationDAOImpl;
import Persistence.IMPL.VehicleDAOImpl;
import Business.Entities.OccupancyTracker;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.ReservationService;
import Business.Services.StatisticsService;
import Presentation.Controllers.AdminController;
import Presentation.Controllers.AuthController;
import Presentation.Controllers.MainController;
import Presentation.Controllers.ParkingController;
import Presentation.Controllers.StatisticsController;
import Presentation.Controllers.UserService;
import Presentation.Controllers.AdminSlotBookingController;
import Presentation.Views.AdminParkingManagementView;
import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.AdminSlotBookingManagementView;
import Presentation.Views.SignupView;
import java.util.LinkedList;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {

            // 1. Initialize core services
            DatabaseManager db = new DatabaseManager("localhost", 3306, "parking_db", "root", "");
            UserService userService = new UserService(db);

            // 2. Initialize Views
            LoginView loginView = new LoginView();
            SignupView signupView = new SignupView();

            // 3. Initialize Auth Controller
            AuthController controller = new AuthController(loginView, userService);

            // 4. Wire the Auth Controller to both the Login and Signup Views
            loginView.authenControllerSetter(controller);
            signupView.setController(controller);

            controller.setSignupView(signupView);

            // 5. Initialize View Layouts
            loginView.initComponents();
            signupView.initComponents();

            // 6. Main Menu Logic
            MainMenuView mainMenuView = new MainMenuView();
            mainMenuView.initComponents();

            MainController mainController = new MainController(mainMenuView);
            mainMenuView.setController(mainController);

            // 7. Link Auth Controller to Main Menu
            controller.setMainMenuController(mainController, mainMenuView);

            // 8. Give the MainController access to the AuthController
            mainController.setAuthController(controller);

            // 9. Wire up the occupancy chart
            OccupancyDAOImpl occupancyDAO       = new OccupancyDAOImpl(db);
            ParkingSpaceDAOImpl parkingSpaceDAO = new ParkingSpaceDAOImpl(db);
            OccupancyTracker tracker            = new OccupancyTracker(new LinkedList<>(), 60);
            StatisticsService statsService      = new StatisticsService(tracker, parkingSpaceDAO, occupancyDAO);
            StatisticsController statsCtrl      = new StatisticsController(mainMenuView.getOccupancyChartView(), statsService);
            mainController.setStatisticsController(statsCtrl);

            // 10. Wire up the admin parking management
            VehicleDAOImpl vehicleDAO           = new VehicleDAOImpl(db);
            ReservationDAOImpl reservationDAO   = new ReservationDAOImpl(db);
            ParkingService parkingService       = new ParkingService(parkingSpaceDAO, vehicleDAO, reservationDAO);
            ParkingController parkingController = new ParkingController(null, null, parkingService);
            parkingController.setMainMenuView(mainMenuView);
            parkingController.setUserService(userService);
            mainController.setParkingController(parkingController);

            AdminParkingManagementView adminView = new AdminParkingManagementView(mainMenuView);
            AdminController adminController     = new AdminController(adminView, parkingService);
            mainController.setAdminController(adminController);

            // 11. Wire up the admin slot booking management
            AdminService adminService = new AdminService(parkingService, reservationDAO);
            parkingController.setAdminService(adminService);
            ReservationService reservationService = new ReservationService(reservationDAO, parkingSpaceDAO, vehicleDAO);
            AdminSlotBookingManagementView bookingView = new AdminSlotBookingManagementView(mainMenuView);
            AdminSlotBookingController bookingController = new AdminSlotBookingController(
                    bookingView,
                    parkingService,
                    adminService,
                    reservationService,
                    userService);
            mainController.setSlotBookingController(bookingController);
        });
    }
}
