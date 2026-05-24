package Presentation.Views;

import Business.Entities.VehicleType;

/**
 * Actions provided by the controller for the admin parking dialog. This interface keeps the promise clear
 * so another class can use it without depending on a specific implementation.
 * <p>
 * The interface describes what another class must do, which lets the project connect parts together without
 * depending on one concrete class.
 * </p>
 */
public interface AdminParkingActions {

    /**
     * Loads spaces.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    void loadSpaces();

    /**
     * Creates space.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @param floor floor number
     * @param type accepted vehicle type
     */
    void createSpace(int floor, VehicleType type);

    /**
     * Handles edit space.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param code space code
     * @param floor new floor number
     * @param type new accepted vehicle type
     */
    void editSpace(String code, int floor, VehicleType type);

    /**
     * Deletes space.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param code space code
     */
    void deleteSpace(String code);
}
