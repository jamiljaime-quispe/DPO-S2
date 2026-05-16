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
	 * @param id
	 * @param floor
	 * @param vehicleType
	 * @param occupied
	 * @param reserved
	 * @param parkedVehicle
	 * @param reservation
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
	 * @return space code
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return floor number
	 */
	public int getFloor() {
		return floor;
	}

	/**
	 * @return vehicle type this space accepts
	 */
	public VehicleType getVehicleType() {
		return vehicleType;
	}

	/**
	 * @return true if a vehicle is currently parked here
	 */
	public boolean isOccupied() {
		return occupied;
	}

	/**
	 * @return vehicle currently parked, or null
	 */
	public Vehicle getParkedVehicle() {
		return parkedVehicle;
	}

	/**
	 * @return active reservation for this space, or null
	 */
	public Reservation getReservation() {
		return reservation;
	}

	// --- Setters ---

	public void setFloor(int floor) {
		this.floor = floor;
	}

	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	public void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

	public void setReserved(boolean reserved) {
		this.reserved = reserved;
	}

	public void setParkedVehicle(Vehicle parkedVehicle) {
		this.parkedVehicle = parkedVehicle;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

	/**
	 * Parks a vehicle in this space.
	 * 
	 * @param vehicle vehicle to park
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
	 * Assigns a reservation to this space.
	 * 
	 * @param reservation reservation to assign
	 */
	public void reserve(Reservation reservation) {
		this.reservation = reservation;
		this.reserved = true;
	}

	/**
	 * Removes the active reservation from this space.
	 */
	public void cancelReservation() {
		this.reservation = null;
		this.reserved = false;
	}

	/**
	 * @return true if the space is neither occupied nor reserved
	 */
	public boolean isAvailable() {
		return !occupied && !reserved;
	}

	/**
	 * @return true if the space has an active reservation
	 */
	public boolean isReserved() {
		return reserved;
	}
}
