package Persistence;

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
     * Retrieves the last 60 occupancy records ordered from oldest to newest.
     *
     * @return list of occupied-space counts; may be empty
     */
    List<Integer> getLastHourData();
}
