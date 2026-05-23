package Business.Services;

import Persistence.OccupancyDAO;
import Persistence.ParkingSpaceDAO;
import Business.Entities.OccupancyTracker;
import Business.Entities.ParkingSpace;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Calculates and records parking occupancy statistics.
 */
public class StatisticsService {
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
	 * @return ordered list of occupancy counts (oldest first)
	 */
	public List<Integer> getLastHourData() {
		return loadLastHourOccupancyData();
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
	private List<Integer> loadLastHourOccupancyData() {
		return occupancyDAO.getLastHourData();
	}
}
