package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;

/**
 * Stores the result of a parking entry attempt.
 */
class ParkingEntryResult {
    private ParkingEntryStatus status;
    private ParkingSpace space;
    private String message;

    /**
     * Creates one parking entry result.
     *
     * @param status  result status
     * @param space   related parking space, when one exists
     * @param message message to show, when needed
     */
    private ParkingEntryResult(ParkingEntryStatus status, ParkingSpace space, String message) {
        this.status = status;
        this.space = space;
        this.message = message;
    }

    /** Creates a result that asks the user for vehicle type. */
    static ParkingEntryResult needsVehicleType() {
        return new ParkingEntryResult(ParkingEntryStatus.NEEDS_VEHICLE_TYPE, null, null);
    }

    /** Creates a result for a reserved-space entry. */
    static ParkingEntryResult assignedFromReservation(ParkingSpace space) {
        return new ParkingEntryResult(ParkingEntryStatus.ASSIGNED_FROM_RESERVATION, space, null);
    }

    /** Creates a result for an entry without reservation. */
    static ParkingEntryResult assignedWithoutReservation(ParkingSpace space) {
        return new ParkingEntryResult(ParkingEntryStatus.ASSIGNED_WITHOUT_RESERVATION, space, null);
    }

    /** Creates a result for a vehicle that is already parked. */
    static ParkingEntryResult alreadyParked(ParkingSpace space) {
        return new ParkingEntryResult(ParkingEntryStatus.ALREADY_PARKED, space, null);
    }

    /** Creates a result for a full compatible parking area. */
    static ParkingEntryResult noSpace(VehicleType type) {
        return new ParkingEntryResult(ParkingEntryStatus.NO_SPACE, null,
                "No vacant unreserved " + type.name() + " spaces are available.");
    }

    /** Creates a result for an entry error. */
    static ParkingEntryResult error(String message) {
        return new ParkingEntryResult(ParkingEntryStatus.ERROR, null, message);
    }

    /** Gets the result status. */
    ParkingEntryStatus getStatus() {
        return status;
    }

    /** Gets the related parking space. */
    ParkingSpace getSpace() {
        return space;
    }

    /** Gets the result message. */
    String getMessage() {
        return message;
    }
}
