package Presentation.Views;

import Business.Entities.VehicleType;

/**
 * Actions provided by the controller for the slot booking dialog.
 */
public interface SlotBookingActions {

    /** Reloads slot booking data. */
    void loadBookings();

    /**
     * Stores the vehicle chosen by the user before booking a space.
     *
     * @param plate license plate
     * @param type  vehicle type
     */
    void prepareUserBooking(String plate, VehicleType type);

    /**
     * Creates a booking.
     *
     * @param plate     license plate
     * @param type      vehicle type
     * @param spaceCode target space code
     */
    void createBooking(String plate, VehicleType type, String spaceCode);

    /**
     * Edits an existing booking.
     *
     * @param originalSpaceCode previous space code
     * @param plate             license plate
     * @param type              vehicle type
     * @param spaceCode         new space code
     */
    void editBooking(String originalSpaceCode, String plate, VehicleType type, String spaceCode);

    /**
     * Deletes a booking.
     *
     * @param spaceCode space code
     * @param plate     license plate
     */
    void deleteBooking(String spaceCode, String plate);
}
