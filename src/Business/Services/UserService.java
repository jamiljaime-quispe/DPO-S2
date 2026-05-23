package Business.Services;

import Persistence.UserDAO;
import Persistence.VehicleDAO;
import Business.Entities.Client;
import Business.Entities.ParkingSpace;
import Business.Entities.User;
import Business.Entities.Vehicle;
import Persistence.ParkingSpaceDAO;
import Persistence.TransactionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles user authentication, registration, and account management.
 */
public class UserService {
	private UserDAO userDAO;
	private VehicleDAO vehicleDAO;
	private ParkingSpaceDAO parkingSpaceDAO;
	private TransactionManager transactionManager;

	private String lastLoggedInUsername;
	private int lastLoggedInUserId = -1;

	/**
	 * Constructs a new UserService.
	 *
	 * @param userDAO    the data access object for users
	 * @param vehicleDAO the data access object for vehicles
	 * @param parkingSpaceDAO the data access object for parking spaces
	 * @param transactionManager object that controls database transactions
	 */
	public UserService(UserDAO userDAO, VehicleDAO vehicleDAO, ParkingSpaceDAO parkingSpaceDAO,
			TransactionManager transactionManager) {
		this.userDAO = userDAO;
		this.vehicleDAO = vehicleDAO;
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.transactionManager = transactionManager;
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
		User user = findUserByUsername(id);
		if (user == null) {
			user = findUserByEmail(id);
		}
		if (user == null)
			return null;
		// Admin password is verified by AuthController against config.json, not here
		if (!"admin".equals(user.getUsername()) && !passwordMatches(password, user.getPassword()))
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
		if (!isEmailAvailable(email))
			return null;
		if (!validatePassword(password))
			return null;

		Client newUser = new Client(null, username, email, hashPassword(password), "CLIENT", new ArrayList<>());
		saveUserRecord(newUser);
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
		return matchesPattern(regex, email);
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
		return findUserByUsername(username) == null;
	}

	/**
	 * Checks that an email address is not already used by another account.
	 *
	 * @param email email to check
	 * @return true if available
	 */
	public boolean isEmailAvailable(String email) {
		if (email == null || email.isBlank())
			return false;
		return findUserByEmail(email) == null;
	}

	/**
	 * Deletes a user account and all associated data.
	 * Occupied spaces are freed before persistence cascades vehicles and reservations.
	 * This method synchronizes the transaction because the user's parked vehicles
	 * are cleared before the user row is deleted. Both changes must be committed
	 * together so no space remains occupied by a deleted account.
	 * 
	 * @param userId ID of the user to delete
	 */
	public void deleteUser(int userId) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();
				clearParkedVehiclesForUser(userId);
				deleteUserRecord(userId);
				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/** Frees spaces occupied by vehicles owned by the user being deleted. */
	private void clearParkedVehiclesForUser(int userId) {
		if (parkingSpaceDAO == null || userId <= 0) return;

		List<Vehicle> vehicles = findVehiclesByUser(userId);
		Set<String> plates = new HashSet<>();
		for (Vehicle vehicle : vehicles) {
			plates.add(vehicle.getLicensePlate());
		}

		if (plates.isEmpty()) return;

		List<ParkingSpace> spaces = loadAllParkingSpaces();
		for (ParkingSpace space : spaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& plates.contains(space.getParkedVehicle().getLicensePlate())) {
				space.freeSpace();
				updateParkingSpaceRecord(space);
			}
		}
	}

	/**
	 * Retrieves a user by their numeric ID.
	 * 
	 * @param userId user ID
	 * @return User, or null if not found
	 */
	public User getUserById(int userId) {
		return findUserById(userId);
	}

	/**
	 * Registers a vehicle and associates it with a user.
	 * This method synchronizes the transaction because saving the vehicle and
	 * refreshing the user record belong to the same account update.
	 * 
	 * @param userId  owner's user ID
	 * @param vehicle vehicle to register
	 */
	public void addVehicle(int userId, Vehicle vehicle) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();
				saveVehicleRecord(vehicle);
				User user = findUserById(userId);
				if (user != null) {
					user.addVehicle(vehicle);
					updateUserRecord(user);
				}
				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Removes a vehicle from the system and from the user's vehicle list.
	 * This method synchronizes the transaction because the vehicle deletion and
	 * user update must stay consistent.
	 *
	 * @param userId owner's user ID
	 * @param plate  license plate of the vehicle to remove
	 */
	public void removeVehicle(int userId, String plate) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();
				deleteVehicleRecord(plate);
				User user = findUserById(userId);
				if (user != null) {
					user.removeVehicle(plate);
					updateUserRecord(user);
				}
				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Authenticates a user and returns their role code.
	 * Admin password is intentionally not checked here; AuthController verifies it against config.json.
	 *
	 * @param id       username or email
	 * @param password plain-text password
	 * @return 1 for admin, 2 for regular user, 0 for invalid credentials
	 */
	public int authenticate(String id, String password) {
		User user = login(id, password);
		if (user == null) return 0;
		lastLoggedInUsername = user.getUsername();
		lastLoggedInUserId = Integer.parseInt(user.getId());
		return "ADMIN".equals(user.getUserType()) ? 1 : 2;
	}

	/** Gets the username of the currently logged-in user. */
	public String getLastLoggedInUsername() { return lastLoggedInUsername; }

	/** Gets the ID of the currently logged-in user, or -1 if none. */
	public int getLastLoggedInUserId() { return lastLoggedInUserId; }

	/** Clears the current session state without deleting the account (called on logout). */
	public void clearSession() {
		lastLoggedInUserId = -1;
		lastLoggedInUsername = null;
	}

	/** Deletes the currently logged-in user's account and clears session state. */
	public void deleteCurrentUser() {
		if (lastLoggedInUserId != -1) {
			deleteUser(lastLoggedInUserId);
			clearSession();
		}
	}

	/**
	 * Registers a new client account. Convenience wrapper over signup().
	 *
	 * @param username desired username
	 * @param email    email address
	 * @param password plain-text password
	 * @return true if registration succeeded
	 */
	public boolean register(String username, String email, String password) {
		return signup(username, email, password) != null;
	}

	/** Finds a user by username through persistence. */
	private User findUserByUsername(String username) {
		return userDAO.findByUsername(username);
	}

	/** Finds a user by email through persistence. */
	private User findUserByEmail(String email) {
		return userDAO.findByEmail(email);
	}

	/** Finds a user by ID through persistence. */
	private User findUserById(int userId) {
		return userDAO.findById(userId);
	}

	/** Saves a user through persistence. */
	private void saveUserRecord(User user) {
		userDAO.save(user);
	}

	/** Updates a user through persistence. */
	private void updateUserRecord(User user) {
		userDAO.update(user);
	}

	/** Deletes a user through persistence. */
	private void deleteUserRecord(int userId) {
		userDAO.delete(userId);
	}

	/** Finds vehicles belonging to a user through persistence. */
	private List<Vehicle> findVehiclesByUser(int userId) {
		return vehicleDAO.findByUser(userId);
	}

	/** Saves a vehicle through persistence. */
	private void saveVehicleRecord(Vehicle vehicle) {
		vehicleDAO.save(vehicle);
	}

	/** Deletes a vehicle through persistence. */
	private void deleteVehicleRecord(String plate) {
		vehicleDAO.delete(plate);
	}

	/** Loads every parking space through persistence. */
	private List<ParkingSpace> loadAllParkingSpaces() {
		return parkingSpaceDAO.findAll();
	}

	/** Updates a parking space through persistence. */
	private void updateParkingSpaceRecord(ParkingSpace space) {
		parkingSpaceDAO.update(space);
	}

	/** Hashes a password through the password helper. */
	private String hashPassword(String password) {
		return PasswordUtil.hash(password);
	}

	/** Checks a password against a stored hash. */
	private boolean passwordMatches(String password, String storedPassword) {
		return PasswordUtil.verify(password, storedPassword);
	}

	/** Checks whether text matches a regular expression. */
	private boolean matchesPattern(String regex, String text) {
		return Pattern.matches(regex, text);
	}

	/**
	 * Gets the shared lock used by synchronized transaction blocks.
	 * The simulation thread and SwingWorkers can both change parking data, so
	 * this lock makes one multi-step database operation finish before another begins.
	 */
	private Object transactionLock() {
		return transactionManager != null ? transactionManager : this;
	}

	/** Starts a transaction when transaction support is available. */
	private void beginTransaction() {
		if (transactionManager != null) transactionManager.beginTransaction();
	}

	/** Commits a transaction when transaction support is available. */
	private void commitTransaction() {
		if (transactionManager != null) transactionManager.commit();
	}

	/** Rolls back a transaction when transaction support is available. */
	private void rollbackTransaction() {
		if (transactionManager != null) transactionManager.rollback();
	}
}
