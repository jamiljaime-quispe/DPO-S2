package Business.Services;

import Business.Entities.Config;
import Business.Entities.ParkingSpace;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Business.Listeners.ParkingStatusChangeListener;
import Persistence.VehicleDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulates vehicle traffic in the parking lot using a background thread.
 * Simulated vehicles only use spaces that are not reserved.
 */
public class SimulationService implements Runnable {
	private static final int CHAOS_MODE_DELAY_MS = 1000;

	private ParkingService parkingService;
	private Config config;
	private Random random;
	private List<Vehicle> simulatedVehicles;
	private VehicleDAO vehicleDAO;
	private Thread simulationThread;
	private volatile boolean running = false;
	private ParkingStatusChangeListener parkingStatusChangeListener;

	/**
	 * Constructs a new SimulationService.
	 *
	 * @param parkingService    service used for entry and exit
	 * @param config            simulation configuration
	 * @param random            random number generator
	 * @param simulatedVehicles list of currently parked simulated vehicles
	 * @param vehicleDAO        DAO used to avoid duplicate simulated plates
	 */
	public SimulationService(ParkingService parkingService, Config config, Random random,
			List<Vehicle> simulatedVehicles, VehicleDAO vehicleDAO) {
		this.parkingService = parkingService;
		this.config = config;
		this.random = random;
		this.simulatedVehicles = simulatedVehicles != null ? simulatedVehicles : new ArrayList<>();
		this.vehicleDAO = vehicleDAO;
	}

	/** Sets the listener notified after simulation changes. */
	public void setParkingStatusChangeListener(ParkingStatusChangeListener parkingStatusChangeListener) {
		this.parkingStatusChangeListener = parkingStatusChangeListener;
	}

	/**
	 * Runs the simulation loop until it is stopped.
	 * Errors in one simulation step are reported and the next step can still run.
	 */
	/** Runs the background simulation loop. */
	@Override
	public void run() {
		while (running) {
			try {
				simulateStep();
			} catch (RuntimeException e) {
				String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
				notifyParkingStatusChanged("[SIM] Error: " + message);
			}

			try {
				int maxDelay = Math.max(1, config.getSimulatedVehicleDelay());
				int delay = random.nextInt(maxDelay) + 1;
				Thread.sleep(delay * 1000L);

				// Chaos mode for UI stress testing.
				// Thread.sleep(CHAOS_MODE_DELAY_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	/** Starts the simulation in its own background thread. */
	public void startSimulation() {
		if (running) return;

		running = true;
		simulationThread = new Thread(this);
		simulationThread.start();
	}

	/** Stops the simulation and wakes the thread if it is sleeping. */
	public void stopSimulation() {
		running = false;
		if (simulationThread != null) simulationThread.interrupt();
	}

	/** Performs one simulated entry or exit decision. */
	public void simulateStep() {
		double entryProbability = calculateEntryProbability();
		if (random.nextDouble() < entryProbability) {
			simulateEntry();
		} else {
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

	/** Simulates one vehicle entering the parking lot. */
	public void simulateEntry() {
		VehicleType[] types = VehicleType.values();
		VehicleType type = types[random.nextInt(types.length)];

		List<ParkingSpace> available = parkingService.findAvailableSpaces(type);
		if (available == null || available.isEmpty()) return;

		String plate = generateRandomPlate();
		ParkingSpace assigned = parkingService.handleVehicleEntry(plate, type);
		if (assigned != null) {
			notifyParkingStatusChanged("[SIM] Entry: " + plate + " -> " + assigned.getId());
			simulatedVehicles.add(new Vehicle(plate, type, "SIMULATED", true));
		}
	}

	/** Simulates one vehicle leaving the parking lot. */
	public void simulateExit() {
		if (simulatedVehicles.isEmpty()) return;

		int index = random.nextInt(simulatedVehicles.size());
		Vehicle vehicle = simulatedVehicles.get(index);
		parkingService.handleVehicleExit(vehicle.getLicensePlate());
		notifyParkingStatusChanged("[SIM] Exit:  " + vehicle.getLicensePlate());
		simulatedVehicles.remove(index);
	}

	/**
	 * Calculates the chance that the next step should be an entry.
	 *
	 * @return entry probability between 0 and 1
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
	 * Generates an unused simulated license plate.
	 *
	 * @return generated plate
	 */
	public String generateRandomPlate() {
		for (int attempt = 0; attempt < 1000; attempt++) {
			String candidate = "SIM-" + String.format("%04d", random.nextInt(10000));
			if (isPlateAvailable(candidate)) return candidate;
		}

		while (true) {
			String candidate = "SIM-" + System.nanoTime() + "-" + random.nextInt(10000);
			if (isPlateAvailable(candidate)) return candidate;
		}
	}

	/** Notifies the parking listener when the simulation changes the lot. */
	private void notifyParkingStatusChanged(String message) {
		if (parkingStatusChangeListener != null) {
			parkingStatusChangeListener.parkingStatusChanged(message);
		}
	}

	/** Checks that a simulated plate is not already in use. */
	private boolean isPlateAvailable(String plate) {
		if (vehicleDAO != null && vehicleDAO.findByPlate(plate) != null) return false;
		for (Vehicle vehicle : simulatedVehicles) {
			if (plate.equalsIgnoreCase(vehicle.getLicensePlate())) return false;
		}
		return true;
	}
}
