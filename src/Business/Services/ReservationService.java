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
 */
public class ReservationService {
	private ReservationDAO reservationDAO;
	private ParkingSpaceDAO parkingSpaceDAO;
	private VehicleDAO vehicleDAO;
	private TransactionManager transactionManager;

	/**
	 * Constructs a new ReservationService.
	 *
	 * @param reservationDAO     DAO used for reservations
	 * @param parkingSpaceDAO    DAO used for parking spaces
	 * @param vehicleDAO         DAO used for vehicles
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
	 * Creates a reservation for a vehicle in a selected space.
	 * This method synchronizes the transaction because it checks the space, checks
	 * the vehicle, may create the vehicle, and saves the reservation. Those steps
	 * must be protected from simulation or user actions that could take the same
	 * space at the same time.
	 *
	 * @param userId    ID of the user making the reservation
	 * @param plate     license plate of the vehicle
	 * @param type      vehicle type chosen by the user
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
	 * Reassigns an active reservation to a different available space.
	 * This method synchronizes the transaction because the reservation and the
	 * target space are validated together before the reservation is moved.
	 *
	 * @param plate        license plate whose active reservation should be moved
	 * @param newSpaceCode target parking space code
	 * @return updated reservation
	 */
	public Reservation reassignReservation(String plate, String newSpaceCode) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = reservationDAO.findByPlate(plate);
				if (reservation == null || !reservation.isActive()) {
					throw new IllegalArgumentException("No active reservation found for license plate " + plate + ".");
				}

				ParkingSpace newSpace = parkingSpaceDAO.findByCode(newSpaceCode);
				String currentSpaceCode = reservation.getParkingSpace().getId();
				validateSpaceCanReceiveReassignedBooking(newSpace, newSpaceCode, currentSpaceCode);
				if (reservation.getVehicle() != null && reservation.getVehicle().getType() != newSpace.getVehicleType()) {
					throw new IllegalArgumentException("Parking space " + newSpaceCode
							+ " only accepts " + newSpace.getVehicleType().name() + " vehicles.");
				}

				reservation.setParkingSpace(newSpace);
				reservationDAO.update(reservation);

				commitTransaction();
				return reservation;
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Cancels a reservation by ID.
	 * This method synchronizes the transaction because cancelling the reservation
	 * and freeing its space must happen as one consistent change.
	 *
	 * @param reservationId ID of the reservation to cancel
	 */
	public void cancelReservation(int reservationId) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = reservationDAO.findById(reservationId);
				if (reservation != null && reservation.isActive()) {
					cancelReservationInTransaction(reservation);
				}

				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Cancels the active reservation for a license plate.
	 * This method synchronizes the transaction because it finds the active
	 * reservation and updates its parking space in the same operation.
	 *
	 * @param plate license plate of the vehicle whose reservation should be cancelled
	 */
	public void cancelReservationByPlate(String plate) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = reservationDAO.findByPlate(plate);
				if (reservation != null && reservation.isActive()) {
					cancelReservationInTransaction(reservation);
				}

				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Cancels the active reservation for a plate only if it belongs to the user.
	 * This method synchronizes the transaction because ownership validation and
	 * cancellation must not be separated by another update.
	 *
	 * @param userId user requesting the cancellation
	 * @param plate  license plate whose reservation should be cancelled
	 * @return true if a matching active reservation was cancelled
	 */
	public boolean cancelReservationByPlateForUser(int userId, String plate) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = reservationDAO.findByPlate(plate);
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
	 *
	 * @param userId user ID
	 * @return list of reservations
	 */
	public List<Reservation> getReservationsByUser(int userId) {
		return reservationDAO.findByUser(userId);
	}

	/**
	 * Returns all spaces available for reservation for the given vehicle type.
	 *
	 * @param type vehicle type
	 * @return list of available spaces
	 */
	public List<ParkingSpace> getAvailableSpaces(VehicleType type) {
		return parkingSpaceDAO.findAvailableByType(type);
	}

	/**
	 * Returns admin-cancelled reservations the user has not seen yet.
	 *
	 * @param userId user ID
	 * @return list of pending notifications
	 */
	public List<Reservation> getCancelledByAdminNotNotified(int userId) {
		List<Reservation> all = reservationDAO.findByUser(userId);
		List<Reservation> pending = new ArrayList<>();
		for (Reservation reservation : all) {
			if (reservation.isCancelledByAdmin() && !reservation.isNotified()) {
				pending.add(reservation);
			}
		}
		return pending;
	}

	/**
	 * Marks a reservation notification as seen by the user.
	 *
	 * @param reservation the reservation to mark
	 */
	public void markNotified(Reservation reservation) {
		reservation.setNotified(true);
		reservationDAO.update(reservation);
	}

	/**
	 * Checks whether a space is available for reservation.
	 *
	 * @param spaceCode space code
	 * @return true if the space exists and is available
	 */
	public boolean isSpaceAvailable(String spaceCode) {
		ParkingSpace space = parkingSpaceDAO.findByCode(spaceCode);
		return space != null && space.isAvailable();
	}

	/** Creates a reservation while the caller owns the transaction. */
	private Reservation createReservationInTransaction(int userId, String plate, VehicleType type, String spaceCode) {
		if (userId <= 0) {
			throw new IllegalArgumentException("No logged-in user was found.");
		}

		ParkingSpace space = parkingSpaceDAO.findByCode(spaceCode);
		validateSpaceCanBeBooked(space, spaceCode);
		if (space.getVehicleType() != type) {
			throw new IllegalArgumentException("Parking space " + spaceCode
					+ " only accepts " + space.getVehicleType().name() + " vehicles.");
		}

		Reservation activeReservation = reservationDAO.findByPlate(plate);
		if (activeReservation != null && activeReservation.isActive()) {
			throw new IllegalArgumentException("License plate " + plate + " already has an active reservation.");
		}

		ParkingSpace occupiedSpace = findOccupiedSpaceByPlate(plate);
		if (occupiedSpace != null) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is already parked in space " + occupiedSpace.getId() + ".");
		}

		Vehicle vehicle = vehicleDAO.findByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, String.valueOf(userId), false);
			vehicleDAO.save(vehicle);
		} else if (!userOwnsVehicle(userId, plate)) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is registered to another user.");
		} else if (vehicle.getType() != type) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is registered as " + vehicle.getType().name() + ".");
		}

		Reservation reservation = new Reservation(0, null, vehicle, space, LocalDateTime.now());
		space.reserve(reservation);
		reservationDAO.save(reservation);
		return reservation;
	}

	/** Cancels a reservation while the caller owns the transaction. */
	private void cancelReservationInTransaction(Reservation reservation) {
		reservation.cancel();
		reservationDAO.update(reservation);

		ParkingSpace space = reservation.getParkingSpace();
		if (space != null) {
			space.cancelReservation();
			parkingSpaceDAO.update(space);
		}
	}

	/** Checks that a space can receive a new reservation. */
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

	/** Builds the message shown when a user tries to book an occupied space. */
	private String buildOccupiedSpaceMessage(ParkingSpace space, String spaceCode) {
		if (space.getParkedVehicle() != null) {
			return "Parking space " + spaceCode + " cannot be booked because vehicle "
					+ space.getParkedVehicle().getLicensePlate() + " is parked there.";
		}
		return "Parking space " + spaceCode + " cannot be booked because a vehicle is parked there.";
	}

	/** Checks whether a plate belongs to the given user. */
	private boolean userOwnsVehicle(int userId, String plate) {
		List<Vehicle> vehicles = vehicleDAO.findByUser(userId);
		for (Vehicle vehicle : vehicles) {
			if (plate.equalsIgnoreCase(vehicle.getLicensePlate())) {
				return true;
			}
		}
		return false;
	}

	/** Finds where a license plate is currently parked, if it is parked. */
	private ParkingSpace findOccupiedSpaceByPlate(String plate) {
		List<ParkingSpace> spaces = parkingSpaceDAO.findAll();
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
