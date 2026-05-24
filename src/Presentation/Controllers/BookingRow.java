package Presentation.Controllers;

import Business.Entities.ParkingSpace;

/**
 * Stores one booking row loaded for the slot booking table.
 */
class BookingRow {
    private ParkingSpace space;
    private boolean userBooking;

    /**
     * Creates one booking row.
     *
     * @param space       parking space to display
     * @param userBooking true when the row belongs to the current user
     */
    BookingRow(ParkingSpace space, boolean userBooking) {
        this.space = space;
        this.userBooking = userBooking;
    }

    /** Gets the parking space. */
    ParkingSpace getSpace() {
        return space;
    }

    /** Checks whether the row belongs to the current user. */
    boolean isUserBooking() {
        return userBooking;
    }
}
