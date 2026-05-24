package Persistence;

import Business.Entities.User;

/**
 * Data access interface for user accounts. This interface keeps the promise clear so another class can use
 * it without depending on a specific implementation.
 * <p>
 * The interface lets the business layer ask for stored data without depending on the class that talks
 * directly to the database.
 * </p>
 */
public interface UserDAO {

    /**
     * Persists a new user to the database. The operation is kept together so the stored data remains
     * consistent if something goes wrong halfway through.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param user the user to save
     */
    void save(User user);

    /**
     * Finds by id.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param ID the user ID
     * @return the user, or null if not found
     */
    User findById(int id);

    /**
     * Finds by username.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param username the username to search for
     * @return the user, or null if not found
     */
    User findByUsername(String username);

    /**
     * Finds by email.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param email the email to search for
     * @return the user, or null if not found
     */
    User findByEmail(String email);

    /**
     * Deletes value.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param ID the user ID to delete
     */
    void delete(int id);

    /**
     * Updates value.
     * <p>
     * This helper keeps the step named and separate, which makes the larger operation easier to read and
     * follow.
     * </p>
     *
     * @param user the user with updated fields
     */
    void update(User user);
}
