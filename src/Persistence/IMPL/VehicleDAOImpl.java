package Persistence.IMPL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import Business.Entities.*;
import Persistence.DatabaseManager;
import Persistence.VehicleDAO;

/**
 * MySQL/JDBC implementation of {@link Persistence.VehicleDAO}.
 * Maps the {@code vehicles} table to {@link Business.Entities.Vehicle} objects.
 */
public class VehicleDAOImpl implements VehicleDAO {
    private final DatabaseManager db;

    /** Creates the DAO with the shared database manager. */
    public VehicleDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    /** Saves a vehicle. */
    @Override
    public void save(Vehicle vehicle) {
        String sql = """
                INSERT INTO vehicle (licensePlate, userId, vehicleType)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE vehicleType = VALUES(vehicleType)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, vehicle.getLicensePlate());
            ps.setInt(2, resolveOwnerUserId(vehicle.getOwner()));
            ps.setString(3, vehicle.getType().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save vehicle: " + e.getMessage(), e);
        }
    }

    /** Finds a vehicle by license plate. */
    @Override
    public Vehicle findByPlate(String plate) {
        String sql = """
                SELECT v.licensePlate, v.vehicleType, u.username
                FROM vehicle v
                JOIN user u ON u.userId = v.userId
                WHERE v.licensePlate = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, plate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find vehicle by plate: " + e.getMessage(), e);
        }

        return null;
    }

    /** Loads vehicles owned by a user. */
    @Override
    public List<Vehicle> findByUser(int userId) {
        String sql = """
                SELECT v.licensePlate, v.vehicleType, u.username
                FROM vehicle v
                JOIN user u ON u.userId = v.userId
                WHERE v.userId = ?
                ORDER BY v.licensePlate
                """;
        List<Vehicle> list = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find vehicles by user: " + e.getMessage(), e);
        }

        return list;
    }

    /** Deletes a vehicle by license plate. */
    @Override
    public void delete(String plate) {
        String sql = "DELETE FROM vehicle WHERE licensePlate = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, plate);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete vehicle: " + e.getMessage(), e);
        }
    }

    /** Converts a database row into a Vehicle object. */
    private Vehicle mapRow(ResultSet rs) throws SQLException {
        return new Vehicle(
                rs.getString("licensePlate"),
                VehicleType.valueOf(rs.getString("vehicleType")),
                rs.getString("username"),
                false);
    }

    /** Resolves the owner value into a database user ID. */
    private int resolveOwnerUserId(String owner) throws SQLException {
        if (owner == null || owner.isBlank()) {
            throw new SQLException("Vehicle owner is required.");
        }

        try {
            return Integer.parseInt(owner);
        } catch (NumberFormatException ignored) {
            String sql = "SELECT userId FROM user WHERE username = ?";
            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, owner);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("userId");
                    }
                }
            }
        }

        throw new SQLException("Vehicle owner was not found.");
    }

    /** Gets the database connection through this DAO's database manager. */
    private java.sql.Connection getConnection() {
        return db.getConnection();
    }
}
