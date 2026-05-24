package Business.Services;

import Persistence.UserDAO;
import Persistence.VehicleDAO;
import Business.Entities.Client;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.User;
import Business.Entities.Vehicle;
import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.TransactionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handles user authentication, registration, and account management.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class UserService {
	private UserDAO userDAO;
	private VehicleDAO vehicleDAO;
	private ParkingSpaceDAO parkingSpaceDAO;
	private ReservationDAO reservationDAO;
	private TransactionManager transactionManager;

	private String lastLoggedInUsername;
	private int lastLoggedInUserId = -1;

	/**
	 * Constructs a new UserService.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param userDAO the data access object for users
	 * @param vehicleDAO the data access object for vehicles
	 * @param parkingSpaceDAO the data access object for parking spaces
	 * @param reservationDAO the data access object for reservations
	 * @param transactionManager object that controls database transactions
	 */
	public UserService(UserDAO userDAO, VehicleDAO vehicleDAO, ParkingSpaceDAO parkingSpaceDAO,
			ReservationDAO reservationDAO,
			TransactionManager transactionManager) {
		this.userDAO = userDAO;
		this.vehicleDAO = vehicleDAO;
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.reservationDAO = reservationDAO;
		this.transactionManager = transactionManager;
	}

	/**
	 * Authenticates a user by username or email and password. Admin username is always "admin"; password is
	 * checked against config value by the caller (AuthController), not here.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param ID username or email
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
	 * Registers a new client. Validates username uniqueness, email format and password policy.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param username desired username
	 * @param email email address
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
	 * Validates that a password meets the minimum policy:. at least 8 characters, one uppercase letter, one
	 * lowercase letter, one digit.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
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
	 * Checks whether email valid.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
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
	 * Checks that a username is not already taken in the database. The operation is kept together so the
	 * stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
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
	 * Checks whether email available.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
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
	 * Deletes a user account and all associated data. Occupied spaces are freed before persistence cascades
	 * vehicles and reservations. This method synchronizes the transaction because the user's parked vehicles
	 * are cleared before the user row is deleted. Both changes must be committed together so no space remains
	 * occupied by a deleted account.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param userId ID of the user to delete
	 */
	public void deleteUser(int userId) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				// Load the user's vehicles once so the same list is used for every cleanup step.
				List<Vehicle> userVehicles = findVehiclesByUser(userId);
				clearParkedVehiclesForUser(userVehicles);
				deleteReservationsForUser(userId);
				deleteVehiclesForUser(userVehicles);
				deleteUserRecord(userId);
				clearSessionIfItBelongsTo(userId);

				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Handles clear parked vehicles for user.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param vehicles vehicles used by this operation
	 */
	private void clearParkedVehiclesForUser(List<Vehicle> vehicles) {
		if (parkingSpaceDAO == null || vehicles == null || vehicles.isEmpty()) return;

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
	 * Deletes reservations for user.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 */
	private void deleteReservationsForUser(int userId) {
		if (reservationDAO == null || userId <= 0) return;

		List<Reservation> reservations = findReservationsByUser(userId);
		for (Reservation reservation : reservations) {
			deleteReservationRecord(reservation.getId());
		}
	}

	/**
	 * Deletes vehicles for user.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param vehicles vehicles used by this operation
	 */
	private void deleteVehiclesForUser(List<Vehicle> vehicles) {
		if (vehicles == null || vehicles.isEmpty()) return;

		for (Vehicle vehicle : vehicles) {
			deleteVehicleRecord(vehicle.getLicensePlate());
		}
	}

	/**
	 * Handles clear session if it belongs to.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 */
	private void clearSessionIfItBelongsTo(int userId) {
		if (lastLoggedInUserId == userId) {
			clearSession();
		}
	}


	/**
	 * Authenticates a user and returns their role code. Admin password is intentionally not checked here;
	 * AuthController verifies it against config.json.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param ID username or email
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

	/**
	 * Gets the username of the currently logged-in user.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current last logged in username
	 */
	public String getLastLoggedInUsername() { return lastLoggedInUsername; }

	/**
	 * Gets the ID of the currently logged-in user, or -1 if none.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current last logged in user ID
	 */
	public int getLastLoggedInUserId() { return lastLoggedInUserId; }

	/**
	 * Handles clear session.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	public void clearSession() {
		lastLoggedInUserId = -1;
		lastLoggedInUsername = null;
	}

	/**
	 * Deletes current user.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 */
	public void deleteCurrentUser() {
		if (lastLoggedInUserId != -1) {
			deleteUser(lastLoggedInUserId);
			clearSession();
		}
	}

	/**
	 * Handles register.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param username desired username
	 * @param email email address
	 * @param password plain-text password
	 * @return true if registration succeeded
	 */
	public boolean register(String username, String email, String password) {
		return signup(username, email, password) != null;
	}

	/**
	 * Finds user by username.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param username username entered or stored for the user
	 * @return the matching user by username, or null when it is not found
	 */
	private User findUserByUsername(String username) {
		return userDAO.findByUsername(username);
	}

	/**
	 * Finds user by email.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param email email entered or stored for the user
	 * @return the matching user by email, or null when it is not found
	 */
	private User findUserByEmail(String email) {
		return userDAO.findByEmail(email);
	}


	/**
	 * Handles save user record.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param user user used by this operation
	 */
	private void saveUserRecord(User user) {
		userDAO.save(user);
	}


	/**
	 * Deletes user record.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 */
	private void deleteUserRecord(int userId) {
		userDAO.delete(userId);
	}

	/**
	 * Finds reservations by user.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 * @return the matching reservations by user, or null when it is not found
	 */
	private List<Reservation> findReservationsByUser(int userId) {
		return reservationDAO.findByUser(userId);
	}

	/**
	 * Deletes reservation record.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param reservationId reservation ID used by this operation
	 */
	private void deleteReservationRecord(int reservationId) {
		reservationDAO.delete(reservationId);
	}

	/**
	 * Finds vehicles by user.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 * @return the matching vehicles by user, or null when it is not found
	 */
	private List<Vehicle> findVehiclesByUser(int userId) {
		return vehicleDAO.findByUser(userId);
	}


	/**
	 * Deletes vehicle record.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 */
	private void deleteVehicleRecord(String plate) {
		vehicleDAO.delete(plate);
	}

	/**
	 * Loads all parking spaces.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @return the loaded all parking spaces
	 */
	private List<ParkingSpace> loadAllParkingSpaces() {
		return parkingSpaceDAO.findAll();
	}

	/**
	 * Updates parking space record.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param space space used by this operation
	 */
	private void updateParkingSpaceRecord(ParkingSpace space) {
		parkingSpaceDAO.update(space);
	}

	/**
	 * Checks whether h password exists.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param password password entered by the user
	 * @return true when the condition is met, false otherwise
	 */
	private String hashPassword(String password) {
		return PasswordUtil.hash(password);
	}

	/**
	 * Handles password matches.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param password password entered by the user
	 * @param storedPassword password entered by the user
	 * @return the result of the operation
	 */
	private boolean passwordMatches(String password, String storedPassword) {
		return PasswordUtil.verify(password, storedPassword);
	}

	/**
	 * Handles matches pattern.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param regex regex used by this operation
	 * @param text text used by this operation
	 * @return the result of the operation
	 */
	private boolean matchesPattern(String regex, String text) {
		return Pattern.matches(regex, text);
	}

	/**
	 * Gets the shared lock used by synchronized transaction blocks. The simulation thread and SwingWorkers can
	 * both change parking data, so this lock makes one multi-step database operation finish before another
	 * begins.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @return the result of the operation
	 */
	private Object transactionLock() {
		return transactionManager != null ? transactionManager : this;
	}

	/**
	 * Starts a transaction when transaction support is available. The operation is kept together so the stored
	 * data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	private void beginTransaction() {
		if (transactionManager != null) transactionManager.beginTransaction();
	}

	/**
	 * Commits a transaction when transaction support is available. The operation is kept together so the
	 * stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	private void commitTransaction() {
		if (transactionManager != null) transactionManager.commit();
	}

	/**
	 * Rolls back a transaction when transaction support is available. The operation is kept together so the
	 * stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	private void rollbackTransaction() {
		if (transactionManager != null) transactionManager.rollback();
	}
}
