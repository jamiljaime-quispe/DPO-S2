package Business.Entities;

import java.util.List;

/**
 * Represents a regular (non-admin) client in the system.
 * Reservation and parking operations are delegated to ReservationService and
 * ParkingService; entity methods are stubs kept for UML compatibility.
 */
public class Client extends User {
	/**
	 * Constructs a new Client.
	 *
	 * @param id       the unique user identifier
	 * @param username the chosen username
	 * @param email    the client's email address
	 * @param password the client's plain-text password
	 * @param userType the role string ("CLIENT")
	 * @param vehicles the initial list of vehicles owned by this client
	 */
	public Client(String id, String username, String email, String password, String userType,
			List<Vehicle> vehicles) {
		super(id, username, email, password, userType, vehicles);
	}

	/**
	 * Stub — use ReservationService.createReservation() instead.
	 *
	 * @param vehiclePlate the plate of the vehicle to reserve for
	 * @param type         the vehicle type code
	 * @return null (no-op at entity level)
	 */
	public Reservation reserveSpace(String vehiclePlate, int type) {
		return null;
	}

	/**
	 * Stub — use ReservationService.cancelReservationByPlate() instead.
	 *
	 * @param plate the license plate whose reservation to cancel
	 * @return false (no-op at entity level)
	 */
	public boolean cancelReservation(String plate) {
		return false;
	}

	/**
	 * Stub — use ParkingService.handleVehicleEntry() instead.
	 *
	 * @param plate the license plate of the entering vehicle
	 * @return null (no-op at entity level)
	 */
	public ParkingSpace enterParking(String plate) {
		return null;
	}

	/**
	 * Stub — use ParkingService.handleVehicleExit() instead.
	 *
	 * @param plate the license plate of the exiting vehicle
	 */
	public void exitParking(String plate) {
	}

	/**
	 * Stub — use ReservationService.getReservationsByUser() instead.
	 *
	 * @return null (no-op at entity level)
	 */
	public List<Reservation> getReservations() {
		return null;
	}
}
