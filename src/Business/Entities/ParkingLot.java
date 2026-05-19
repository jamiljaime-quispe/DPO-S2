package Business.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-memory representation of the entire parking lot.
 * Used by ParkingService to operate on cached space state.
 */
public class ParkingLot {
	private List<ParkingSpace> spaces;
	private List<Reservation> reservations;
	private Map<String, ParkingSpace> parkedVehicles; // plate → space

	/**
	 * @param spaces         list of all parking spaces
	 * @param reservations   list of all active reservations
	 * @param parkedVehicles map of plate → occupied space
	 */
	public ParkingLot(List<ParkingSpace> spaces, List<Reservation> reservations,
					  Map<String, ParkingSpace> parkedVehicles) {
		this.spaces = spaces;
		this.reservations = reservations;
		this.parkedVehicles = parkedVehicles;
	}

	/**
	 * Adds a new parking space to the lot.
	 * @param space space to add
	 */
	public void addParkingSpace(ParkingSpace space) {
		spaces.add(space);
	}

	/**
	 * Removes a parking space by its code.
	 * @param code space code to remove
	 * @return true if the space was found and removed
	 */
	public boolean removeParkingSpace(String code) {
		return spaces.removeIf(s -> code.equals(s.getId()));
	}

	/**
	 * Finds the first available space matching the given vehicle type.
	 * @param type vehicle type required
	 * @return available ParkingSpace, or null if none found
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
	 * @param vehicle vehicle to park
	 * @return assigned ParkingSpace, or null if lot is full for this type
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
	 * @param licencePlate plate of the exiting vehicle
	 */
	public void exitVehicle(String licencePlate) {
		ParkingSpace space = parkedVehicles.get(licencePlate);
		if (space != null) {
			space.freeSpace();
			parkedVehicles.remove(licencePlate);
		}
	}

	/**
	 * @return snapshot list of all spaces with their current state
	 */
	public List<ParkingSpace> getParkingStatus() {
		return new ArrayList<>(spaces);
	}

	/**
	 * @return number of currently occupied spaces
	 */
	public int getOccupiedSpaces() {
		int count = 0;
		for (ParkingSpace space : spaces) {
			if (space.isOccupied()) count++;
		}
		return count;
	}

	/**
	 * @return all spaces
	 */
	public List<ParkingSpace> getSpaces() { return spaces; }

	/**
	 * @return all reservations
	 */
	public List<Reservation> getReservations() { return reservations; }
}
