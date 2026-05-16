package Business.Entities;

import java.util.List;

/**
 * Represents an admin user in the system.
 * Admin-specific operations are handled by AdminService and ParkingService;
 * the entity methods below are stubs kept for UML compatibility.
 */
public class Admin extends User {

	/**
	 * Constructs a new Admin.
	 *
	 * @param id       the unique user identifier
	 * @param username the admin username (always "admin")
	 * @param email    the admin email address
	 * @param password the admin plain-text password
	 * @param userType the role string ("ADMIN")
	 * @param vehicles the list of vehicles owned by this admin
	 */
	public Admin(String id, String username, String email, String password, String userType,
				 List<Vehicle> vehicles) {
		super(id, username, email, password, userType, vehicles);
	}

	/**
	 * Stub — use AdminService / ParkingService instead.
	 *
	 * @param space the space to create
	 */
	public void createParkingSpace(ParkingSpace space) {}

	/**
	 * Stub — use AdminService / ParkingService instead.
	 *
	 * @param space the space to edit
	 */
	public void editParkingSpace(ParkingSpace space) {}

	/**
	 * Stub — use AdminService / ParkingService instead.
	 *
	 * @param spaceId the code of the space to delete
	 * @return false (no-op at entity level)
	 */
	public boolean deleteParkingSpace(String spaceId) { return false; }

	/**
	 * Stub — use AdminService instead.
	 *
	 * @param reservationId the ID of the reservation to cancel
	 */
	public void cancelReservation(String reservationId) {}

	/**
	 * Stub — use AdminService.getFullParkingStatus() instead.
	 *
	 * @return null (no-op at entity level)
	 */
	public List<ParkingSpace> getParkingStatus() { return null; }
}
