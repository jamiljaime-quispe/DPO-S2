package Persistence.IMPL;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Business.Entities.Client;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Persistence.DatabaseManager;
import Persistence.ParkingSpaceDAO;

public class ParkingSpaceDAOImpl implements ParkingSpaceDAO {
    private final DatabaseManager db;

    public ParkingSpaceDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public List<ParkingSpace> findAll() {
        String sql = selectWithActiveReservation() + " ORDER BY ps.code";
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

    @Override
    public ParkingSpace findByCode(String code) {
        String sql = selectWithActiveReservation() + " WHERE ps.code = ?";

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

    @Override
    public List<ParkingSpace> findAvailableByType(VehicleType type) {
        String sql = selectWithActiveReservation()
                + " WHERE ps.vehicleType = ? AND ps.isOccupied = FALSE AND r.reservationId IS NULL";

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

    private static String selectWithActiveReservation() {
        return "SELECT ps.code, ps.floor, ps.vehicleType, ps.isOccupied, ps.occupiedByPlate, "
                + "r.reservationId, r.licensePlate AS resPlate, r.reservationDate, r.cancelledByAdmin, r.notified, r.isActive AS resIsActive, "
                + "rv.vehicleType AS resVehicleType, "
                + "u.userId AS ownerUserId, u.username AS ownerUsername, u.email AS ownerEmail, u.password AS ownerPassword, u.isAdmin AS ownerIsAdmin "
                + "FROM parking_space ps "
                + "LEFT JOIN reservation r ON r.spaceId = ps.spaceId AND r.isActive = 1 "
                + "LEFT JOIN vehicle rv ON rv.licensePlate = r.licensePlate "
                + "LEFT JOIN user u ON u.userId = rv.userId ";
    }

    private ParkingSpace mapRow(ResultSet rs) throws SQLException {
        String code = rs.getString("code");
        int floor = rs.getInt("floor");
        VehicleType type = VehicleType.valueOf(rs.getString("vehicleType"));
        boolean occupied = rs.getBoolean("isOccupied");
        String plate = rs.getString("occupiedByPlate");

        Vehicle parkedVehicle = null;
        if (plate != null && !plate.isEmpty()) {
            parkedVehicle = new Vehicle(plate, type, null, true);
        }

        int resId = rs.getInt("reservationId");
        if (rs.wasNull()) {
            return new ParkingSpace(code, floor, type, occupied, false, parkedVehicle, null);
        }

        String resPlate = rs.getString("resPlate");
        Timestamp resTs = rs.getTimestamp("reservationDate");
        LocalDateTime resWhen = resTs != null ? resTs.toLocalDateTime() : LocalDateTime.now();
        boolean cancelledByAdmin = rs.getBoolean("cancelledByAdmin");
        boolean notified = rs.getBoolean("notified");
        boolean resActive = rs.getBoolean("resIsActive");

        VehicleType resVehType = VehicleType.valueOf(rs.getString("resVehicleType"));
        String ownerUsername = rs.getString("ownerUsername");
        Vehicle resVehicle = new Vehicle(resPlate, resVehType, ownerUsername, false);

        int ownerUserId = rs.getInt("ownerUserId");
        String username = rs.getString("ownerUsername");
        String email = rs.getString("ownerEmail");
        String password = rs.getString("ownerPassword");
        boolean ownerIsAdmin = rs.getBoolean("ownerIsAdmin");
        String userType = ownerIsAdmin ? "ADMIN" : "CLIENT";
        Client client = new Client(String.valueOf(ownerUserId), username, email, password, userType, new ArrayList<>());

        ParkingSpace space = new ParkingSpace(code, floor, type, occupied, false, parkedVehicle, null);
        Reservation reservation = new Reservation(resId, client, resVehicle, space, resWhen);
        reservation.setCancelledByAdmin(cancelledByAdmin);
        reservation.setNotified(notified);
        reservation.setActive(resActive);
        space.reserve(reservation);
        return space;
    }
}
