package Business.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-memory representation of the entire parking lot.
 * Holds all parking spaces, active reservations, and a map of currently parked vehicles.
 */
public class ParkingLot {
	private List<ParkingSpace> spaces;
	private List<Reservation> reservations;
	private Map<String, ParkingSpace> parkedVehicles;

	/**
	 * Constructs a new ParkingLot.
	 *
	 * @param spaces         the list of all parking spaces
	 * @param reservations   the list of all active reservations
	 * @param parkedVehicles the map of license plate to occupied parking space
	 */
	public ParkingLot(List<ParkingSpace> spaces, List<Reservation> reservations,
					  Map<String, ParkingSpace> parkedVehicles) {
		this.spaces = spaces;
		this.reservations = reservations;
		this.parkedVehicles = parkedVehicles;
	}

	/**
	 * Gets all parking spaces in the lot.
	 *
	 * @return the list of all parking spaces
	 */
	public List<ParkingSpace> getSpaces() { return spaces; }

	/**
	 * Gets all active reservations in the lot.
	 *
	 * @return the list of all reservations
	 */
	public List<Reservation> getReservations() { return reservations; }

	/**
	 * Adds a new parking space to the lot.
	 *
	 * @param space the space to add
	 */
	public void addParkingSpace(ParkingSpace space) {
		spaces.add(space);
	}

	/**
	 * Removes a parking space from the lot by its code.
	 *
	 * @param code the code of the space to remove
	 * @return true if the space was found and removed
	 */
	public boolean removeParkingSpace(String code) {
		return spaces.removeIf(s -> code.equals(s.getId()));
	}

	/**
	 * Finds the first available space matching the given vehicle type.
	 *
	 * @param type the vehicle type required
	 * @return an available ParkingSpace, or null if none is found
	 */
	public ParkingSpace findAvailableSpace(VehicleType type) {
		for (ParkingSpace space : spaces) {
			if (space.getVehicleType() == type && space.isAvailable()) {
				return space;
			}
		}
		return null;
	}

	/**
	 * Parks a vehicle in the first available compatible space.
	 *
	 * @param vehicle the vehicle to park
	 * @return the assigned ParkingSpace, or null if the lot is full for this type
	 */
	public ParkingSpace parkVehicle(Vehicle vehicle) {
		ParkingSpace space = findAvailableSpace(vehicle.getType());
		if (space != null) {
			space.occupy(vehicle);
			parkedVehicles.put(vehicle.getLicensePlate(), space);
		}
		return space;
	}

	/**
	 * Removes a vehicle from the lot and frees its space.
	 *
	 * @param licencePlate the license plate of the exiting vehicle
	 */
	public void exitVehicle(String licencePlate) {
		ParkingSpace space = parkedVehicles.get(licencePlate);
		if (space != null) {
			space.freeSpace();
			parkedVehicles.remove(licencePlate);
		}
	}

	/**
	 * Gets a snapshot of all spaces with their current state.
	 *
	 * @return a copy of the list of all parking spaces
	 */
	public List<ParkingSpace> getParkingStatus() {
		return new ArrayList<>(spaces);
	}

	/**
	 * Gets the number of currently occupied spaces.
	 *
	 * @return the count of occupied spaces
	 */
	public int getOccupiedSpaces() {
		int count = 0;
		for (ParkingSpace space : spaces) {
			if (space.isOccupied()) count++;
		}
		return count;
	}
}
