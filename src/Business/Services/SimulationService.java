package Business.Services;

import Business.Entities.Config;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulates random vehicle traffic in the parking lot using a background daemon thread.
 * Simulated vehicles only use unreserved spaces.
 */
public class SimulationService {
	private ParkingService parkingService;
	private Config config;
	private Random random;
	private List<Vehicle> simulatedVehicles;

	private Thread simulationThread;
	private volatile boolean running = false;

	/**
	 * @param parkingService    parking service for entry/exit operations
	 * @param config            application config (provides simulatedVehicleDelay)
	 * @param random            random number generator
	 * @param simulatedVehicles mutable list tracking currently simulated vehicles
	 */
	public SimulationService(ParkingService parkingService, Config config, Random random,
							 List<Vehicle> simulatedVehicles) {
		this.parkingService = parkingService;
		this.config = config;
		this.random = random;
		this.simulatedVehicles = simulatedVehicles != null ? simulatedVehicles : new ArrayList<>();
	}

	/**
	 * Starts the simulation loop in a background daemon thread.
	 * Does nothing if the simulation is already running.
	 */
	public void startSimulation() {
		if (running) return;
		running = true;
		simulationThread = new Thread(() -> {
			while (running) {
				simulateStep();
				try {
					int maxDelay = Math.max(1, config.getSimulatedVehicleDelay());
					int delay = random.nextInt(maxDelay) + 1;
					Thread.sleep(delay * 1000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		});
		simulationThread.setDaemon(true);
		simulationThread.setName("SimulationThread");
		simulationThread.start();
	}

	/**
	 * Stops the simulation loop and interrupts the background thread.
	 */
	public void stopSimulation() {
		running = false;
		if (simulationThread != null) simulationThread.interrupt();
	}

	/**
	 * Performs one simulation step: decides entry or exit based on occupancy probability
	 * and executes the corresponding action.
	 */
	public void simulateStep() {
		double entryProbability = calculateEntryProbability();
		if (random.nextDouble() < entryProbability) {
			simulateEntry();
		} else {
			simulateExit();
		}
	}

	/**
	 * Simulates a vehicle entering the lot.
	 * Picks a random vehicle type and assigns an unreserved available space.
	 */
	public void simulateEntry() {
		VehicleType[] types = VehicleType.values();
		VehicleType type = types[random.nextInt(types.length)];

		List<ParkingSpace> available = parkingService.findAvailableSpaces(type);
		if (available == null || available.isEmpty()) return;

		String plate = generateRandomPlate();
		ParkingSpace assigned = parkingService.handleVehicleEntry(plate, type);
		if (assigned != null) {
			simulatedVehicles.add(new Vehicle(plate, type, "SIMULATED", true));
		}
	}

	/**
	 * Simulates a vehicle exiting the lot.
	 * Picks a random vehicle from the simulated list and removes it.
	 */
	public void simulateExit() {
		if (simulatedVehicles.isEmpty()) return;
		int idx = random.nextInt(simulatedVehicles.size());
		Vehicle vehicle = simulatedVehicles.get(idx);
		parkingService.handleVehicleExit(vehicle.getLicensePlate());
		simulatedVehicles.remove(idx);
	}

	/**
	 * Calculates the probability of entry for the next simulation step.
	 * Formula: vacantUnreservedSpaces / totalUnreservedSpaces
	 * Returns 0 if there are no unreserved spaces.
	 * @return entry probability in [0, 1]
	 */
	public double calculateEntryProbability() {
		List<ParkingSpace> allSpaces = parkingService.getAllSpaces();
		if (allSpaces == null || allSpaces.isEmpty()) return 0;

		int totalUnreserved = 0;
		int vacantUnreserved = 0;
		for (ParkingSpace space : allSpaces) {
			if (!space.isReserved()) {
				totalUnreserved++;
				if (!space.isOccupied()) vacantUnreserved++;
			}
		}
		if (totalUnreserved == 0) return 0;
		return (double) vacantUnreserved / totalUnreserved;
	}

	/**
	 * Generates a random license plate for a simulated vehicle.
	 * Format: "SIM-" followed by 4 zero-padded random digits.
	 * @return generated plate string
	 */
	public String generateRandomPlate() {
		return "SIM-" + String.format("%04d", random.nextInt(10000));
	}
}
