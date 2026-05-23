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
 */
public class ParkingService {
	private ParkingSpaceDAO parkingSpaceDAO;
	private VehicleDAO vehicleDAO;
	private ReservationDAO reservationDAO;
	private TransactionManager transactionManager;

	/**
	 * Constructs a new ParkingService.
	 *
	 * @param parkingSpaceDAO    DAO used for parking spaces
	 * @param vehicleDAO         DAO used for vehicles
	 * @param reservationDAO     DAO used for reservations
	 * @param transactionManager object that controls database transactions
	 */
	public ParkingService(ParkingSpaceDAO parkingSpaceDAO, VehicleDAO vehicleDAO,
			ReservationDAO reservationDAO, TransactionManager transactionManager) {
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.vehicleDAO = vehicleDAO;
		this.reservationDAO = reservationDAO;
		this.transactionManager = transactionManager;
	}

	/** Gets all parking spaces. */
	public List<ParkingSpace> getAllSpaces() {
		return parkingSpaceDAO.findAll();
	}

	/**
	 * Finds a parking space by its code.
	 *
	 * @param code space code
	 * @return matching space, or null if not found
	 */
	public ParkingSpace findByCode(String code) {
		return parkingSpaceDAO.findByCode(code);
	}

	/**
	 * Updates editable details for an existing space.
	 *
	 * @param space space with updated details
	 */
	public void updateParkingSpaceDetails(ParkingSpace space) {
		ParkingSpace existing = parkingSpaceDAO.findByCode(space.getId());
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

		parkingSpaceDAO.updateDetails(space);
	}

	/**
	 * Creates a new empty parking space.
	 *
	 * @param space space to create
	 */
	public void createParkingSpace(ParkingSpace space) {
		if (space == null || space.getVehicleType() == null) {
			throw new IllegalArgumentException("Vehicle type is required.");
		}

		if (space.getId() == null || space.getId().isBlank()) {
			space.setId(generateNextCodeForFloor(space.getFloor()));
		} else if (parkingSpaceDAO.findByCode(space.getId()) != null) {
			throw new IllegalArgumentException("Space code already exists: " + space.getId());
		}

		space.setOccupied(false);
		space.setReserved(false);
		space.setParkedVehicle(null);
		space.cancelReservation();
		parkingSpaceDAO.save(space);
	}

	/**
	 * Updates the occupied state of a parking space.
	 *
	 * @param space space with updated state
	 */
	public void updateParkingSpace(ParkingSpace space) {
		parkingSpaceDAO.update(space);
	}

	/**
	 * Deletes a vacant and unreserved parking space.
	 *
	 * @param code space code to delete
	 * @return true if the space was deleted
	 */
	public boolean deleteParkingSpace(String code) {
		ParkingSpace space = parkingSpaceDAO.findByCode(code);
		if (space == null || space.isOccupied() || space.isReserved()) return false;

		parkingSpaceDAO.delete(code);
		return true;
	}

	/**
	 * Finds all spaces that can receive a vehicle of the given type.
	 *
	 * @param type vehicle type
	 * @return available spaces
	 */
	public List<ParkingSpace> findAvailableSpaces(VehicleType type) {
		return parkingSpaceDAO.findAvailableByType(type);
	}

	/**
	 * Finds the active reservation for a license plate.
	 *
	 * @param plate license plate to check
	 * @return active reservation, or null if none exists
	 */
	public Reservation findActiveReservationByPlate(String plate) {
		if (plate == null || plate.isBlank()) return null;

		Reservation reservation = reservationDAO.findByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			return reservation;
		}
		return null;
	}

	/**
	 * Finds the occupied space for a license plate.
	 *
	 * @param plate license plate to check
	 * @return occupied space, or null if the vehicle is not parked
	 */
	public ParkingSpace findOccupiedSpaceByPlate(String plate) {
		if (plate == null || plate.isBlank()) return null;

		List<ParkingSpace> allSpaces = parkingSpaceDAO.findAll();
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
	 *
	 * @param userId owner user ID
	 * @return occupied spaces for that user
	 */
	public List<ParkingSpace> getParkedSpacesByUser(int userId) {
		List<ParkingSpace> parkedSpaces = new ArrayList<>();
		if (userId <= 0) return parkedSpaces;

		List<Vehicle> vehicles = vehicleDAO.findByUser(userId);
		Set<String> userPlates = new HashSet<>();
		for (Vehicle vehicle : vehicles) {
			userPlates.add(vehicle.getLicensePlate());
		}

		List<ParkingSpace> spaces = parkingSpaceDAO.findAll();
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
	 * Assigns a vehicle to a space using the normal entry rules.
	 *
	 * @param vehicle vehicle to park
	 * @return assigned space, or null if no space is available
	 */
	public ParkingSpace assignVehicleToSpace(Vehicle vehicle) {
		return handleVehicleEntry(vehicle.getLicensePlate(), vehicle.getType());
	}

	/**
	 * Handles entry for a logged-in user and registers new vehicles to that user.
	 * This method synchronizes the transaction because it may create a vehicle,
	 * check ownership, and occupy a space as one database operation. Without this,
	 * the simulation thread or another UI action could change the same space between
	 * those steps.
	 *
	 * @param userId owner user ID
	 * @param plate  license plate
	 * @param type   vehicle type
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
	 * Handles a vehicle entering the parking lot.
	 * A reserved plate uses its reserved space; otherwise the first compatible
	 * available space is used.
	 * This method synchronizes the transaction because entry checks availability
	 * and then changes a parking space. Those steps must not be interleaved with
	 * another vehicle entry, exit, or simulation update.
	 *
	 * @param plate license plate of the entering vehicle
	 * @param type  vehicle type
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
	 * Handles a vehicle exiting the parking lot.
	 * This method synchronizes the transaction because it searches for the parked
	 * vehicle, frees its space, and removes any active reservation that was already
	 * used by that vehicle. Those steps must see the same parking state.
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
	 * Handles exit for a logged-in user.
	 * This method synchronizes the transaction because it verifies that the parked
	 * vehicle belongs to the user before freeing the space and removing any used
	 * reservation. The verification and update must happen together.
	 *
	 * @param userId owner user ID
	 * @param plate  license plate to exit with
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

	/** Gets the current parking status. */
	public List<ParkingSpace> getParkingStatus() {
		return parkingSpaceDAO.findAll();
	}

	/** Registers the user's vehicle if needed, then parks it in the same transaction. */
	private ParkingSpace handleUserVehicleEntryInTransaction(int userId, String plate, VehicleType type) {
		if (userId <= 0) {
			throw new IllegalArgumentException("No logged-in user was found.");
		}

		Vehicle vehicle = vehicleDAO.findByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, String.valueOf(userId), false);
			vehicleDAO.save(vehicle);
		} else if (!userOwnsVehicle(userId, plate)) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is registered to another user.");
		}

		return handleVehicleEntryInTransaction(plate, type);
	}

	/** Parks a vehicle while the caller owns the transaction. */
	private ParkingSpace handleVehicleEntryInTransaction(String plate, VehicleType type) {
		Reservation reservation = reservationDAO.findByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			ParkingSpace reservedSpace = reservation.getParkingSpace();
			if (reservedSpace != null) {
				if (reservedSpace.isOccupied()) {
					throw new IllegalStateException("The reserved space is already occupied.");
				}

				Vehicle vehicle = vehicleDAO.findByPlate(plate);
				if (vehicle == null) {
					vehicle = new Vehicle(plate, type, null, false);
				}

				reservedSpace.occupy(vehicle);
				parkingSpaceDAO.update(reservedSpace);
				return reservedSpace;
			}
		}

		List<ParkingSpace> available = parkingSpaceDAO.findAvailableByType(type);
		if (available == null || available.isEmpty()) return null;

		ParkingSpace space = available.get(0);
		Vehicle vehicle = vehicleDAO.findByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, null, false);
		}

		space.occupy(vehicle);
		parkingSpaceDAO.update(space);
		return space;
	}

	/** Frees the space occupied by a plate while the caller owns the transaction. */
	private void handleVehicleExitInTransaction(String plate) {
		List<ParkingSpace> allSpaces = parkingSpaceDAO.findAll();
		for (ParkingSpace space : allSpaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& plate.equals(space.getParkedVehicle().getLicensePlate())) {
				space.freeSpace();
				deleteUsedReservationForPlate(plate, space);
				parkingSpaceDAO.update(space);
				return;
			}
		}
	}

	/** Frees a user's parked vehicle while the caller owns the transaction. */
	private ParkingSpace handleUserVehicleExitInTransaction(int userId, String plate) {
		List<ParkingSpace> parkedSpaces = getParkedSpacesByUser(userId);
		for (ParkingSpace space : parkedSpaces) {
			if (space.getParkedVehicle() != null
					&& plate.equals(space.getParkedVehicle().getLicensePlate())) {
				space.freeSpace();
				deleteUsedReservationForPlate(plate, space);
				parkingSpaceDAO.update(space);
				return space;
			}
		}
		return null;
	}

	/** Deletes the active reservation for a plate after the reserved vehicle leaves. */
	private void deleteUsedReservationForPlate(String plate, ParkingSpace space) {
		Reservation reservation = reservationDAO.findByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			reservationDAO.delete(reservation.getId());
			if (space != null) {
				space.cancelReservation();
			}
		}
	}

	/** Checks whether a plate belongs to the given user. */
	private boolean userOwnsVehicle(int userId, String plate) {
		List<Vehicle> vehicles = vehicleDAO.findByUser(userId);
		for (Vehicle vehicle : vehicles) {
			if (plate.equals(vehicle.getLicensePlate())) {
				return true;
			}
		}
		return false;
	}

	/** Generates the next free parking-space code for a floor. */
	private String generateNextCodeForFloor(int floor) {
		String prefix = floorPrefix(floor) + "-";
		TreeSet<Integer> used = new TreeSet<>();
		for (ParkingSpace existing : parkingSpaceDAO.findAll()) {
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

	/** Converts a floor number into the letter prefix used by space codes. */
	private String floorPrefix(int floor) {
		int normalized = Math.max(0, floor - 1) % 26;
		return String.valueOf((char) ('A' + normalized));
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
