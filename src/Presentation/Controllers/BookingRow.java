package Presentation.Controllers;

import Business.Entities.ParkingSpace;

/**
 * Stores one booking row loaded for the slot booking table.
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
 */
class BookingRow {
    private ParkingSpace space;
    private boolean userBooking;

    /**
     * Creates one booking row.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param space parking space to display
     * @param userBooking true when the row belongs to the current user
     */
    BookingRow(ParkingSpace space, boolean userBooking) {
        this.space = space;
        this.userBooking = userBooking;
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
     * Checks whether user booking.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    boolean isUserBooking() {
        return userBooking;
    }
}
