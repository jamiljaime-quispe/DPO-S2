package Presentation.Views;

import Business.Entities.ParkingSpace;

/**
 * Option shown when a user chooses which parked vehicle should exit.
 */
class ExitSpaceOption {
    private ParkingSpace space;

    /**
     * Stores one selectable parked vehicle.
     *
     * @param space parking space linked to the option
     */
    ExitSpaceOption(ParkingSpace space) {
        this.space = space;
    }

    /**
     * Gets the parking space behind this option.
     *
     * @return parking space selected by the user
     */
    ParkingSpace getSpace() {
        return space;
    }

    /**
     * Formats the option shown in the exit combo box.
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
