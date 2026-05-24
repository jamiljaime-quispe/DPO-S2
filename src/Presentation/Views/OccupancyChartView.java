package Presentation.Views;

import Business.Entities.OccupancyRecord;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that displays a bar chart of parking occupancy over the last hour.
 */
public class OccupancyChartView extends JPanel {
    private static final int REFRESH_INTERVAL_SECONDS = 60;

    private OccupancyChartPanel chartPanel;
    private JButton backButton;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private int secondsLeft = REFRESH_INTERVAL_SECONDS;

    /** Creates the occupancy chart panel. */
    public OccupancyChartView() {
    }

    /** Builds the chart panel. */
    public void initComponents() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JPanel header = createHeaderPanel();
        add(header, BorderLayout.NORTH);

        chartPanel = createChartPanel();
        add(chartPanel, BorderLayout.CENTER);

        countdownTimer = createCountdownTimer();
        countdownTimer.start();
    }

    /**
     * Adds a listener to the button that returns to the main menu.
     *
     * @param listener action to run when the back button is clicked
     */
    public void addBackListener(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    /**
     * Updates the chart values.
     *
     * @param data occupancy values to show
     */
    public void updateChart(List<OccupancyRecord> data) {
        List<OccupancyRecord> chartData = getSafeChartData(data);
        if (chartPanel != null) {
            chartPanel.setData(chartData);
            chartPanel.repaint();
        }
        resetCountdownLabel();
    }

    /**
     * Creates the header that contains the back button, title, and countdown.
     *
     * @return configured header panel
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        backButton = createBackBoxButton();
        header.add(createBackCorner(), BorderLayout.WEST);
        header.add(createTitleLabel(), BorderLayout.CENTER);

        countdownLabel = createCountdownLabel();
        header.add(createCountdownCorner(), BorderLayout.EAST);
        return header;
    }

    /**
     * Creates the panel that holds the back button.
     *
     * @return configured corner panel
     */
    private JPanel createBackCorner() {
        JPanel backCorner = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backCorner.setOpaque(false);
        backCorner.add(backButton);
        return backCorner;
    }

    /**
     * Creates the chart title label.
     *
     * @return configured title label
     */
    private JLabel createTitleLabel() {
        JLabel title = new JLabel("Parking Occupancy - Last Hour", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(40, 40, 50));
        return title;
    }

    /**
     * Creates the countdown label.
     *
     * @return configured countdown label
     */
    private JLabel createCountdownLabel() {
        JLabel label = new JLabel("Next update in " + REFRESH_INTERVAL_SECONDS + "s", SwingConstants.RIGHT);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(new Color(140, 140, 150));
        return label;
    }

    /**
     * Creates the panel that holds the countdown label.
     *
     * @return configured corner panel
     */
    private JPanel createCountdownCorner() {
        JPanel countdownCorner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        countdownCorner.setOpaque(false);
        countdownCorner.add(countdownLabel);
        return countdownCorner;
    }

    /**
     * Creates the compact back button for the chart panel.
     *
     * @return configured back button
     */
    private JButton createBackBoxButton() {
        JButton b = new JButton("<-");
        b.setPreferredSize(new Dimension(42, 34));
        b.setMinimumSize(new Dimension(42, 34));
        b.setMaximumSize(new Dimension(42, 34));
        b.setForeground(new Color(33, 99, 168));
        b.setBackground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(33, 99, 168), 1, true));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText("Back to main menu");
        return b;
    }

    /**
     * Creates the chart drawing panel.
     *
     * @return chart drawing panel
     */
    private OccupancyChartPanel createChartPanel() {
        return new OccupancyChartPanel();
    }

    /**
     * Creates the countdown timer used by the chart header.
     *
     * @return configured timer
     */
    private Timer createCountdownTimer() {
        return new Timer(1_000, event -> updateCountdownLabel());
    }

    /** Decreases and redraws the countdown label. */
    private void updateCountdownLabel() {
        secondsLeft--;

        if (secondsLeft < 0) {
            secondsLeft = 0;
        }
        countdownLabel.setText("Next update in " + secondsLeft + "s");
    }

    /** Restores the countdown label to the full interval. */
    private void resetCountdownLabel() {
        secondsLeft = REFRESH_INTERVAL_SECONDS;
        if (countdownLabel != null) {
            countdownLabel.setText("Next update in " + REFRESH_INTERVAL_SECONDS + "s");
        }
    }

    /**
     * Converts null chart data to an empty list.
     *
     * @param data source chart data
     * @return data that can safely be passed to the chart panel
     */
    private List<OccupancyRecord> getSafeChartData(List<OccupancyRecord> data) {
        if (data == null) {
            return new ArrayList<>();
        }
        return data;
    }
}
