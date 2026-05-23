package Business.Entities;

/**
 * Represents a single parking space in the lot.
 * The {@code id} field maps to the {@code code} column in the database.
 */
public class ParkingSpace {
	private String id;
	private int floor;
	private VehicleType vehicleType;
	private boolean occupied;
	private boolean reserved;
	private Vehicle parkedVehicle;
	private Reservation reservation;

	/**
	 * Constructs a new ParkingSpace.
	 *
	 * @param id            the unique space code (e.g. "A-01")
	 * @param floor         the floor number where the space is located
	 * @param vehicleType   the type of vehicle this space accepts
	 * @param occupied      whether a vehicle is currently parked here
	 * @param reserved      whether the space has an active reservation
	 * @param parkedVehicle the vehicle currently parked, or null
	 * @param reservation   the active reservation for this space, or null
	 */
	public ParkingSpace(String id, int floor, VehicleType vehicleType, boolean occupied, boolean reserved,
			Vehicle parkedVehicle, Reservation reservation) {
		this.id = id;
		this.floor = floor;
		this.vehicleType = vehicleType;
		this.occupied = occupied;
		this.reserved = reserved;
		this.parkedVehicle = parkedVehicle;
		this.reservation = reservation;
	}

	/**
	 * Gets the space code.
	 *
	 * @return the space code
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets the floor number.
	 *
	 * @return the floor number
	 */
	public int getFloor() {
		return floor;
	}

	/**
	 * Gets the vehicle type this space accepts.
	 *
	 * @return the vehicle type
	 */
	public VehicleType getVehicleType() {
		return vehicleType;
	}

	/**
	 * Returns whether a vehicle is currently parked here.
	 *
	 * @return true if a vehicle is currently parked here
	 */
	public boolean isOccupied() {
		return occupied;
	}

	/**
	 * Returns whether the space has an active reservation.
	 *
	 * @return true if the space has an active reservation
	 */
	public boolean isReserved() {
		return reserved;
	}

	/**
	 * Gets the vehicle currently parked in this space.
	 *
	 * @return the parked vehicle, or null if the space is free
	 */
	public Vehicle getParkedVehicle() {
		return parkedVehicle;
	}

	/**
	 * Gets the active reservation for this space.
	 *
	 * @return the active reservation, or null if none exists
	 */
	public Reservation getReservation() {
		return reservation;
	}

	/**
	 * Sets the space code.
	 *
	 * @param id the new space code
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Sets the floor number.
	 *
	 * @param floor the new floor number
	 */
	public void setFloor(int floor) {
		this.floor = floor;
	}

	/**
	 * Sets the vehicle type this space accepts.
	 *
	 * @param vehicleType the new vehicle type
	 */
	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	/**
	 * Sets the occupied status.
	 *
	 * @param occupied the new occupied status
	 */
	public void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

	/**
	 * Sets the reserved status.
	 *
	 * @param reserved the new reserved status
	 */
	public void setReserved(boolean reserved) {
		this.reserved = reserved;
	}

	/**
	 * Sets the vehicle currently parked in this space.
	 *
	 * @param parkedVehicle the parked vehicle, or null to clear it
	 */
	public void setParkedVehicle(Vehicle parkedVehicle) {
		this.parkedVehicle = parkedVehicle;
	}

	/**
	 * Sets the active reservation for this space.
	 *
	 * @param reservation the reservation to assign, or null to clear it
	 */
	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

	/**
	 * Parks a vehicle in this space and marks it as occupied.
	 *
	 * @param vehicle the vehicle to park
	 */
	public void occupy(Vehicle vehicle) {
		this.parkedVehicle = vehicle;
		this.occupied = true;
		if (vehicle != null)
			vehicle.setParked(true);
	}

	/**
	 * Removes the parked vehicle and marks the space as free.
	 */
	public void freeSpace() {
		if (parkedVehicle != null)
			parkedVehicle.setParked(false);
		this.parkedVehicle = null;
		this.occupied = false;
	}

	/**
	 * Assigns a reservation to this space and marks it as reserved.
	 *
	 * @param reservation the reservation to assign
	 */
	public void reserve(Reservation reservation) {
		this.reservation = reservation;
		this.reserved = true;
	}

	/**
	 * Removes the active reservation and marks the space as unreserved.
	 */
	public void cancelReservation() {
		this.reservation = null;
		this.reserved = false;
	}

	/**
	 * Returns whether the space is available for parking or reservation.
	 *
	 * @return true if the space is neither occupied nor reserved
	 */
	public boolean isAvailable() {
		return !occupied && !reserved;
	}
}
