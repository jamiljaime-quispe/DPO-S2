package Presentation.Controllers;

/**
 * Possible results of a parking entry attempt.
 */
enum ParkingEntryStatus {
    NEEDS_VEHICLE_TYPE,
    ASSIGNED_FROM_RESERVATION,
    ASSIGNED_WITHOUT_RESERVATION,
    ALREADY_PARKED,
    NO_SPACE,
    ERROR
}
