package Presentation.Views;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Colors the admin parking status column according to whether a space is vacant or occupied.
 * <p>
 * The view builds or updates Swing components and leaves the decisions to controllers and services. This
 * keeps the screen code focused on what the user sees.
 * </p>
 */
class AdminStatusCellRenderer extends DefaultTableCellRenderer {
    private static final Color VACANT_COLOR = new Color(232, 248, 238);
    private static final Color OCCUPIED_COLOR = new Color(253, 235, 235);

    /**
     * Returns the table cell component with the status color applied.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @param table table being painted
     * @param value value shown in the cell
     * @param isSelected whether the row is selected
     * @param hasFocus whether the cell has focus
     * @param row view row
     * @param column view column
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
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param cell cell being painted
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
