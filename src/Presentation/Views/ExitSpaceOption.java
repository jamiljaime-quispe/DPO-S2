package Presentation.Views;

import Business.Entities.ParkingSpace;

/**
 * Option shown when a user chooses which parked vehicle should exit.
 * <p>
 * The view builds or updates Swing components and leaves the decisions to controllers and services. This
 * keeps the screen code focused on what the user sees.
 * </p>
 */
class ExitSpaceOption {
    private ParkingSpace space;

    /**
     * Stores one selectable parked vehicle.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param space parking space linked to the option
     */
    ExitSpaceOption(ParkingSpace space) {
        this.space = space;
    }

    /**
     * Gets the parking space behind this option.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return parking space selected by the user
     */
    ParkingSpace getSpace() {
        return space;
    }

    /**
     * Handles to string.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @return readable vehicle and space text
     */
    @Override
    public String toString() {
        String plate = space.getParkedVehicle() != null
                ? space.getParkedVehicle().getLicensePlate()
                : "";
        String type = space.getParkedVehicle() != null && space.getParkedVehicle().getType() != null
                ? space.getParkedVehicle().getType().name()
                : space.getVehicleType().name();
        return "Plate " + plate + " - Type " + type
                + " - Space " + space.getId() + " - Floor " + space.getFloor();
    }
}
