package Presentation.Controllers;

import Business.Services.StatisticsService;
import Presentation.Views.OccupancyChartView;

import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Controller for the occupancy bar chart.
 * Loads historical occupancy data in the background and schedules periodic refreshes.
 */
public class StatisticsController {
    private StatisticsService statisticsService;
    private OccupancyChartView chartView;
    private Timer refreshTimer;

    private static final int REFRESH_INTERVAL_MS = 5_000;

    public StatisticsController(OccupancyChartView chartView, StatisticsService statisticsService) {
        this.chartView = chartView;
        this.statisticsService = statisticsService;
    }

    // DB query on background thread, the chart updates back on the EDT.
    public void loadChartData() {
        new SwingWorker<List<Integer>, Void>() {
            @Override
            protected List<Integer> doInBackground() {
                return statisticsService.getLastHourData();
            }

            @Override
            protected void done() {
                try {
                    chartView.updateChart(get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    public void refreshChart() {
        loadChartData();
    }

    public void startTracking() {
        if (refreshTimer != null && refreshTimer.isRunning())
            return;
        loadChartData();
        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshChart());
        refreshTimer.start();
    }

    public void stopTracking() {
        if (refreshTimer != null)
            refreshTimer.stop();
    }
}
