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
 * Simulates vehicle traffic in the parking lot using a background thread. Simulated vehicles only use
 * spaces that are not reserved.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class SimulationService implements Runnable {
	// private static final int CHAOS_MODE_DELAY_MS = 1000;
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
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param parkingService service used for entry and exit
	 * @param config simulation configuration
	 * @param random random number generator
	 * @param simulatedVehicles list of currently parked simulated vehicles
	 * @param vehicleDAO DAO used to avoid duplicate simulated plates
	 */
	public SimulationService(ParkingService parkingService, Config config, Random random,
			List<Vehicle> simulatedVehicles, VehicleDAO vehicleDAO) {
		this.parkingService = parkingService;
		this.config = config;
		this.random = random;
		this.simulatedVehicles = simulatedVehicles != null ? simulatedVehicles : new ArrayList<>();
		this.vehicleDAO = vehicleDAO;
	}

	/**
	 * Sets the listener notified after simulation changes.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param parkingStatusChangeListener action that will run when the related event happens
	 */
	public void setParkingStatusChangeListener(ParkingStatusChangeListener parkingStatusChangeListener) {
		this.parkingStatusChangeListener = parkingStatusChangeListener;
	}

	/**
	 * Runs the background simulation loop.
	 * <p>
	 * This is the body of the background thread. It repeats the simulation while it is active and sleeps
	 * between steps so the program can keep running normally.
	 * </p>
	 */
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

	/**
	 * Starts the simulation in its own background thread.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	public void startSimulation() {
		if (running) return;

		running = true;
		createSimulationThread();
		startSimulationThread();
	}

	/**
	 * Stops the simulation and wakes the thread if it is sleeping. The method supports the simulated traffic
	 * flow while keeping the parking updates coordinated with the rest of the system.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	public void stopSimulation() {
		running = false;
		interruptSimulationThread();
	}

	/**
	 * Handles simulate step.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
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

	/**
	 * Handles simulate entry.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
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

	/**
	 * Handles simulate exit.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	public void simulateExit() {
		if (hasNoSimulatedVehicles()) return;

		int index = chooseRandomSimulatedVehicleIndex();
		Vehicle vehicle = getSimulatedVehicle(index);
		handleVehicleExit(vehicle.getLicensePlate());
		notifyParkingStatusChanged("[SIM] Exit:  " + vehicle.getLicensePlate());
		removeSimulatedVehicle(index);
	}

	/**
	 * Handles calculate entry probability.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
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
	 * Handles generate random plate.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
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

	/**
	 * Notifies the parking listener when the simulation changes the lot. The method supports the simulated
	 * traffic flow while keeping the parking updates coordinated with the rest of the system.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 */
	private void notifyParkingStatusChanged(String message) {
		if (parkingStatusChangeListener != null) {
			notifyParkingListener(message);
		}
	}

	/**
	 * Checks whether plate available.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return true when the condition is met, false otherwise
	 */
	private boolean isPlateAvailable(String plate) {
		if (plateExistsInDatabase(plate)) return false;
		for (Vehicle vehicle : simulatedVehicles) {
			if (plate.equalsIgnoreCase(vehicle.getLicensePlate())) return false;
		}
		return true;
	}

	/**
	 * Sleeps until the next simulation step. The method supports the simulated traffic flow while keeping the
	 * parking updates coordinated with the rest of the system.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @throws InterruptedException if the operation cannot be completed correctly
	 */
	private void sleepBeforeNextStep() throws InterruptedException {
		int maxDelay = Math.max(1, getSimulatedVehicleDelay());
		int delay = randomInt(maxDelay) + 1;
		Thread.sleep(delay * 1000L);
	}

	/**
	 * Gets the configured maximum simulated vehicle delay.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current simulated vehicle delay
	 */
	private int getSimulatedVehicleDelay() {
		return config.getSimulatedVehicleDelay();
	}

	/**
	 * Creates the simulation thread. The method supports the simulated traffic flow while keeping the parking
	 * updates coordinated with the rest of the system.
	 * <p>
	 * This method checks the rule for the operation and then asks persistence to save the change in the
	 * database.
	 * </p>
	 */
	private void createSimulationThread() {
		simulationThread = new Thread(this);
	}

	/**
	 * Starts the simulation thread. The method supports the simulated traffic flow while keeping the parking
	 * updates coordinated with the rest of the system.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	private void startSimulationThread() {
		simulationThread.start();
	}

	/**
	 * Interrupts the simulation thread if it exists. The method supports the simulated traffic flow while
	 * keeping the parking updates coordinated with the rest of the system.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	private void interruptSimulationThread() {
		if (simulationThread != null) simulationThread.interrupt();
	}

	/**
	 * Handles should simulate entry.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param entryProbability entry probability used by this operation
	 * @return the result of the operation
	 */
	private boolean shouldSimulateEntry(double entryProbability) {
		return random.nextDouble() < entryProbability;
	}

	/**
	 * Handles choose random vehicle type.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @return the result of the operation
	 */
	private VehicleType chooseRandomVehicleType() {
		VehicleType[] types = VehicleType.values();
		return types[randomInt(types.length)];
	}

	/**
	 * Finds available spaces.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param type vehicle type involved in the operation
	 * @return the matching available spaces, or null when it is not found
	 */
	private List<ParkingSpace> findAvailableSpaces(VehicleType type) {
		return parkingService.findAvailableSpaces(type);
	}

	/**
	 * Handles vehicle entry.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @param type vehicle type involved in the operation
	 * @return the result of the operation
	 */
	private ParkingSpace handleVehicleEntry(String plate, VehicleType type) {
		return parkingService.handleVehicleEntry(plate, type);
	}

	/**
	 * Handles vehicle exit.
	 * <p>
	 * This method groups the complete parking operation so the controller does not need to know each database
	 * step.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 */
	private void handleVehicleExit(String plate) {
		parkingService.handleVehicleExit(plate);
	}

	/**
	 * Adds simulated vehicle.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param vehicle vehicle used by this operation
	 */
	private void addSimulatedVehicle(Vehicle vehicle) {
		simulatedVehicles.add(vehicle);
	}

	/**
	 * Loads parked simulated vehicles without stopping the simulation if loading fails. The method supports
	 * the simulated traffic flow while keeping the parking updates coordinated with the rest of the system.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 */
	private void loadExistingSimulatedVehiclesSafely() {
		try {
			loadExistingSimulatedVehicles();
		} catch (RuntimeException e) {
			String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			notifyParkingStatusChanged("[SIM] Error loading existing simulated vehicles: " + message);
		}
	}

	/**
	 * Rebuilds the simulated vehicle list from currently parked SIM plates in the database. The operation is
	 * kept together so the stored data remains consistent if something goes wrong halfway through.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 */
	private void loadExistingSimulatedVehicles() {
		clearSimulatedVehicles();
		List<ParkingSpace> spaces = loadAllSpaces();
		for (ParkingSpace space : spaces) {
			if (isOccupiedBySimulatedVehicle(space)) {
				addSimulatedVehicle(space.getParkedVehicle());
			}
		}
	}

	/**
	 * Handles clear simulated vehicles.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
	private void clearSimulatedVehicles() {
		simulatedVehicles.clear();
	}

	/**
	 * Checks whether occupied by simulated vehicle.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param space space used by this operation
	 * @return true when the condition is met, false otherwise
	 */
	private boolean isOccupiedBySimulatedVehicle(ParkingSpace space) {
		if (space == null || !space.isOccupied() || space.getParkedVehicle() == null) {
			return false;
		}
		return isSimulatedPlate(space.getParkedVehicle().getLicensePlate());
	}

	/**
	 * Checks whether simulated plate.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return true when the condition is met, false otherwise
	 */
	private boolean isSimulatedPlate(String plate) {
		return plate != null && plate.startsWith(SIMULATED_PLATE_PREFIX);
	}

	/**
	 * Checks whether no simulated vehicles exists.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @return true when the condition is met, false otherwise
	 */
	private boolean hasNoSimulatedVehicles() {
		return simulatedVehicles.isEmpty();
	}

	/**
	 * Handles choose random simulated vehicle index.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @return the result of the operation
	 */
	private int chooseRandomSimulatedVehicleIndex() {
		return randomInt(simulatedVehicles.size());
	}

	/**
	 * Gets a simulated vehicle by index.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @param index index used by this operation
	 * @return the current simulated vehicle
	 */
	private Vehicle getSimulatedVehicle(int index) {
		return simulatedVehicles.get(index);
	}

	/**
	 * Handles remove simulated vehicle.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param index index used by this operation
	 */
	private void removeSimulatedVehicle(int index) {
		simulatedVehicles.remove(index);
	}

	/**
	 * Loads all spaces.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @return the loaded all spaces
	 */
	private List<ParkingSpace> loadAllSpaces() {
		return parkingService.getAllSpaces();
	}

	/**
	 * Gets a random integer below the given limit.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param limit limit used by this operation
	 * @return the result of the operation
	 */
	private int randomInt(int limit) {
		return random.nextInt(limit);
	}

	/**
	 * Handles notify parking listener.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 */
	private void notifyParkingListener(String message) {
		parkingStatusChangeListener.parkingStatusChanged(message);
	}

	/**
	 * Handles plate exists in database.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return the result of the operation
	 */
	private boolean plateExistsInDatabase(String plate) {
		return vehicleDAO != null && vehicleDAO.findByPlate(plate) != null;
	}
}
