package Persistence;

import java.util.List;

import Business.Entities.Reservation;

/**
 * Data access interface for reservations. This interface keeps the promise clear so another class can use
 * it without depending on a specific implementation.
 * <p>
 * The interface lets the business layer ask for stored data without depending on the class that talks
 * directly to the database.
 * </p>
 */
public interface ReservationDAO {

    /**
     * Persists a new reservation to the database. The operation is kept together so the stored data remains
     * consistent if something goes wrong halfway through.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param reservation the reservation to save
     */
    void save(Reservation reservation);

    /**
     * Deletes value.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param ID the reservation ID to delete
     */
    void delete(int id);

    /**
     * Finds by id.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param ID the reservation ID
     * @return the reservation, or null if not found
     */
    Reservation findById(int id);

    /**
     * Finds by user.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param userId the user ID
     * @return list of reservations; empty if none
     */
    List<Reservation> findByUser(int userId);

    /**
     * Finds by plate.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param plate the license plate
     * @return the reservation, or null if not found
     */
    Reservation findByPlate(String plate);

    /**
     * Finds all.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @return list of all reservations; empty if none
     */
    List<Reservation> findAll();

    /**
     * Updates value.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param reservation the reservation with updated fields
     */
    void update(Reservation reservation);
}
