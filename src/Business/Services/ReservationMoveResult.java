package Business.Services;

/**
 * Holds the reservation part of a delete-space operation.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
class ReservationMoveResult {
    private String affectedPlate;
    private String newSpaceCode;
    private boolean reservationCancelled;

    /**
     * Stores the result of moving or cancelling a reservation.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param affectedPlate reservation plate affected, or null
     * @param newSpaceCode replacement space code, or null
     * @param reservationCancelled true if the reservation was cancelled
     */
    ReservationMoveResult(String affectedPlate, String newSpaceCode, boolean reservationCancelled) {
        this.affectedPlate = affectedPlate;
        this.newSpaceCode = newSpaceCode;
        this.reservationCancelled = reservationCancelled;
    }

    /**
     * Gets the affected plate.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current affected plate
     */
    String getAffectedPlate() {
        return affectedPlate;
    }

    /**
     * Gets the new space code.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current new space code
     */
    String getNewSpaceCode() {
        return newSpaceCode;
    }

    /**
     * Checks whether reservation cancelled.
     * <p>
     * This method keeps the business decision in the service layer before anything is sent back to the
     * screen.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    boolean isReservationCancelled() {
        return reservationCancelled;
    }
}
