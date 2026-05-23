package Business.Listeners;

/**
 * Listener notified whenever the parking status changes (vehicle enters, exits, or the simulation updates a space).
 * Implemented by ParkingController to trigger real-time UI refreshes.
 */
public interface ParkingStatusChangeListener {
    /** Called when any parking space changes state. */
    void parkingStatusChanged();
}
