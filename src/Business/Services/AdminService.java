package Business.Services;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;
import Persistence.ReservationDAO;
import Persistence.TransactionManager;

import java.util.List;

/**
 * Provides admin-only actions for reservations and parking spaces.
 */
public class AdminService {
	private ParkingService parkingService;
	private ReservationDAO reservationDAO;
	private TransactionManager transactionManager;

	/**
	 * Constructs a new AdminService.
	 *
	 * @param parkingService     service used for parking-space changes
	 * @param reservationDAO     DAO used for reservation access
	 * @param transactionManager object that controls database transactions
	 */
	public AdminService(ParkingService parkingService, ReservationDAO reservationDAO,
			TransactionManager transactionManager) {
		this.parkingService = parkingService;
		this.reservationDAO = reservationDAO;
		this.transactionManager = transactionManager;
	}

	/**
	 * Deletes a vacant parking space.
	 * If the space has an active reservation, the reservation is moved to another
	 * compatible space. If none exists, the reservation is cancelled and the user
	 * will be notified on their next login.
	 * This method synchronizes the transaction because deleting a space may also
	 * move or cancel a reservation. Those related database changes must not be
	 * interrupted by another parking update.
	 *
	 * @param spaceCode code of the space being removed
	 */
	public void deleteParkingSpace(String spaceCode) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				ParkingSpace space = parkingService.findByCode(spaceCode);
				if (space == null) {
					throw new IllegalArgumentException("Space not found: " + spaceCode);
				}
				if (space.isOccupied()) {
					throw new IllegalStateException("Cannot delete space \"" + spaceCode
							+ "\": it is currently occupied.");
				}

				reassignOrCancelReservationInTransaction(spaceCode);
				if (!parkingService.deleteParkingSpace(spaceCode)) {
					throw new IllegalStateException("Could not delete space \"" + spaceCode + "\".");
				}

				commitTransaction();
			} catch (RuntimeException e) {
				rollbackTransaction();
				throw e;
			}
		}
	}

	/**
	 * Reassigns the active reservation for a space, or cancels it when no suitable
	 * replacement exists.
	 * This method synchronizes the transaction because the old reservation and the
	 * replacement space are chosen and saved together.
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
	 * Older name kept so existing controller code can still call the same service.
	 *
	 * @param spaceCode code of the space being removed
	 */
	public void reassignOrDeleteReservation(String spaceCode) {
		reassignOrCancelReservation(spaceCode);
	}

	/**
	 * Cancels a reservation as an admin action.
	 * The user will be notified the next time they log in.
	 * This method synchronizes the transaction because the reservation state and
	 * the space state must be updated together.
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
	 * Cancels the active reservation associated with a license plate.
	 * This method synchronizes the transaction because it finds the active booking
	 * and frees the reserved space in one protected operation.
	 *
	 * @param plate license plate associated with the booking
	 * @return true if an active booking was cancelled
	 */
	public boolean cancelReservationByPlate(String plate) {
		synchronized (transactionLock()) {
			try {
				beginTransaction();

				Reservation reservation = reservationDAO.findByPlate(plate);
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

	/** Gets the current status of every parking space. */
	public List<ParkingSpace> getFullParkingStatus() {
		return parkingService.getParkingStatus();
	}

	/** Reassigns or cancels a reservation while the caller owns the transaction. */
	private void reassignOrCancelReservationInTransaction(String spaceCode) {
		Reservation target = null;
		for (Reservation reservation : reservationDAO.findAll()) {
			if (reservation.isActive()
					&& reservation.getParkingSpace() != null
					&& spaceCode.equals(reservation.getParkingSpace().getId())) {
				target = reservation;
				break;
			}
		}
		if (target == null) return;

		ParkingSpace oldSpace = target.getParkingSpace();
		VehicleType type = target.getVehicle() != null ? target.getVehicle().getType() : null;
		if (type == null && oldSpace != null) {
			type = oldSpace.getVehicleType();
		}

		List<ParkingSpace> alternatives = type != null
				? parkingService.findAvailableSpaces(type)
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

		reservationDAO.update(target);
	}

	/** Cancels a reservation while the caller owns the transaction. */
	private void cancelReservationInTransaction(Reservation reservation) {
		reservation.cancel();
		reservation.setCancelledByAdmin(true);
		reservation.setNotified(false);
		reservationDAO.update(reservation);

		ParkingSpace space = reservation.getParkingSpace();
		if (space != null) {
			space.cancelReservation();
			parkingService.updateParkingSpace(space);
		}
	}

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
