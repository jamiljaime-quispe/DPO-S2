package Business.Entities;

import java.util.List;

/**
 * Represents a regular client in the system. Reservation and parking actions are handled by the service
 * layer.
 * <p>
 * The class stores project data in a clear object so the services, controllers, and persistence code can
 * pass the same information around safely.
 * </p>
 */
public class Client extends User {

	/**
	 * Constructs a new Client.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param ID the unique user identifier
	 * @param username the chosen username
	 * @param email the client's email address
	 * @param password the client's password
	 * @param userType the role string
	 * @param vehicles the initial list of vehicles owned by this client
	 */
	public Client(String id, String username, String email, String password, String userType,
			List<Vehicle> vehicles) {
		super(id, username, email, password, userType, vehicles);
	}
}
