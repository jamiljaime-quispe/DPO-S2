package Business.Listeners;

/**
 * Listener notified whenever the parking status changes (vehicle enters, exits, or the simulation updates a
 * space). Implemented by ParkingController to trigger real-time UI refreshes.
 * <p>
 * The interface describes what another class must do, which lets the project connect parts together without
 * depending on one concrete class.
 * </p>
 */
public interface ParkingStatusChangeListener {
    /**
     * Handles parking status changed.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     */
    void parkingStatusChanged();

    /**
     * Handles parking status changed.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param message message describing the parking change
     */
    void parkingStatusChanged(String message);
}
