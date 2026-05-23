package Persistence;

import java.util.List;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;

/**
 * Data access interface for parking spaces.
 */
public interface ParkingSpaceDAO {

    /**
     * Persists a new parking space to the database.
     *
     * @param space the parking space to save
     */
    void save(ParkingSpace space);

    /**
     * Updates the full state (occupancy, reservation, parked vehicle) of an existing space.
     *
     * @param space the parking space with updated state
     */
    void update(ParkingSpace space);

    /**
     * Updates only the editable details (floor, vehicle type) of an existing space.
     *
     * @param space the parking space with updated details
     */
    void updateDetails(ParkingSpace space);

    /**
     * Deletes a parking space by its code.
     *
     * @param code the space code to delete
     */
    void delete(String code);

    /**
     * Retrieves a parking space by its code.
     *
     * @param code the space code
     * @return the parking space, or null if not found
     */
    ParkingSpace findByCode(String code);

    /**
     * Retrieves all parking spaces.
     *
     * @return list of all spaces; empty if none
     */
    List<ParkingSpace> findAll();

    /**
     * Retrieves all vacant, unreserved parking spaces that match the given vehicle type.
     *
     * @param type the vehicle type to filter by
     * @return list of available spaces; empty if none
     */
    List<ParkingSpace> findAvailableByType(VehicleType type);
}
