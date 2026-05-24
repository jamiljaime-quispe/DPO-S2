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
 * MySQL/JDBC implementation of {@link Persistence.OccupancyDAO}.
 * Reads and writes to the {@code occupancy_log} table.
 */
public class OccupancyDAOImpl implements OccupancyDAO {
    private final DatabaseManager db;

    /** Creates the DAO with the shared database manager. */
    public OccupancyDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    /** Saves one occupancy value in the database. */
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

    /** Loads occupancy values recorded during the last hour. */
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

    /** Removes seconds and smaller values through the epoch-minute value. */
    private LocalDateTime normalizeToMinute(LocalDateTime timestamp) {
        return toLocalDateTime(toEpochMinute(timestamp));
    }

    /**
     * Converts a date to minutes elapsed since January 1, 1970.
     *
     * @param timestamp date to convert
     * @return epoch minute for the date
     */
    private long toEpochMinute(LocalDateTime timestamp) {
        return timestamp.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond() / 60;
    }

    /**
     * Gets the current minute using January 1, 1970 as reference.
     *
     * @return current epoch minute
     */
    private long getCurrentEpochMinute() {
        return Instant.now().getEpochSecond() / 60;
    }

    /**
     * Converts an epoch minute back to local date-time for database use.
     *
     * @param epochMinute minute elapsed since January 1, 1970
     * @return local date-time at that minute
     */
    private LocalDateTime toLocalDateTime(long epochMinute) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochMinute * 60), ZoneId.systemDefault());
    }

    /** Gets the database connection through this DAO's database manager. */
    private java.sql.Connection getConnection() {
        return db.getConnection();
    }
}
