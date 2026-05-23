package Presentation.Views;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OccupancyChartView extends JPanel {
    private List<Integer> data = new ArrayList<>();
    private ChartPanel chartPanel;
    private JButton backButton;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private int secondsLeft = 5;

    public OccupancyChartView() {
    }

    public void initComponents() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        backButton = createBackBoxButton();
        JPanel backCorner = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backCorner.setOpaque(false);
        backCorner.add(backButton);
        header.add(backCorner, BorderLayout.WEST);

        JLabel title = new JLabel("Parking Occupancy — Last Hour", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(40, 40, 50));
        header.add(title, BorderLayout.CENTER);

        countdownLabel = new JLabel("Next update in 5s", SwingConstants.RIGHT);
        countdownLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countdownLabel.setForeground(new Color(140, 140, 150));
        JPanel countdownCorner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        countdownCorner.setOpaque(false);
        countdownCorner.add(countdownLabel);
        header.add(countdownCorner, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        chartPanel = new ChartPanel();
        add(chartPanel, BorderLayout.CENTER);

        countdownTimer = new Timer(1_000, e -> {
            secondsLeft--;

            if (secondsLeft < 0)
                secondsLeft = 0;
            countdownLabel.setText("Next update in " + secondsLeft + "s");
        });
        countdownTimer.start();
    }

    public JButton getBackButton() {
        return backButton;
    }

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

    public void updateChart(List<Integer> data) {
        this.data = (data != null) ? data : new ArrayList<>();
        if (chartPanel != null)
            chartPanel.repaint();
        secondsLeft = 5;
        if (countdownLabel != null)
            countdownLabel.setText("Next update in 5s");
    }

    public void startAutoRefresh() {
    }

    public void stopAutoRefresh() {
    }


    private class ChartPanel extends JPanel {
        private static final int PAD_LEFT = 50;
        private static final int PAD_RIGHT = 20;
        private static final int PAD_TOP = 20;
        private static final int PAD_BOTTOM = 40;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(600, 300);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int chartW = w - PAD_LEFT - PAD_RIGHT;
            int chartH = h - PAD_TOP - PAD_BOTTOM;

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);

            if (data.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                String msg = "No data available";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int maxVal = 1;
            for (Integer value : data) {
                if (value > maxVal) {
                    maxVal = value;
                }
            }
            int yScale = Math.max(((maxVal / 5) + 1) * 5, 5);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int gridLines = 5;
            for (int i = 0; i <= gridLines; i++) {
                int yVal = yScale * i / gridLines;
                int y = PAD_TOP + chartH - (chartH * i / gridLines);

                g2.setColor(new Color(220, 220, 220));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);

                g2.setColor(Color.DARK_GRAY);
                String label = String.valueOf(yVal);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, PAD_LEFT - fm.stringWidth(label) - 5, y + fm.getAscent() / 2);
            }

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);
            g2.drawLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(Color.GRAY);
            int n = data.size();
            String oldestLabel = n > 0 ? ((n - 1) * 5) + " sec ago" : "older";
            String newest = "now";
            FontMetrics fmX = g2.getFontMetrics();
            g2.drawString(oldestLabel, PAD_LEFT + 4, PAD_TOP + chartH + 28);
            g2.drawString(newest, PAD_LEFT + chartW - fmX.stringWidth(newest), PAD_TOP + chartH + 28);

            g2.rotate(-Math.PI / 2);
            g2.setColor(Color.GRAY);
            g2.drawString("Occupied spaces", -(PAD_TOP + chartH / 2 + 40), 12);
            g2.rotate(Math.PI / 2);

            int slotW = Math.max(1, chartW / Math.max(n, 1));
            int gap = Math.max(1, slotW / 6);
            int barW = Math.max(1, slotW - gap);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            FontMetrics fmBar = g2.getFontMetrics();

            for (int i = 0; i < n; i++) {
                int value = data.get(i);
                int barH = (int) ((double) value / yScale * chartH);
                int x = PAD_LEFT + i * slotW + gap / 2;
                int y = PAD_TOP + chartH - barH;
                boolean isLatest = (i == n - 1);

                g2.setColor(isLatest ? new Color(33, 99, 168) : new Color(33, 99, 168, 170));
                g2.fillRect(x, y, barW, barH);
                g2.setColor(new Color(20, 60, 110));
                g2.drawRect(x, y, barW, barH);

                if (value > 0) {
                    String label = String.valueOf(value);
                    int labelX = x + (barW - fmBar.stringWidth(label)) / 2;
                    int labelY = y - 2;
                    if (labelY < PAD_TOP + 10) labelY = PAD_TOP + 10;
                    g2.setColor(new Color(40, 40, 50));
                    g2.drawString(label, labelX, labelY);
                }
            }

            g2.dispose();
        }
    }
}
