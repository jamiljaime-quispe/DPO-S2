package Persistence;

import java.util.List;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;

/**
 * Data access interface for parking spaces. This interface keeps the promise clear so another class can use
 * it without depending on a specific implementation.
 * <p>
 * The interface lets the business layer ask for stored data without depending on the class that talks
 * directly to the database.
 * </p>
 */
public interface ParkingSpaceDAO {

    /**
     * Persists a new parking space to the database. The operation is kept together so the stored data
     * remains consistent if something goes wrong halfway through.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param space the parking space to save
     */
    void save(ParkingSpace space);

    /**
     * Updates value.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param space the parking space with updated state
     */
    void update(ParkingSpace space);

    /**
     * Updates details.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param space the parking space with updated details
     */
    void updateDetails(ParkingSpace space);

    /**
     * Deletes value.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param code the space code to delete
     */
    void delete(String code);

    /**
     * Finds a record by its code.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param code the space code
     * @return the parking space, or null if not found
     */
    ParkingSpace findByCode(String code);

    /**
     * Finds all.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @return list of all spaces; empty if none
     */
    List<ParkingSpace> findAll();

    /**
     * Finds available by type.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param type the vehicle type to filter by
     * @return list of available spaces; empty if none
     */
    List<ParkingSpace> findAvailableByType(VehicleType type);
}
