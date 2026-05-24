package Presentation.Controllers;

import Business.Entities.ParkingSpace;

/**
 * Stores one parking status row loaded for the main menu table.
 */
class ParkingStatusRow {
    private ParkingSpace space;
    private boolean userParkedVehicle;

    /**
     * Creates one parking status row.
     *
     * @param space             parking space to display
     * @param userParkedVehicle true when the current user owns the parked vehicle
     */
    ParkingStatusRow(ParkingSpace space, boolean userParkedVehicle) {
        this.space = space;
        this.userParkedVehicle = userParkedVehicle;
    }

    /** Gets the parking space. */
    ParkingSpace getSpace() {
        return space;
    }

    /** Checks whether this row belongs to the current user's parked vehicle. */
    boolean isUserParkedVehicle() {
        return userParkedVehicle;
    }
}
