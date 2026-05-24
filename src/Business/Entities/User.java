package Business.Entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for users of the parking system. A user has login data, a role, and a list of registered
 * vehicles.
 * <p>
 * The class stores project data in a clear object so the services, controllers, and persistence code can
 * pass the same information around safely.
 * </p>
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
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param ID the unique user identifier
	 * @param username the chosen username
	 * @param email the user's email address
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

	/**
	 * Gets the user ID.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current ID
	 */
	public String getId() { return id; }

	/**
	 * Gets the username.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current username
	 */
	public String getUsername() { return username; }

	/**
	 * Gets the email address.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current email
	 */
	public String getEmail() { return email; }

	/**
	 * Gets the stored password value.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current password
	 */
	public String getPassword() { return password; }

	/**
	 * Gets the user role.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current user type
	 */
	public String getUserType() { return userType; }

	/**
	 * Gets this user's vehicles.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current vehicles
	 */
	public List<Vehicle> getVehicles() { return vehicles; }

	/**
	 * Sets the user ID.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param ID ID used by this operation
	 */
	public void setId(String id) { this.id = id; }

	/**
	 * Sets the username.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param username username entered or stored for the user
	 */
	public void setUsername(String username) { this.username = username; }

	/**
	 * Sets the email address.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param email email entered or stored for the user
	 */
	public void setEmail(String email) { this.email = email; }

	/**
	 * Sets the stored password value.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param password password entered by the user
	 */
	public void setPassword(String password) { this.password = password; }

}
