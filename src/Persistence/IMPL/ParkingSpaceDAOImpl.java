package Persistence.IMPL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Client;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Persistence.DatabaseManager;
import Persistence.ParkingSpaceDAO;

import java.time.LocalDateTime;

/**
 * MySQL/JDBC implementation of {@link Persistence.ParkingSpaceDAO}.
 * Maps the {@code parking_space} table to {@link Business.Entities.ParkingSpace} objects,
 * including any active reservation and currently parked vehicle.
 */
public class ParkingSpaceDAOImpl implements ParkingSpaceDAO {
    private final DatabaseManager db;

    /** Creates the DAO with the shared database manager. */
    public ParkingSpaceDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    /** Loads all parking spaces. */
    @Override
    public List<ParkingSpace> findAll() {
        String sql = """
                SELECT p.code, p.floor, p.vehicleType, p.isOccupied, p.occupiedByPlate,
                       r.reservationId, r.licensePlate AS reservedPlate, r.reservationDate,
                       r.cancelledByAdmin, r.notified, r.isActive,
                       ru.userId AS reservedUserId, ru.username AS reservedUsername,
                       ru.email AS reservedEmail, rv.vehicleType AS reservedVehicleType
                FROM parking_space p
                LEFT JOIN reservation r ON r.spaceId = p.spaceId AND r.isActive = TRUE
                LEFT JOIN vehicle rv ON rv.licensePlate = r.licensePlate
                LEFT JOIN user ru ON ru.userId = rv.userId
                ORDER BY p.code
                """;
        List<ParkingSpace> list = new ArrayList<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch parking spaces: " + e.getMessage(), e);
        }
        return list;
    }

    /** Finds one parking space by code. */
    @Override
    public ParkingSpace findByCode(String code) {
        String sql = """
                SELECT p.code, p.floor, p.vehicleType, p.isOccupied, p.occupiedByPlate,
                       r.reservationId, r.licensePlate AS reservedPlate, r.reservationDate,
                       r.cancelledByAdmin, r.notified, r.isActive,
                       ru.userId AS reservedUserId, ru.username AS reservedUsername,
                       ru.email AS reservedEmail, rv.vehicleType AS reservedVehicleType
                FROM parking_space p
                LEFT JOIN reservation r ON r.spaceId = p.spaceId AND r.isActive = TRUE
                LEFT JOIN vehicle rv ON rv.licensePlate = r.licensePlate
                LEFT JOIN user ru ON ru.userId = rv.userId
                WHERE p.code = ?
                """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find parking space: " + e.getMessage(), e);
        }
        return null;
    }

    /** Loads vacant and unreserved spaces for a vehicle type. */
    @Override
    public List<ParkingSpace> findAvailableByType(VehicleType type) {
        String sql = """
                SELECT p.code, p.floor, p.vehicleType, p.isOccupied, p.occupiedByPlate,
                       r.reservationId, r.licensePlate AS reservedPlate, r.reservationDate,
                       r.cancelledByAdmin, r.notified, r.isActive,
                       ru.userId AS reservedUserId, ru.username AS reservedUsername,
                       ru.email AS reservedEmail, rv.vehicleType AS reservedVehicleType
                FROM parking_space p
                LEFT JOIN reservation r ON r.spaceId = p.spaceId AND r.isActive = TRUE
                LEFT JOIN vehicle rv ON rv.licensePlate = r.licensePlate
                LEFT JOIN user ru ON ru.userId = rv.userId
                WHERE p.vehicleType = ? AND p.isOccupied = FALSE AND r.reservationId IS NULL
                ORDER BY p.code
                """;
        List<ParkingSpace> list = new ArrayList<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch available parking spaces: " + e.getMessage(), e);
        }
        return list;
    }

    /** Saves a new parking space. */
    @Override
    public void save(ParkingSpace space) {
        String sql = "INSERT INTO parking_space (code, floor, vehicleType, isOccupied, occupiedByPlate) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, space.getId());
            ps.setInt(2, space.getFloor());
            ps.setString(3, space.getVehicleType().name());
            ps.setBoolean(4, space.isOccupied());
            ps.setString(5, space.getParkedVehicle() != null ? space.getParkedVehicle().getLicensePlate() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save parking space: " + e.getMessage(), e);
        }
    }

    /** Updates the occupied state of a parking space. */
    @Override
    public void update(ParkingSpace space) {
        String sql = "UPDATE parking_space SET isOccupied = ?, occupiedByPlate = ? WHERE code = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, space.isOccupied());
            ps.setString(2, space.getParkedVehicle() != null ? space.getParkedVehicle().getLicensePlate() : null);
            ps.setString(3, space.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update parking space: " + e.getMessage(), e);
        }
    }

    /** Updates the editable details of a parking space. */
    @Override
    public void updateDetails(ParkingSpace space) {
        String sql = "UPDATE parking_space SET floor = ?, vehicleType = ? WHERE code = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, space.getFloor());
            ps.setString(2, space.getVehicleType().name());
            ps.setString(3, space.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update parking space details: " + e.getMessage(), e);
        }
    }

    /** Deletes a parking space by code. */
    @Override
    public void delete(String code) {
        String sql = "DELETE FROM parking_space WHERE code = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete parking space: " + e.getMessage(), e);
        }
    }

    /** Converts a database row into a ParkingSpace object. */
    private ParkingSpace mapRow(ResultSet rs) throws SQLException {
        String code = rs.getString("code");
        int floor = rs.getInt("floor");
        VehicleType type = VehicleType.valueOf(rs.getString("vehicleType"));
        boolean occupied = rs.getBoolean("isOccupied");
        String plate = rs.getString("occupiedByPlate");
        String reservedPlate = rs.getString("reservedPlate");

        Vehicle vehicle = null;

        if (plate != null && !plate.isEmpty()) {
            vehicle = new Vehicle(plate, type, null, true);
        }

        ParkingSpace space = new ParkingSpace(code, floor, type, occupied, false, vehicle, null);

        if (reservedPlate != null && !reservedPlate.isEmpty()) {
            String reservedVehicleType = rs.getString("reservedVehicleType");
            VehicleType vehicleType = reservedVehicleType != null
                    ? VehicleType.valueOf(reservedVehicleType)
                    : type;
            String username = rs.getString("reservedUsername");
            String email = rs.getString("reservedEmail");
            String userId = String.valueOf(rs.getInt("reservedUserId"));

            Client reservedUser = null;
            if (username != null) {
                reservedUser = new Client(userId, username, email, "", "CLIENT", new ArrayList<>());
            }

            Vehicle reservedVehicle = new Vehicle(reservedPlate, vehicleType, username, false);
            LocalDateTime reservationDate = rs.getTimestamp("reservationDate").toLocalDateTime();
            Reservation reservation = new Reservation(
                    rs.getInt("reservationId"),
                    reservedUser,
                    reservedVehicle,
                    space,
                    reservationDate);
            reservation.setCancelledByAdmin(rs.getBoolean("cancelledByAdmin"));
            reservation.setNotified(rs.getBoolean("notified"));
            reservation.setActive(rs.getBoolean("isActive"));
            space.reserve(reservation);
        }

        return space;
    }
}
