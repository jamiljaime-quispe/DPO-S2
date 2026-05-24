package Business.Services;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;
import Persistence.ReservationDAO;
import Persistence.TransactionManager;

import java.util.List;

/**
 * Provides admin-only actions for reservations and parking spaces.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class AdminService {
	private ParkingService parkingService;
	private ReservationDAO reservationDAO;
	private TransactionManager transactionManager;

	/**
	 * Constructs a new AdminService.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param parkingService service used for parking-space changes
	 * @param reservationDAO DAO used for reservation access
	 * @param transactionManager object that controls database transactions
	 */
	public AdminService(ParkingService parkingService, ReservationDAO reservationDAO,
			TransactionManager transactionManager) {
		this.parkingService = parkingService;
		this.reservationDAO = reservationDAO;
		this.transactionManager = transactionManager;
	}

	/**
	 * Deletes a vacant parking space. If the space has an active reservation, the reservation is moved to
	 * another compatible space. If none exists, the reservation is cancelled and the user will be notified on
	 * their next login. This method synchronizes the transaction because deleting a space may also move or
	 * cancel a reservation. Those related database changes must not be interrupted by another parking update.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param spaceCode code of the space being removed
	 * @return the result of the operation
	 */
	public DeleteParkingSpaceResult deleteParkingSpace(String spaceCode) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				ParkingSpace space = findParkingSpace(spaceCode);
				if (space == null) {
					throw new IllegalArgumentException("Space not found: " + spaceCode);
				}
				if (space.isOccupied()) {
					throw new IllegalStateException("Cannot delete space \"" + spaceCode
							+ "\": it is currently occupied.");
				}

				ReservationMoveResult reservationResult = reassignOrCancelReservationInTransaction(spaceCode);
				if (!deleteParkingSpaceThroughService(spaceCode)) {
					throw new IllegalStateException("Could not delete space \"" + spaceCode + "\".");
				}

				commitTransaction();
				return buildDeleteParkingSpaceResult(spaceCode, reservationResult);
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Reassigns the active reservation for a space, or cancels it when no suitable. replacement exists. This
	 * method synchronizes the transaction because the old reservation and the replacement space are chosen and
	 * saved together.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param spaceCode code of the space being removed
	 */
	public void reassignOrCancelReservation(String spaceCode) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();
				reassignOrCancelReservationInTransaction(spaceCode);
				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Cancels the active reservation associated with a license plate. This method synchronizes the transaction
	 * because it finds the active booking and frees the reserved space in one protected operation.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param plate license plate associated with the booking
	 * @return true if an active booking was cancelled
	 */
	public boolean cancelReservationByPlate(String plate) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = findReservationByPlate(plate);
				if (reservation == null || !reservation.isActive()) {
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
	 * Reassigns or cancels a reservation while the caller owns the transaction. The operation is kept together
	 * so the stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param spaceCode parking space code involved in the operation
	 * @return the result of the operation
	 */
	private ReservationMoveResult reassignOrCancelReservationInTransaction(String spaceCode) {
		Reservation target = null;
		for (Reservation reservation : findAllReservations()) {
			if (reservation.isActive()
					&& reservation.getParkingSpace() != null
					&& spaceCode.equals(reservation.getParkingSpace().getId())) {
				target = reservation;
				break;
			}
		}
		if (target == null) return new ReservationMoveResult(null, null, false);

		ParkingSpace oldSpace = target.getParkingSpace();
		String affectedPlate = target.getVehicle() != null ? target.getVehicle().getLicensePlate() : null;
		VehicleType type = target.getVehicle() != null ? target.getVehicle().getType() : null;
		if (type == null && oldSpace != null) {
			type = oldSpace.getVehicleType();
		}

		List<ParkingSpace> alternatives = type != null
				? findAvailableSpaces(type)
				: null;
		ParkingSpace newSpace = findBestAlternativeSpace(alternatives, spaceCode, oldSpace);

		target.setPreviousSpaceCode(spaceCode);
		target.setCancelledByAdmin(true);
		target.setNotified(false);

		if (newSpace != null) {
			target.setParkingSpace(newSpace);
		} else {
			target.setParkingSpace(null);
			target.cancel();
		}

		updateReservation(target);
		return new ReservationMoveResult(affectedPlate, newSpace != null ? newSpace.getId() : null, newSpace == null);
	}

	/**
	 * Builds delete parking space result.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param spaceCode parking space code involved in the operation
	 * @param reservationResult reservation result used by this operation
	 * @return the built delete parking space result
	 */
	private DeleteParkingSpaceResult buildDeleteParkingSpaceResult(String spaceCode,
			ReservationMoveResult reservationResult) {
		if (reservationResult == null) {
			return new DeleteParkingSpaceResult(spaceCode, null, null, false);
		}

		return new DeleteParkingSpaceResult(spaceCode, reservationResult.getAffectedPlate(),
				reservationResult.getNewSpaceCode(), reservationResult.isReservationCancelled());
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
		reservation.setCancelledByAdmin(true);
		reservation.setNotified(false);
		updateReservation(reservation);

		ParkingSpace space = reservation.getParkingSpace();
		if (space != null) {
			space.cancelReservation();
			updateParkingSpace(space);
		}
	}

	/**
	 * Finds best alternative space.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param alternatives alternatives used by this operation
	 * @param deletedCode deleted code used by this operation
	 * @param oldSpace old space used by this operation
	 * @return the matching best alternative space, or null when it is not found
	 */
	private ParkingSpace findBestAlternativeSpace(List<ParkingSpace> alternatives, String deletedCode,
			ParkingSpace oldSpace) {
		if (alternatives == null) return null;

		ParkingSpace fallback = null;
		int preferredFloor = oldSpace != null ? oldSpace.getFloor() : -1;

		for (ParkingSpace alternative : alternatives) {
			if (alternative.getId().equals(deletedCode)) {
				continue;
			}
			if (alternative.getFloor() == preferredFloor) {
				return alternative;
			}
			if (fallback == null) {
				fallback = alternative;
			}
		}

		return fallback;
	}

	/**
	 * Finds parking space.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param spaceCode parking space code involved in the operation
	 * @return the matching parking space, or null when it is not found
	 */
	private ParkingSpace findParkingSpace(String spaceCode) {
		return parkingService.findByCode(spaceCode);
	}

	/**
	 * Deletes parking space through service.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param spaceCode parking space code involved in the operation
	 * @return the result of the operation
	 */
	private boolean deleteParkingSpaceThroughService(String spaceCode) {
		return parkingService.deleteParkingSpace(spaceCode);
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
	 * Finds all reservations.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @return the matching all reservations, or null when it is not found
	 */
	private List<Reservation> findAllReservations() {
		return reservationDAO.findAll();
	}

	/**
	 * Finds available spaces.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param type vehicle type involved in the operation
	 * @return the matching available spaces, or null when it is not found
	 */
	private List<ParkingSpace> findAvailableSpaces(VehicleType type) {
		return parkingService.findAvailableSpaces(type);
	}

	/**
	 * Updates reservation.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param reservation reservation used by this operation
	 */
	private void updateReservation(Reservation reservation) {
		reservationDAO.update(reservation);
	}

	/**
	 * Updates a parking space.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 *
	 * @param space space used by this operation
	 */
	private void updateParkingSpace(ParkingSpace space) {
		parkingService.updateParkingSpace(space);
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
