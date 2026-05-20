package Business.Services;

import Business.Entities.Config;
import Business.Entities.ParkingSpace;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Business.Listeners.ParkingStatusChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulates random vehicle traffic in the parking lot using a background thread.
 * Simulated vehicles only use unreserved spaces.
 */
public class SimulationService implements Runnable {
	private static final int CHAOS_MODE_DELAY_MS = 1000;

	// The simulation uses ParkingService so it can reuse the same entry/exit logic as the real app.
	private ParkingService parkingService;
	// The config gives the simulation its maximum random delay between bot actions.
	private Config config;
	// Random is used to choose entry/exit, vehicle type, delay, and simulated plates.
	private Random random;
	// This list tracks only the vehicles created by the simulation, so bots only exit bot vehicles.
	private List<Vehicle> simulatedVehicles;

	// This is the actual Java thread object that runs the simulation loop in the background.
	private Thread simulationThread;
	// This flag controls whether the run() loop should keep going; volatile makes updates visible across threads.
	private volatile boolean running = false;

    //Listeners:
    // This will listen to whenever we need to update the UI with a parking status change
    private ParkingStatusChangeListener parkingStatusChangeListener;

    // And of course, its respective setter
    public void setParkingStatusChangeListener(ParkingStatusChangeListener parkingStatusChangeListener){
        this.parkingStatusChangeListener = parkingStatusChangeListener;
    }

    private void notifyParkingStatusChanged() {
        // Whenever the listener has been activated, it will not be null
        if (parkingStatusChangeListener != null) {
            parkingStatusChangeListener.parkingStatusChanged();
        }
    }

	/**
	 * Constructs a new SimulationService.
	 *
	 * @param parkingService    the parking service for entry/exit operations
	 * @param config            the application config (provides simulatedVehicleDelay)
	 * @param random            the random number generator
	 * @param simulatedVehicles the mutable list tracking currently simulated vehicles
	 */
	public SimulationService(ParkingService parkingService, Config config, Random random,
							 List<Vehicle> simulatedVehicles) {
		// Store the business service used later to update the database through normal parking logic.
		this.parkingService = parkingService;
		// Store the config so run() can read the delay between simulated actions.
		this.config = config;
		// Store the random generator so tests or callers can provide predictable randomness if needed.
		this.random = random;
		// Use the provided simulated-vehicle list, or create an empty one if none was provided.
		this.simulatedVehicles = simulatedVehicles != null ? simulatedVehicles : new ArrayList<>();
	}

	@Override
	public void run() {
		// The thread keeps looping until stopSimulation() changes running to false.
		while (running) {
			// One loop iteration means one simulated decision: either one entry or one exit.
			simulateStep();
			try {
				// The configured delay is treated as a maximum, but never less than one second.
				int maxDelay = Math.max(1, config.getSimulatedVehicleDelay());
				// Pick a random delay from 1 second up to maxDelay seconds.
				int delay = random.nextInt(maxDelay) + 1;
				// Pause this background thread; this does not freeze the Swing EDT.
				Thread.sleep(delay * 1000L);

				// Chaos mode for UI stress testing.
				// Thread.sleep(CHAOS_MODE_DELAY_MS);
			} catch (InterruptedException e) {
				// Restore the interrupted flag so the thread records that it was asked to stop.
				Thread.currentThread().interrupt();
				// Leave the loop immediately after an interrupt, usually caused by stopSimulation().
				break;
			}
		}
	}

	/**
	 * Starts the simulation loop in a background thread.
	 * Does nothing if the simulation is already running.
	 */
	public void startSimulation() {
		// If the simulation is already running, do not create a second background thread.
		if (running) return;
		// Turn on the loop condition before the thread starts.
		running = true;
		// Create a Thread whose task is this SimulationService object's run() method.
		simulationThread = new Thread(this);
		// Start the real background thread; Java will call run() on that new thread.
		simulationThread.start();
	}

	/**
	 * Stops the simulation loop and interrupts the background thread.
	 */
	public void stopSimulation() {
		// Tell the run() loop to stop after the current iteration.
		running = false;
		// If the thread is sleeping, interrupt wakes it so it can stop right away.
		if (simulationThread != null) simulationThread.interrupt();
	}

	/**
	 * Performs one simulation step: decides entry or exit based on occupancy probability
	 * and executes the corresponding action.
	 */
	public void simulateStep() {
		// Calculate how likely the next bot action should be an entry instead of an exit.
		double entryProbability = calculateEntryProbability();
		// random.nextDouble() returns a value from 0.0 to less than 1.0.
		if (random.nextDouble() < entryProbability) {
			// If the random value is below the probability, simulate one vehicle entering.
			simulateEntry();
		} else {
			// Otherwise, simulate one vehicle leaving.
			simulateExit();
		}

		// Chaos mode for UI stress testing.
		// boolean shouldEnter = random.nextBoolean();
		// if (shouldEnter || simulatedVehicles.isEmpty()) {
		// 	simulateEntry();
		// } else {
		// 	simulateExit();
		// }
	}

	/**
	 * Simulates a vehicle entering the lot.
	 * Picks a random vehicle type and assigns an unreserved available space.
	 */
	public void simulateEntry() {
		// Get every supported vehicle type so the bot can pick one randomly.
		VehicleType[] types = VehicleType.values();
		// Pick one vehicle type at random for this simulated entry.
		VehicleType type = types[random.nextInt(types.length)];

		// Ask the business layer for vacant, unreserved spaces that accept that type.
		List<ParkingSpace> available = parkingService.findAvailableSpaces(type);
		// If no compatible space exists, this bot entry cannot happen.
		if (available == null || available.isEmpty()) return;

		// Generate a fake license plate for the simulated vehicle.
		String plate = generateRandomPlate();
		// Reuse normal parking entry logic; this updates the database if a space is assigned.
		ParkingSpace assigned = parkingService.handleVehicleEntry(plate, type);
		// If ParkingService found a place, remember this simulated vehicle so it can exit later.
		if (assigned != null) {
            // Bot was parked successfully
            notifyParkingStatusChanged();
			// Store only simulated vehicles here; real user vehicles are not added to this list.
			simulatedVehicles.add(new Vehicle(plate, type, "SIMULATED", true));
			// Print a small trace in the console so we can see bot activity while testing.
			System.out.println("[SIM] Entry: " + plate + " -> " + assigned.getId());
		}
	}

	/**
	 * Simulates a vehicle exiting the lot.
	 * Picks a random vehicle from the simulated list and removes it.
	 */
	public void simulateExit() {
		// If the bot has no simulated vehicles parked, there is nothing for it to exit.
		if (simulatedVehicles.isEmpty()) return;
		// Pick one currently parked simulated vehicle at random.
		int idx = random.nextInt(simulatedVehicles.size());
		// Get that simulated vehicle from the in-memory list.
		Vehicle vehicle = simulatedVehicles.get(idx);
		// Reuse normal parking exit logic; this frees the vehicle's database space.
		parkingService.handleVehicleExit(vehicle.getLicensePlate());
        // Bot has left
        notifyParkingStatusChanged();
		// Print a small trace in the console so we can see bot exit activity while testing.
		System.out.println("[SIM] Exit:  " + vehicle.getLicensePlate());
		// Remove the vehicle from the simulated list because it is no longer parked.
		simulatedVehicles.remove(idx);
	}

	/**
	 * Calculates the probability of entry for the next simulation step.
	 * Formula: vacantUnreservedSpaces / totalUnreservedSpaces
	 * Returns 0 if there are no unreserved spaces.
	 * @return entry probability in [0, 1]
	 */
	public double calculateEntryProbability() {
		// Load the current parking state from the database through ParkingService.
		List<ParkingSpace> allSpaces = parkingService.getAllSpaces();
		// If the lot has no spaces, the bot cannot enter.
		if (allSpaces == null || allSpaces.isEmpty()) return 0;

		// Count all spaces that are not reserved.
		int totalUnreserved = 0;
		// Count only the unreserved spaces that are also vacant.
		int vacantUnreserved = 0;
		// Check every space from the current database snapshot.
		for (ParkingSpace space : allSpaces) {
			// Bots are only allowed to use spaces that are not reserved.
			if (!space.isReserved()) {
				// This space is part of the unreserved total.
				totalUnreserved++;
				// If the unreserved space is empty, it increases the entry probability.
				if (!space.isOccupied()) vacantUnreserved++;
			}
		}
		// If every space is reserved, bots should never enter.
		if (totalUnreserved == 0) return 0;
		// More vacant unreserved spaces means a higher chance of entry.
		return (double) vacantUnreserved / totalUnreserved;
	}

	/**
	 * Generates a random license plate for a simulated vehicle.
	 * Format: "SIM-" followed by 4 zero-padded random digits.
	 * @return generated plate string
	 */
	public String generateRandomPlate() {
		// Create a plate like SIM-0427 so simulated cars are easy to recognize.
		return "SIM-" + String.format("%04d", random.nextInt(10000));
	}
}
