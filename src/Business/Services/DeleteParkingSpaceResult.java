package Business.Services;

/**
 * Describes what happened when an admin deleted a parking space.
 */
public class DeleteParkingSpaceResult {
    private String deletedSpaceCode;
    private String affectedPlate;
    private String newSpaceCode;
    private boolean reservationCancelled;

    /**
     * Stores the result of a parking-space deletion.
     *
     * @param deletedSpaceCode     code of the removed space
     * @param affectedPlate        reservation plate affected by the removal, or null
     * @param newSpaceCode         replacement space code, or null
     * @param reservationCancelled true if the reservation had to be cancelled
     */
    DeleteParkingSpaceResult(String deletedSpaceCode, String affectedPlate, String newSpaceCode,
                             boolean reservationCancelled) {
        this.deletedSpaceCode = deletedSpaceCode;
        this.affectedPlate = affectedPlate;
        this.newSpaceCode = newSpaceCode;
        this.reservationCancelled = reservationCancelled;
    }

    /** Gets the code of the removed space. */
    public String getDeletedSpaceCode() {
        return deletedSpaceCode;
    }

    /** Gets the plate affected by the deletion. */
    public String getAffectedPlate() {
        return affectedPlate;
    }

    /** Gets the replacement space code. */
    public String getNewSpaceCode() {
        return newSpaceCode;
    }

    /** Returns whether the reservation was cancelled. */
    public boolean isReservationCancelled() {
        return reservationCancelled;
    }

    /** Returns whether a reservation was affected. */
    public boolean hasAffectedReservation() {
        return affectedPlate != null && !affectedPlate.isBlank();
    }
}
