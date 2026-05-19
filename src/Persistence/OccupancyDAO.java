package Persistence;

import java.time.LocalDateTime;
import java.util.List;

public interface OccupancyDAO {
	void saveRecord(LocalDateTime timestamp, int occupancy);

	List<Integer> getLastHourData();
}