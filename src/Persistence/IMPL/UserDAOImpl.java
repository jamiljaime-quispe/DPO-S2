package Persistence.IMPL;

import Business.Entities.Admin;
import Business.Entities.Client;
import Business.Entities.User;
import Persistence.DatabaseManager;
import Persistence.UserDAO;

import java.sql.*;
import java.util.ArrayList;

/**
 * MySQL/JDBC implementation of {@link Persistence.UserDAO}. Maps the {@code users} table to {@link
 * Business.Entities.User} objects.
 * <p>
 * The class belongs to the persistence layer, so it is responsible for reading or writing stored data while
 * the other layers use cleaner methods.
 * </p>
 */
public class UserDAOImpl implements UserDAO {
    private final DatabaseManager db;

    /**
     * Creates the DAO with the shared database manager.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param db database manager used by persistence classes
     */
    public UserDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Handles save.
     * <p>
     * This method inserts a new row in the database using the values from the project object.
     * </p>
     *
     * @param user user used by this operation
     */
    @Override
    public void save(User user) {
        String sql = "INSERT INTO user (username, email, password, isAdmin) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setBoolean(4, "ADMIN".equals(user.getUserType()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(String.valueOf(keys.getInt(1)));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    /**
     * Finds by id.
     * <p>
     * This method runs the select query for this lookup and turns the database row back into a project
     * object.
     * </p>
     *
     * @param ID ID used by this operation
     * @return the matching by ID, or null when it is not found
     */
    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE userId = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds by username.
     * <p>
     * This method runs the select query for this lookup and turns the database row back into a project
     * object.
     * </p>
     *
     * @param username username entered or stored for the user
     * @return the matching by username, or null when it is not found
     */
    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by username: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds by email.
     * <p>
     * This method runs the select query for this lookup and turns the database row back into a project
     * object.
     * </p>
     *
     * @param email email entered or stored for the user
     * @return the matching by email, or null when it is not found
     */
    @Override
    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Deletes value.
     * <p>
     * This method removes the matching row from the database while hiding the SQL details from the service.
     * </p>
     *
     * @param ID ID used by this operation
     */
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM user WHERE userId = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /**
     * Updates value.
     * <p>
     * This method writes the changed values back to the database for an existing row.
     * </p>
     *
     * @param user user used by this operation
     */
    @Override
    public void update(User user) {
        String sql = "UPDATE user SET username = ?, email = ?, password = ?, isAdmin = ? WHERE userId = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setBoolean(4, "ADMIN".equals(user.getUserType()));
            ps.setInt(5, Integer.parseInt(user.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a database row into a User object. The operation is kept together so the stored data remains
     * consistent if something goes wrong halfway through.
     * <p>
     * This method keeps the SQL work inside persistence so the business layer does not need
     * database-specific code.
     * </p>
     *
     * @param rs rs used by this operation
     * @return the result of the operation
     * @throws SQLException if the operation cannot be completed correctly
     */
    private User mapRow(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getInt("userId"));
        String username = rs.getString("username");
        String email = rs.getString("email");
        String password = rs.getString("password");
        boolean isAdmin = rs.getBoolean("isAdmin");
        String userType = isAdmin ? "ADMIN" : "CLIENT";

        if (isAdmin) {
            return new Admin(id, username, email, password, userType, new ArrayList<>());
        } else {
            return new Client(id, username, email, password, userType, new ArrayList<>());
        }
    }

    /**
     * Gets the database connection through this DAO's database manager.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current connection
     */
    private java.sql.Connection getConnection() {
        return db.getConnection();
    }
}
