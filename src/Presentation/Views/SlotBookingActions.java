package Presentation.Views;

import Business.Entities.VehicleType;

/**
 * Actions provided by the controller for the slot booking dialog. This interface keeps the promise clear so
 * another class can use it without depending on a specific implementation.
 * <p>
 * The interface describes what another class must do, which lets the project connect parts together without
 * depending on one concrete class.
 * </p>
 */
public interface SlotBookingActions {

    /**
     * Loads bookings.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    void loadBookings();

    /**
     * Handles prepare user booking.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param plate license plate
     * @param type vehicle type
     */
    void prepareUserBooking(String plate, VehicleType type);

    /**
     * Creates booking.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @param plate license plate
     * @param type vehicle type
     * @param spaceCode target space code
     */
    void createBooking(String plate, VehicleType type, String spaceCode);

    /**
     * Handles edit booking.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param originalSpaceCode previous space code
     * @param plate license plate
     * @param type vehicle type
     * @param spaceCode new space code
     */
    void editBooking(String originalSpaceCode, String plate, VehicleType type, String spaceCode);

    /**
     * Deletes booking.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param spaceCode space code
     * @param plate license plate
     */
    void deleteBooking(String spaceCode, String plate);
}
