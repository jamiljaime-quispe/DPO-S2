package Presentation.Views;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OccupancyChartView extends JPanel {
    private List<Integer> data = new ArrayList<>();
    private ChartPanel chartPanel;

    public OccupancyChartView() {
    }

    public void initComponents() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Parking Occupancy — Last Hour", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        chartPanel = new ChartPanel();
        add(chartPanel, BorderLayout.CENTER);
    }

    public void updateChart(List<Integer> data) {
        this.data = (data != null) ? data : new ArrayList<>();
        if (chartPanel != null)
            chartPanel.repaint();
    }

    public void startAutoRefresh() {
    }

    public void stopAutoRefresh() {
    }

    // -------------------------------------------------------------------------

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

            // White background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);

            // No data message
            if (data.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                String msg = "No data available";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            // Y scale: round up to nearest 5
            int maxVal = 1;
            for (Integer value : data) {
                if (value > maxVal) {
                    maxVal = value;
                }
            }
            int yScale = Math.max(((maxVal / 5) + 1) * 5, 5);

            // Grid lines + Y-axis labels
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

            // Axes
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);
            g2.drawLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);

            // X-axis direction labels
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(Color.GRAY);
            g2.drawString("← older", PAD_LEFT + 4, PAD_TOP + chartH + 28);
            String newer = "newer →";
            FontMetrics fmX = g2.getFontMetrics();
            g2.drawString(newer, PAD_LEFT + chartW - fmX.stringWidth(newer), PAD_TOP + chartH + 28);

            // Y-axis label
            g2.rotate(-Math.PI / 2);
            g2.setColor(Color.GRAY);
            g2.drawString("Occupied spaces", -(PAD_TOP + chartH / 2 + 40), 12);
            g2.rotate(Math.PI / 2);

            // Compute point coordinates
            int n = data.size();
            int[] xs = new int[n];
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = PAD_LEFT + (n == 1 ? chartW / 2 : chartW * i / (n - 1));
                ys[i] = PAD_TOP + chartH - (int) ((double) data.get(i) / yScale * chartH);
            }

            // Filled area under line (semi-transparent)
            Polygon area = new Polygon();
            area.addPoint(xs[0], PAD_TOP + chartH);
            for (int i = 0; i < n; i++)
                area.addPoint(xs[i], ys[i]);
            area.addPoint(xs[n - 1], PAD_TOP + chartH);
            g2.setColor(new Color(70, 130, 180, 55));
            g2.fillPolygon(area);

            // Line
            g2.setColor(new Color(70, 130, 180));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n - 1; i++) {
                g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
            }

            // Data point dots
            g2.setColor(new Color(30, 90, 150));
            for (int i = 0; i < n; i++) {
                g2.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
            }

            // Current value label on last point
            if (n > 0) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.setColor(new Color(30, 90, 150));
                String val = String.valueOf(data.get(n - 1));
                g2.drawString(val, xs[n - 1] + 6, ys[n - 1] - 6);
            }

            g2.dispose();
        }
    }
}
