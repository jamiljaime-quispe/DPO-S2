package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;

/**
 * Stores the result of a parking entry attempt.
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
 */
class ParkingEntryResult {
    private ParkingEntryStatus status;
    private ParkingSpace space;
    private String message;

    /**
     * Creates one parking entry result.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param status result status
     * @param space related parking space, when one exists
     * @param message message to show, when needed
     */
    private ParkingEntryResult(ParkingEntryStatus status, ParkingSpace space, String message) {
        this.status = status;
        this.space = space;
        this.message = message;
    }

    /**
     * Handles needs vehicle type.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return the result of the operation
     */
    static ParkingEntryResult needsVehicleType() {
        return new ParkingEntryResult(ParkingEntryStatus.NEEDS_VEHICLE_TYPE, null, null);
    }

    /**
     * Handles assigned from reservation.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param space space used by this operation
     * @return the result of the operation
     */
    static ParkingEntryResult assignedFromReservation(ParkingSpace space) {
        return new ParkingEntryResult(ParkingEntryStatus.ASSIGNED_FROM_RESERVATION, space, null);
    }

    /**
     * Handles assigned without reservation.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param space space used by this operation
     * @return the result of the operation
     */
    static ParkingEntryResult assignedWithoutReservation(ParkingSpace space) {
        return new ParkingEntryResult(ParkingEntryStatus.ASSIGNED_WITHOUT_RESERVATION, space, null);
    }

    /**
     * Handles already parked.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param space space used by this operation
     * @return the result of the operation
     */
    static ParkingEntryResult alreadyParked(ParkingSpace space) {
        return new ParkingEntryResult(ParkingEntryStatus.ALREADY_PARKED, space, null);
    }

    /**
     * Handles no space.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param type vehicle type involved in the operation
     * @return the result of the operation
     */
    static ParkingEntryResult noSpace(VehicleType type) {
        return new ParkingEntryResult(ParkingEntryStatus.NO_SPACE, null,
                "No vacant unreserved " + type.name() + " spaces are available.");
    }

    /**
     * Handles error.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param message message shown to the user or written to the log
     * @return the result of the operation
     */
    static ParkingEntryResult error(String message) {
        return new ParkingEntryResult(ParkingEntryStatus.ERROR, null, message);
    }

    /**
     * Gets the result status.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current status
     */
    ParkingEntryStatus getStatus() {
        return status;
    }

    /**
     * Gets the related parking space.
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
     * Gets the result message.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current message
     */
    String getMessage() {
        return message;
    }
}
