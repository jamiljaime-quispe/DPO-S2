package Business.Entities;

/**
 * Represents a vehicle registered in the system.
 * A vehicle has a unique license plate, a type, an owner, and a parked status.
 */
public class Vehicle {
	private String licencePlate;
	private VehicleType vehicleType;
	private String owner;
	private boolean parked;

	/**
	 * Constructs a new Vehicle.
	 *
	 * @param licencePlate the unique license plate
	 * @param vehicleType  the type of vehicle
	 * @param owner        the username of the owner
	 * @param parked       whether the vehicle is currently parked
	 */
	public Vehicle(String licencePlate, VehicleType vehicleType, String owner, boolean parked) {
		this.licencePlate = licencePlate;
		this.vehicleType = vehicleType;
		this.owner = owner;
		this.parked = parked;
	}

	/**
	 * Gets the license plate.
	 *
	 * @return the license plate
	 */
	public String getLicensePlate() { return licencePlate; }

	/**
	 * Gets the vehicle type.
	 *
	 * @return the vehicle type enum value
	 */
	public VehicleType getType() { return vehicleType; }

	/**
	 * Gets the owner username.
	 *
	 * @return the owner username
	 */
	public String getOwner() { return owner; }

	/**
	 * Returns whether the vehicle is currently parked.
	 *
	 * @return true if the vehicle is currently parked
	 */
	public boolean isParked() { return parked; }

	/**
	 * Sets the parked status.
	 *
	 * @param parked the new parked status
	 */
	public void setParked(boolean parked) { this.parked = parked; }

	/**
	 * Sets the owner username.
	 *
	 * @param owner the new owner username
	 */
	public void setOwner(String owner) { this.owner = owner; }

	/**
	 * Sets the vehicle type.
	 *
	 * @param vehicleType the new vehicle type
	 */
	public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
}
