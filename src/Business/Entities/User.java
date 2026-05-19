package Business.Entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all system users.
 */
public abstract class User {
	private String id;
	private String username;
	private String email;
	private String password;
	private String userType;
	private List<Vehicle> vehicles;

	public User(String id, String username, String email, String password, String userType,
				List<Vehicle> vehicles) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.password = password;
		this.userType = userType;
		this.vehicles = vehicles != null ? vehicles : new ArrayList<>();
	}

	// --- Getters ---

	/**
	 * @return user ID
	 */
	public String getId() { return id; }

	/**
	 * @return username
	 */
	public String getUsername() { return username; }

	/**
	 * @return email address
	 */
	public String getEmail() { return email; }

	/**
	 * @return hashed/plain password
	 */
	public String getPassword() { return password; }

	/**
	 * @return user type string ("ADMIN" or "CLIENT")
	 */
	public String getUserType() { return userType; }

	/**
	 * @return list of vehicles owned by this user
	 */
	public List<Vehicle> getVehicles() { return vehicles; }

	// --- Setters ---

	public void setId(String id) { this.id = id; }
	public void setUsername(String username) { this.username = username; }
	public void setEmail(String email) { this.email = email; }
	public void setPassword(String password) { this.password = password; }
	public void setUserType(String userType) { this.userType = userType; }

	// --- Behaviour ---

	/**
	 * Adds a vehicle to this user's vehicle list.
	 * @param vehicle vehicle to add
	 */
	public void addVehicle(Vehicle vehicle) {
		if (vehicles == null) vehicles = new ArrayList<>();
		vehicles.add(vehicle);
	}

	/**
	 * Removes a vehicle from this user's vehicle list by license plate.
	 * @param licencePlate plate of the vehicle to remove
	 */
	public void removeVehicle(String licencePlate) {
		if (vehicles != null) {
			vehicles.removeIf(v -> licencePlate.equals(v.getLicensePlate()));
		}
	}

	/**
	 * Authentication check — logic delegated to UserService.
	 * @param id username or email
	 * @param password password
	 * @return always false at entity level
	 */
	public boolean login(String id, String password) {
		return false;
	}

	/**
	 * Logout — logic delegated to AuthController.
	 */
	public void logout() {}

	/**
	 * Account deletion — logic delegated to UserService.
	 */
	public void deleteAccount() {}
}
