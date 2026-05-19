package Business.Services;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;
import Persistence.ReservationDAO;
import java.util.List;

/**
 * Provides admin-only operations: reservation cancellation with notification
 * and forced space removal with reservation reassignment.
 */
public class AdminService {
	private ParkingService parkingService;
	private ReservationDAO reservationDAO;

	/**
	 * @param parkingService  parking service for space operations
	 * @param reservationDAO  DAO for reservation access
	 */
	public AdminService(ParkingService parkingService, ReservationDAO reservationDAO) {
		this.parkingService = parkingService;
		this.reservationDAO = reservationDAO;
	}

	/**
	 * Called before deleting a space. Finds the active reservation for that space
	 * and either reassigns it to a compatible free space, or cancels it if none exists.
	 * @param spaceCode code of the space being removed
	 */
	public void reassignOrDeleteReservation(String spaceCode) {
		// Find the active reservation for this space
		Reservation target = null;
		for (Reservation r : reservationDAO.findAll()) {
			if (r.isActive()
					&& r.getParkingSpace() != null
					&& spaceCode.equals(r.getParkingSpace().getId())) {
				target = r;
				break;
			}
		}
		if (target == null) return;

		VehicleType type = target.getVehicle() != null ? target.getVehicle().getType() : null;
		List<ParkingSpace> alternatives = type != null
				? parkingService.findAvailableSpaces(type)
				: null;

		if (alternatives != null && !alternatives.isEmpty()) {
			// Reassign to the first available compatible space
			ParkingSpace oldSpace = target.getParkingSpace();
			ParkingSpace newSpace = alternatives.get(0);
			if (oldSpace != null) {
				oldSpace.cancelReservation();
				parkingService.updateParkingSpace(oldSpace);
			}
			target.setParkingSpace(newSpace);
			newSpace.reserve(target);
			parkingService.updateParkingSpace(newSpace);
			reservationDAO.update(target);
		} else {
			// No alternative — cancel and flag for user notification
			target.cancel();
			target.setCancelledByAdmin(true);
			target.setNotified(false);
			reservationDAO.update(target);
		}
	}

	/**
	 * Cancels a reservation as an admin action.
	 * Sets {@code cancelledByAdmin = true} and {@code notified = false} so the
	 * user receives an in-app notification at next login.
	 * @param reservationId ID of the reservation to cancel
	 */
	public void cancelReservation(int reservationId) {
		Reservation reservation = reservationDAO.findById(reservationId);
		if (reservation == null || !reservation.isActive()) return;

		reservation.cancel();
		reservation.setCancelledByAdmin(true);
		reservation.setNotified(false);
		reservationDAO.update(reservation);

		// Free the parking space
		ParkingSpace space = reservation.getParkingSpace();
		if (space != null) {
			space.cancelReservation();
			parkingService.updateParkingSpace(space);
		}
	}

	/**
	 * @return current status of all parking spaces
	 */
	public List<ParkingSpace> getFullParkingStatus() {
		return parkingService.getParkingStatus();
	}
}
