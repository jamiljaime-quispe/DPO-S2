package Business.Services;

/**
 * Holds the reservation part of a delete-space operation.
 */
class ReservationMoveResult {
    private String affectedPlate;
    private String newSpaceCode;
    private boolean reservationCancelled;

    /**
     * Stores the result of moving or cancelling a reservation.
     *
     * @param affectedPlate        reservation plate affected, or null
     * @param newSpaceCode         replacement space code, or null
     * @param reservationCancelled true if the reservation was cancelled
     */
    ReservationMoveResult(String affectedPlate, String newSpaceCode, boolean reservationCancelled) {
        this.affectedPlate = affectedPlate;
        this.newSpaceCode = newSpaceCode;
        this.reservationCancelled = reservationCancelled;
    }

    /** Gets the affected plate. */
    String getAffectedPlate() {
        return affectedPlate;
    }

    /** Gets the new space code. */
    String getNewSpaceCode() {
        return newSpaceCode;
    }

    /** Checks whether the reservation was cancelled. */
    boolean isReservationCancelled() {
        return reservationCancelled;
    }
}
