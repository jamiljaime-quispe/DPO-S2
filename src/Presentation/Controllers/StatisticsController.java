package Presentation.Controllers;

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
        new SwingWorker<List<Integer>, Void>() {
            /** Loads chart values away from the EDT. */
            @Override
            protected List<Integer> doInBackground() {
                return statisticsService.getLastHourData();
            }

            /** Updates the chart after data has loaded. */
            @Override
            protected void done() {
                try {
                    chartView.updateChart(get());
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Failed to load occupancy chart data.", e);
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
        if (refreshTimer != null && refreshTimer.isRunning())
            return;
        loadChartData();
        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshChart());
        refreshTimer.start();
    }

    /** Stops automatic chart refresh. */
    public void stopTracking() {
        if (refreshTimer != null)
            refreshTimer.stop();
    }
}
