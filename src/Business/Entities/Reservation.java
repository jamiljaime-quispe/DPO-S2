package Business.Entities;

import java.time.LocalDateTime;

/**
 * Represents a parking space reservation made by a client.
 * Includes admin-cancellation tracking for user notification.
 */
public class Reservation {
	private int id;
	private Client user;
	private Vehicle vehicle;
	private ParkingSpace parkingSpace;
	private LocalDateTime reservationDate;
	private boolean cancelledByAdmin;
	private boolean notified;
	private boolean isActive;

	/**
	 * @param id              reservation ID (0 for new, assigned by DB)
	 * @param user            client who made the reservation
	 * @param vehicle         reserved vehicle
	 * @param parkingSpace    reserved space
	 * @param reservationDate date/time of reservation
	 */
	public Reservation(int id, Client user, Vehicle vehicle, ParkingSpace parkingSpace,
					   LocalDateTime reservationDate) {
		this.id = id;
		this.user = user;
		this.vehicle = vehicle;
		this.parkingSpace = parkingSpace;
		this.reservationDate = reservationDate;
		this.cancelledByAdmin = false;
		this.notified = false;
		this.isActive = true;
	}

	// --- Getters ---

	/**
	 * @return reservation ID
	 */
	public int getId() { return id; }

	/**
	 * @return client who made the reservation
	 */
	public Client getUser() { return user; }

	/**
	 * @return reserved vehicle
	 */
	public Vehicle getVehicle() { return vehicle; }

	/**
	 * @return reserved parking space
	 */
	public ParkingSpace getParkingSpace() { return parkingSpace; }

	/**
	 * @return date and time when the reservation was made
	 */
	public LocalDateTime getReservationDate() { return reservationDate; }

	/**
	 * @return true if this reservation was cancelled by an admin
	 */
	public boolean isCancelledByAdmin() { return cancelledByAdmin; }

	/**
	 * @return true if the user has been notified of admin cancellation
	 */
	public boolean isNotified() { return notified; }

	/**
	 * @return true if the reservation is currently active
	 */
	public boolean isActive() { return isActive; }

	// --- Setters ---

	public void setId(int id) { this.id = id; }
	public void setUser(Client user) { this.user = user; }
	public void setParkingSpace(ParkingSpace parkingSpace) { this.parkingSpace = parkingSpace; }
	public void setCancelledByAdmin(boolean cancelledByAdmin) { this.cancelledByAdmin = cancelledByAdmin; }
	public void setNotified(boolean notified) { this.notified = notified; }
	public void setActive(boolean active) { this.isActive = active; }

	// --- Behaviour ---

	/**
	 * Marks this reservation as active.
	 */
	public void confirm() {
		this.isActive = true;
	}

	/**
	 * Marks this reservation as inactive (cancelled).
	 */
	public void cancel() {
		this.isActive = false;
	}

	/**
	 * @return human-readable reservation summary
	 */
	public String getReservationInfo() {
		return "Reservation{"
				+ "id=" + id
				+ ", user=" + (user != null ? user.getUsername() : "null")
				+ ", vehicle=" + (vehicle != null ? vehicle.getLicensePlate() : "null")
				+ ", space=" + (parkingSpace != null ? parkingSpace.getId() : "null")
				+ ", date=" + reservationDate
				+ ", active=" + isActive
				+ "}";
	}
}
