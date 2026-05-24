package Presentation.Controllers;

/**
 * Possible results of a parking entry attempt. This enum keeps the allowed values in one place, which makes
 * the rest of the code easier to read and harder to misuse.
 * <p>
 * The enum keeps the allowed options in one place so the project does not rely on repeated text values.
 * </p>
 */
enum ParkingEntryStatus {
    NEEDS_VEHICLE_TYPE,
    ASSIGNED_FROM_RESERVATION,
    ASSIGNED_WITHOUT_RESERVATION,
    ALREADY_PARKED,
    NO_SPACE,
    ERROR
}
