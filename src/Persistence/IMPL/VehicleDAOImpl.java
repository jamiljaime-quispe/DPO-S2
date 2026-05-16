package Persistence.IMPL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Persistence.DatabaseManager;
import Persistence.VehicleDAO;

public class VehicleDAOImpl implements VehicleDAO {
    private final DatabaseManager db;

    public VehicleDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void save(Vehicle vehicle) {
        Integer userId = resolveUserId(vehicle.getOwner());
        if (userId == null)
            throw new IllegalArgumentException("Unknown owner username for vehicle: " + vehicle.getOwner());

        String sql = "INSERT INTO vehicle (licensePlate, userId, vehicleType) VALUES (?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, vehicle.getLicensePlate());
            ps.setInt(2, userId);
            ps.setString(3, vehicle.getType().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save vehicle: " + e.getMessage(), e);
        }
    }

    @Override
    public Vehicle findByPlate(String plate) {
        if (plate == null || plate.isBlank())
            return null;
        String sql = "SELECT v.licensePlate, v.vehicleType, u.username, "
                + "EXISTS (SELECT 1 FROM parking_space ps WHERE ps.occupiedByPlate = v.licensePlate AND ps.isOccupied = 1) AS parked "
                + "FROM vehicle v JOIN user u ON u.userId = v.userId WHERE v.licensePlate = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, plate.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapVehicle(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find vehicle: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Vehicle> findByUser(int userId) {
        String sql = "SELECT v.licensePlate, v.vehicleType, u.username, "
                + "EXISTS (SELECT 1 FROM parking_space ps WHERE ps.occupiedByPlate = v.licensePlate AND ps.isOccupied = 1) AS parked "
                + "FROM vehicle v JOIN user u ON u.userId = v.userId WHERE v.userId = ?";
        List<Vehicle> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapVehicle(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list vehicles: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void delete(String plate) {
        String sql = "DELETE FROM vehicle WHERE licensePlate = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, plate);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete vehicle: " + e.getMessage(), e);
        }
    }

    private Integer resolveUserId(String username) {
        if (username == null || username.isBlank())
            return null;
        String sql = "SELECT userId FROM user WHERE username = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("userId");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resolve user id: " + e.getMessage(), e);
        }
        return null;
    }

    private static Vehicle mapVehicle(ResultSet rs) throws SQLException {
        String plate = rs.getString("licensePlate");
        VehicleType type = VehicleType.valueOf(rs.getString("vehicleType"));
        String username = rs.getString("username");
        boolean parked = rs.getBoolean("parked");
        return new Vehicle(plate, type, username, parked);
    }
}
