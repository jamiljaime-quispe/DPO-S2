package Business.Services;

import Persistence.OccupancyDAO;
import Business.Entities.OccupancyRecord;
import Persistence.ParkingSpaceDAO;
import Business.Entities.OccupancyTracker;
import Business.Entities.ParkingSpace;

import java.util.ArrayList;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Calculates and records parking occupancy statistics.
 */
public class StatisticsService {
	private static final int LAST_HOUR_MINUTES = 60;

	private OccupancyTracker occupancyTracker;
	private ParkingSpaceDAO parkingSpaceDAO;
	private OccupancyDAO occupancyDAO;

	/**
	 * Constructs a new StatisticsService.
	 *
	 * @param occupancyTracker the in-memory occupancy history buffer
	 * @param parkingSpaceDAO  the DAO used to count occupied spaces
	 * @param occupancyDAO     the DAO used to persist and retrieve occupancy records
	 */
	public StatisticsService(OccupancyTracker occupancyTracker, ParkingSpaceDAO parkingSpaceDAO,
							 OccupancyDAO occupancyDAO) {
		this.occupancyTracker = occupancyTracker;
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.occupancyDAO = occupancyDAO;
	}

	/**
	 * Counts the number of currently occupied parking spaces.
	 * @return count of occupied spaces
	 */
	public int calculateCurrentOccupancy() {
		List<ParkingSpace> spaces = loadParkingSpaces();
		int count = 0;
		for (ParkingSpace space : spaces) {
			if (space.isOccupied()) count++;
		}
		return count;
	}

	/**
	 * Records the current occupancy count to the database and the in-memory tracker.
	 * Should be called once per minute (e.g. from a Swing Timer or scheduled executor).
	 */
	public void recordOccupancy() {
		int count = calculateCurrentOccupancy();
		saveOccupancyRecord(count);
		recordOccupancyInMemory(count);
	}

	/**
	 * Returns occupancy data for the last hour (up to 60 entries, one per minute).
	 * @return ordered list of occupancy records (oldest first)
	 */
	public List<OccupancyRecord> getLastHourData() {
		return buildLastHourSeries(loadLastHourOccupancyData());
	}

	/** Loads all parking spaces from persistence. */
	private List<ParkingSpace> loadParkingSpaces() {
		return parkingSpaceDAO.findAll();
	}

	/** Saves one occupancy count in persistence. */
	private void saveOccupancyRecord(int count) {
		occupancyDAO.saveRecord(LocalDateTime.now(), count);
	}

	/** Stores one occupancy count in the in-memory tracker. */
	private void recordOccupancyInMemory(int count) {
		occupancyTracker.recordOccupancy(count);
	}

	/** Loads the occupancy data recorded during the last hour. */
	private List<OccupancyRecord> loadLastHourOccupancyData() {
		return occupancyDAO.getLastHourData();
	}

	/** Builds a 60-minute series where each position represents one minute of the last hour. */
	private List<OccupancyRecord> buildLastHourSeries(List<OccupancyRecord> records) {
		List<OccupancyRecord> series = new ArrayList<>();
		long latestMinute = findLatestEpochMinute(records);

		for (int minutesAgo = LAST_HOUR_MINUTES - 1; minutesAgo >= 0; minutesAgo--) {
			long minute = latestMinute - minutesAgo;
			int count = findCountForMinute(records, minute);
			series.add(new OccupancyRecord(toLocalDateTime(minute), count));
		}

		return series;
	}

	/** Finds the latest recorded epoch minute, or the current minute when no records exist. */
	private long findLatestEpochMinute(List<OccupancyRecord> records) {
		if (records == null || records.isEmpty()) {
			return getCurrentEpochMinute();
		}

		long latest = -1;
		for (OccupancyRecord record : records) {
			long minute = toEpochMinute(record.getTimestamp());
			if (latest == -1 || minute > latest) {
				latest = minute;
			}
		}
		return latest;
	}

	/** Gets the occupancy count stored for one minute, or 0 if that minute has no record. */
	private int findCountForMinute(List<OccupancyRecord> records, long minute) {
		if (records == null) {
			return 0;
		}

		int count = 0;
		for (OccupancyRecord record : records) {
			long recordMinute = toEpochMinute(record.getTimestamp());
			if (minute == recordMinute) {
				count = record.getOccupiedCount();
			}
		}
		return count;
	}

	/**
	 * Converts a date to minutes elapsed since January 1, 1970.
	 *
	 * @param timestamp date to convert
	 * @return epoch minute for the date
	 */
	private long toEpochMinute(LocalDateTime timestamp) {
		return timestamp.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond() / 60;
	}

	/**
	 * Gets the current minute using January 1, 1970 as reference.
	 *
	 * @return current epoch minute
	 */
	private long getCurrentEpochMinute() {
		return Instant.now().getEpochSecond() / 60;
	}

	/**
	 * Converts an epoch minute back to local date-time for display.
	 *
	 * @param epochMinute minute elapsed since January 1, 1970
	 * @return local date-time at that minute
	 */
	private LocalDateTime toLocalDateTime(long epochMinute) {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochMinute * 60), ZoneId.systemDefault());
	}
}
