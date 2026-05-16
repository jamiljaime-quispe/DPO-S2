package Business.Services;

import Persistence.OccupancyDAO;
import Persistence.ParkingSpaceDAO;
import Business.Entities.OccupancyTracker;
import Business.Entities.ParkingSpace;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Calculates and records parking occupancy statistics.
 * NOTE: constructor signature extended with parkingSpaceDAO and occupancyDAO
 * compared to the original UML stub — update Main.java wiring accordingly.
 */
public class StatisticsService {
	private OccupancyTracker occupancyTracker;
	private ParkingSpaceDAO parkingSpaceDAO;
	private OccupancyDAO occupancyDAO;

	/**
	 * @param occupancyTracker in-memory occupancy history buffer
	 * @param parkingSpaceDAO  DAO used to count occupied spaces
	 * @param occupancyDAO     DAO used to persist and retrieve occupancy records
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
		List<ParkingSpace> spaces = parkingSpaceDAO.findAll();
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
		occupancyDAO.saveRecord(LocalDateTime.now(), count);
		occupancyTracker.recordOccupancy(count);
	}

	/**
	 * Returns occupancy data for the last hour (up to 60 entries, one per minute).
	 * @return ordered list of occupancy counts (oldest first)
	 */
	public List<Integer> getLastHourData() {
		return occupancyDAO.getLastHourData();
	}
}
