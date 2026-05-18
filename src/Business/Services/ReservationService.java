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
		if (userId <= 0) {
			throw new IllegalArgumentException("No logged-in user was found.");
		}

		ParkingSpace space = parkingSpaceDAO.findByCode(spaceCode);
		if (space == null) {
			throw new IllegalArgumentException("Parking space not found: " + spaceCode);
		}
		if (!space.isAvailable()) {
			throw new IllegalArgumentException("Parking space " + spaceCode + " is not available.");
		}
		if (space.getVehicleType() != type) {
			throw new IllegalArgumentException("Parking space " + spaceCode
					+ " only accepts " + space.getVehicleType().name() + " vehicles.");
		}

		Reservation activeReservation = reservationDAO.findByPlate(plate);
		if (activeReservation != null && activeReservation.isActive()) {
			throw new IllegalArgumentException("License plate " + plate + " already has an active reservation.");
		}

		Vehicle vehicle = vehicleDAO.findByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, String.valueOf(userId), false);
			vehicleDAO.save(vehicle);
		} else if (vehicle.getType() != type) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is registered as " + vehicle.getType().name() + ".");
		}

		Reservation reservation = new Reservation(0, null, vehicle, space, LocalDateTime.now());
		space.reserve(reservation);
		reservationDAO.save(reservation);
		return reservation;
	}

	/**
	 * Reassigns an active reservation to a different available space.
	 * @param plate license plate whose active reservation should be moved
	 * @param newSpaceCode target parking space code
	 * @return updated reservation
	 */
	public Reservation reassignReservation(String plate, String newSpaceCode) {
		Reservation reservation = reservationDAO.findByPlate(plate);
		if (reservation == null || !reservation.isActive()) {
			throw new IllegalArgumentException("No active reservation found for license plate " + plate + ".");
		}

		ParkingSpace newSpace = parkingSpaceDAO.findByCode(newSpaceCode);
		if (newSpace == null) {
			throw new IllegalArgumentException("Parking space not found: " + newSpaceCode);
		}
		if (newSpace.isOccupied()) {
			throw new IllegalArgumentException("Parking space " + newSpaceCode + " is currently occupied.");
		}

		String currentSpaceCode = reservation.getParkingSpace().getId();
		if (newSpace.isReserved() && !currentSpaceCode.equals(newSpaceCode)) {
			throw new IllegalArgumentException("Parking space " + newSpaceCode + " is already reserved.");
		}
		if (reservation.getVehicle() != null && reservation.getVehicle().getType() != newSpace.getVehicleType()) {
			throw new IllegalArgumentException("Parking space " + newSpaceCode
					+ " only accepts " + newSpace.getVehicleType().name() + " vehicles.");
		}

		reservation.setParkingSpace(newSpace);
		reservationDAO.update(reservation);
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
	 * Cancels the active reservation for a license plate only if it belongs to the user.
	 * @param userId user requesting the cancellation
	 * @param plate license plate whose reservation should be cancelled
	 * @return true if a matching active reservation was cancelled
	 */
	public boolean cancelReservationByPlateForUser(int userId, String plate) {
		Reservation reservation = reservationDAO.findByPlate(plate);
		if (reservation == null || !reservation.isActive() || reservation.getUser() == null) {
			return false;
		}

		int reservationUserId = Integer.parseInt(reservation.getUser().getId());
		if (reservationUserId != userId) {
			return false;
		}

		cancelReservation(reservation.getId());
		return true;
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
	 * Marks a reservation as notified so the alert is not shown again at login.
	 * @param reservation the reservation to mark
	 */
	public void markNotified(Reservation reservation) {
		reservation.setNotified(true);
		reservationDAO.update(reservation);
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
