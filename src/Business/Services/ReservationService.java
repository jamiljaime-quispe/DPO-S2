package Business.Services;

import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.VehicleDAO;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the lifecycle of parking space reservations.
 */
public class ReservationService {
	private ReservationDAO reservationDAO;
	private ParkingSpaceDAO parkingSpaceDAO;
	private VehicleDAO vehicleDAO;

	/**
	 * Constructs a new ReservationService.
	 *
	 * @param reservationDAO  the DAO for reservations
	 * @param parkingSpaceDAO the DAO for parking spaces
	 * @param vehicleDAO      the DAO for vehicles
	 */
	public ReservationService(ReservationDAO reservationDAO, ParkingSpaceDAO parkingSpaceDAO,
							  VehicleDAO vehicleDAO) {
		this.reservationDAO = reservationDAO;
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.vehicleDAO = vehicleDAO;
	}

	/**
	 * Creates a reservation for a vehicle in the specified space.
	 * Fails if the space does not exist or is not available.
	 * @param userId    ID of the user making the reservation
	 * @param plate     license plate of the vehicle
	 * @param type      vehicle type (used for validation)
	 * @param spaceCode code of the space to reserve
	 * @return created Reservation, or null if the space is unavailable or vehicle not found
	 */
	public Reservation createReservation(int userId, String plate, VehicleType type, String spaceCode) {
		ParkingSpace space = parkingSpaceDAO.findByCode(spaceCode);
		if (space == null || !space.isAvailable()) return null;

		Vehicle vehicle = vehicleDAO.findByPlate(plate);
		if (vehicle == null) return null;

		Reservation reservation = new Reservation(0, null, vehicle, space, LocalDateTime.now());
		space.reserve(reservation);
		parkingSpaceDAO.update(space);
		reservationDAO.save(reservation);
		return reservation;
	}

	/**
	 * Cancels a reservation by ID, marking it inactive and freeing the space.
	 * @param reservationId ID of the reservation to cancel
	 */
	public void cancelReservation(int reservationId) {
		Reservation reservation = reservationDAO.findById(reservationId);
		if (reservation == null || !reservation.isActive()) return;

		reservation.cancel();
		reservationDAO.update(reservation);

		ParkingSpace space = reservation.getParkingSpace();
		if (space != null) {
			space.cancelReservation();
			parkingSpaceDAO.update(space);
		}
	}

	/**
	 * Cancels the active reservation for a given license plate.
	 * @param plate license plate of the vehicle whose reservation to cancel
	 */
	public void cancelReservationByPlate(String plate) {
		Reservation reservation = reservationDAO.findByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			cancelReservation(reservation.getId());
		}
	}

	/**
	 * Returns all active reservations belonging to a user.
	 * @param userId user ID
	 * @return list of active reservations
	 */
	public List<Reservation> getReservationsByUser(int userId) {
		List<Reservation> all = reservationDAO.findByUser(userId);
		List<Reservation> active = new ArrayList<>();
		for (Reservation r : all) {
			if (r.isActive()) active.add(r);
		}
		return active;
	}

	/**
	 * Returns all spaces available for reservation for the given vehicle type.
	 * @param type vehicle type
	 * @return list of available spaces
	 */
	public List<ParkingSpace> getAvailableSpaces(VehicleType type) {
		return parkingSpaceDAO.findAvailableByType(type);
	}

	/**
	 * Returns reservations cancelled by an admin that the user has not yet been notified about.
	 * Used by AuthController at login to display cancellation alerts.
	 * @param userId user ID
	 * @return list of unnotified admin-cancelled reservations
	 */
	public List<Reservation> getCancelledByAdminNotNotified(int userId) {
		List<Reservation> all = reservationDAO.findByUser(userId);
		List<Reservation> pending = new ArrayList<>();
		for (Reservation r : all) {
			if (r.isCancelledByAdmin() && !r.isNotified()) pending.add(r);
		}
		return pending;
	}

	/**
	 * Checks whether a specific space is available for reservation.
	 * @param spaceCode space code
	 * @return true if the space exists and is available
	 */
	public boolean isSpaceAvailable(String spaceCode) {
		ParkingSpace space = parkingSpaceDAO.findByCode(spaceCode);
		return space != null && space.isAvailable();
	}
}
