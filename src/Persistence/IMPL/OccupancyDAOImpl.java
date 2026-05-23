package Persistence.IMPL;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "INSERT INTO occupancy_log (timestamp, occupiedCount) VALUES (?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(timestamp));
            ps.setInt(2, occupancy);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save occupancy record: " + e.getMessage(), e);
        }
    }

    /** Loads occupancy values recorded during the last hour. */
    @Override
    public List<Integer> getLastHourData() {
        String sql = """
                SELECT occupiedCount
                FROM occupancy_log
                WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
                ORDER BY timestamp ASC
                """;
        List<Integer> data = new ArrayList<>();

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                data.add(rs.getInt("occupiedCount"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve occupancy data: " + e.getMessage(), e);
        }
        return data;
    }
}
