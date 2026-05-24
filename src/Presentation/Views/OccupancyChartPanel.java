package Presentation.Views;

import Business.Entities.OccupancyRecord;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that draws the occupancy bars for the last hour.
 */
class OccupancyChartPanel extends JPanel {
    private static final int PAD_LEFT = 50;
    private static final int PAD_RIGHT = 20;
    private static final int PAD_TOP = 30;
    private static final int PAD_BOTTOM = 58;
    private static final int VALUE_LABEL_FONT_SIZE = 6;
    private static final int X_LABEL_FONT_SIZE = 6;

    private List<OccupancyRecord> data = new ArrayList<>();

    /**
     * Stores the data to draw on the next repaint.
     *
     * @param data occupancy records to show
     */
    void setData(List<OccupancyRecord> data) {
        if (data == null) {
            this.data = new ArrayList<>();
            return;
        }
        this.data = data;
    }

    /**
     * Gives the chart a stable preferred size.
     *
     * @return preferred chart size
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 300);
    }

    /**
     * Draws the bar chart.
     *
     * @param g graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = createChartGraphics(g);
        int w = getWidth();
        int h = getHeight();
        int chartW = w - PAD_LEFT - PAD_RIGHT;
        int chartH = h - PAD_TOP - PAD_BOTTOM;

        paintChartBackground(g2, w, h);

        if (data.isEmpty()) {
            drawNoDataMessage(g2, w, h);
            g2.dispose();
            return;
        }

        int n = data.size();
        int maxVal = findMaximumOccupancy();
        int yScale = Math.max(((maxVal / 5) + 1) * 5, 5);
        double slotW = calculateSlotWidth(chartW, n);
        double gap = calculateGap(slotW);

        drawYAxisGridAndLabels(g2, chartH, chartW, yScale);
        drawAxes(g2, chartH, chartW);
        drawYAxisTitle(g2, chartH);
        drawBars(g2, n, yScale, chartH, chartW, slotW, gap);
        drawBarValues(g2, n, yScale, chartH, chartW, slotW, gap);
        drawXAxisLabels(g2, n, chartH, slotW);
        drawXAxisTitle(g2, chartH, chartW);
        g2.dispose();
    }

    /**
     * Creates a graphics copy with the chart rendering settings.
     *
     * @param g original graphics context
     * @return prepared graphics context
     */
    private Graphics2D createChartGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /**
     * Paints the chart background.
     *
     * @param g2     graphics context
     * @param width  component width
     * @param height component height
     */
    private void paintChartBackground(Graphics2D g2, int width, int height) {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
    }

    /**
     * Draws the empty-state message when there are no occupancy records.
     *
     * @param g2     graphics context
     * @param width  component width
     * @param height component height
     */
    private void drawNoDataMessage(Graphics2D g2, int width, int height) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String message = "No data available";
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(message, (width - metrics.stringWidth(message)) / 2, height / 2);
    }

    /**
     * Draws Y-axis grid lines and their numeric labels.
     *
     * @param g2     graphics context
     * @param chartH chart height
     * @param chartW chart width
     * @param yScale highest value shown on the axis
     */
    private void drawYAxisGridAndLabels(Graphics2D g2, int chartH, int chartW, int yScale) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fmY = g2.getFontMetrics();
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            int yVal = yScale * i / gridLines;
            int y = PAD_TOP + chartH - (chartH * i / gridLines);

            g2.setColor(new Color(220, 220, 220));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);

            g2.setColor(Color.DARK_GRAY);
            String label = String.valueOf(yVal);
            g2.drawString(label, PAD_LEFT - fmY.stringWidth(label) - 5, y + fmY.getAscent() / 2);
        }
    }

    /**
     * Draws the horizontal and vertical chart axes.
     *
     * @param g2     graphics context
     * @param chartH chart height
     * @param chartW chart width
     */
    private void drawAxes(Graphics2D g2, int chartH, int chartW) {
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);
        g2.drawLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);
    }

    /**
     * Draws the Y-axis title.
     *
     * @param g2     graphics context
     * @param chartH chart height
     */
    private void drawYAxisTitle(Graphics2D g2, int chartH) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(Color.GRAY);
        g2.rotate(-Math.PI / 2);
        String yTitle = "Number of vehicles";
        FontMetrics fmYT = g2.getFontMetrics();
        g2.drawString(yTitle, -(PAD_TOP + chartH / 2 + fmYT.stringWidth(yTitle) / 2), 13);
        g2.rotate(Math.PI / 2);
    }

    /**
     * Draws the occupancy bars.
     *
     * @param g2     graphics context
     * @param n      number of records
     * @param yScale highest value shown on the axis
     * @param chartH chart height
     * @param chartW chart width
     * @param slotW  width reserved for one bar
     * @param gap    gap between bars
     */
    private void drawBars(Graphics2D g2, int n, int yScale, int chartH, int chartW, double slotW, double gap) {
        for (int i = 0; i < n; i++) {
            int value = data.get(i).getOccupiedCount();
            int barH = (int) ((double) value / yScale * chartH);
            int x = calculateBarX(i, slotW, gap);
            int barW = calculateBarWidth(i, n, chartW, slotW, gap, x);
            int y = PAD_TOP + chartH - barH;
            boolean isLatest = (i == n - 1);

            g2.setColor(isLatest ? new Color(70, 130, 200) : new Color(70, 130, 200, 180));
            g2.fillRect(x, y, barW, barH);
        }
    }

    /**
     * Draws the X-axis labels from 59 to 0 minutes ago.
     *
     * @param g2     graphics context
     * @param n      number of records
     * @param chartH chart height
     * @param slotW  width reserved for one bar
     */
    private void drawXAxisLabels(Graphics2D g2, int n, int chartH, double slotW) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, X_LABEL_FONT_SIZE));
        g2.setColor(Color.DARK_GRAY);

        for (int i = 0; i < n; i++) {
            String label = formatMinuteOffset(i, n);
            int x = calculateSlotCenter(i, slotW);
            drawVerticalXAxisLabel(g2, label, x, PAD_TOP + chartH + 4);
        }
    }

    /**
     * Draws the X-axis title.
     *
     * @param g2     graphics context
     * @param chartH chart height
     * @param chartW chart width
     */
    private void drawXAxisTitle(Graphics2D g2, int chartH, int chartW) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fmX = g2.getFontMetrics();
        String xTitle = "Minutes ago (59 to 0)";
        g2.drawString(xTitle, PAD_LEFT + (chartW - fmX.stringWidth(xTitle)) / 2,
                PAD_TOP + chartH + 50);
    }

    /**
     * Calculates a proportional slot width so all minute bars fill the chart width.
     *
     * @param chartWidth drawable chart width
     * @param count      number of bars
     * @return width reserved for each bar
     */
    private double calculateSlotWidth(int chartWidth, int count) {
        return (double) chartWidth / Math.max(count, 1);
    }

    /**
     * Calculates the visual gap between bars.
     *
     * @param slotWidth width reserved for each bar
     * @return gap between bars
     */
    private double calculateGap(double slotWidth) {
        return Math.max(1.0, Math.min(4.0, slotWidth * 0.12));
    }

    /**
     * Calculates the left edge of a bar.
     *
     * @param index     bar index
     * @param slotWidth width reserved for each bar
     * @param gap       gap between bars
     * @return x coordinate
     */
    private int calculateBarX(int index, double slotWidth, double gap) {
        if (index == 0) {
            return PAD_LEFT;
        }
        return (int) Math.round(PAD_LEFT + index * slotWidth + gap / 2.0);
    }

    /**
     * Calculates a bar width without leaving unused pixels at the right side.
     *
     * @param index      bar index
     * @param count      number of bars
     * @param chartWidth drawable chart width
     * @param slotWidth  width reserved for each bar
     * @param gap        gap between bars
     * @param x          bar x coordinate
     * @return bar width
     */
    private int calculateBarWidth(int index, int count, int chartWidth, double slotWidth, double gap, int x) {
        int rightEdge;
        if (index == count - 1) {
            rightEdge = PAD_LEFT + chartWidth;
        } else {
            rightEdge = (int) Math.round(PAD_LEFT + (index + 1) * slotWidth - gap / 2.0);
        }
        return Math.max(1, rightEdge - x);
    }

    /**
     * Calculates the center of one minute slot.
     *
     * @param index     slot index
     * @param slotWidth width reserved for each bar
     * @return x coordinate of the slot center
     */
    private int calculateSlotCenter(int index, double slotWidth) {
        return (int) Math.round(PAD_LEFT + index * slotWidth + slotWidth / 2.0);
    }

    /**
     * Draws one rotated X-axis label so all 60 minute labels can fit.
     *
     * @param g2    graphics context
     * @param label label to draw
     * @param x     label x coordinate
     * @param y     label y coordinate
     */
    private void drawVerticalXAxisLabel(Graphics2D g2, String label, int x, int y) {
        Graphics2D labelGraphics = (Graphics2D) g2.create();
        FontMetrics fm = labelGraphics.getFontMetrics();
        labelGraphics.translate(x - fm.getAscent() / 2, y);
        labelGraphics.rotate(Math.PI / 2);
        labelGraphics.drawString(label, 0, 0);
        labelGraphics.dispose();
    }

    /**
     * Finds the largest occupancy value shown in the chart.
     *
     * @return largest occupancy count
     */
    private int findMaximumOccupancy() {
        int maxVal = 1;
        for (OccupancyRecord record : data) {
            if (record.getOccupiedCount() > maxVal) {
                maxVal = record.getOccupiedCount();
            }
        }
        return maxVal;
    }

    /**
     * Draws numeric occupancy values above the bars.
     *
     * @param g2     graphics context
     * @param n      number of records
     * @param yScale highest value shown on the axis
     * @param chartH chart height
     * @param chartW chart width
     * @param slotW  width reserved for one bar
     * @param gap    gap between bars
     */
    private void drawBarValues(Graphics2D g2, int n, int yScale, int chartH, int chartW, double slotW,
                               double gap) {
        g2.setFont(new Font("SansSerif", Font.BOLD, VALUE_LABEL_FONT_SIZE));

        for (int i = 0; i < n; i++) {
            int value = data.get(i).getOccupiedCount();
            int barH = (int) ((double) value / yScale * chartH);
            int x = calculateBarX(i, slotW, gap);
            int barW = calculateBarWidth(i, n, chartW, slotW, gap, x);
            int y = PAD_TOP + chartH - barH;
            String label = String.valueOf(value);
            drawVerticalBarValue(g2, label, x + barW / 2, y);
        }
    }

    /**
     * Draws one rotated value label above a bar so all 60 values can fit.
     *
     * @param g2     graphics context
     * @param label  value label
     * @param x      label x coordinate
     * @param barTop top of the bar
     */
    private void drawVerticalBarValue(Graphics2D g2, String label, int x, int barTop) {
        Graphics2D labelGraphics = (Graphics2D) g2.create();
        FontMetrics fm = labelGraphics.getFontMetrics();
        int labelY = Math.max(PAD_TOP + fm.stringWidth(label) + 1, barTop - 3);
        labelGraphics.setColor(new Color(45, 45, 55));
        labelGraphics.translate(x + fm.getAscent() / 3, labelY);
        labelGraphics.rotate(-Math.PI / 2);
        labelGraphics.drawString(label, 0, 0);
        labelGraphics.dispose();
    }

    /**
     * Formats one X-axis label as minutes ago.
     *
     * @param index current index
     * @param total total records shown
     * @return minute offset label
     */
    private String formatMinuteOffset(int index, int total) {
        return String.valueOf(total - 1 - index);
    }
}
