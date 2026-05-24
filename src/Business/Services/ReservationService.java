package Business.Services;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.TransactionManager;
import Persistence.VehicleDAO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages parking-space reservations for regular users and admins.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class ReservationService {
	private ReservationDAO reservationDAO;
	private ParkingSpaceDAO parkingSpaceDAO;
	private VehicleDAO vehicleDAO;
	private TransactionManager transactionManager;

	/**
	 * Constructs a new ReservationService.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param reservationDAO DAO used for reservations
	 * @param parkingSpaceDAO DAO used for parking spaces
	 * @param vehicleDAO DAO used for vehicles
	 * @param transactionManager object that controls database transactions
	 */
	public ReservationService(ReservationDAO reservationDAO, ParkingSpaceDAO parkingSpaceDAO,
			VehicleDAO vehicleDAO, TransactionManager transactionManager) {
		this.reservationDAO = reservationDAO;
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.vehicleDAO = vehicleDAO;
		this.transactionManager = transactionManager;
	}

	/**
	 * Creates a reservation for a vehicle in a selected space. This method synchronizes the transaction
	 * because it checks the space, checks the vehicle, may create the vehicle, and saves the reservation.
	 * Those steps must be protected from simulation or user actions that could take the same space at the same
	 * time.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param userId ID of the user making the reservation
	 * @param plate license plate of the vehicle
	 * @param type vehicle type chosen by the user
	 * @param spaceCode code of the space to reserve
	 * @return the created reservation
	 */
	public Reservation createReservation(int userId, String plate, VehicleType type, String spaceCode) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = createReservationInTransaction(userId, plate, type, spaceCode);

				commitTransaction();
				return reservation;
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Reassigns an active reservation to a different available space. This method synchronizes the transaction
	 * because the reservation and the target space are validated together before the reservation is moved.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param plate license plate whose active reservation should be moved
	 * @param newSpaceCode target parking space code
	 * @return updated reservation
	 */
	public Reservation reassignReservation(String plate, String newSpaceCode) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = findReservationByPlate(plate);
				if (reservation == null || !reservation.isActive()) {
					throw new IllegalArgumentException("No active reservation found for license plate " + plate + ".");
				}

				ParkingSpace newSpace = findSpaceByCode(newSpaceCode);
				String currentSpaceCode = reservation.getParkingSpace().getId();
				validateSpaceCanReceiveReassignedBooking(newSpace, newSpaceCode, currentSpaceCode);
				if (reservation.getVehicle() != null && reservation.getVehicle().getType() != newSpace.getVehicleType()) {
					throw new IllegalArgumentException("Parking space " + newSpaceCode
							+ " only accepts " + newSpace.getVehicleType().name() + " vehicles.");
				}

				reservation.setParkingSpace(newSpace);
				updateReservationRecord(reservation);

				commitTransaction();
				return reservation;
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}


	/**
	 * Cancels the active reservation for a plate only if it belongs to the user. This method synchronizes the
	 * transaction because ownership validation and cancellation must not be separated by another update.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param userId user requesting the cancellation
	 * @param plate license plate whose reservation should be cancelled
	 * @return true if a matching active reservation was cancelled
	 */
	public boolean cancelReservationByPlateForUser(int userId, String plate) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = findReservationByPlate(plate);
				if (reservation == null || !reservation.isActive() || reservation.getUser() == null) {
					commitTransaction();
					return false;
				}

				int reservationUserId = Integer.parseInt(reservation.getUser().getId());
				if (reservationUserId != userId) {
					commitTransaction();
					return false;
				}

				cancelReservationInTransaction(reservation);
				commitTransaction();
				return true;
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Returns all reservations belonging to a user.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @param userId user ID
	 * @return list of reservations
	 */
	public List<Reservation> getReservationsByUser(int userId) {
		return findReservationsByUser(userId);
	}

	/**
	 * Returns all spaces available for reservation for the given vehicle type.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @param type vehicle type
	 * @return list of available spaces
	 */
	public List<ParkingSpace> getAvailableSpaces(VehicleType type) {
		return findAvailableSpacesByType(type);
	}

	/**
	 * Returns admin-cancelled reservations the user has not seen yet.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @param userId user ID
	 * @return list of pending notifications
	 */
	public List<Reservation> getCancelledByAdminNotNotified(int userId) {
		List<Reservation> all = findReservationsByUser(userId);
		List<Reservation> pending = new ArrayList<>();
		for (Reservation reservation : all) {
			if (reservation.isCancelledByAdmin() && !reservation.isNotified()) {
				pending.add(reservation);
			}
		}
		return pending;
	}

	/**
	 * Handles mark notified.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param reservation the reservation to mark
	 */
	public void markNotified(Reservation reservation) {
		reservation.setNotified(true);
		updateReservationRecord(reservation);
	}

	/**
	 * Creates a reservation while the caller owns the transaction. The operation is kept together so the
	 * stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 * @param plate license plate involved in the operation
	 * @param type vehicle type involved in the operation
	 * @param spaceCode parking space code involved in the operation
	 * @return the created reservation in transaction
	 */
	private Reservation createReservationInTransaction(int userId, String plate, VehicleType type, String spaceCode) {
		if (userId <= 0) {
			throw new IllegalArgumentException("No logged-in user was found.");
		}

		ParkingSpace space = findSpaceByCode(spaceCode);
		validateSpaceCanBeBooked(space, spaceCode);
		if (space.getVehicleType() != type) {
			throw new IllegalArgumentException("Parking space " + spaceCode
					+ " only accepts " + space.getVehicleType().name() + " vehicles.");
		}

		Reservation activeReservation = findReservationByPlate(plate);
		if (activeReservation != null && activeReservation.isActive()) {
			throw new IllegalArgumentException("License plate " + plate + " already has an active reservation.");
		}

		ParkingSpace occupiedSpace = findOccupiedSpaceByPlate(plate);
		if (occupiedSpace != null) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is already parked in space " + occupiedSpace.getId() + ".");
		}

		Vehicle vehicle = findVehicleByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, String.valueOf(userId), false);
			saveVehicleRecord(vehicle);
		} else if (!userOwnsVehicle(userId, plate)) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is registered to another user.");
		} else if (vehicle.getType() != type) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is registered as " + vehicle.getType().name() + ".");
		}

		Reservation reservation = new Reservation(0, null, vehicle, space, LocalDateTime.now());
		space.reserve(reservation);
		saveReservationRecord(reservation);
		return reservation;
	}

	/**
	 * Cancels a reservation while the caller owns the transaction. The operation is kept together so the
	 * stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param reservation reservation used by this operation
	 */
	private void cancelReservationInTransaction(Reservation reservation) {
		reservation.cancel();
		updateReservationRecord(reservation);

		ParkingSpace space = reservation.getParkingSpace();
		if (space != null) {
			space.cancelReservation();
			updateParkingSpaceRecord(space);
		}
	}

	/**
	 * Handles validate space can be booked.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param space space used by this operation
	 * @param spaceCode parking space code involved in the operation
	 */
	private void validateSpaceCanBeBooked(ParkingSpace space, String spaceCode) {
		if (space == null) {
			throw new IllegalArgumentException("Parking space not found: " + spaceCode);
		}
		if (space.isOccupied()) {
			throw new IllegalArgumentException(buildOccupiedSpaceMessage(space, spaceCode));
		}
		if (space.isReserved()) {
			throw new IllegalArgumentException("Parking space " + spaceCode + " is already booked.");
		}
	}

	/**
	 * Handles validate space can receive reassigned booking.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param space target parking space
	 * @param spaceCode target parking space code
	 * @param currentSpaceCode space where the reservation is currently placed
	 */
	private void validateSpaceCanReceiveReassignedBooking(ParkingSpace space, String spaceCode,
			String currentSpaceCode) {
		if (space == null) {
			throw new IllegalArgumentException("Parking space not found: " + spaceCode);
		}
		if (space.isOccupied()) {
			throw new IllegalArgumentException(buildOccupiedSpaceMessage(space, spaceCode));
		}
		if (space.isReserved() && !currentSpaceCode.equals(spaceCode)) {
			throw new IllegalArgumentException("Parking space " + spaceCode + " is already booked.");
		}
	}

	/**
	 * Builds occupied space message.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param space space used by this operation
	 * @param spaceCode parking space code involved in the operation
	 * @return the built occupied space message
	 */
	private String buildOccupiedSpaceMessage(ParkingSpace space, String spaceCode) {
		if (space.getParkedVehicle() != null) {
			return "Parking space " + spaceCode + " cannot be booked because vehicle "
					+ space.getParkedVehicle().getLicensePlate() + " is parked there.";
		}
		return "Parking space " + spaceCode + " cannot be booked because a vehicle is parked there.";
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
			if (plate.equalsIgnoreCase(vehicle.getLicensePlate())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds occupied space by plate.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return the matching occupied space by plate, or null when it is not found
	 */
	private ParkingSpace findOccupiedSpaceByPlate(String plate) {
		List<ParkingSpace> spaces = loadAllSpaces();
		for (ParkingSpace space : spaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& plate.equalsIgnoreCase(space.getParkedVehicle().getLicensePlate())) {
				return space;
			}
		}
		return null;
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
	 * Handles save reservation record.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param reservation reservation used by this operation
	 */
	private void saveReservationRecord(Reservation reservation) {
		reservationDAO.save(reservation);
	}

	/**
	 * Updates reservation record.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param reservation reservation used by this operation
	 */
	private void updateReservationRecord(Reservation reservation) {
		reservationDAO.update(reservation);
	}

	/**
	 * Finds space by code.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param spaceCode parking space code involved in the operation
	 * @return the matching space by code, or null when it is not found
	 */
	private ParkingSpace findSpaceByCode(String spaceCode) {
		return parkingSpaceDAO.findByCode(spaceCode);
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
