package Business.Entities;

import java.time.LocalDateTime;

/**
 * Stores one occupancy value with the minute when it was recorded.
 */
public class OccupancyRecord {
    private final LocalDateTime timestamp;
    private final int occupiedCount;

    /**
     * Creates one occupancy record.
     *
     * @param timestamp     minute when the count was recorded
     * @param occupiedCount number of occupied parking spaces
     */
    public OccupancyRecord(LocalDateTime timestamp, int occupiedCount) {
        this.timestamp = timestamp;
        this.occupiedCount = occupiedCount;
    }

    /**
     * Gets the minute when the count was recorded.
     *
     * @return record timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the number of occupied parking spaces.
     *
     * @return occupied-space count
     */
    public int getOccupiedCount() {
        return occupiedCount;
    }
}
