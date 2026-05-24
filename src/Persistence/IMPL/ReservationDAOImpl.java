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
 * MySQL/JDBC implementation of {@link Persistence.ReservationDAO}. Maps the {@code reservations} table to
 * {@link Business.Entities.Reservation} objects.
 * <p>
 * The class belongs to the persistence layer, so it is responsible for reading or writing stored data while
 * the other layers use cleaner methods.
 * </p>
 */
public class ReservationDAOImpl implements ReservationDAO {
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
    public ReservationDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Handles save.
     * <p>
     * This method inserts a new row in the database using the values from the project object.
     * </p>
     *
     * @param reservation reservation used by this operation
     */
    @Override
    public void save(Reservation reservation) {
        String sql = """
                INSERT INTO reservation (spaceId, licensePlate, reservationDate, cancelledByAdmin, notified, isActive, previousSpaceCode)
                VALUES ((SELECT spaceId FROM parking_space WHERE code = ?), ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reservation.getParkingSpace() != null ? reservation.getParkingSpace().getId() : null);
            ps.setString(2, reservation.getVehicle().getLicensePlate());
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getReservationDate()));
            ps.setBoolean(4, reservation.isCancelledByAdmin());
            ps.setBoolean(5, reservation.isNotified());
            ps.setBoolean(6, reservation.isActive());
            ps.setString(7, reservation.getPreviousSpaceCode());
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
        String sql = "DELETE FROM reservation WHERE reservationId = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete reservation: " + e.getMessage(), e);
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
    public Reservation findById(int id) {
        String sql = baseReservationQuery() + " WHERE r.reservationId = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id: " + e.getMessage(), e);
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
    public List<Reservation> findByUser(int userId) {
        String sql = baseReservationQuery() + " WHERE u.userId = ? ORDER BY r.reservationDate DESC";
        List<Reservation> reservations = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
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
    public Reservation findByPlate(String plate) {
        String sql = baseReservationQuery() + " WHERE r.licensePlate = ? AND r.isActive = TRUE";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, plate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by plate: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Finds all.
     * <p>
     * This method runs the select query for this lookup and turns the database row back into a project
     * object.
     * </p>
     *
     * @return the matching all, or null when it is not found
     */
    @Override
    public List<Reservation> findAll() {
        String sql = baseReservationQuery() + " ORDER BY r.reservationDate DESC";
        List<Reservation> reservations = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reservations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations: " + e.getMessage(), e);
        }
        return reservations;
    }

    /**
     * Updates value.
     * <p>
     * This method writes the changed values back to the database for an existing row.
     * </p>
     *
     * @param reservation reservation used by this operation
     */
    @Override
    public void update(Reservation reservation) {
        String sql = """
                UPDATE reservation
                SET spaceId = (SELECT spaceId FROM parking_space WHERE code = ?),
                    licensePlate = ?,
                    reservationDate = ?,
                    cancelledByAdmin = ?,
                    notified = ?,
                    isActive = ?,
                    previousSpaceCode = ?
                WHERE reservationId = ?
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, reservation.getParkingSpace() != null ? reservation.getParkingSpace().getId() : null);
            ps.setString(2, reservation.getVehicle().getLicensePlate());
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getReservationDate()));
            ps.setBoolean(4, reservation.isCancelledByAdmin());
            ps.setBoolean(5, reservation.isNotified());
            ps.setBoolean(6, reservation.isActive());
            ps.setString(7, reservation.getPreviousSpaceCode());
            ps.setInt(8, reservation.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation: " + e.getMessage(), e);
        }
    }

    /**
     * Handles base reservation query.
     * <p>
     * This method keeps the SQL work inside persistence so the business layer does not need
     * database-specific code.
     * </p>
     *
     * @return the result of the operation
     */
    private String baseReservationQuery() {
        return """
                SELECT r.reservationId, r.licensePlate, r.reservationDate,
                       r.cancelledByAdmin, r.notified, r.isActive, r.previousSpaceCode,
                       p.code, p.floor, p.vehicleType AS spaceVehicleType,
                       p.isOccupied, p.occupiedByPlate,
                       v.vehicleType AS vehicleType,
                       u.userId, u.username, u.email
                FROM reservation r
                LEFT JOIN parking_space p ON p.spaceId = r.spaceId
                JOIN vehicle v ON v.licensePlate = r.licensePlate
                JOIN user u ON u.userId = v.userId
                """;
    }

    /**
     * Converts a database row into a Reservation object. The operation is kept together so the stored data
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
    private Reservation mapRow(ResultSet rs) throws SQLException {
        VehicleType vehicleType = VehicleType.valueOf(rs.getString("vehicleType"));
        String spaceCode = rs.getString("code");

        ParkingSpace space = null;
        if (spaceCode != null) {
            VehicleType spaceType = VehicleType.valueOf(rs.getString("spaceVehicleType"));
            String parkedPlate = rs.getString("occupiedByPlate");

            Vehicle parkedVehicle = null;
            if (parkedPlate != null && !parkedPlate.isEmpty()) {
                parkedVehicle = new Vehicle(parkedPlate, spaceType, null, true);
            }

            space = new ParkingSpace(
                    spaceCode,
                    rs.getInt("floor"),
                    spaceType,
                    rs.getBoolean("isOccupied"),
                    false,
                    parkedVehicle,
                    null);
        }

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
        reservation.setPreviousSpaceCode(rs.getString("previousSpaceCode"));

        if (reservation.isActive() && space != null) {
            space.reserve(reservation);
        }

        return reservation;
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
