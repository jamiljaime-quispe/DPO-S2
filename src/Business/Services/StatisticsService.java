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
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class StatisticsService {
	private static final int LAST_HOUR_MINUTES = 60;

	private OccupancyTracker occupancyTracker;
	private ParkingSpaceDAO parkingSpaceDAO;
	private OccupancyDAO occupancyDAO;

	/**
	 * Constructs a new StatisticsService.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param occupancyTracker the in-memory occupancy history buffer
	 * @param parkingSpaceDAO the DAO used to count occupied spaces
	 * @param occupancyDAO the DAO used to persist and retrieve occupancy records
	 */
	public StatisticsService(OccupancyTracker occupancyTracker, ParkingSpaceDAO parkingSpaceDAO,
							 OccupancyDAO occupancyDAO) {
		this.occupancyTracker = occupancyTracker;
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.occupancyDAO = occupancyDAO;
	}

	/**
	 * Handles calculate current occupancy.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
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
	 * Records the current occupancy count to the database and the in-memory tracker. Should be called once per
	 * minute (e.g. from a Swing Timer or scheduled executor).
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	public void recordOccupancy() {
		int count = calculateCurrentOccupancy();
		saveOccupancyRecord(count);
		recordOccupancyInMemory(count);
	}

	/**
	 * Returns occupancy data for the last hour (up to 60 entries, one per minute).
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return ordered list of occupancy records (oldest first)
	 */
	public List<OccupancyRecord> getLastHourData() {
		return buildLastHourSeries(loadLastHourOccupancyData());
	}

	/**
	 * Loads parking spaces.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @return the loaded parking spaces
	 */
	private List<ParkingSpace> loadParkingSpaces() {
		return parkingSpaceDAO.findAll();
	}

	/**
	 * Handles save occupancy record.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param count count used by this operation
	 */
	private void saveOccupancyRecord(int count) {
		occupancyDAO.saveRecord(LocalDateTime.now(), count);
	}

	/**
	 * Handles record occupancy in memory.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param count count used by this operation
	 */
	private void recordOccupancyInMemory(int count) {
		occupancyTracker.recordOccupancy(count);
	}

	/**
	 * Loads last hour occupancy data.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @return the loaded last hour occupancy data
	 */
	private List<OccupancyRecord> loadLastHourOccupancyData() {
		return occupancyDAO.getLastHourData();
	}

	/**
	 * Builds last hour series.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param records records used by this operation
	 * @return the built last hour series
	 */
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

	/**
	 * Finds latest epoch minute.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param records records used by this operation
	 * @return the matching latest epoch minute, or null when it is not found
	 */
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

	/**
	 * Gets the occupancy count stored for one minute, or 0 if that minute has no record.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param records records used by this operation
	 * @param minute minute used by this operation
	 * @return the matching count for minute, or null when it is not found
	 */
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
	 * Handles to epoch minute.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param timestamp date to convert
	 * @return epoch minute for the date
	 */
	private long toEpochMinute(LocalDateTime timestamp) {
		return timestamp.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond() / 60;
	}

	/**
	 * Gets the current minute using January 1, 1970 as reference.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return current epoch minute
	 */
	private long getCurrentEpochMinute() {
		return Instant.now().getEpochSecond() / 60;
	}

	/**
	 * Handles to local date time.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param epochMinute minute elapsed since January 1, 1970
	 * @return local date-time at that minute
	 */
	private LocalDateTime toLocalDateTime(long epochMinute) {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochMinute * 60), ZoneId.systemDefault());
	}
}
