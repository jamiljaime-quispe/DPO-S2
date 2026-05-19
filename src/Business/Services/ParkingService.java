package Business.Services;

import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.VehicleDAO;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;

import java.util.List;

/**
 * Manages parking space lifecycle and vehicle entry/exit logic.
 */
public class ParkingService {
	private ParkingSpaceDAO parkingSpaceDAO;
	private VehicleDAO vehicleDAO;
	private ReservationDAO reservationDAO;

	/**
	 * @param parkingSpaceDAO DAO for parking spaces
	 * @param vehicleDAO      DAO for vehicles
	 * @param reservationDAO  DAO for reservations
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
	 * Persists floor and vehicleType changes for an existing space.
	 * @param space space with updated floor/vehicleType
	 */
	public void updateParkingSpaceDetails(ParkingSpace space) {
		parkingSpaceDAO.updateDetails(space);
	}

	/**
	 * Creates a new parking space if its code is not already taken.
	 * @param space space to create
	 */
	public void createParkingSpace(ParkingSpace space) {
		if (parkingSpaceDAO.findByCode(space.getId()) == null) {
			parkingSpaceDAO.save(space);
		}
	}

	/**
	 * Persists updated data for an existing parking space.
	 * @param space space with updated fields
	 */
	public void updateParkingSpace(ParkingSpace space) {
		parkingSpaceDAO.update(space);
	}

	/**
	 * Deletes a parking space.
	 * Fails if the space is currently occupied.
	 * If the space has an active reservation, it is cancelled before deletion.
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
					// Try to reassign to another available space of the same type
					List<ParkingSpace> alternatives = parkingSpaceDAO.findAvailableByType(space.getVehicleType());
					alternatives.removeIf(s -> s.getId().equals(code));
					if (!alternatives.isEmpty()) {
						ParkingSpace newSpace = alternatives.get(0);
						newSpace.setReserved(true);
						r.setParkingSpace(newSpace);
						reservationDAO.update(r);
					} else {
						r.cancel();
						reservationDAO.update(r);
					}
					break;
				}
			}
		}
		parkingSpaceDAO.delete(code);
		return true;
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
	 * Assigns a vehicle to the best available space based on its type.
	 * @param vehicle vehicle to park
	 * @return assigned ParkingSpace, or null if no space is available
	 */
	public ParkingSpace assignVehicleToSpace(Vehicle vehicle) {
		return handleVehicleEntry(vehicle.getLicensePlate(), vehicle.getType());
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
	public ParkingSpace handleVehicleEntry(String plate, VehicleType type) {
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
	public void handleVehicleExit(String plate) {
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
	 * @return current status of all parking spaces
	 */
	public List<ParkingSpace> getParkingStatus() {
		return parkingSpaceDAO.findAll();
	}
}
