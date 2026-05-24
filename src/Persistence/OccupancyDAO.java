package Persistence;

import Business.Entities.OccupancyRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access interface for parking occupancy logs.
 */
public interface OccupancyDAO {

    /**
     * Saves an occupancy snapshot to the log.
     *
     * @param timestamp the moment the snapshot was taken
     * @param occupancy the number of occupied spaces at that moment
     */
    void saveRecord(LocalDateTime timestamp, int occupancy);

    /**
     * Retrieves occupancy records from the last hour ordered from oldest to newest.
     *
     * @return list of occupancy records; may be empty
     */
    List<OccupancyRecord> getLastHourData();
}
