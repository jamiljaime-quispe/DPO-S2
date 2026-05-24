package Presentation.Views;

import Business.Entities.VehicleType;

/**
 * Actions provided by the controller for the admin parking dialog.
 */
public interface AdminParkingActions {

    /** Reloads all parking spaces. */
    void loadSpaces();

    /**
     * Creates a parking space.
     *
     * @param floor floor number
     * @param type  accepted vehicle type
     */
    void createSpace(int floor, VehicleType type);

    /**
     * Edits a parking space.
     *
     * @param code  space code
     * @param floor new floor number
     * @param type  new accepted vehicle type
     */
    void editSpace(String code, int floor, VehicleType type);

    /**
     * Deletes a parking space.
     *
     * @param code space code
     */
    void deleteSpace(String code);
}
