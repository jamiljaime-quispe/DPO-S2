package Business.Entities;

import java.util.List;

/**
 * Represents an admin user. Admin-specific operations are handled by AdminService
 * and ParkingService; these entity methods are stubs kept for UML compatibility.
 */
public class Admin extends User {

	public Admin(String id, String username, String email, String password, String userType,
				 List<Vehicle> vehicles) {
		super(id, username, email, password, userType, vehicles);
	}

	/**
	 * Stub — use AdminService / ParkingService instead.
	 * @param space space to create
	 */
	public void createParkingSpace(ParkingSpace space) {}

	/**
	 * Stub — use AdminService / ParkingService instead.
	 * @param space space to edit
	 */
	public void editParkingSpace(ParkingSpace space) {}

	/**
	 * Stub — use AdminService / ParkingService instead.
	 * @param spaceId space code
	 * @return false (no-op)
	 */
	public boolean deleteParkingSpace(String spaceId) { return false; }

	/**
	 * Stub — use AdminService instead.
	 * @param reservationId reservation to cancel
	 */
	public void cancelReservation(String reservationId) {}

	/**
	 * Stub — use AdminService.getFullParkingStatus() instead.
	 * @return null
	 */
	public List<ParkingSpace> getParkingStatus() { return null; }
}
