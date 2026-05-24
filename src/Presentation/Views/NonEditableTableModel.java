package Presentation.Views;

import javax.swing.table.DefaultTableModel;

/**
 * Table model used when a table should only display data and not allow direct cell editing.
 * <p>
 * The view builds or updates Swing components and leaves the decisions to controllers and services. This
 * keeps the screen code focused on what the user sees.
 * </p>
 */
class NonEditableTableModel extends DefaultTableModel {

    /**
     * Creates an empty read-only table model with the given columns.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param columns table column names
     * @param rowCount number of empty rows to create
     */
    NonEditableTableModel(Object[] columns, int rowCount) {
        super(columns, rowCount);
    }

    /**
     * Creates a read-only table model with the given rows and columns.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param rows table row data
     * @param columns table column names
     */
    NonEditableTableModel(Object[][] rows, Object[] columns) {
        super(rows, columns);
    }

    /**
     * Checks whether cell editable.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param row row being checked
     * @param column column being checked
     * @return false because table cells are not edited directly
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
