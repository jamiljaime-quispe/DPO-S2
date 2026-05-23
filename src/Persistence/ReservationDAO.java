package Persistence;

import java.util.List;

import Business.Entities.Reservation;

/**
 * Data access interface for reservations.
 */
public interface ReservationDAO {

    /**
     * Persists a new reservation to the database.
     *
     * @param reservation the reservation to save
     */
    void save(Reservation reservation);

    /**
     * Deletes a reservation by its ID.
     *
     * @param id the reservation ID to delete
     */
    void delete(int id);

    /**
     * Retrieves a reservation by its ID.
     *
     * @param id the reservation ID
     * @return the reservation, or null if not found
     */
    Reservation findById(int id);

    /**
     * Retrieves all reservations (active and cancelled) belonging to a user.
     *
     * @param userId the user ID
     * @return list of reservations; empty if none
     */
    List<Reservation> findByUser(int userId);

    /**
     * Retrieves the active reservation for a given license plate, if any.
     *
     * @param plate the license plate
     * @return the reservation, or null if not found
     */
    Reservation findByPlate(String plate);

    /**
     * Retrieves all reservations in the system.
     *
     * @return list of all reservations; empty if none
     */
    List<Reservation> findAll();

    /**
     * Updates an existing reservation (e.g., to mark it cancelled or change the space).
     *
     * @param reservation the reservation with updated fields
     */
    void update(Reservation reservation);
}
