package Persistence.IMPL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Business.Entities.Client;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Persistence.DatabaseManager;
import Persistence.ReservationDAO;

/**
 * MySQL/JDBC implementation of {@link Persistence.ReservationDAO}.
 * Maps the {@code reservations} table to {@link Business.Entities.Reservation} objects.
 */
public class ReservationDAOImpl implements ReservationDAO {
    private final DatabaseManager db;

    public ReservationDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void save(Reservation reservation) {
        String sql = """
                INSERT INTO reservation (spaceId, licensePlate, reservationDate, cancelledByAdmin, notified, isActive)
                VALUES ((SELECT spaceId FROM parking_space WHERE code = ?), ?, ?, ?, ?, ?)
                """;

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
        String sql = baseReservationQuery() + " WHERE r.reservationId = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Reservation> findByUser(int userId) {
        String sql = baseReservationQuery() + " WHERE u.userId = ? ORDER BY r.reservationDate DESC";
        List<Reservation> reservations = new ArrayList<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by user: " + e.getMessage(), e);
        }
        return reservations;
    }

    @Override
    public Reservation findByPlate(String plate) {
        String sql = baseReservationQuery() + " WHERE r.licensePlate = ? AND r.isActive = TRUE";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, plate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by plate: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Reservation> findAll() {
        String sql = baseReservationQuery() + " ORDER BY r.reservationDate DESC";
        List<Reservation> reservations = new ArrayList<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations: " + e.getMessage(), e);
        }
        return reservations;
    }

    @Override
    public void update(Reservation reservation) {
        String sql = """
                UPDATE reservation
                SET spaceId = (SELECT spaceId FROM parking_space WHERE code = ?),
                    licensePlate = ?,
                    reservationDate = ?,
                    cancelledByAdmin = ?,
                    notified = ?,
                    isActive = ?
                WHERE reservationId = ?
                """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, reservation.getParkingSpace().getId());
            ps.setString(2, reservation.getVehicle().getLicensePlate());
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

    private String baseReservationQuery() {
        return """
                SELECT r.reservationId, r.licensePlate, r.reservationDate,
                       r.cancelledByAdmin, r.notified, r.isActive,
                       p.code, p.floor, p.vehicleType AS spaceVehicleType,
                       p.isOccupied, p.occupiedByPlate,
                       v.vehicleType AS vehicleType,
                       u.userId, u.username, u.email
                FROM reservation r
                JOIN parking_space p ON p.spaceId = r.spaceId
                JOIN vehicle v ON v.licensePlate = r.licensePlate
                JOIN user u ON u.userId = v.userId
                """;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        VehicleType spaceType = VehicleType.valueOf(rs.getString("spaceVehicleType"));
        VehicleType vehicleType = VehicleType.valueOf(rs.getString("vehicleType"));
        String parkedPlate = rs.getString("occupiedByPlate");

        Vehicle parkedVehicle = null;
        if (parkedPlate != null && !parkedPlate.isEmpty()) {
            parkedVehicle = new Vehicle(parkedPlate, spaceType, null, true);
        }

        ParkingSpace space = new ParkingSpace(
                rs.getString("code"),
                rs.getInt("floor"),
                spaceType,
                rs.getBoolean("isOccupied"),
                false,
                parkedVehicle,
                null);

        Client user = new Client(
                String.valueOf(rs.getInt("userId")),
                rs.getString("username"),
                rs.getString("email"),
                "",
                "CLIENT",
                new ArrayList<>());

        Vehicle vehicle = new Vehicle(rs.getString("licensePlate"), vehicleType, user.getUsername(), false);
        LocalDateTime reservationDate = rs.getTimestamp("reservationDate").toLocalDateTime();
        Reservation reservation = new Reservation(
                rs.getInt("reservationId"),
                user,
                vehicle,
                space,
                reservationDate);
        reservation.setCancelledByAdmin(rs.getBoolean("cancelledByAdmin"));
        reservation.setNotified(rs.getBoolean("notified"));
        reservation.setActive(rs.getBoolean("isActive"));

        if (reservation.isActive()) {
            space.reserve(reservation);
        }

        return reservation;
    }
}
