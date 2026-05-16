package Business.Services;

import Persistence.UserDAO;
import Persistence.VehicleDAO;
import Business.Entities.Client;
import Business.Entities.User;
import Business.Entities.Vehicle;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Handles user authentication, registration, and account management.
 */
public class UserService {
	private UserDAO userDAO;
	private VehicleDAO vehicleDAO;

	/**
	 * @param userDAO    data access object for users
	 * @param vehicleDAO data access object for vehicles
	 */
	public UserService(UserDAO userDAO, VehicleDAO vehicleDAO) {
		this.userDAO = userDAO;
		this.vehicleDAO = vehicleDAO;
	}

	/**
	 * Authenticates a user by username or email and password.
	 * Admin username is always "admin"; password is checked against config value
	 * by the caller (AuthController), not here.
	 * 
	 * @param id       username or email
	 * @param password plain text password
	 * @return authenticated User (Admin or Client), or null if credentials invalid
	 */
	public User login(String id, String password) {
		if (id == null || id.isBlank() || password == null)
			return null;

		// Try by username first, then by email
		User user = userDAO.findByUsername(id);
		if (user == null) {
			user = userDAO.findByEmail(id);
		}
		if (user == null)
			return null;
		// Admin password is verified by AuthController against config.json, not here
		if (!"admin".equals(user.getUsername()) && !PasswordUtil.verify(password, user.getPassword()))
			return null;
		return user;
	}

	/**
	 * Registers a new client.
	 * Validates username uniqueness, email format and password policy.
	 * 
	 * @param username desired username
	 * @param email    email address
	 * @param password plain text password
	 * @return newly created Client, or null if any validation fails
	 */
	public User signup(String username, String email, String password) {
		if (!isUsernameAvailable(username))
			return null;
		if (!isEmailValid(email))
			return null;
		if (!validatePassword(password))
			return null;

		Client newUser = new Client(null, username, email, PasswordUtil.hash(password), "CLIENT", new ArrayList<>());
		userDAO.save(newUser);
		return newUser;
	}

	/**
	 * Validates that a password meets the minimum policy:
	 * at least 8 characters, one uppercase letter, one lowercase letter, one digit.
	 * 
	 * @param password password to validate
	 * @return true if the password meets the policy
	 */
	public boolean validatePassword(String password) {
		if (password == null || password.length() < 8)
			return false;
		boolean hasUpper = false, hasLower = false, hasDigit = false;

		for (char c : password.toCharArray()) {
			if (Character.isUpperCase(c))
				hasUpper = true;
			else if (Character.isLowerCase(c))
				hasLower = true;
			else if (Character.isDigit(c))
				hasDigit = true;
		}
		return hasUpper && hasLower && hasDigit;
	}

	/**
	 * Checks that an email address matches a standard format.
	 * 
	 * @param email email to validate
	 * @return true if the format is valid
	 */
	public boolean isEmailValid(String email) {
		if (email == null)
			return false;
		String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
		return Pattern.matches(regex, email);
	}

	/**
	 * Checks that a username is not already taken in the database.
	 * 
	 * @param username username to check
	 * @return true if available
	 */
	public boolean isUsernameAvailable(String username) {
		if (username == null || username.isBlank())
			return false;
		return userDAO.findByUsername(username) == null;
	}

	/**
	 * Deletes a user account and all associated data.
	 * Note: the Persistence layer must cascade-delete vehicles and reservations.
	 * 
	 * @param userId ID of the user to delete
	 */
	public void deleteUser(int userId) {
		userDAO.delete(userId);
	}

	/**
	 * Retrieves a user by their numeric ID.
	 * 
	 * @param userId user ID
	 * @return User, or null if not found
	 */
	public User getUserById(int userId) {
		return userDAO.findById(userId);
	}

	/**
	 * Registers a vehicle and associates it with a user.
	 * 
	 * @param userId  owner's user ID
	 * @param vehicle vehicle to register
	 */
	public void addVehicle(int userId, Vehicle vehicle) {
		vehicleDAO.save(vehicle);
		User user = userDAO.findById(userId);
		if (user != null) {
			user.addVehicle(vehicle);
			userDAO.update(user);
		}
	}

	/**
	 * Removes a vehicle from the system and from the user's vehicle list.
	 * 
	 * @param userId owner's user ID
	 * @param plate  license plate of the vehicle to remove
	 */
	public void removeVehicle(int userId, String plate) {
		vehicleDAO.delete(plate);
		User user = userDAO.findById(userId);
		if (user != null) {
			user.removeVehicle(plate);
			userDAO.update(user);
		}
	}
}
