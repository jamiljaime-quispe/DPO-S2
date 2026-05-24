package Business.Services;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.TransactionManager;
import Persistence.VehicleDAO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manages parking spaces and vehicle entry or exit.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class ParkingService {
	private ParkingSpaceDAO parkingSpaceDAO;
	private VehicleDAO vehicleDAO;
	private ReservationDAO reservationDAO;
	private TransactionManager transactionManager;

	/**
	 * Constructs a new ParkingService.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param parkingSpaceDAO DAO used for parking spaces
	 * @param vehicleDAO DAO used for vehicles
	 * @param reservationDAO DAO used for reservations
	 * @param transactionManager object that controls database transactions
	 */
	public ParkingService(ParkingSpaceDAO parkingSpaceDAO, VehicleDAO vehicleDAO,
			ReservationDAO reservationDAO, TransactionManager transactionManager) {
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.vehicleDAO = vehicleDAO;
		this.reservationDAO = reservationDAO;
		this.transactionManager = transactionManager;
	}

	/**
	 * Gets all parking spaces.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return all spaces
	 */
	public List<ParkingSpace> getAllSpaces() {
		return loadAllSpaces();
	}

	/**
	 * Finds a record by its code.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param code space code
	 * @return matching space, or null if not found
	 */
	public ParkingSpace findByCode(String code) {
		return findSpaceByCode(code);
	}

	/**
	 * Updates parking space details.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param space space with updated details
	 */
	public void updateParkingSpaceDetails(ParkingSpace space) {
		ParkingSpace existing = findSpaceByCode(space.getId());
		if (existing == null) {
			throw new IllegalArgumentException("Space not found: " + space.getId());
		}

		boolean typeChanged = existing.getVehicleType() != space.getVehicleType();
		if (typeChanged && existing.isOccupied()) {
			throw new IllegalArgumentException("Vehicle type cannot be edited because the space is occupied.");
		}
		if (typeChanged && existing.isReserved()) {
			throw new IllegalArgumentException("Vehicle type cannot be edited because the space is reserved.");
		}

		updateParkingSpaceDetailsRecord(space);
	}

	/**
	 * Creates a parking space.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param space space to create
	 */
	public void createParkingSpace(ParkingSpace space) {
		if (space == null || space.getVehicleType() == null) {
			throw new IllegalArgumentException("Vehicle type is required.");
		}

		if (space.getId() == null || space.getId().isBlank()) {
			space.setId(generateNextCodeForFloor(space.getFloor()));
		} else if (findSpaceByCode(space.getId()) != null) {
			throw new IllegalArgumentException("Space code already exists: " + space.getId());
		}

		space.setOccupied(false);
		space.setReserved(false);
		space.setParkedVehicle(null);
		space.cancelReservation();
		saveParkingSpaceRecord(space);
	}

	/**
	 * Updates a parking space.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param space space with updated state
	 */
	public void updateParkingSpace(ParkingSpace space) {
		updateParkingSpaceRecord(space);
	}

	/**
	 * Deletes a parking space.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param code space code to delete
	 * @return true if the space was deleted
	 */
	public boolean deleteParkingSpace(String code) {
		ParkingSpace space = findSpaceByCode(code);
		if (space == null || space.isOccupied() || space.isReserved()) return false;

		deleteParkingSpaceRecord(code);
		return true;
	}

	/**
	 * Finds available spaces.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param type vehicle type
	 * @return available spaces
	 */
	public List<ParkingSpace> findAvailableSpaces(VehicleType type) {
		return findAvailableSpacesByType(type);
	}

	/**
	 * Finds active reservation by plate.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param plate license plate to check
	 * @return active reservation, or null if none exists
	 */
	public Reservation findActiveReservationByPlate(String plate) {
		if (plate == null || plate.isBlank()) return null;

		Reservation reservation = findReservationByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			return reservation;
		}
		return null;
	}

	/**
	 * Finds occupied space by plate.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param plate license plate to check
	 * @return occupied space, or null if the vehicle is not parked
	 */
	public ParkingSpace findOccupiedSpaceByPlate(String plate) {
		if (plate == null || plate.isBlank()) return null;

		List<ParkingSpace> allSpaces = loadAllSpaces();
		for (ParkingSpace space : allSpaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& plate.equalsIgnoreCase(space.getParkedVehicle().getLicensePlate())) {
				return space;
			}
		}
		return null;
	}

	/**
	 * Returns spaces occupied by vehicles registered to a user.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @param userId owner user ID
	 * @return occupied spaces for that user
	 */
	public List<ParkingSpace> getParkedSpacesByUser(int userId) {
		List<ParkingSpace> parkedSpaces = new ArrayList<>();
		if (userId <= 0) return parkedSpaces;

		List<Vehicle> vehicles = findVehiclesByUser(userId);
		Set<String> userPlates = new HashSet<>();
		for (Vehicle vehicle : vehicles) {
			userPlates.add(vehicle.getLicensePlate());
		}

		List<ParkingSpace> spaces = loadAllSpaces();
		for (ParkingSpace space : spaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& userPlates.contains(space.getParkedVehicle().getLicensePlate())) {
				parkedSpaces.add(space);
			}
		}

		return parkedSpaces;
	}


	/**
	 * Handles entry for a logged-in user and registers new vehicles to that user. This method synchronizes the
	 * transaction because it may create a vehicle, check ownership, and occupy a space as one database
	 * operation. Without this, the simulation thread or another UI action could change the same space between
	 * those steps.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param userId owner user ID
	 * @param plate license plate
	 * @param type vehicle type
	 * @return assigned space, or null if no space is available
	 */
	public ParkingSpace handleUserVehicleEntry(int userId, String plate, VehicleType type) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				ParkingSpace assigned = handleUserVehicleEntryInTransaction(userId, plate, type);

				commitTransaction();
				return assigned;
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Handles a vehicle entering the parking lot. A reserved plate uses its reserved space; otherwise the
	 * first compatible available space is used. This method synchronizes the transaction because entry checks
	 * availability and then changes a parking space. Those steps must not be interleaved with another vehicle
	 * entry, exit, or simulation update.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param plate license plate of the entering vehicle
	 * @param type vehicle type
	 * @return assigned space, or null if the lot has no compatible space
	 */
	public ParkingSpace handleVehicleEntry(String plate, VehicleType type) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				ParkingSpace assigned = handleVehicleEntryInTransaction(plate, type);

				commitTransaction();
				return assigned;
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Handles a vehicle exiting the parking lot. This method synchronizes the transaction because it searches
	 * for the parked vehicle, frees its space, and removes any active reservation that was already used by
	 * that vehicle. Those steps must see the same parking state.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param plate license plate of the exiting vehicle
	 */
	public void handleVehicleExit(String plate) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();
				handleVehicleExitInTransaction(plate);
				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Handles exit for a logged-in user. This method synchronizes the transaction because it verifies that the
	 * parked vehicle belongs to the user before freeing the space and removing any used reservation. The
	 * verification and update must happen together.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param userId owner user ID
	 * @param plate license plate to exit with
	 * @return freed space, or null if the vehicle is not parked for that user
	 */
	public ParkingSpace handleUserVehicleExit(int userId, String plate) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				ParkingSpace freedSpace = handleUserVehicleExitInTransaction(userId, plate);

				commitTransaction();
				return freedSpace;
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Gets the current parking status.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current parking status
	 */
	public List<ParkingSpace> getParkingStatus() {
		return loadAllSpaces();
	}

	/**
	 * Registers the user's vehicle if needed, then parks it in the same transaction. The operation is kept
	 * together so the stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 * @param plate license plate involved in the operation
	 * @param type vehicle type involved in the operation
	 * @return the result of the operation
	 */
	private ParkingSpace handleUserVehicleEntryInTransaction(int userId, String plate, VehicleType type) {
		if (userId <= 0) {
			throw new IllegalArgumentException("No logged-in user was found.");
		}

		Vehicle vehicle = findVehicleByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, String.valueOf(userId), false);
			saveVehicleRecord(vehicle);
		} else if (!userOwnsVehicle(userId, plate)) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is registered to another user.");
		}

		return handleVehicleEntryInTransaction(plate, type);
	}

	/**
	 * Parks a vehicle while the caller owns the transaction. The operation is kept together so the stored data
	 * remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @param type vehicle type involved in the operation
	 * @return the result of the operation
	 */
	private ParkingSpace handleVehicleEntryInTransaction(String plate, VehicleType type) {
		Reservation reservation = findReservationByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			ParkingSpace reservedSpace = reservation.getParkingSpace();
			if (reservedSpace != null) {
				if (reservedSpace.isOccupied()) {
					throw new IllegalStateException("The reserved space is already occupied.");
				}

				Vehicle vehicle = findVehicleByPlate(plate);
				if (vehicle == null) {
					vehicle = new Vehicle(plate, type, null, false);
				}

				reservedSpace.occupy(vehicle);
				updateParkingSpaceRecord(reservedSpace);
				return reservedSpace;
			}
		}

		List<ParkingSpace> available = findAvailableSpacesByType(type);
		if (available == null || available.isEmpty()) return null;

		ParkingSpace space = available.get(0);
		Vehicle vehicle = findVehicleByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, null, false);
		}

		space.occupy(vehicle);
		updateParkingSpaceRecord(space);
		return space;
	}

	/**
	 * Frees the space occupied by a plate while the caller owns the transaction. The operation is kept
	 * together so the stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 */
	private void handleVehicleExitInTransaction(String plate) {
		List<ParkingSpace> allSpaces = loadAllSpaces();
		for (ParkingSpace space : allSpaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& plate.equals(space.getParkedVehicle().getLicensePlate())) {
				space.freeSpace();
				deleteUsedReservationForPlate(plate, space);
				updateParkingSpaceRecord(space);
				return;
			}
		}
	}

	/**
	 * Frees a user's parked vehicle while the caller owns the transaction. The operation is kept together so
	 * the stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 * @param plate license plate involved in the operation
	 * @return the result of the operation
	 */
	private ParkingSpace handleUserVehicleExitInTransaction(int userId, String plate) {
		List<ParkingSpace> parkedSpaces = getParkedSpacesByUser(userId);
		for (ParkingSpace space : parkedSpaces) {
			if (space.getParkedVehicle() != null
					&& plate.equals(space.getParkedVehicle().getLicensePlate())) {
				space.freeSpace();
				deleteUsedReservationForPlate(plate, space);
				updateParkingSpaceRecord(space);
				return space;
			}
		}
		return null;
	}

	/**
	 * Deletes used reservation for plate.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @param space space used by this operation
	 */
	private void deleteUsedReservationForPlate(String plate, ParkingSpace space) {
		Reservation reservation = findReservationByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			deleteReservationRecord(reservation.getId());
			if (space != null) {
				space.cancelReservation();
			}
		}
	}

	/**
	 * Handles user owns vehicle.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 * @param plate license plate involved in the operation
	 * @return the result of the operation
	 */
	private boolean userOwnsVehicle(int userId, String plate) {
		List<Vehicle> vehicles = findVehiclesByUser(userId);
		for (Vehicle vehicle : vehicles) {
			if (plate.equals(vehicle.getLicensePlate())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Handles generate next code for floor.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param floor floor used by this operation
	 * @return the result of the operation
	 */
	private String generateNextCodeForFloor(int floor) {
		String prefix = floorPrefix(floor) + "-";
		TreeSet<Integer> used = new TreeSet<>();
		for (ParkingSpace existing : loadAllSpaces()) {
			String id = existing.getId();
			if (id == null || !id.startsWith(prefix)) continue;
			try {
				used.add(Integer.parseInt(id.substring(prefix.length())));
			} catch (NumberFormatException ignored) {
			}
		}

		int next = 1;
		while (used.contains(next)) next++;
		return prefix + String.format("%02d", next);
	}

	/**
	 * Handles floor prefix.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param floor floor used by this operation
	 * @return the result of the operation
	 */
	private String floorPrefix(int floor) {
		int normalized = Math.max(0, floor - 1) % 26;
		return String.valueOf((char) ('A' + normalized));
	}

	/**
	 * Loads all spaces.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @return the loaded all spaces
	 */
	private List<ParkingSpace> loadAllSpaces() {
		return parkingSpaceDAO.findAll();
	}

	/**
	 * Finds space by code.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param code parking space code involved in the operation
	 * @return the matching space by code, or null when it is not found
	 */
	private ParkingSpace findSpaceByCode(String code) {
		return parkingSpaceDAO.findByCode(code);
	}

	/**
	 * Handles save parking space record.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param space space used by this operation
	 */
	private void saveParkingSpaceRecord(ParkingSpace space) {
		parkingSpaceDAO.save(space);
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
	 * Updates parking space details record.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param space space used by this operation
	 */
	private void updateParkingSpaceDetailsRecord(ParkingSpace space) {
		parkingSpaceDAO.updateDetails(space);
	}

	/**
	 * Deletes parking space record.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param code parking space code involved in the operation
	 */
	private void deleteParkingSpaceRecord(String code) {
		parkingSpaceDAO.delete(code);
	}

	/**
	 * Finds available spaces by type.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param type vehicle type involved in the operation
	 * @return the matching available spaces by type, or null when it is not found
	 */
	private List<ParkingSpace> findAvailableSpacesByType(VehicleType type) {
		return parkingSpaceDAO.findAvailableByType(type);
	}

	/**
	 * Finds reservation by plate.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return the matching reservation by plate, or null when it is not found
	 */
	private Reservation findReservationByPlate(String plate) {
		return reservationDAO.findByPlate(plate);
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
	 * Finds vehicle by plate.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return the matching vehicle by plate, or null when it is not found
	 */
	private Vehicle findVehicleByPlate(String plate) {
		return vehicleDAO.findByPlate(plate);
	}

	/**
	 * Handles save vehicle record.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param vehicle vehicle used by this operation
	 */
	private void saveVehicleRecord(Vehicle vehicle) {
		vehicleDAO.save(vehicle);
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
