package Business.Entities;

/**
 * Represents a vehicle registered in the system. A vehicle has a unique license plate, a type, an owner,
 * and a parked status.
 * <p>
 * The class stores project data in a clear object so the services, controllers, and persistence code can
 * pass the same information around safely.
 * </p>
 */
public class Vehicle {
	private String licencePlate;
	private VehicleType vehicleType;
	private String owner;
	private boolean parked;

	/**
	 * Constructs a new Vehicle.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param licencePlate the unique license plate
	 * @param vehicleType the type of vehicle
	 * @param owner the username of the owner
	 * @param parked whether the vehicle is currently parked
	 */
	public Vehicle(String licencePlate, VehicleType vehicleType, String owner, boolean parked) {
		this.licencePlate = licencePlate;
		this.vehicleType = vehicleType;
		this.owner = owner;
		this.parked = parked;
	}

	/**
	 * Gets the license plate.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the license plate
	 */
	public String getLicensePlate() { return licencePlate; }

	/**
	 * Gets the vehicle type.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the vehicle type enum value
	 */
	public VehicleType getType() { return vehicleType; }

	/**
	 * Gets the owner username.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the owner username
	 */
	public String getOwner() { return owner; }

	/**
	 * Returns whether the vehicle is currently parked.
	 * <p>
	 * This helper keeps the step named and separate, which makes the larger operation easier to read and
	 * follow.
	 * </p>
	 *
	 * @return true if the vehicle is currently parked
	 */
	public boolean isParked() { return parked; }

	/**
	 * Sets the parked status.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param parked the new parked status
	 */
	public void setParked(boolean parked) { this.parked = parked; }


	/**
	 * Sets the vehicle type.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param vehicleType the new vehicle type
	 */
	public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
}
