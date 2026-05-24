package Presentation.Controllers;

import Business.Entities.OccupancyRecord;
import Business.Services.StatisticsService;
import Presentation.Views.OccupancyChartView;

import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the occupancy bar chart.
 * Loads historical occupancy data in the background and schedules periodic refreshes.
 */
public class StatisticsController {
    private static final Logger LOGGER = Logger.getLogger(StatisticsController.class.getName());

    private StatisticsService statisticsService;
    private OccupancyChartView chartView;
    private Timer refreshTimer;

    private static final int REFRESH_INTERVAL_MS = 60_000;

    /** Creates the controller for the occupancy chart. */
    public StatisticsController(OccupancyChartView chartView, StatisticsService statisticsService) {
        this.chartView = chartView;
        this.statisticsService = statisticsService;
    }

    // DB query on background thread, the chart updates back on the EDT.
    /** Loads chart data from the service and updates the view. */
    public void loadChartData() {
        new SwingWorker<List<OccupancyRecord>, Void>() {
            /** Loads chart values away from the EDT. */
            @Override
            protected List<OccupancyRecord> doInBackground() {
                return loadLastHourData();
            }

            /** Updates the chart after data has loaded. */
            @Override
            protected void done() {
                try {
                    updateChartWith(get());
                } catch (InterruptedException | ExecutionException e) {
                    logChartLoadFailure(e);
                }
            }
        }.execute();
    }

    /** Records the current occupancy and refreshes the chart if it is visible. */
    public void recordAndRefreshVisibleChart() {
        if (!isChartVisible()) {
            return;
        }

        new SwingWorker<List<OccupancyRecord>, Void>() {
            /** Records the latest occupancy and reloads the chart data away from the EDT. */
            @Override
            protected List<OccupancyRecord> doInBackground() {
                recordCurrentOccupancy();
                return loadLastHourData();
            }

            /** Updates the chart after the real-time occupancy refresh. */
            @Override
            protected void done() {
                try {
                    updateChartWith(get());
                } catch (InterruptedException | ExecutionException e) {
                    logChartLoadFailure(e);
                }
            }
        }.execute();
    }

    /** Refreshes the occupancy chart once. */
    public void refreshChart() {
        loadChartData();
    }

    /** Starts automatic chart refresh. */
    public void startTracking() {
        if (isRefreshTimerRunning())
            return;
        loadChartData();
        createRefreshTimer();
        startRefreshTimer();
    }

    /** Stops automatic chart refresh. */
    public void stopTracking() {
        stopRefreshTimer();
    }

    /** Stops chart timers when a user session ends. */
    public void clearSessionState() {
        stopTracking();
    }

    /** Loads occupancy values through the statistics service. */
    private List<OccupancyRecord> loadLastHourData() {
        return statisticsService.getLastHourData();
    }

    /** Sends loaded chart data to the view. */
    private void updateChartWith(List<OccupancyRecord> data) {
        chartView.updateChart(data);
    }

    /** Records the current number of occupied spaces through the statistics service. */
    private void recordCurrentOccupancy() {
        statisticsService.recordOccupancy();
    }

    /** Checks whether the chart is currently visible to the user. */
    private boolean isChartVisible() {
        return chartView != null && chartView.isVisible();
    }

    /** Logs a chart loading error. */
    private void logChartLoadFailure(Exception e) {
        LOGGER.log(Level.WARNING, "Failed to load occupancy chart data.", e);
    }

    /** Checks whether the refresh timer is already active. */
    private boolean isRefreshTimerRunning() {
        return refreshTimer != null && refreshTimer.isRunning();
    }

    /** Creates the timer that refreshes the chart. */
    private void createRefreshTimer() {
        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshChart());
    }

    /** Starts the refresh timer. */
    private void startRefreshTimer() {
        refreshTimer.start();
    }

    /** Stops the refresh timer if it exists. */
    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}
