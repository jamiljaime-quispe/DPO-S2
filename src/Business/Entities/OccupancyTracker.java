package Business.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Tracks parking occupancy over time using a bounded queue.
 * Holds up to {@code maxHistorySize} entries (one per minute = 60 for the last hour).
 */
public class OccupancyTracker {
	private Queue<Integer> occupancyHistory;
	private int maxHistorySize;

	/**
	 * @param occupancyHistory backing queue (typically a LinkedList)
	 * @param maxHistory       maximum number of entries to retain
	 */
	public OccupancyTracker(Queue<Integer> occupancyHistory, int maxHistory) {
		this.occupancyHistory = occupancyHistory;
		this.maxHistorySize = maxHistory;
	}

	/**
	 * Records a new occupancy count. If the queue is full, the oldest entry is removed.
	 * @param count number of currently occupied spaces
	 */
	public void recordOccupancy(int count) {
		if (occupancyHistory.size() >= maxHistorySize) {
			occupancyHistory.poll();
		}
		occupancyHistory.offer(count);
	}

	/**
	 * @return ordered list of all recorded occupancy values (oldest first)
	 */
	public List<Integer> getHistory() {
		return new ArrayList<>(occupancyHistory);
	}

	/**
	 * @return the most recently recorded occupancy count, or 0 if no data
	 */
	public int getCurrentOccupancy() {
		if (occupancyHistory.isEmpty()) return 0;
		int last = 0;
		for (int value : occupancyHistory) {
			last = value;
		}
		return last;
	}
}
