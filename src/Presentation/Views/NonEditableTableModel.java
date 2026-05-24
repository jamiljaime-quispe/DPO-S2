package Presentation.Views;

import javax.swing.table.DefaultTableModel;

/**
 * Table model used when a table should only display data and not allow direct cell editing.
 */
class NonEditableTableModel extends DefaultTableModel {

    /**
     * Creates an empty read-only table model with the given columns.
     *
     * @param columns  table column names
     * @param rowCount number of empty rows to create
     */
    NonEditableTableModel(Object[] columns, int rowCount) {
        super(columns, rowCount);
    }

    /**
     * Creates a read-only table model with the given rows and columns.
     *
     * @param rows    table row data
     * @param columns table column names
     */
    NonEditableTableModel(Object[][] rows, Object[] columns) {
        super(rows, columns);
    }

    /**
     * Keeps every cell read-only because changes must go through the controller.
     *
     * @param row    row being checked
     * @param column column being checked
     * @return false because table cells are not edited directly
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
