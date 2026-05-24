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
	private static final String SIMULATED_PLATE_PREFIX = "SIM-";

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

	/** Runs the background simulation loop. */
	@Override
	public void run() {
		loadExistingSimulatedVehiclesSafely();
		while (running) {
			try {
				simulateStep();
			} catch (RuntimeException e) {
				String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
				notifyParkingStatusChanged("[SIM] Error: " + message);
			}

			try {
				sleepBeforeNextStep();

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
		createSimulationThread();
		startSimulationThread();
	}

	/** Stops the simulation and wakes the thread if it is sleeping. */
	public void stopSimulation() {
		running = false;
		interruptSimulationThread();
	}

	/** Performs one simulated entry or exit decision. */
	public void simulateStep() {
		double entryProbability = calculateEntryProbability();
		if (shouldSimulateEntry(entryProbability)) {
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
		VehicleType type = chooseRandomVehicleType();

		List<ParkingSpace> available = findAvailableSpaces(type);
		if (available == null || available.isEmpty()) return;

		String plate = generateRandomPlate();
		ParkingSpace assigned = handleVehicleEntry(plate, type);
		if (assigned != null) {
			notifyParkingStatusChanged("[SIM] Entry: " + plate + " -> " + assigned.getId());
			addSimulatedVehicle(new Vehicle(plate, type, "SIMULATED", true));
		}
	}

	/** Simulates one vehicle leaving the parking lot. */
	public void simulateExit() {
		if (hasNoSimulatedVehicles()) return;

		int index = chooseRandomSimulatedVehicleIndex();
		Vehicle vehicle = getSimulatedVehicle(index);
		handleVehicleExit(vehicle.getLicensePlate());
		notifyParkingStatusChanged("[SIM] Exit:  " + vehicle.getLicensePlate());
		removeSimulatedVehicle(index);
	}

	/**
	 * Calculates the chance that the next step should be an entry.
	 *
	 * @return entry probability between 0 and 1
	 */
	public double calculateEntryProbability() {
		List<ParkingSpace> allSpaces = loadAllSpaces();
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
			String candidate = SIMULATED_PLATE_PREFIX + String.format("%04d", randomInt(10000));
			if (isPlateAvailable(candidate)) return candidate;
		}

		while (true) {
			String candidate = SIMULATED_PLATE_PREFIX + System.nanoTime() + "-" + randomInt(10000);
			if (isPlateAvailable(candidate)) return candidate;
		}
	}

	/** Notifies the parking listener when the simulation changes the lot. */
	private void notifyParkingStatusChanged(String message) {
		if (parkingStatusChangeListener != null) {
			notifyParkingListener(message);
		}
	}

	/** Checks that a simulated plate is not already in use. */
	private boolean isPlateAvailable(String plate) {
		if (plateExistsInDatabase(plate)) return false;
		for (Vehicle vehicle : simulatedVehicles) {
			if (plate.equalsIgnoreCase(vehicle.getLicensePlate())) return false;
		}
		return true;
	}

	/** Sleeps until the next simulation step. */
	private void sleepBeforeNextStep() throws InterruptedException {
		int maxDelay = Math.max(1, getSimulatedVehicleDelay());
		int delay = randomInt(maxDelay) + 1;
		Thread.sleep(delay * 1000L);
	}

	/** Gets the configured maximum simulated vehicle delay. */
	private int getSimulatedVehicleDelay() {
		return config.getSimulatedVehicleDelay();
	}

	/** Creates the simulation thread. */
	private void createSimulationThread() {
		simulationThread = new Thread(this);
	}

	/** Starts the simulation thread. */
	private void startSimulationThread() {
		simulationThread.start();
	}

	/** Interrupts the simulation thread if it exists. */
	private void interruptSimulationThread() {
		if (simulationThread != null) simulationThread.interrupt();
	}

	/** Decides whether the next simulated action should be an entry. */
	private boolean shouldSimulateEntry(double entryProbability) {
		return random.nextDouble() < entryProbability;
	}

	/** Chooses a random vehicle type for simulated entry. */
	private VehicleType chooseRandomVehicleType() {
		VehicleType[] types = VehicleType.values();
		return types[randomInt(types.length)];
	}

	/** Finds available spaces through the parking service. */
	private List<ParkingSpace> findAvailableSpaces(VehicleType type) {
		return parkingService.findAvailableSpaces(type);
	}

	/** Handles simulated vehicle entry through the parking service. */
	private ParkingSpace handleVehicleEntry(String plate, VehicleType type) {
		return parkingService.handleVehicleEntry(plate, type);
	}

	/** Handles simulated vehicle exit through the parking service. */
	private void handleVehicleExit(String plate) {
		parkingService.handleVehicleExit(plate);
	}

	/** Adds a vehicle to the simulated vehicle list. */
	private void addSimulatedVehicle(Vehicle vehicle) {
		simulatedVehicles.add(vehicle);
	}

	/** Loads parked simulated vehicles without stopping the simulation if loading fails. */
	private void loadExistingSimulatedVehiclesSafely() {
		try {
			loadExistingSimulatedVehicles();
		} catch (RuntimeException e) {
			String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			notifyParkingStatusChanged("[SIM] Error loading existing simulated vehicles: " + message);
		}
	}

	/** Rebuilds the simulated vehicle list from currently parked SIM plates in the database. */
	private void loadExistingSimulatedVehicles() {
		clearSimulatedVehicles();
		List<ParkingSpace> spaces = loadAllSpaces();
		for (ParkingSpace space : spaces) {
			if (isOccupiedBySimulatedVehicle(space)) {
				addSimulatedVehicle(space.getParkedVehicle());
			}
		}
	}

	/** Clears the in-memory list of parked simulated vehicles. */
	private void clearSimulatedVehicles() {
		simulatedVehicles.clear();
	}

	/** Checks whether a parking space is occupied by a simulated vehicle. */
	private boolean isOccupiedBySimulatedVehicle(ParkingSpace space) {
		if (space == null || !space.isOccupied() || space.getParkedVehicle() == null) {
			return false;
		}
		return isSimulatedPlate(space.getParkedVehicle().getLicensePlate());
	}

	/** Checks whether a plate belongs to a simulated vehicle. */
	private boolean isSimulatedPlate(String plate) {
		return plate != null && plate.startsWith(SIMULATED_PLATE_PREFIX);
	}

	/** Checks whether there are no simulated vehicles parked. */
	private boolean hasNoSimulatedVehicles() {
		return simulatedVehicles.isEmpty();
	}

	/** Chooses one parked simulated vehicle index. */
	private int chooseRandomSimulatedVehicleIndex() {
		return randomInt(simulatedVehicles.size());
	}

	/** Gets a simulated vehicle by index. */
	private Vehicle getSimulatedVehicle(int index) {
		return simulatedVehicles.get(index);
	}

	/** Removes a simulated vehicle by index. */
	private void removeSimulatedVehicle(int index) {
		simulatedVehicles.remove(index);
	}

	/** Loads every parking space through the parking service. */
	private List<ParkingSpace> loadAllSpaces() {
		return parkingService.getAllSpaces();
	}

	/** Gets a random integer below the given limit. */
	private int randomInt(int limit) {
		return random.nextInt(limit);
	}

	/** Notifies the listener about a parking status change. */
	private void notifyParkingListener(String message) {
		parkingStatusChangeListener.parkingStatusChanged(message);
	}

	/** Checks whether a plate already exists in persistence. */
	private boolean plateExistsInDatabase(String plate) {
		return vehicleDAO != null && vehicleDAO.findByPlate(plate) != null;
	}
}
