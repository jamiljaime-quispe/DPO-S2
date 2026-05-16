package Business.Entities;

import java.util.List;

/**
 * Represents a regular (non-admin) user. Reservation and parking operations
 * are delegated to ReservationService and ParkingService; entity methods are
 * stubs kept for UML compatibility.
 */
public class Client extends User {
    public Client(String id, String username, String email, String password, String userType,
                  List<Vehicle> vehicles) {
        super(id, username, email, password, userType, vehicles);
    }

    /**
     * Stub — use ReservationService.createReservation() instead.
     * @param vehiclePlate plate of the vehicle to reserve for
     * @param type         vehicle type code
     * @return null
     */
    public Reservation reserveSpace(String vehiclePlate, int type) { return null; }

    /**
     * Stub — use ReservationService.cancelReservationByPlate(userId, plate) instead.
     * @param plate license plate
     * @return false
     */
    public boolean cancelReservation(String plate) { return false; }

    /**
     * Stub — use ParkingService.handleVehicleEntry() instead.
     * @param plate license plate
     * @return null
     */
    public ParkingSpace enterParking(String plate) { return null; }

    /**
     * Stub — use ParkingService.handleVehicleExit() instead.
     * @param plate license plate
     */
    public void exitParking(String plate) {}

    /**
     * Stub — use ReservationService.getReservationsByUser() instead.
     * @return null
     */
    public List<Reservation> getReservations() { return null; }
}
