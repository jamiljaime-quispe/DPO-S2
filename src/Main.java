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
import Presentation.Controllers.ReservationController;
import Presentation.Controllers.StatisticsController;
import Presentation.Controllers.UserService;
import Presentation.Views.AdminParkingManagementView;
import Presentation.Views.LoginView;
import Presentation.Views.MainMenuView;
import Presentation.Views.ParkingSpaceDetailsView;
import Presentation.Views.ParkingStatusView;
import Presentation.Views.ReservationManagementView;
import Presentation.Views.ReservationView;
import Presentation.Views.SignupView;
import java.util.LinkedList;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {

            DatabaseManager db = new DatabaseManager("localhost", 3306, "parking_db", "root", "");
            UserService userService = new UserService(db);

            LoginView loginView = new LoginView();
            SignupView signupView = new SignupView();
            AuthController controller = new AuthController(loginView, userService);
            loginView.authenControllerSetter(controller);
            signupView.setController(controller);
            controller.setSignupView(signupView);
            loginView.initComponents();
            signupView.initComponents();

            MainMenuView mainMenuView = new MainMenuView();
            mainMenuView.initComponents();
            MainController mainController = new MainController(mainMenuView);
            mainMenuView.setController(mainController);
            mainController.setAuthController(controller);

            OccupancyDAOImpl occupancyDAO = new OccupancyDAOImpl(db);
            ParkingSpaceDAOImpl parkingSpaceDAO = new ParkingSpaceDAOImpl(db);
            mainController.setParkingSpaceDAO(parkingSpaceDAO);
            OccupancyTracker tracker = new OccupancyTracker(new LinkedList<>(), 60);
            StatisticsService statsService = new StatisticsService(tracker, parkingSpaceDAO, occupancyDAO);
            StatisticsController statsCtrl = new StatisticsController(mainMenuView.getOccupancyChartView(), statsService);
            mainController.setStatisticsController(statsCtrl);

            VehicleDAOImpl vehicleDAO = new VehicleDAOImpl(db);
            ReservationDAOImpl reservationDAO = new ReservationDAOImpl(db);
            UserDAOImpl userDAO = new UserDAOImpl(db);
            ParkingService parkingService = new ParkingService(parkingSpaceDAO, vehicleDAO, reservationDAO);
            AdminService adminService = new AdminService(parkingService, reservationDAO);
            AdminParkingManagementView adminView = new AdminParkingManagementView(mainMenuView);
            AdminController adminController = new AdminController(adminView, parkingService);
            mainController.setAdminController(adminController);

            Business.Services.ReservationService reservationBusinessService =
                    new Business.Services.ReservationService(reservationDAO, parkingSpaceDAO, vehicleDAO, userDAO);
            controller.setMainMenuController(mainController, mainMenuView, reservationBusinessService);

            ParkingStatusView parkingStatusView = new ParkingStatusView(mainMenuView);
            ParkingSpaceDetailsView parkingSpaceDetailsView = new ParkingSpaceDetailsView(mainMenuView);
            ParkingController parkingController = new ParkingController(parkingStatusView, null, parkingService,
                    adminService, parkingSpaceDetailsView);
            mainController.setParkingController(parkingController);

            ReservationView reservationView = new ReservationView(mainMenuView);
            ReservationManagementView reservationMgmtView = new ReservationManagementView();
            reservationView.initComponents(reservationMgmtView);
            ReservationController reservationController = new ReservationController(reservationView, reservationMgmtView,
                    reservationBusinessService);
            reservationView.setController(reservationController);
            reservationMgmtView.setController(reservationController);

            reservationView.getVehicleTypeCombo().addActionListener(ev -> reservationController
                    .searchAvailableSpaces(reservationView.getVehicleType()));
            reservationView.getReserveButton().addActionListener(ev -> reservationController.reserveSpace(
                    reservationView.getVehiclePlate(),
                    reservationView.getVehicleType(),
                    reservationView.getSelectedSpace()));
            reservationMgmtView.getCancelReservationButton().addActionListener(ev -> reservationController
                    .cancelReservation(reservationMgmtView.getSelectedReservationPlate()));

            mainController.setReservationShell(reservationView.getDialog(), reservationController, userService);
        });
    }
}
