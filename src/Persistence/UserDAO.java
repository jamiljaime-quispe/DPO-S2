package Persistence;

import Business.Entities.User;

/**
 * Data access interface for user accounts.
 */
public interface UserDAO {

    /**
     * Persists a new user to the database.
     *
     * @param user the user to save
     */
    void save(User user);

    /**
     * Retrieves a user by their numeric ID.
     *
     * @param id the user ID
     * @return the user, or null if not found
     */
    User findById(int id);

    /**
     * Retrieves a user by their username.
     *
     * @param username the username to search for
     * @return the user, or null if not found
     */
    User findByUsername(String username);

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email to search for
     * @return the user, or null if not found
     */
    User findByEmail(String email);

    /**
     * Deletes a user and their associated data by ID.
     *
     * @param id the user ID to delete
     */
    void delete(int id);

    /**
     * Updates an existing user record.
     *
     * @param user the user with updated fields
     */
    void update(User user);
}
