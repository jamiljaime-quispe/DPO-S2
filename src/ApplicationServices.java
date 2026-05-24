import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.ReservationService;
import Business.Services.SimulationService;
import Business.Services.StatisticsService;
import Business.Services.UserService;

/**
 * Holds service objects while the application is being wired.
 * <p>
 * This helper keeps the application setup readable by grouping related objects or startup steps instead of
 * leaving all details in one long method.
 * </p>
 */
public class ApplicationServices {
    private UserService userService;
    private ParkingService parkingService;
    private ReservationService reservationService;
    private AdminService adminService;
    private StatisticsService statisticsService;
    private SimulationService simulationService;

    /**
     * Gets the user service.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current user service
     */
    public UserService getUserService() {
        return userService;
    }

    /**
     * Sets the user service.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param userService user service used to apply the needed project logic
     */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gets the parking service.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current parking service
     */
    public ParkingService getParkingService() {
        return parkingService;
    }

    /**
     * Sets the parking service.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param parkingService parking service used to apply the needed project logic
     */
    public void setParkingService(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /**
     * Gets the reservation service.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current reservation service
     */
    public ReservationService getReservationService() {
        return reservationService;
    }

    /**
     * Sets the reservation service.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param reservationService reservation service used to apply the needed project logic
     */
    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Gets the admin service.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current admin service
     */
    public AdminService getAdminService() {
        return adminService;
    }

    /**
     * Sets the admin service.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param adminService admin service used to apply the needed project logic
     */
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Gets the statistics service.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current statistics service
     */
    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    /**
     * Sets the statistics service.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param statisticsService statistics service used to apply the needed project logic
     */
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * Gets the simulation service.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current simulation service
     */
    public SimulationService getSimulationService() {
        return simulationService;
    }

    /**
     * Sets the simulation service.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param simulationService simulation service used to apply the needed project logic
     */
    public void setSimulationService(SimulationService simulationService) {
        this.simulationService = simulationService;
    }
}
