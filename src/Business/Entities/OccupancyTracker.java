package Business.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Tracks parking occupancy over time using a bounded queue. Holds up to {@code maxHistorySize} entries (one
 * per minute, 60 for the last hour).
 * <p>
 * The class stores project data in a clear object so the services, controllers, and persistence code can
 * pass the same information around safely.
 * </p>
 */
public class OccupancyTracker {
	private Queue<Integer> occupancyHistory;
	private int maxHistorySize;

	/**
	 * Constructs a new OccupancyTracker.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param occupancyHistory the backing queue (typically a LinkedList)
	 * @param maxHistory the maximum number of entries to retain
	 */
	public OccupancyTracker(Queue<Integer> occupancyHistory, int maxHistory) {
		this.occupancyHistory = occupancyHistory;
		this.maxHistorySize = maxHistory;
	}

	/**
	 * Records a new occupancy count. If the queue is full, the oldest entry is removed before adding the new
	 * one.
	 * <p>
	 * This helper keeps the step named and separate, which makes the larger operation easier to read and
	 * follow.
	 * </p>
	 *
	 * @param count the number of currently occupied spaces
	 */
	public void recordOccupancy(int count) {
		if (occupancyHistory.size() >= maxHistorySize) {
			occupancyHistory.poll();
		}
		occupancyHistory.offer(count);
	}

}
