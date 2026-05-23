package Business.Entities;

import java.util.List;

/**
 * Represents an admin user in the system.
 * Admin-only actions are handled by the service layer.
 */
public class Admin extends User {

	/**
	 * Constructs a new Admin.
	 *
	 * @param id       the unique user identifier
	 * @param username the admin username
	 * @param email    the admin email address
	 * @param password the admin password
	 * @param userType the role string
	 * @param vehicles the list of vehicles owned by this admin
	 */
	public Admin(String id, String username, String email, String password, String userType,
				 List<Vehicle> vehicles) {
		super(id, username, email, password, userType, vehicles);
	}
}
