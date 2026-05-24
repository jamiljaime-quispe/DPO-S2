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
 * MySQL/JDBC implementation of {@link Persistence.VehicleDAO}. Maps the {@code vehicles} table to {@link
 * Business.Entities.Vehicle} objects.
 * <p>
 * The class belongs to the persistence layer, so it is responsible for reading or writing stored data while
 * the other layers use cleaner methods.
 * </p>
 */
public class VehicleDAOImpl implements VehicleDAO {
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
    public VehicleDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Handles save.
     * <p>
     * This method inserts a new row in the database using the values from the project object.
     * </p>
     *
     * @param vehicle vehicle used by this operation
     */
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

    /**
     * Finds by plate.
     * <p>
     * This method runs the select query for this lookup and turns the database row back into a project
     * object.
     * </p>
     *
     * @param plate license plate involved in the operation
     * @return the matching by plate, or null when it is not found
     */
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

    /**
     * Finds by user.
     * <p>
     * This method runs the select query for this lookup and turns the database row back into a project
     * object.
     * </p>
     *
     * @param userId identifier of the user involved in the operation
     * @return the matching by user, or null when it is not found
     */
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

    /**
     * Deletes value.
     * <p>
     * This method removes the matching row from the database while hiding the SQL details from the service.
     * </p>
     *
     * @param plate license plate involved in the operation
     */
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

    /**
     * Converts a database row into a Vehicle object. The operation is kept together so the stored data
     * remains consistent if something goes wrong halfway through.
     * <p>
     * This method keeps the SQL work inside persistence so the business layer does not need
     * database-specific code.
     * </p>
     *
     * @param rs rs used by this operation
     * @return the result of the operation
     * @throws SQLException if the operation cannot be completed correctly
     */
    private Vehicle mapRow(ResultSet rs) throws SQLException {
        return new Vehicle(
                rs.getString("licensePlate"),
                VehicleType.valueOf(rs.getString("vehicleType")),
                rs.getString("username"),
                false);
    }

    /**
     * Resolves the owner value into a database user ID. The operation is kept together so the stored data
     * remains consistent if something goes wrong halfway through.
     * <p>
     * This method keeps the SQL work inside persistence so the business layer does not need
     * database-specific code.
     * </p>
     *
     * @param owner owner used by this operation
     * @return the result of the operation
     * @throws SQLException if the operation cannot be completed correctly
     */
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
