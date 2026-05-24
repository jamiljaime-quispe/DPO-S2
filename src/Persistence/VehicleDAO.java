package Persistence;

import java.util.List;

import Business.Entities.Vehicle;

/**
 * Data access interface for vehicles. This interface keeps the promise clear so another class can use it
 * without depending on a specific implementation.
 * <p>
 * The interface lets the business layer ask for stored data without depending on the class that talks
 * directly to the database.
 * </p>
 */
public interface VehicleDAO {

    /**
     * Persists a new vehicle to the database. The operation is kept together so the stored data remains
     * consistent if something goes wrong halfway through.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param vehicle the vehicle to save
     */
    void save(Vehicle vehicle);

    /**
     * Finds by plate.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param plate the license plate
     * @return the vehicle, or null if not found
     */
    Vehicle findByPlate(String plate);

    /**
     * Finds by user.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param userId the owner's user ID
     * @return list of vehicles; empty if none
     */
    List<Vehicle> findByUser(int userId);

    /**
     * Deletes value.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param plate the license plate to delete
     */
    void delete(String plate);
}
