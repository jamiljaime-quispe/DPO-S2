package Persistence;

import java.util.List;

import Business.Entities.Vehicle;

/**
 * Data access interface for vehicles.
 */
public interface VehicleDAO {

    /**
     * Persists a new vehicle to the database.
     *
     * @param vehicle the vehicle to save
     */
    void save(Vehicle vehicle);

    /**
     * Retrieves a vehicle by its license plate.
     *
     * @param plate the license plate
     * @return the vehicle, or null if not found
     */
    Vehicle findByPlate(String plate);

    /**
     * Retrieves all vehicles owned by the given user.
     *
     * @param userId the owner's user ID
     * @return list of vehicles; empty if none
     */
    List<Vehicle> findByUser(int userId);

    /**
     * Deletes a vehicle by its license plate.
     *
     * @param plate the license plate to delete
     */
    void delete(String plate);
}
