package Presentation.Views;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Colors the booking table so own bookings, vacant slots, and occupied slots are easy to distinguish.
 */
class BookingCellRenderer extends DefaultTableCellRenderer {
    private static final int PARKING_STATUS_COLUMN = 3;
    private static final int MY_BOOKING_COLUMN = 7;
    private static final Color VACANT_COLOR = new Color(232, 248, 238);
    private static final Color OCCUPIED_COLOR = new Color(253, 235, 235);
    private static final Color MY_BOOKING_COLOR = new Color(225, 240, 255);

    /**
     * Returns the table cell component with the booking colors applied.
     *
     * @param table      table being painted
     * @param value      value shown in the cell
     * @param isSelected whether the row is selected
     * @param hasFocus   whether the cell has focus
     * @param row        view row
     * @param column     view column
     * @return component used to paint the cell
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {
            applyBookingColors(table, cell, row, column);
        }

        return cell;
    }

    /**
     * Applies row and status colors using the hidden table columns.
     *
     * @param table  table being painted
     * @param cell   cell being painted
     * @param row    view row
     * @param column view column
     */
    private void applyBookingColors(JTable table, Component cell, int row, int column) {
        int modelRow = table.convertRowIndexToModel(row);
        int modelColumn = table.convertColumnIndexToModel(column);
        boolean myBooking = Boolean.TRUE.equals(table.getModel().getValueAt(modelRow, MY_BOOKING_COLUMN));
        String status = String.valueOf(table.getModel().getValueAt(modelRow, PARKING_STATUS_COLUMN));

        if (myBooking) {
            cell.setBackground(MY_BOOKING_COLOR);
        } else {
            cell.setBackground(Color.WHITE);
        }

        if (modelColumn == PARKING_STATUS_COLUMN && "Vacant".equals(status)) {
            cell.setBackground(VACANT_COLOR);
        } else if (modelColumn == PARKING_STATUS_COLUMN && "Occupied".equals(status)) {
            cell.setBackground(OCCUPIED_COLOR);
        }
    }
}
