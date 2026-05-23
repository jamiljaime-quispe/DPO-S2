package Business.Entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for users of the parking system.
 * A user has login data, a role, and a list of registered vehicles.
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
	 * @param password the stored password value
	 * @param userType the role string
	 * @param vehicles the initial vehicles owned by this user
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

	/** Gets the user ID. */
	public String getId() { return id; }

	/** Gets the username. */
	public String getUsername() { return username; }

	/** Gets the email address. */
	public String getEmail() { return email; }

	/** Gets the stored password value. */
	public String getPassword() { return password; }

	/** Gets the user role. */
	public String getUserType() { return userType; }

	/** Gets this user's vehicles. */
	public List<Vehicle> getVehicles() { return vehicles; }

	/** Sets the user ID. */
	public void setId(String id) { this.id = id; }

	/** Sets the username. */
	public void setUsername(String username) { this.username = username; }

	/** Sets the email address. */
	public void setEmail(String email) { this.email = email; }

	/** Sets the stored password value. */
	public void setPassword(String password) { this.password = password; }

	/** Sets the user role. */
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
	 * @param licensePlate the plate of the vehicle to remove
	 */
	public void removeVehicle(String licensePlate) {
		if (vehicles == null) return;

		for (int i = vehicles.size() - 1; i >= 0; i--) {
			Vehicle vehicle = vehicles.get(i);
			if (vehicle != null && licensePlate.equals(vehicle.getLicensePlate())) {
				vehicles.remove(i);
			}
		}
	}
}
