package Presentation.Views;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Colors the admin parking status column according to whether a space is vacant or occupied.
 */
class AdminStatusCellRenderer extends DefaultTableCellRenderer {
    private static final Color VACANT_COLOR = new Color(232, 248, 238);
    private static final Color OCCUPIED_COLOR = new Color(253, 235, 235);

    /**
     * Returns the table cell component with the status color applied.
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
            applyStatusColor(cell, value);
        }

        return cell;
    }

    /**
     * Applies the background color that matches the parking status.
     *
     * @param cell  cell being painted
     * @param value status value shown in the cell
     */
    private void applyStatusColor(Component cell, Object value) {
        String status = String.valueOf(value);
        if ("Vacant".equals(status)) {
            cell.setBackground(VACANT_COLOR);
        } else if ("Occupied".equals(status)) {
            cell.setBackground(OCCUPIED_COLOR);
        } else {
            cell.setBackground(Color.WHITE);
        }
    }
}
