import Persistence.OccupancyDAO;
import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.UserDAO;
import Persistence.VehicleDAO;

/**
 * Holds DAO objects while the application is being wired.
 */
public class ApplicationDaos {
    private UserDAO userDAO;
    private VehicleDAO vehicleDAO;
    private ParkingSpaceDAO parkingSpaceDAO;
    private ReservationDAO reservationDAO;
    private OccupancyDAO occupancyDAO;

    /** Gets the user DAO. */
    public UserDAO getUserDAO() {
        return userDAO;
    }

    /** Sets the user DAO. */
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /** Gets the vehicle DAO. */
    public VehicleDAO getVehicleDAO() {
        return vehicleDAO;
    }

    /** Sets the vehicle DAO. */
    public void setVehicleDAO(VehicleDAO vehicleDAO) {
        this.vehicleDAO = vehicleDAO;
    }

    /** Gets the parking-space DAO. */
    public ParkingSpaceDAO getParkingSpaceDAO() {
        return parkingSpaceDAO;
    }

    /** Sets the parking-space DAO. */
    public void setParkingSpaceDAO(ParkingSpaceDAO parkingSpaceDAO) {
        this.parkingSpaceDAO = parkingSpaceDAO;
    }

    /** Gets the reservation DAO. */
    public ReservationDAO getReservationDAO() {
        return reservationDAO;
    }

    /** Sets the reservation DAO. */
    public void setReservationDAO(ReservationDAO reservationDAO) {
        this.reservationDAO = reservationDAO;
    }

    /** Gets the occupancy DAO. */
    public OccupancyDAO getOccupancyDAO() {
        return occupancyDAO;
    }

    /** Sets the occupancy DAO. */
    public void setOccupancyDAO(OccupancyDAO occupancyDAO) {
        this.occupancyDAO = occupancyDAO;
    }
}
