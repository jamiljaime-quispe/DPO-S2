package Presentation.Views;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Colors the current parking status table according to status and current-user ownership.
 */
class ParkingStatusCellRenderer extends DefaultTableCellRenderer {
    private static final int STATUS_COLUMN = 2;
    private static final int MY_PARKED_COLUMN = 4;
    private static final Color VACANT_COLOR = new Color(232, 248, 238);
    private static final Color OCCUPIED_COLOR = new Color(253, 235, 235);
    private static final Color MY_PARKED_COLOR = new Color(225, 240, 255);

    /**
     * Returns the table cell component with the parking status colors applied.
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
            applyParkingStatusColor(table, cell, row, column);
        }

        return cell;
    }

    /**
     * Applies the correct background for the current row and column.
     *
     * @param table  table being painted
     * @param cell   cell being painted
     * @param row    view row
     * @param column view column
     */
    private void applyParkingStatusColor(JTable table, Component cell, int row, int column) {
        int modelRow = table.convertRowIndexToModel(row);
        int modelColumn = table.convertColumnIndexToModel(column);
        boolean myParkedVehicle = Boolean.TRUE.equals(table.getModel().getValueAt(modelRow, MY_PARKED_COLUMN));
        String status = String.valueOf(table.getModel().getValueAt(modelRow, STATUS_COLUMN));

        if (myParkedVehicle) {
            cell.setBackground(MY_PARKED_COLOR);
        } else if (modelColumn == STATUS_COLUMN && status.startsWith("Vacant")) {
            cell.setBackground(VACANT_COLOR);
        } else if (modelColumn == STATUS_COLUMN && status.startsWith("Occupied")) {
            cell.setBackground(OCCUPIED_COLOR);
        } else {
            cell.setBackground(Color.WHITE);
        }
    }
}
