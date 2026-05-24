package Presentation.Views;

import Business.Entities.VehicleType;

/**
 * Plate and type chosen before a regular user books a slot.
 * <p>
 * The view builds or updates Swing components and leaves the decisions to controllers and services. This
 * keeps the screen code focused on what the user sees.
 * </p>
 */
public class BookingVehicleInput {
    private String plate;
    private VehicleType type;

    /**
     * Stores the vehicle chosen for a new booking.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param plate selected license plate
     * @param type selected vehicle type
     */
    public BookingVehicleInput(String plate, VehicleType type) {
        this.plate = plate;
        this.type = type;
    }

    /**
     * Gets the selected license plate.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return selected license plate
     */
    public String getPlate() {
        return plate;
    }

    /**
     * Gets the selected vehicle type.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return selected vehicle type
     */
    public VehicleType getType() {
        return type;
    }
}
