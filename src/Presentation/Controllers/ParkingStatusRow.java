package Presentation.Controllers;

import Business.Entities.ParkingSpace;

/**
 * Stores one parking status row loaded for the main menu table.
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
 */
class ParkingStatusRow {
    private ParkingSpace space;
    private boolean userParkedVehicle;

    /**
     * Creates one parking status row.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param space parking space to display
     * @param userParkedVehicle true when the current user owns the parked vehicle
     */
    ParkingStatusRow(ParkingSpace space, boolean userParkedVehicle) {
        this.space = space;
        this.userParkedVehicle = userParkedVehicle;
    }

    /**
     * Gets the parking space.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current space
     */
    ParkingSpace getSpace() {
        return space;
    }

    /**
     * Checks whether user parked vehicle.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    boolean isUserParkedVehicle() {
        return userParkedVehicle;
    }
}
