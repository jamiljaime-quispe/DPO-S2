package Presentation.Controllers;

import Business.Services.StatisticsService;
import Presentation.Views.OccupancyChartView;

import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.util.List;

public class StatisticsController {
    private StatisticsService statisticsService;
    private OccupancyChartView chartView;
    private Timer refreshTimer;

    private static final int REFRESH_INTERVAL_MS = 30_000;

    public StatisticsController(OccupancyChartView chartView, StatisticsService statisticsService) {
        this.chartView = chartView;
        this.statisticsService = statisticsService;
    }

    // DB query on background thread, the chart updates back on the EDT.
    public void loadChartData() {
        new SwingWorker<List<Integer>, Void>() {
            @Override
            protected List<Integer> doInBackground() {
                return statisticsService.getLastHourData(); // Here I have the background thread.
            }

            @Override
            protected void done() {
                try {
                    chartView.updateChart(get()); // This is the EDT.
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    public void refreshChart() {
        loadChartData();
    }

    // Starts an EDT timer that runs every 30s, each tick offloads DB.
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
