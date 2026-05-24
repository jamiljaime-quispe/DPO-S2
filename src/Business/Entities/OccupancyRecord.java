package Business.Entities;

import java.time.LocalDateTime;

/**
 * Stores one occupancy value with the minute when it was recorded.
 * <p>
 * The class stores project data in a clear object so the services, controllers, and persistence code can
 * pass the same information around safely.
 * </p>
 */
public class OccupancyRecord {
    private final LocalDateTime timestamp;
    private final int occupiedCount;

    /**
     * Creates one occupancy record.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param timestamp minute when the count was recorded
     * @param occupiedCount number of occupied parking spaces
     */
    public OccupancyRecord(LocalDateTime timestamp, int occupiedCount) {
        this.timestamp = timestamp;
        this.occupiedCount = occupiedCount;
    }

    /**
     * Gets the minute when the count was recorded.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return record timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the number of occupied parking spaces.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return occupied-space count
     */
    public int getOccupiedCount() {
        return occupiedCount;
    }
}
