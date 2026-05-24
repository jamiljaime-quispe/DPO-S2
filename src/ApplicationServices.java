import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.ReservationService;
import Business.Services.SimulationService;
import Business.Services.StatisticsService;
import Business.Services.UserService;

/**
 * Holds service objects while the application is being wired.
 */
public class ApplicationServices {
    private UserService userService;
    private ParkingService parkingService;
    private ReservationService reservationService;
    private AdminService adminService;
    private StatisticsService statisticsService;
    private SimulationService simulationService;

    /** Gets the user service. */
    public UserService getUserService() {
        return userService;
    }

    /** Sets the user service. */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /** Gets the parking service. */
    public ParkingService getParkingService() {
        return parkingService;
    }

    /** Sets the parking service. */
    public void setParkingService(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /** Gets the reservation service. */
    public ReservationService getReservationService() {
        return reservationService;
    }

    /** Sets the reservation service. */
    public void setReservationService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** Gets the admin service. */
    public AdminService getAdminService() {
        return adminService;
    }

    /** Sets the admin service. */
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    /** Gets the statistics service. */
    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    /** Sets the statistics service. */
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /** Gets the simulation service. */
    public SimulationService getSimulationService() {
        return simulationService;
    }

    /** Sets the simulation service. */
    public void setSimulationService(SimulationService simulationService) {
        this.simulationService = simulationService;
    }
}
