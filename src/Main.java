import Business.Entities.Config;
import Business.Entities.OccupancyTracker;
import Business.Services.AdminService;
import Business.Services.ConfigService;
import Business.Services.ParkingService;
import Business.Services.ReservationService;
import Business.Services.SimulationService;
import Business.Services.StatisticsService;
import Business.Services.UserService;
import Persistence.ConfigDAO;
import Persistence.DatabaseManager;
import Persistence.IMPL.ConfigDAOImpl;
import Persistence.IMPL.OccupancyDAOImpl;
import Persistence.IMPL.ParkingSpaceDAOImpl;
import Persistence.IMPL.ReservationDAOImpl;
import Persistence.IMPL.UserDAOImpl;
import Persistence.IMPL.VehicleDAOImpl;
import Persistence.OccupancyDAO;
import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.UserDAO;
import Persistence.VehicleDAO;
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
import Presentation.Views.OccupancyChartView;
import Presentation.Views.SignupView;
import Presentation.Views.WindowClosingAction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts the parking application and wires the views, controllers, services, and DAOs.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int OCCUPANCY_TRACKER_CAPACITY = 60;
    private static final int OCCUPANCY_RECORD_INTERVAL_MS = 60_000;

    /**
     * Application entry point.
     *
     * @param args command-line arguments, not used
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> startApplication());
    }

    /** Builds and starts the application. */
    private static void startApplication() {
        try {
            ConfigService configService = createConfigService();
            DatabaseManager db = createDatabaseManager(configService);
            ApplicationDaos daos = createDaos(db);
            ApplicationServices services = createServices(configService, db, daos);
            ApplicationViews views = createViews();

            AuthController authController = createAuthController(views, services, configService);
            initializeViews(views);
            wireAuthenticationActions(views, authController);

            MainController mainController = createMainController(views, authController);
            StatisticsController statisticsController = createStatisticsController(views, services);
            ParkingController parkingController = createParkingController(views, services, mainController,
                    statisticsController);
            parkingController.setLogoutAction(createLogoutAction(authController));

            wireAdminParking(views, services, mainController, parkingController, authController);
            wireSlotBooking(views, services, mainController, parkingController, authController);
            startSimulation(views, services, parkingController);
        } catch (RuntimeException e) {
            LoginView.showStartupError("Failed to start the application:\n" + e.getMessage());
            System.exit(1);
        }
    }

    /** Creates the configuration service. */
    private static ConfigService createConfigService() {
        ConfigDAO configDAO = new ConfigDAOImpl();
        return new ConfigService(configDAO);
    }

    /** Creates the shared database manager. */
    private static DatabaseManager createDatabaseManager(ConfigService configService) {
        return new DatabaseManager(
                getDbIP(configService),
                getDbPort(configService),
                getDbName(configService),
                getDbUser(configService),
                getDbPassword(configService));
    }

    /** Creates all DAO implementations used by the services. */
    private static ApplicationDaos createDaos(DatabaseManager db) {
        ApplicationDaos daos = new ApplicationDaos();
        setUserDAO(daos, createUserDAO(db));
        setVehicleDAO(daos, createVehicleDAO(db));
        setParkingSpaceDAO(daos, createParkingSpaceDAO(db));
        setReservationDAO(daos, createReservationDAO(db));
        setOccupancyDAO(daos, createOccupancyDAO(db));
        return daos;
    }

    /** Creates all business services. */
    private static ApplicationServices createServices(ConfigService configService, DatabaseManager db,
                                                       ApplicationDaos daos) {
        ApplicationServices services = new ApplicationServices();
        setUserService(services, createUserService(daos, db));
        setParkingService(services, createParkingService(daos, db));
        setReservationService(services, createReservationService(daos, db));
        setAdminService(services, createAdminService(services, daos, db));
        setStatisticsService(services, createStatisticsService(daos));
        setSimulationService(services, createSimulationService(configService, daos, getParkingService(services)));
        return services;
    }

    /** Creates the statistics service. */
    private static StatisticsService createStatisticsService(ApplicationDaos daos) {
        OccupancyTracker tracker = new OccupancyTracker(new LinkedList<>(), OCCUPANCY_TRACKER_CAPACITY);
        return new StatisticsService(tracker, getParkingSpaceDAO(daos), getOccupancyDAO(daos));
    }

    /** Creates the simulation service. */
    private static SimulationService createSimulationService(ConfigService configService, ApplicationDaos daos,
                                                             ParkingService parkingService) {
        return new SimulationService(parkingService, getConfig(configService), new Random(), new ArrayList<>(),
                getVehicleDAO(daos));
    }

    /** Creates the main application views. */
    private static ApplicationViews createViews() {
        ApplicationViews views = new ApplicationViews();
        setLoginView(views, createLoginView());
        setSignupView(views, createSignupView());
        setOccupancyChartView(views, createOccupancyChartView());
        setMainMenuView(views, createMainMenuView(views));
        return views;
    }

    /** Creates and connects the authentication controller. */
    private static AuthController createAuthController(ApplicationViews views, ApplicationServices services,
                                                       ConfigService configService) {
        AuthController authController = new AuthController(getLoginView(views), getUserService(services));
        setSignupView(authController, views);
        setConfigService(authController, configService);
        setReservationService(authController, services);
        return authController;
    }

    /** Connects login and signup view actions to the authentication controller. */
    private static void wireAuthenticationActions(ApplicationViews views, AuthController authController) {
        addLoginAction(views, authController);
        addSignupNavigationAction(views, authController);
        addRegistrationAction(views, authController);
        addBackToLoginAction(views, authController);
    }

    /** Initializes view components. */
    private static void initializeViews(ApplicationViews views) {
        initializeLoginView(views);
        initializeSignupView(views);
        initializeMainMenuView(views);
    }

    /** Creates and connects the main menu controller. */
    private static MainController createMainController(ApplicationViews views, AuthController authController) {
        MainController mainController = new MainController(getMainMenuView(views));
        setMainMenuController(authController, mainController, views);
        setAuthController(mainController, authController);
        return mainController;
    }

    /** Creates the statistics controller and its periodic recorder. */
    private static StatisticsController createStatisticsController(ApplicationViews views,
                                                                   ApplicationServices services) {
        StatisticsController statisticsController = new StatisticsController(getOccupancyChartView(views),
                getStatisticsService(services));
        startOccupancyRecorder(getStatisticsService(services));
        return statisticsController;
    }

    /** Starts the timer that records occupancy snapshots. */
    private static void startOccupancyRecorder(StatisticsService statisticsService) {
        javax.swing.Timer occupancyRecorder = new javax.swing.Timer(OCCUPANCY_RECORD_INTERVAL_MS,
                event -> createOccupancyRecorderWorker(statisticsService).execute());
        occupancyRecorder.setInitialDelay(0);
        occupancyRecorder.start();
    }

    /** Creates the worker used to save one occupancy snapshot. */
    private static javax.swing.SwingWorker<Void, Void> createOccupancyRecorderWorker(
            StatisticsService statisticsService) {
        return new javax.swing.SwingWorker<>() {
            /** Saves the occupancy snapshot outside the EDT. */
            @Override
            protected Void doInBackground() {
                recordOccupancy(statisticsService);
                return null;
            }

            /** Reports snapshot errors after the worker finishes. */
            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Failed to record occupancy snapshot.", e);
                }
            }
        };
    }

    /** Creates and connects the parking controller. */
    private static ParkingController createParkingController(ApplicationViews views, ApplicationServices services,
                                                            MainController mainController,
                                                            StatisticsController statisticsController) {
        ParkingController parkingController = new ParkingController(getParkingService(services));
        setMainMenuView(parkingController, views);
        setUserService(parkingController, services);
        setAdminService(parkingController, services);
        setStatisticsController(parkingController, statisticsController);
        setStatisticsController(mainController, statisticsController);
        setParkingController(mainController, parkingController);
        return parkingController;
    }

    /** Creates and connects admin parking management. */
    private static void wireAdminParking(ApplicationViews views, ApplicationServices services,
                                         MainController mainController,
                                         ParkingController parkingController, AuthController authController) {
        AdminParkingManagementView adminView = createAdminParkingManagementView(views);
        AdminController adminController = createAdminController(adminView, services);
        setAdminService(adminController, services);
        setLogoutAction(adminController, authController);
        setParkingStatusDisplayAction(adminController, parkingController);
        setAdminController(parkingController, adminController);
        setAdminController(mainController, adminController);
    }

    /** Creates and connects slot booking management. */
    private static void wireSlotBooking(ApplicationViews views, ApplicationServices services,
                                        MainController mainController,
                                        ParkingController parkingController, AuthController authController) {
        AdminSlotBookingManagementView bookingView = createSlotBookingManagementView(views);
        AdminSlotBookingController bookingController = createSlotBookingController(bookingView, services);
        setLogoutAction(bookingController, authController);
        setSlotBookingController(parkingController, bookingController);
        setSlotBookingController(mainController, bookingController);
    }

    /** Creates a shared logout action for secondary views. */
    private static Runnable createLogoutAction(AuthController authController) {
        return () -> logout(authController);
    }

    /** Creates an action that opens the current parking status screen. */
    private static Runnable createParkingStatusDisplayAction(ParkingController parkingController) {
        return () -> loadParkingStatus(parkingController);
    }

    /** Starts the simulation thread and connects window shutdown. */
    private static void startSimulation(ApplicationViews views, ApplicationServices services,
                                        ParkingController parkingController) {
        setParkingStatusChangeListener(services, parkingController);
        startSimulationService(services);
        addMainWindowCloseListener(views, services);
    }

    /** Adds the shutdown listener to the main window. */
    private static void addMainWindowCloseListener(ApplicationViews views, ApplicationServices services) {
        getMainMenuView(views).addWindowListener(new WindowClosingAction(() -> stopSimulationService(services)));
    }

    /** Gets the loaded configuration object. */
    private static Config getConfig(ConfigService configService) {
        return configService.getConfig();
    }

    /** Gets the configured database IP. */
    private static String getDbIP(ConfigService configService) {
        return getConfig(configService).getDbIP();
    }

    /** Gets the configured database port. */
    private static int getDbPort(ConfigService configService) {
        return getConfig(configService).getDbPort();
    }

    /** Gets the configured database name. */
    private static String getDbName(ConfigService configService) {
        return getConfig(configService).getDbName();
    }

    /** Gets the configured database user. */
    private static String getDbUser(ConfigService configService) {
        return getConfig(configService).getDbUser();
    }

    /** Gets the configured database password. */
    private static String getDbPassword(ConfigService configService) {
        return getConfig(configService).getDbPassword();
    }

    /** Creates the user DAO. */
    private static UserDAO createUserDAO(DatabaseManager db) {
        return new UserDAOImpl(db);
    }

    /** Creates the vehicle DAO. */
    private static VehicleDAO createVehicleDAO(DatabaseManager db) {
        return new VehicleDAOImpl(db);
    }

    /** Creates the parking-space DAO. */
    private static ParkingSpaceDAO createParkingSpaceDAO(DatabaseManager db) {
        return new ParkingSpaceDAOImpl(db);
    }

    /** Creates the reservation DAO. */
    private static ReservationDAO createReservationDAO(DatabaseManager db) {
        return new ReservationDAOImpl(db);
    }

    /** Creates the occupancy DAO. */
    private static OccupancyDAO createOccupancyDAO(DatabaseManager db) {
        return new OccupancyDAOImpl(db);
    }

    /** Creates the user service. */
    private static UserService createUserService(ApplicationDaos daos, DatabaseManager db) {
        return new UserService(getUserDAO(daos), getVehicleDAO(daos), getParkingSpaceDAO(daos),
                getReservationDAO(daos), db);
    }

    /** Creates the parking service. */
    private static ParkingService createParkingService(ApplicationDaos daos, DatabaseManager db) {
        return new ParkingService(getParkingSpaceDAO(daos), getVehicleDAO(daos), getReservationDAO(daos), db);
    }

    /** Creates the reservation service. */
    private static ReservationService createReservationService(ApplicationDaos daos, DatabaseManager db) {
        return new ReservationService(getReservationDAO(daos), getParkingSpaceDAO(daos), getVehicleDAO(daos), db);
    }

    /** Creates the admin service. */
    private static AdminService createAdminService(ApplicationServices services, ApplicationDaos daos,
                                                   DatabaseManager db) {
        return new AdminService(getParkingService(services), getReservationDAO(daos), db);
    }

    /** Gets the user DAO from the setup holder. */
    private static UserDAO getUserDAO(ApplicationDaos daos) {
        return daos.getUserDAO();
    }

    /** Sets the user DAO on the setup holder. */
    private static void setUserDAO(ApplicationDaos daos, UserDAO userDAO) {
        daos.setUserDAO(userDAO);
    }

    /** Gets the vehicle DAO from the setup holder. */
    private static VehicleDAO getVehicleDAO(ApplicationDaos daos) {
        return daos.getVehicleDAO();
    }

    /** Sets the vehicle DAO on the setup holder. */
    private static void setVehicleDAO(ApplicationDaos daos, VehicleDAO vehicleDAO) {
        daos.setVehicleDAO(vehicleDAO);
    }

    /** Gets the parking-space DAO from the setup holder. */
    private static ParkingSpaceDAO getParkingSpaceDAO(ApplicationDaos daos) {
        return daos.getParkingSpaceDAO();
    }

    /** Sets the parking-space DAO on the setup holder. */
    private static void setParkingSpaceDAO(ApplicationDaos daos, ParkingSpaceDAO parkingSpaceDAO) {
        daos.setParkingSpaceDAO(parkingSpaceDAO);
    }

    /** Gets the reservation DAO from the setup holder. */
    private static ReservationDAO getReservationDAO(ApplicationDaos daos) {
        return daos.getReservationDAO();
    }

    /** Sets the reservation DAO on the setup holder. */
    private static void setReservationDAO(ApplicationDaos daos, ReservationDAO reservationDAO) {
        daos.setReservationDAO(reservationDAO);
    }

    /** Gets the occupancy DAO from the setup holder. */
    private static OccupancyDAO getOccupancyDAO(ApplicationDaos daos) {
        return daos.getOccupancyDAO();
    }

    /** Sets the occupancy DAO on the setup holder. */
    private static void setOccupancyDAO(ApplicationDaos daos, OccupancyDAO occupancyDAO) {
        daos.setOccupancyDAO(occupancyDAO);
    }

    /** Gets the user service from the setup holder. */
    private static UserService getUserService(ApplicationServices services) {
        return services.getUserService();
    }

    /** Sets the user service on the setup holder. */
    private static void setUserService(ApplicationServices services, UserService userService) {
        services.setUserService(userService);
    }

    /** Gets the parking service from the setup holder. */
    private static ParkingService getParkingService(ApplicationServices services) {
        return services.getParkingService();
    }

    /** Sets the parking service on the setup holder. */
    private static void setParkingService(ApplicationServices services, ParkingService parkingService) {
        services.setParkingService(parkingService);
    }

    /** Gets the reservation service from the setup holder. */
    private static ReservationService getReservationService(ApplicationServices services) {
        return services.getReservationService();
    }

    /** Sets the reservation service on the setup holder. */
    private static void setReservationService(ApplicationServices services, ReservationService reservationService) {
        services.setReservationService(reservationService);
    }

    /** Gets the admin service from the setup holder. */
    private static AdminService getAdminService(ApplicationServices services) {
        return services.getAdminService();
    }

    /** Sets the admin service on the setup holder. */
    private static void setAdminService(ApplicationServices services, AdminService adminService) {
        services.setAdminService(adminService);
    }

    /** Gets the statistics service from the setup holder. */
    private static StatisticsService getStatisticsService(ApplicationServices services) {
        return services.getStatisticsService();
    }

    /** Sets the statistics service on the setup holder. */
    private static void setStatisticsService(ApplicationServices services, StatisticsService statisticsService) {
        services.setStatisticsService(statisticsService);
    }

    /** Gets the simulation service from the setup holder. */
    private static SimulationService getSimulationService(ApplicationServices services) {
        return services.getSimulationService();
    }

    /** Sets the simulation service on the setup holder. */
    private static void setSimulationService(ApplicationServices services, SimulationService simulationService) {
        services.setSimulationService(simulationService);
    }

    /** Creates the login window. */
    private static LoginView createLoginView() {
        return new LoginView();
    }

    /** Creates the signup window. */
    private static SignupView createSignupView() {
        return new SignupView();
    }

    /** Creates the occupancy chart view. */
    private static OccupancyChartView createOccupancyChartView() {
        return new OccupancyChartView();
    }

    /** Creates the main menu view. */
    private static MainMenuView createMainMenuView(ApplicationViews views) {
        return new MainMenuView(getOccupancyChartView(views));
    }

    /** Gets the login view from the setup holder. */
    private static LoginView getLoginView(ApplicationViews views) {
        return views.getLoginView();
    }

    /** Sets the login view on the setup holder. */
    private static void setLoginView(ApplicationViews views, LoginView loginView) {
        views.setLoginView(loginView);
    }

    /** Gets the signup view from the setup holder. */
    private static SignupView getSignupView(ApplicationViews views) {
        return views.getSignupView();
    }

    /** Sets the signup view on the setup holder. */
    private static void setSignupView(ApplicationViews views, SignupView signupView) {
        views.setSignupView(signupView);
    }

    /** Gets the main menu view from the setup holder. */
    private static MainMenuView getMainMenuView(ApplicationViews views) {
        return views.getMainMenuView();
    }

    /** Sets the main menu view on the setup holder. */
    private static void setMainMenuView(ApplicationViews views, MainMenuView mainMenuView) {
        views.setMainMenuView(mainMenuView);
    }

    /** Gets the occupancy chart view from the setup holder. */
    private static OccupancyChartView getOccupancyChartView(ApplicationViews views) {
        return views.getOccupancyChartView();
    }

    /** Sets the occupancy chart view on the setup holder. */
    private static void setOccupancyChartView(ApplicationViews views, OccupancyChartView occupancyChartView) {
        views.setOccupancyChartView(occupancyChartView);
    }

    /** Assigns the signup view to the authentication controller. */
    private static void setSignupView(AuthController authController, ApplicationViews views) {
        authController.setSignupView(getSignupView(views));
    }

    /** Assigns the configuration service to the authentication controller. */
    private static void setConfigService(AuthController authController, ConfigService configService) {
        authController.setConfigService(configService);
    }

    /** Assigns the reservation service to the authentication controller. */
    private static void setReservationService(AuthController authController, ApplicationServices services) {
        authController.setReservationService(getReservationService(services));
    }

    /** Adds the login button action. */
    private static void addLoginAction(ApplicationViews views, AuthController authController) {
        getLoginView(views).addLoginListener(e -> handleLogin(authController));
    }

    /** Adds the signup navigation action. */
    private static void addSignupNavigationAction(ApplicationViews views, AuthController authController) {
        getLoginView(views).addSignupNavigationListener(e -> handleSignupNavigation(authController));
    }

    /** Adds the registration action. */
    private static void addRegistrationAction(ApplicationViews views, AuthController authController) {
        getSignupView(views).addRegistrationListener(e -> handleRegistration(authController));
    }

    /** Adds the back-to-login action. */
    private static void addBackToLoginAction(ApplicationViews views, AuthController authController) {
        getSignupView(views).addBackToLoginListener(e -> handleBackToLogin(authController));
    }

    /** Starts the login flow through the authentication controller. */
    private static void handleLogin(AuthController authController) {
        authController.handleLogin();
    }

    /** Starts the signup navigation flow through the authentication controller. */
    private static void handleSignupNavigation(AuthController authController) {
        authController.handleSignup();
    }

    /** Starts the registration flow through the authentication controller. */
    private static void handleRegistration(AuthController authController) {
        authController.handleRegistrationSubmission();
    }

    /** Returns from signup to login through the authentication controller. */
    private static void handleBackToLogin(AuthController authController) {
        authController.handleBackToLogin();
    }

    /** Initializes the login view. */
    private static void initializeLoginView(ApplicationViews views) {
        getLoginView(views).initComponents();
    }

    /** Initializes the signup view. */
    private static void initializeSignupView(ApplicationViews views) {
        getSignupView(views).initComponents();
    }

    /** Initializes the main menu view. */
    private static void initializeMainMenuView(ApplicationViews views) {
        getMainMenuView(views).initComponents();
    }

    /** Assigns the main menu controller to the authentication controller. */
    private static void setMainMenuController(AuthController authController, MainController mainController,
                                              ApplicationViews views) {
        authController.setMainMenuController(mainController, getMainMenuView(views));
    }

    /** Assigns the authentication controller to the main menu controller. */
    private static void setAuthController(MainController mainController, AuthController authController) {
        mainController.setAuthController(authController);
    }

    /** Records one occupancy snapshot. */
    private static void recordOccupancy(StatisticsService statisticsService) {
        statisticsService.recordOccupancy();
    }

    /** Assigns the main menu view to the parking controller. */
    private static void setMainMenuView(ParkingController parkingController, ApplicationViews views) {
        parkingController.setMainMenuView(getMainMenuView(views));
    }

    /** Assigns the user service to the parking controller. */
    private static void setUserService(ParkingController parkingController, ApplicationServices services) {
        parkingController.setUserService(getUserService(services));
    }

    /** Assigns the admin service to the parking controller. */
    private static void setAdminService(ParkingController parkingController, ApplicationServices services) {
        parkingController.setAdminService(getAdminService(services));
    }

    /** Assigns the statistics controller to the parking controller. */
    private static void setStatisticsController(ParkingController parkingController,
                                                StatisticsController statisticsController) {
        parkingController.setStatisticsController(statisticsController);
    }

    /** Assigns the statistics controller to the main menu controller. */
    private static void setStatisticsController(MainController mainController,
                                                StatisticsController statisticsController) {
        mainController.setStatisticsController(statisticsController);
    }

    /** Assigns the parking controller to the main menu controller. */
    private static void setParkingController(MainController mainController, ParkingController parkingController) {
        mainController.setParkingController(parkingController);
    }

    /** Creates the admin parking management dialog. */
    private static AdminParkingManagementView createAdminParkingManagementView(ApplicationViews views) {
        return new AdminParkingManagementView(getMainMenuView(views));
    }

    /** Creates the admin controller. */
    private static AdminController createAdminController(AdminParkingManagementView adminView,
                                                         ApplicationServices services) {
        return new AdminController(adminView, getParkingService(services));
    }

    /** Assigns the admin service to the admin controller. */
    private static void setAdminService(AdminController adminController, ApplicationServices services) {
        adminController.setAdminService(getAdminService(services));
    }

    /** Assigns logout behavior to the admin controller. */
    private static void setLogoutAction(AdminController adminController, AuthController authController) {
        adminController.setLogoutAction(createLogoutAction(authController));
    }

    /** Assigns the parking status display action to the admin controller. */
    private static void setParkingStatusDisplayAction(AdminController adminController,
                                                      ParkingController parkingController) {
        adminController.setParkingStatusDisplayAction(createParkingStatusDisplayAction(parkingController));
    }

    /** Assigns the admin controller to the parking controller. */
    private static void setAdminController(ParkingController parkingController, AdminController adminController) {
        parkingController.setAdminController(adminController);
    }

    /** Assigns the admin controller to the main menu controller. */
    private static void setAdminController(MainController mainController, AdminController adminController) {
        mainController.setAdminController(adminController);
    }

    /** Creates the slot booking dialog. */
    private static AdminSlotBookingManagementView createSlotBookingManagementView(ApplicationViews views) {
        return new AdminSlotBookingManagementView(getMainMenuView(views));
    }

    /** Creates the slot booking controller. */
    private static AdminSlotBookingController createSlotBookingController(AdminSlotBookingManagementView bookingView,
                                                                          ApplicationServices services) {
        return new AdminSlotBookingController(bookingView, getParkingService(services), getAdminService(services),
                getReservationService(services), getUserService(services));
    }

    /** Assigns logout behavior to the slot booking controller. */
    private static void setLogoutAction(AdminSlotBookingController bookingController, AuthController authController) {
        bookingController.setLogoutAction(createLogoutAction(authController));
    }

    /** Assigns the booking controller to the parking controller. */
    private static void setSlotBookingController(ParkingController parkingController,
                                                 AdminSlotBookingController bookingController) {
        parkingController.setSlotBookingController(bookingController);
    }

    /** Assigns the booking controller to the main menu controller. */
    private static void setSlotBookingController(MainController mainController,
                                                 AdminSlotBookingController bookingController) {
        mainController.setSlotBookingController(bookingController);
    }

    /** Logs out through the authentication controller. */
    private static void logout(AuthController authController) {
        authController.logout();
    }

    /** Loads the current parking status through the parking controller. */
    private static void loadParkingStatus(ParkingController parkingController) {
        parkingController.loadParkingStatus();
    }

    /** Assigns the parking observer listener to the simulation service. */
    private static void setParkingStatusChangeListener(ApplicationServices services,
                                                       ParkingController parkingController) {
        getSimulationService(services).setParkingStatusChangeListener(parkingController);
    }

    /** Starts the simulation service. */
    private static void startSimulationService(ApplicationServices services) {
        getSimulationService(services).startSimulation();
    }

    /** Stops the simulation service. */
    private static void stopSimulationService(ApplicationServices services) {
        getSimulationService(services).stopSimulation();
    }

}
