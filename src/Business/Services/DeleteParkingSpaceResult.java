package Business.Services;

/**
 * Describes what happened when an admin deleted a parking space.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class DeleteParkingSpaceResult {
    private String deletedSpaceCode;
    private String affectedPlate;
    private String newSpaceCode;
    private boolean reservationCancelled;

    /**
     * Stores the result of a parking-space deletion.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param deletedSpaceCode code of the removed space
     * @param affectedPlate reservation plate affected by the removal, or null
     * @param newSpaceCode replacement space code, or null
     * @param reservationCancelled true if the reservation had to be cancelled
     */
    DeleteParkingSpaceResult(String deletedSpaceCode, String affectedPlate, String newSpaceCode,
                             boolean reservationCancelled) {
        this.deletedSpaceCode = deletedSpaceCode;
        this.affectedPlate = affectedPlate;
        this.newSpaceCode = newSpaceCode;
        this.reservationCancelled = reservationCancelled;
    }

    /**
     * Gets the code of the removed space.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current deleted space code
     */
    public String getDeletedSpaceCode() {
        return deletedSpaceCode;
    }

    /**
     * Gets the plate affected by the deletion.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current affected plate
     */
    public String getAffectedPlate() {
        return affectedPlate;
    }

    /**
     * Gets the replacement space code.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current new space code
     */
    public String getNewSpaceCode() {
        return newSpaceCode;
    }

    /**
     * Returns whether the reservation was cancelled.
     * <p>
     * This method keeps the business decision in the service layer before anything is sent back to the
     * screen.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    public boolean isReservationCancelled() {
        return reservationCancelled;
    }

    /**
     * Returns whether a reservation was affected.
     * <p>
     * This method keeps the business decision in the service layer before anything is sent back to the
     * screen.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    public boolean hasAffectedReservation() {
        return affectedPlate != null && !affectedPlate.isBlank();
    }
}
