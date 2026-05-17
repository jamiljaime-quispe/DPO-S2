package Business.Services;

import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.VehicleDAO;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages parking space lifecycle and vehicle entry/exit logic.
 */
public class ParkingService {
	private ParkingSpaceDAO parkingSpaceDAO;
	private VehicleDAO vehicleDAO;
	private ReservationDAO reservationDAO;

	/**
	 * Constructs a new ParkingService.
	 *
	 * @param parkingSpaceDAO the DAO for parking spaces
	 * @param vehicleDAO      the DAO for vehicles
	 * @param reservationDAO  the DAO for reservations
	 */
	public ParkingService(ParkingSpaceDAO parkingSpaceDAO, VehicleDAO vehicleDAO,
						  ReservationDAO reservationDAO) {
		this.parkingSpaceDAO = parkingSpaceDAO;
		this.vehicleDAO = vehicleDAO;
		this.reservationDAO = reservationDAO;
	}

	/**
	 * @return list of all parking spaces
	 */
	public List<ParkingSpace> getAllSpaces() {
		return parkingSpaceDAO.findAll();
	}

	/**
	 * Finds a parking space by its code.
	 * @param code space code
	 * @return matching ParkingSpace, or null if not found
	 */
	public ParkingSpace findByCode(String code) {
		return parkingSpaceDAO.findByCode(code);
	}

	/**
	 * Persists editable details for an existing space.
	 * The vehicle type is not editable after creation because it can conflict
	 * with parked vehicles and active reservations.
	 * @param space space with updated details
	 */
	public void updateParkingSpaceDetails(ParkingSpace space) {
		ParkingSpace existing = parkingSpaceDAO.findByCode(space.getId());
		if (existing == null) {
			throw new IllegalArgumentException("Space not found: " + space.getId());
		}
		if (existing.getVehicleType() != space.getVehicleType()) {
			throw new IllegalArgumentException("Vehicle type cannot be edited after the space is created.");
		}
		parkingSpaceDAO.updateDetails(space);
	}

	/**
	 * Creates a new empty parking space if its code is not already taken.
	 * @param space space to create
	 */
	public void createParkingSpace(ParkingSpace space) {
		if (space == null || space.getId() == null || space.getId().isBlank()) {
			throw new IllegalArgumentException("Space code is required.");
		}
		if (space.getVehicleType() == null) {
			throw new IllegalArgumentException("Vehicle type is required.");
		}
		if (parkingSpaceDAO.findByCode(space.getId()) != null) {
			throw new IllegalArgumentException("Space code already exists: " + space.getId());
		}

		space.setOccupied(false);
		space.setReserved(false);
		space.setParkedVehicle(null);
		space.cancelReservation();
		parkingSpaceDAO.save(space);
	}

	/**
	 * Persists updated data for an existing parking space.
	 * @param space space with updated fields
	 */
	public void updateParkingSpace(ParkingSpace space) {
		parkingSpaceDAO.update(space);
	}

	/**
	 * Deletes a vacant parking space.
	 * Fails if the space is currently occupied.
	 * If the space has an active reservation, the reservation is moved to a
	 * vacant space with the same vehicle type. If no alternative exists, the
	 * reservation is deleted.
	 * @param code space code to delete
	 * @return true if successfully deleted, false if occupied or not found
	 */
	public boolean deleteParkingSpace(String code) {
		ParkingSpace space = parkingSpaceDAO.findByCode(code);
		if (space == null || space.isOccupied()) return false;

		if (space.isReserved()) {
			List<Reservation> all = reservationDAO.findAll();
			for (Reservation r : all) {
				if (r.isActive() && r.getParkingSpace() != null
						&& code.equals(r.getParkingSpace().getId())) {
					List<ParkingSpace> alternatives = parkingSpaceDAO.findAvailableByType(space.getVehicleType());
					ParkingSpace newSpace = findBestAlternativeSpace(alternatives, code, space.getFloor());
					if (newSpace != null) {
						r.setParkingSpace(newSpace);
						reservationDAO.update(r);
					} else {
						reservationDAO.delete(r.getId());
					}
					break;
				}
			}
		}
		parkingSpaceDAO.delete(code);
		return true;
	}

	private ParkingSpace findBestAlternativeSpace(List<ParkingSpace> alternatives, String deletedCode, int preferredFloor) {
		ParkingSpace fallback = null;

		for (ParkingSpace alternative : alternatives) {
			if (alternative.getId().equals(deletedCode)) {
				continue;
			}
			if (alternative.getFloor() == preferredFloor) {
				return alternative;
			}
			if (fallback == null) {
				fallback = alternative;
			}
		}

		return fallback;
	}

	/**
	 * Returns all spaces that are neither occupied nor reserved for the given type.
	 * @param type vehicle type
	 * @return list of available spaces
	 */
	public List<ParkingSpace> findAvailableSpaces(VehicleType type) {
		return parkingSpaceDAO.findAvailableByType(type);
	}

	/**
	 * Finds the active reservation for a license plate, if one exists.
	 * @param plate license plate to check
	 * @return active reservation, or null if none exists
	 */
	public Reservation findActiveReservationByPlate(String plate) {
		if (plate == null || plate.isBlank()) return null;

		Reservation reservation = reservationDAO.findByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			return reservation;
		}
		return null;
	}

	/**
	 * Finds the occupied parking space for a license plate.
	 * @param plate license plate to check
	 * @return occupied space, or null if the vehicle is not currently parked
	 */
	public ParkingSpace findOccupiedSpaceByPlate(String plate) {
		if (plate == null || plate.isBlank()) return null;

		List<ParkingSpace> allSpaces = parkingSpaceDAO.findAll();
		for (ParkingSpace space : allSpaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& plate.equalsIgnoreCase(space.getParkedVehicle().getLicensePlate())) {
				return space;
			}
		}
		return null;
	}

	/**
	 * Returns the spaces currently occupied by vehicles registered to a user.
	 * @param userId owner user ID
	 * @return occupied spaces for the user's registered vehicles
	 */
	public List<ParkingSpace> getParkedSpacesByUser(int userId) {
		List<ParkingSpace> parkedSpaces = new ArrayList<>();
		if (userId <= 0) return parkedSpaces;

		List<Vehicle> vehicles = vehicleDAO.findByUser(userId);
		Set<String> userPlates = new HashSet<>();
		for (Vehicle vehicle : vehicles) {
			userPlates.add(vehicle.getLicensePlate());
		}

		List<ParkingSpace> spaces = parkingSpaceDAO.findAll();
		for (ParkingSpace space : spaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& userPlates.contains(space.getParkedVehicle().getLicensePlate())) {
				parkedSpaces.add(space);
			}
		}

		return parkedSpaces;
	}

	/**
	 * Assigns a vehicle to the best available space based on its type.
	 * @param vehicle vehicle to park
	 * @return assigned ParkingSpace, or null if no space is available
	 */
	public ParkingSpace assignVehicleToSpace(Vehicle vehicle) {
		return handleVehicleEntry(vehicle.getLicensePlate(), vehicle.getType());
	}

	/**
	 * Handles entry for a logged-in user and registers new vehicles to that user.
	 * @param userId owner user ID
	 * @param plate license plate
	 * @param type vehicle type
	 * @return assigned ParkingSpace, or null if no space is available
	 */
	public synchronized ParkingSpace handleUserVehicleEntry(int userId, String plate, VehicleType type) {
		if (userId <= 0) {
			throw new IllegalArgumentException("No logged-in user was found.");
		}

		Vehicle vehicle = vehicleDAO.findByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, String.valueOf(userId), false);
			vehicleDAO.save(vehicle);
		} else if (!userOwnsVehicle(userId, plate)) {
			throw new IllegalArgumentException("License plate " + plate
					+ " is registered to another user.");
		}

		return handleVehicleEntry(plate, type);
	}

	private boolean userOwnsVehicle(int userId, String plate) {
		List<Vehicle> vehicles = vehicleDAO.findByUser(userId);
		for (Vehicle vehicle : vehicles) {
			if (plate.equals(vehicle.getLicensePlate())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Handles a vehicle entering the lot:
	 * <ol>
	 *   <li>If the plate has an active reservation → assign that reserved space.</li>
	 *   <li>Otherwise → assign the first available space matching the vehicle type.</li>
	 * </ol>
	 * @param plate license plate of the entering vehicle
	 * @param type  vehicle type (used when there is no reservation)
	 * @return assigned ParkingSpace, or null if the lot is full
	 */
	public synchronized ParkingSpace handleVehicleEntry(String plate, VehicleType type) {
		// 1. Check for an active reservation
		Reservation reservation = reservationDAO.findByPlate(plate);
		if (reservation != null && reservation.isActive()) {
			ParkingSpace reservedSpace = reservation.getParkingSpace();
			if (reservedSpace != null) {
				Vehicle vehicle = vehicleDAO.findByPlate(plate);
				if (vehicle == null) {
					vehicle = new Vehicle(plate, type, null, false);
				}
				reservedSpace.occupy(vehicle);
				reservedSpace.setReserved(false);
				parkingSpaceDAO.update(reservedSpace);
				reservation.cancel();
				reservationDAO.update(reservation);
				return reservedSpace;
			}
		}

		// 2. No reservation — find a free compatible space
		List<ParkingSpace> available = parkingSpaceDAO.findAvailableByType(type);
		if (available == null || available.isEmpty()) return null;

		ParkingSpace space = available.get(0);
		Vehicle vehicle = vehicleDAO.findByPlate(plate);
		if (vehicle == null) {
			vehicle = new Vehicle(plate, type, null, false);
		}
		space.occupy(vehicle);
		parkingSpaceDAO.update(space);
		return space;
	}

	/**
	 * Handles a vehicle exiting the lot: finds its occupied space and frees it.
	 * @param plate license plate of the exiting vehicle
	 */
	public synchronized void handleVehicleExit(String plate) {
		List<ParkingSpace> allSpaces = parkingSpaceDAO.findAll();
		for (ParkingSpace space : allSpaces) {
			if (space.isOccupied()
					&& space.getParkedVehicle() != null
					&& plate.equals(space.getParkedVehicle().getLicensePlate())) {
				space.freeSpace();
				parkingSpaceDAO.update(space);
				return;
			}
		}
	}

	/**
	 * Handles exit for a logged-in user, only for vehicles registered to that user.
	 * @param userId owner user ID
	 * @param plate license plate to exit with
	 * @return freed parking space, or null if the vehicle is not parked for that user
	 */
	public synchronized ParkingSpace handleUserVehicleExit(int userId, String plate) {
		List<ParkingSpace> parkedSpaces = getParkedSpacesByUser(userId);
		for (ParkingSpace space : parkedSpaces) {
			if (space.getParkedVehicle() != null
					&& plate.equals(space.getParkedVehicle().getLicensePlate())) {
				space.freeSpace();
				parkingSpaceDAO.update(space);
				return space;
			}
		}
		return null;
	}

	/**
	 * @return current status of all parking spaces
	 */
	public List<ParkingSpace> getParkingStatus() {
		return parkingSpaceDAO.findAll();
	}
}
