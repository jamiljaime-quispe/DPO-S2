import Persistence.OccupancyDAO;
import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.UserDAO;
import Persistence.VehicleDAO;

/**
 * Holds DAO objects while the application is being wired.
 * <p>
 * This helper keeps the application setup readable by grouping related objects or startup steps instead of
 * leaving all details in one long method.
 * </p>
 */
public class ApplicationDaos {
    private UserDAO userDAO;
    private VehicleDAO vehicleDAO;
    private ParkingSpaceDAO parkingSpaceDAO;
    private ReservationDAO reservationDAO;
    private OccupancyDAO occupancyDAO;

    /**
     * Gets the user DAO.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current user DAO
     */
    public UserDAO getUserDAO() {
        return userDAO;
    }

    /**
     * Sets the user DAO.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param userDAO user DAO used to read or write the needed data
     */
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Gets the vehicle DAO.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current vehicle DAO
     */
    public VehicleDAO getVehicleDAO() {
        return vehicleDAO;
    }

    /**
     * Sets the vehicle DAO.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param vehicleDAO vehicle DAO used to read or write the needed data
     */
    public void setVehicleDAO(VehicleDAO vehicleDAO) {
        this.vehicleDAO = vehicleDAO;
    }

    /**
     * Gets the parking-space DAO.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current parking space DAO
     */
    public ParkingSpaceDAO getParkingSpaceDAO() {
        return parkingSpaceDAO;
    }

    /**
     * Sets the parking-space DAO.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param parkingSpaceDAO parking space DAO used to read or write the needed data
     */
    public void setParkingSpaceDAO(ParkingSpaceDAO parkingSpaceDAO) {
        this.parkingSpaceDAO = parkingSpaceDAO;
    }

    /**
     * Gets the reservation DAO.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current reservation DAO
     */
    public ReservationDAO getReservationDAO() {
        return reservationDAO;
    }

    /**
     * Sets the reservation DAO.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param reservationDAO reservation DAO used to read or write the needed data
     */
    public void setReservationDAO(ReservationDAO reservationDAO) {
        this.reservationDAO = reservationDAO;
    }

    /**
     * Gets the occupancy DAO.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current occupancy DAO
     */
    public OccupancyDAO getOccupancyDAO() {
        return occupancyDAO;
    }

    /**
     * Sets the occupancy DAO.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param occupancyDAO occupancy DAO used to read or write the needed data
     */
    public void setOccupancyDAO(OccupancyDAO occupancyDAO) {
        this.occupancyDAO = occupancyDAO;
    }
}
