package Business.Entities;

/**
 * Represents a vehicle registered in the system.
 */
public class Vehicle {
	private String licencePlate;
	private VehicleType vehicleType;
	private String owner;
	private boolean parked;
	/**
	 * @param licencePlate unique license plate
	 * @param vehicleType  type of vehicle
	 * @param owner        username of the owner
	 * @param parked       whether currently parked
	 */
	public Vehicle(String licencePlate, VehicleType vehicleType, String owner, boolean parked) {
		this.licencePlate = licencePlate;
		this.vehicleType = vehicleType;
		this.owner = owner;
		this.parked = parked;
	}

	// --- Getters ---

	/**
	 * @return license plate
	 */
	public String getLicensePlate() { return licencePlate; }

	/**
	 * @return vehicle type enum value
	 */
	public VehicleType getType() { return vehicleType; }

	/**
	 * @return owner username
	 */
	public String getOwner() { return owner; }

	/**
	 * @return true if vehicle is currently parked
	 */
	public boolean isParked() { return parked; }

	// --- Setters ---

	public void setParked(boolean parked) { this.parked = parked; }
	public void setOwner(String owner) { this.owner = owner; }
	public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
}
