package Presentation.Views;

import Business.Entities.VehicleType;

/**
 * Plate and type chosen before a regular user books a slot.
 */
public class BookingVehicleInput {
    private String plate;
    private VehicleType type;

    /**
     * Stores the vehicle chosen for a new booking.
     *
     * @param plate selected license plate
     * @param type  selected vehicle type
     */
    public BookingVehicleInput(String plate, VehicleType type) {
        this.plate = plate;
        this.type = type;
    }

    /**
     * Gets the selected license plate.
     *
     * @return selected license plate
     */
    public String getPlate() {
        return plate;
    }

    /**
     * Gets the selected vehicle type.
     *
     * @return selected vehicle type
     */
    public VehicleType getType() {
        return type;
    }
}
