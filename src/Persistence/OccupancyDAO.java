package Persistence;

import Business.Entities.OccupancyRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access interface for parking occupancy logs. This interface keeps the promise clear so another class
 * can use it without depending on a specific implementation.
 * <p>
 * The interface lets the business layer ask for stored data without depending on the class that talks
 * directly to the database.
 * </p>
 */
public interface OccupancyDAO {

    /**
     * Handles save record.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param timestamp the moment the snapshot was taken
     * @param occupancy the number of occupied spaces at that moment
     */
    void saveRecord(LocalDateTime timestamp, int occupancy);

    /**
     * Gets the last hour data.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return list of occupancy records; may be empty
     */
    List<OccupancyRecord> getLastHourData();
}
