package Business.Entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class representing a user in the system.
 * A user has a unique ID, credentials, a role type, and a list of registered vehicles.
 */
public abstract class User {
	private String id;
	private String username;
	private String email;
	private String password;
	private String userType;
	private List<Vehicle> vehicles;

	/**
	 * Constructs a new User.
	 *
	 * @param id       the unique user identifier
	 * @param username the chosen username
	 * @param email    the user's email address
	 * @param password the user's plain-text password
	 * @param userType the role string ("ADMIN" or "CLIENT")
	 * @param vehicles the initial list of vehicles owned by this user
	 */
	public User(String id, String username, String email, String password, String userType,
				List<Vehicle> vehicles) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.password = password;
		this.userType = userType;
		this.vehicles = vehicles != null ? vehicles : new ArrayList<>();
	}

	/**
	 * Gets the user ID.
	 *
	 * @return the user ID
	 */
	public String getId() { return id; }

	/**
	 * Gets the username.
	 *
	 * @return the username
	 */
	public String getUsername() { return username; }

	/**
	 * Gets the email address.
	 *
	 * @return the email address
	 */
	public String getEmail() { return email; }

	/**
	 * Gets the password.
	 *
	 * @return the plain-text password
	 */
	public String getPassword() { return password; }

	/**
	 * Gets the user type.
	 *
	 * @return the user type string ("ADMIN" or "CLIENT")
	 */
	public String getUserType() { return userType; }

	/**
	 * Gets the list of vehicles owned by this user.
	 *
	 * @return the list of vehicles
	 */
	public List<Vehicle> getVehicles() { return vehicles; }

	/**
	 * Sets the user ID.
	 *
	 * @param id the new user ID
	 */
	public void setId(String id) { this.id = id; }

	/**
	 * Sets the username.
	 *
	 * @param username the new username
	 */
	public void setUsername(String username) { this.username = username; }

	/**
	 * Sets the email address.
	 *
	 * @param email the new email address
	 */
	public void setEmail(String email) { this.email = email; }

	/**
	 * Sets the password.
	 *
	 * @param password the new plain-text password
	 */
	public void setPassword(String password) { this.password = password; }

	/**
	 * Sets the user type.
	 *
	 * @param userType the new user type string
	 */
	public void setUserType(String userType) { this.userType = userType; }

	/**
	 * Adds a vehicle to this user's vehicle list.
	 *
	 * @param vehicle the vehicle to add
	 */
	public void addVehicle(Vehicle vehicle) {
		if (vehicles == null) vehicles = new ArrayList<>();
		vehicles.add(vehicle);
	}

	/**
	 * Removes a vehicle from this user's vehicle list by license plate.
	 *
	 * @param licencePlate the plate of the vehicle to remove
	 */
	public void removeVehicle(String licencePlate) {
		if (vehicles != null) {
			vehicles.removeIf(v -> licencePlate.equals(v.getLicensePlate()));
		}
	}

	/**
	 * Authentication stub — logic is delegated to UserService.
	 *
	 * @param id       the username or email
	 * @param password the password to check
	 * @return always false at entity level
	 */
	public boolean login(String id, String password) {
		return false;
	}

	/**
	 * Logout stub — logic is delegated to AuthController.
	 */
	public void logout() {}

	/**
	 * Account deletion stub — logic is delegated to UserService.
	 */
	public void deleteAccount() {}
}
