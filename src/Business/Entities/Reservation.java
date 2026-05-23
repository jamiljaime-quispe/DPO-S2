package Business.Entities;

import java.time.LocalDateTime;

/**
 * Represents a parking space reservation made by a client.
 * Includes admin-cancellation tracking for user notification at next login.
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
	private String previousSpaceCode;

	/**
	 * Constructs a new Reservation.
	 *
	 * @param id              the reservation ID (0 for new, assigned by the database)
	 * @param user            the client who made the reservation
	 * @param vehicle         the vehicle being reserved for
	 * @param parkingSpace    the parking space being reserved
	 * @param reservationDate the date and time the reservation was created
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
		this.previousSpaceCode = null;
	}

	/**
	 * Gets the reservation ID.
	 *
	 * @return the reservation ID
	 */
	public int getId() { return id; }

	/**
	 * Gets the client who made the reservation.
	 *
	 * @return the client who made the reservation
	 */
	public Client getUser() { return user; }

	/**
	 * Gets the vehicle associated with this reservation.
	 *
	 * @return the reserved vehicle
	 */
	public Vehicle getVehicle() { return vehicle; }

	/**
	 * Gets the parking space associated with this reservation.
	 *
	 * @return the reserved parking space
	 */
	public ParkingSpace getParkingSpace() { return parkingSpace; }

	/**
	 * Gets the date and time the reservation was created.
	 *
	 * @return the reservation date and time
	 */
	public LocalDateTime getReservationDate() { return reservationDate; }

	/**
	 * Returns whether this reservation was cancelled by an admin.
	 *
	 * @return true if this reservation was cancelled by an admin
	 */
	public boolean isCancelledByAdmin() { return cancelledByAdmin; }

	/**
	 * Returns whether the user has been notified of the admin cancellation.
	 *
	 * @return true if the user has already been notified
	 */
	public boolean isNotified() { return notified; }

	/**
	 * Returns whether the reservation is currently active.
	 *
	 * @return true if the reservation is currently active
	 */
	public boolean isActive() { return isActive; }

	/**
	 * Sets the reservation ID.
	 *
	 * @param id the new reservation ID
	 */
	public void setId(int id) { this.id = id; }

	/**
	 * Sets the client who made the reservation.
	 *
	 * @param user the new client
	 */
	public void setUser(Client user) { this.user = user; }

	/**
	 * Sets the parking space associated with this reservation.
	 *
	 * @param parkingSpace the new parking space
	 */
	public void setParkingSpace(ParkingSpace parkingSpace) { this.parkingSpace = parkingSpace; }

	/**
	 * Sets whether this reservation was cancelled by an admin.
	 *
	 * @param cancelledByAdmin true if cancelled by an admin
	 */
	public void setCancelledByAdmin(boolean cancelledByAdmin) { this.cancelledByAdmin = cancelledByAdmin; }

	/**
	 * Sets whether the user has been notified of the admin cancellation.
	 *
	 * @param notified true if the user has been notified
	 */
	public void setNotified(boolean notified) { this.notified = notified; }

	/**
	 * Sets the active status of the reservation.
	 *
	 * @param active the new active status
	 */
	public void setActive(boolean active) { this.isActive = active; }

	/**
	 * Returns the code of the space this reservation was originally on,
	 * captured by the admin when the original space was deleted or the
	 * reservation was cancelled. Used to notify the user at next login.
	 *
	 * @return the previous space code, or null if not applicable
	 */
	public String getPreviousSpaceCode() { return previousSpaceCode; }

	/**
	 * Records the code of the space this reservation was originally on
	 * before being reassigned or cancelled by an admin.
	 *
	 * @param previousSpaceCode the original space code, or null to clear
	 */
	public void setPreviousSpaceCode(String previousSpaceCode) { this.previousSpaceCode = previousSpaceCode; }

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
	 * Returns a human-readable summary of this reservation.
	 *
	 * @return a string with the key reservation fields
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
