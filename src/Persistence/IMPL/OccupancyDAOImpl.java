package Persistence.IMPL;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import Business.Entities.OccupancyRecord;
import Persistence.DatabaseManager;
import Persistence.OccupancyDAO;

/**
 * MySQL/JDBC implementation of {@link Persistence.OccupancyDAO}. Reads and writes to the {@code
 * occupancy_log} table.
 * <p>
 * The class belongs to the persistence layer, so it is responsible for reading or writing stored data while
 * the other layers use cleaner methods.
 * </p>
 */
public class OccupancyDAOImpl implements OccupancyDAO {
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
    public OccupancyDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Saves one occupancy value in the database. The operation is kept together so the stored data remains
     * consistent if something goes wrong halfway through.
     * <p>
     * This method inserts a new row in the database using the values from the project object.
     * </p>
     *
     * @param timestamp timestamp used by this operation
     * @param occupancy occupancy used by this operation
     */
    @Override
    public void saveRecord(LocalDateTime timestamp, int occupancy) {
        String sql = """
                INSERT INTO occupancy_log (timestamp, occupiedCount)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE occupiedCount = VALUES(occupiedCount)
                """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(normalizeToMinute(timestamp)));
            ps.setInt(2, occupancy);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save occupancy record: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the last hour data.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return the current last hour data
     */
    @Override
    public List<OccupancyRecord> getLastHourData() {
        String sql = """
                SELECT timestamp, occupiedCount
                FROM occupancy_log
                WHERE timestamp >= ?
                ORDER BY timestamp ASC
                """;
        List<OccupancyRecord> data = new ArrayList<>();
        LocalDateTime firstMinuteToLoad = toLocalDateTime(getCurrentEpochMinute() - 59);

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(firstMinuteToLoad));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new OccupancyRecord(
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getInt("occupiedCount")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve occupancy data: " + e.getMessage(), e);
        }
        return data;
    }

    /**
     * Handles normalize to minute.
     * <p>
     * This method keeps the SQL work inside persistence so the business layer does not need
     * database-specific code.
     * </p>
     *
     * @param timestamp timestamp used by this operation
     * @return the result of the operation
     */
    private LocalDateTime normalizeToMinute(LocalDateTime timestamp) {
        return toLocalDateTime(toEpochMinute(timestamp));
    }

    /**
     * Handles to epoch minute.
     * <p>
     * This method keeps the SQL work inside persistence so the business layer does not need
     * database-specific code.
     * </p>
     *
     * @param timestamp date to convert
     * @return epoch minute for the date
     */
    private long toEpochMinute(LocalDateTime timestamp) {
        return timestamp.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond() / 60;
    }

    /**
     * Gets the current minute using January 1, 1970 as reference.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @return current epoch minute
     */
    private long getCurrentEpochMinute() {
        return Instant.now().getEpochSecond() / 60;
    }

    /**
     * Converts an epoch minute back to local date-time for database use. The operation is kept together so
     * the stored data remains consistent if something goes wrong halfway through.
     * <p>
     * This method keeps the SQL work inside persistence so the business layer does not need
     * database-specific code.
     * </p>
     *
     * @param epochMinute minute elapsed since January 1, 1970
     * @return local date-time at that minute
     */
    private LocalDateTime toLocalDateTime(long epochMinute) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochMinute * 60), ZoneId.systemDefault());
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
