package Persistence.IMPL;

import Business.Entities.Client;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Persistence.DatabaseManager;
import Persistence.ReservationDAO;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAOImpl implements ReservationDAO {
    private final DatabaseManager db;

    public ReservationDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void save(Reservation reservation) {
        if (reservation.getParkingSpace() == null)
            throw new IllegalArgumentException("Reservation has no parking space");

        String sql = "INSERT INTO reservation (spaceId, licensePlate, reservationDate, cancelledByAdmin, notified, isActive) "
                + "VALUES ((SELECT spaceId FROM parking_space WHERE code = ?), ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reservation.getParkingSpace().getId());
            ps.setString(2, reservation.getVehicle().getLicensePlate());
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getReservationDate()));
            ps.setBoolean(4, reservation.isCancelledByAdmin());
            ps.setBoolean(5, reservation.isNotified());
            ps.setBoolean(6, reservation.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    reservation.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM reservation WHERE reservationId = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public Reservation findById(int id) {
        String sql = baseSelect() + " WHERE r.reservationId = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Reservation> findByUser(int userId) {
        String sql = baseSelect() + " WHERE v.userId = ? ORDER BY r.reservationDate DESC";
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by user: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Reservation findByPlate(String plate) {
        if (plate == null)
            return null;
        String sql = baseSelect() + " WHERE r.licensePlate = ? AND r.isActive = 1 LIMIT 1";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, plate.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by plate: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Reservation> findAll() {
        String sql = baseSelect() + " ORDER BY r.reservationId";
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list reservations: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void update(Reservation reservation) {
        String sql = "UPDATE reservation SET spaceId = (SELECT spaceId FROM parking_space WHERE code = ?), "
                + "licensePlate = ?, reservationDate = ?, cancelledByAdmin = ?, notified = ?, isActive = ? "
                + "WHERE reservationId = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, reservation.getParkingSpace() != null ? reservation.getParkingSpace().getId() : null);
            ps.setString(2, reservation.getVehicle() != null ? reservation.getVehicle().getLicensePlate() : null);
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getReservationDate()));
            ps.setBoolean(4, reservation.isCancelledByAdmin());
            ps.setBoolean(5, reservation.isNotified());
            ps.setBoolean(6, reservation.isActive());
            ps.setInt(7, reservation.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation: " + e.getMessage(), e);
        }
    }

    private static String baseSelect() {
        return "SELECT r.reservationId, r.licensePlate, r.reservationDate, r.cancelledByAdmin, r.notified, r.isActive, "
                + "ps.code, ps.floor, ps.vehicleType, ps.isOccupied, ps.occupiedByPlate, "
                + "v.vehicleType AS vehicleTypeFromVehicle, "
                + "u.userId, u.username, u.email, u.password, u.isAdmin "
                + "FROM reservation r "
                + "JOIN parking_space ps ON ps.spaceId = r.spaceId "
                + "JOIN vehicle v ON v.licensePlate = r.licensePlate "
                + "LEFT JOIN user u ON u.userId = v.userId ";
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        int resId = rs.getInt("reservationId");
        String plate = rs.getString("licensePlate");
        LocalDateTime when = rs.getTimestamp("reservationDate").toLocalDateTime();
        boolean cancelledByAdmin = rs.getBoolean("cancelledByAdmin");
        boolean notified = rs.getBoolean("notified");
        boolean active = rs.getBoolean("isActive");

        String code = rs.getString("code");
        int floor = rs.getInt("floor");
        VehicleType spaceType = VehicleType.valueOf(rs.getString("vehicleType"));
        boolean occupied = rs.getBoolean("isOccupied");
        String occPlate = rs.getString("occupiedByPlate");

        Vehicle parked = null;
        if (occPlate != null && !occPlate.isEmpty()) {
            parked = new Vehicle(occPlate, spaceType, null, true);
        }

        ParkingSpace space = new ParkingSpace(code, floor, spaceType, occupied, false, parked, null);

        VehicleType vehicleType = VehicleType.valueOf(rs.getString("vehicleTypeFromVehicle"));
        String ownerUsername = rs.getString("username");
        Vehicle vehicle = new Vehicle(plate, vehicleType, ownerUsername, false);

        int userId = rs.getInt("userId");
        if (rs.wasNull()) {
            userId = 0;
        }
        String username = rs.getString("username");
        String email = rs.getString("email");
        boolean isAdmin = rs.getBoolean("isAdmin");
        String userType = isAdmin ? "ADMIN" : "CLIENT";
        Client client = new Client(userId > 0 ? String.valueOf(userId) : null,
                username != null ? username : "",
                email != null ? email : "",
                "",
                userType,
                new ArrayList<>());

        Reservation reservation = new Reservation(resId, client, vehicle, space, when);
        reservation.setCancelledByAdmin(cancelledByAdmin);
        reservation.setNotified(notified);
        reservation.setActive(active);

        if (active) {
            space.reserve(reservation);
        }
        return reservation;
    }
}
