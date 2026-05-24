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
 * Controller for the occupancy bar chart. Loads historical occupancy data in the background and schedules
 * periodic refreshes.
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
 */
public class StatisticsController {
    private static final Logger LOGGER = Logger.getLogger(StatisticsController.class.getName());

    private StatisticsService statisticsService;
    private OccupancyChartView chartView;
    private Timer refreshTimer;

    private static final int REFRESH_INTERVAL_MS = 60_000;

    /**
     * Creates the controller for the occupancy chart.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param chartView chart view that will be shown or updated
     * @param statisticsService statistics service used to apply the needed project logic
     */
    public StatisticsController(OccupancyChartView chartView, StatisticsService statisticsService) {
        this.chartView = chartView;
        this.statisticsService = statisticsService;
    }

    // DB query on background thread, the chart updates back on the EDT.
    /**
     * Loads chart data from the service and updates the view.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     */
    public void loadChartData() {
        new SwingWorker<List<OccupancyRecord>, Void>() {
            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the list of values found for the operation
             */
            @Override
            protected List<OccupancyRecord> doInBackground() {
                return loadLastHourData();
            }

            /**
             * Finishes the worker task on the Swing screen thread.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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

    /**
     * Handles record and refresh visible chart.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    public void recordAndRefreshVisibleChart() {
        if (!isChartVisible()) {
            return;
        }

        new SwingWorker<List<OccupancyRecord>, Void>() {
            /**
             * Runs the worker task away from the Swing screen thread.
             * <p>
             * This runs away from the Swing screen thread so database work or longer calculations do not
             * freeze the interface while the user is waiting.
             * </p>
             *
             * @return the list of values found for the operation
             */
            @Override
            protected List<OccupancyRecord> doInBackground() {
                recordCurrentOccupancy();
                return loadLastHourData();
            }

            /**
             * Finishes the worker task on the Swing screen thread.
             * <p>
             * This runs when the worker has finished, so it can read the final result, restore buttons or
             * cursors, and show the user a message if something failed.
             * </p>
             */
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

    /**
     * Handles refresh chart.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     */
    public void refreshChart() {
        loadChartData();
    }

    /**
     * Handles start tracking.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    public void startTracking() {
        if (isRefreshTimerRunning())
            return;
        loadChartData();
        createRefreshTimer();
        startRefreshTimer();
    }

    /**
     * Handles stop tracking.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    public void stopTracking() {
        stopRefreshTimer();
    }

    /**
     * Handles clear session state.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    public void clearSessionState() {
        stopTracking();
    }

    /**
     * Loads last hour data.
     * <p>
     * This method asks the service for fresh data and sends it back to the visible table or dialog when the
     * screen needs to change.
     * </p>
     *
     * @return the loaded last hour data
     */
    private List<OccupancyRecord> loadLastHourData() {
        return statisticsService.getLastHourData();
    }

    /**
     * Sends loaded chart data to the view.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param data data used by this operation
     */
    private void updateChartWith(List<OccupancyRecord> data) {
        chartView.updateChart(data);
    }

    /**
     * Handles record current occupancy.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void recordCurrentOccupancy() {
        statisticsService.recordOccupancy();
    }

    /**
     * Checks whether chart visible.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean isChartVisible() {
        return chartView != null && chartView.isVisible();
    }

    /**
     * Handles log chart load failure.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @param e e used by this operation
     */
    private void logChartLoadFailure(Exception e) {
        LOGGER.log(Level.WARNING, "Failed to load occupancy chart data.", e);
    }

    /**
     * Checks whether refresh timer running.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     *
     * @return true when the condition is met, false otherwise
     */
    private boolean isRefreshTimerRunning() {
        return refreshTimer != null && refreshTimer.isRunning();
    }

    /**
     * Creates refresh timer.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void createRefreshTimer() {
        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshChart());
    }

    /**
     * Handles start refresh timer.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void startRefreshTimer() {
        refreshTimer.start();
    }

    /**
     * Handles stop refresh timer.
     * <p>
     * This method keeps the controller action separate from the view code and from the business rule
     * itself.
     * </p>
     */
    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}
