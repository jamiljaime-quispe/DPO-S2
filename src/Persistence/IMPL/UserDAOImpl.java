package Persistence.IMPL;

import Business.Entities.Admin;
import Business.Entities.Client;
import Business.Entities.User;
import Persistence.DatabaseManager;
import Persistence.UserDAO;

import java.sql.*;
import java.util.ArrayList;

/**
 * MySQL/JDBC implementation of {@link Persistence.UserDAO}.
 * Maps the {@code users} table to {@link Business.Entities.User} objects.
 */
public class UserDAOImpl implements UserDAO {
    private final DatabaseManager db;

    /** Creates the DAO with the shared database manager. */
    public UserDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    /** Saves a new user. */
    @Override
    public void save(User user) {
        String sql = "INSERT INTO user (username, email, password, isAdmin) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    /** Finds a user by ID. */
    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE userId = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
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

    /** Finds a user by username. */
    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
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

    /** Finds a user by email address. */
    @Override
    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
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

    /** Deletes a user by ID. */
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM user WHERE userId = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /** Updates an existing user. */
    @Override
    public void update(User user) {
        String sql = "UPDATE user SET username = ?, email = ?, password = ?, isAdmin = ? WHERE userId = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
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

    /** Converts a database row into a User object. */
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
}
