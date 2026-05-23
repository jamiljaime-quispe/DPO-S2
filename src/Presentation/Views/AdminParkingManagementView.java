package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Presentation.Controllers.AdminController;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modal dialog for admin parking space management.
 * Lists all parking spaces in a table and provides Add, Edit, and Delete controls.
 */
public class AdminParkingManagementView extends JDialog {
    private JTable spacesTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private AdminController controller;
    private boolean loading;
    private JDialog activeDeleteDialog;
    private String activeDeleteSpaceCode;
    private JDialog activeEditDialog;
    private String activeEditSpaceCode;
    private boolean activeEditAllowsTypeChange;

    /** Creates the admin parking management dialog. */
    public AdminParkingManagementView(Frame parent) {
        super(parent, "Manage Parking Slots", true);
        initComponents();
    }

    /** Builds the dialog components. */
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 247, 250));
        setSize(750, 480);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] columns = {"Code", "Floor", "Type", "Status", "Reservation", "License Plate"};
        tableModel = new DefaultTableModel(columns, 0) {
            /** Keeps table cells read-only. */
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        spacesTable = new JTable(tableModel);
        spacesTable.setRowHeight(24);
        spacesTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        spacesTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        spacesTable.getTableHeader().setBackground(new Color(33, 99, 168));
        spacesTable.getTableHeader().setForeground(Color.WHITE);
        spacesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spacesTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(spacesTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);

        addButton = new JButton("Add Space");
        editButton = new JButton("Edit Space");
        deleteButton = new JButton("Delete Space");
        refreshButton = new JButton("Refresh");
        stylePrimaryButton(addButton);
        stylePrimaryButton(editButton);
        stylePrimaryButton(refreshButton);
        deleteButton.setForeground(new Color(180, 30, 30));

        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        spacesTable.getSelectionModel().addListSelectionListener(e -> {
            updateActionButtons();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> handleDelete());
        refreshButton.addActionListener(e -> { if (controller != null) controller.loadSpaces(); });
    }

    /** Applies the main button style. */
    private void stylePrimaryButton(JButton b) {
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(33, 99, 168));
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Opens the dialog used to add a parking space. */
    private void showAddDialog() {
        JDialog dialog = new JDialog(this, "Add Parking Space", true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(320, 160);
        dialog.setLocationRelativeTo(this);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        dialog.add(new JLabel("Floor:"));
        JSpinner floorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        dialog.add(floorSpinner);

        dialog.add(new JLabel("Vehicle Type:"));
        JComboBox<VehicleType> typeCombo = new JComboBox<>(VehicleType.values());
        dialog.add(typeCombo);

        JButton confirmBtn = new JButton("Create");
        JButton cancelBtn = new JButton("Cancel");
        stylePrimaryButton(confirmBtn);
        dialog.add(confirmBtn);
        dialog.add(cancelBtn);

        confirmBtn.addActionListener(e -> {
            int floor = (int) floorSpinner.getValue();
            VehicleType type = (VehicleType) typeCombo.getSelectedItem();
            if (controller != null) controller.createSpace(floor, type);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    /** Opens the dialog used to edit the selected parking space. */
    private void showEditDialog() {
        int row = getSelectedModelRow();
        if (row < 0) return;

        String code = (String) tableModel.getValueAt(row, 0);
        int currentFloor = (int) tableModel.getValueAt(row, 1);
        VehicleType currentType = VehicleType.valueOf((String) tableModel.getValueAt(row, 2));
        String currentStatus = String.valueOf(tableModel.getValueAt(row, 3));
        String currentReservation = String.valueOf(tableModel.getValueAt(row, 4));
        boolean typeEditable = "Vacant".equals(currentStatus) && "Available".equals(currentReservation);

        JDialog dialog = new JDialog(this, "Edit Parking Space", true);
        activeEditDialog = dialog;
        activeEditSpaceCode = code;
        activeEditAllowsTypeChange = typeEditable;

        dialog.setLayout(new GridLayout(4, 2, 10, 10));
        dialog.setSize(320, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        dialog.add(new JLabel("Code:"));
        JTextField codeField = new JTextField(code);
        codeField.setEditable(false);
        dialog.add(codeField);

        dialog.add(new JLabel("Floor:"));
        JSpinner floorSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, currentFloor), 1, 99, 1));
        dialog.add(floorSpinner);

        dialog.add(new JLabel("Vehicle Type:"));
        JComboBox<VehicleType> typeCombo = new JComboBox<>(VehicleType.values());
        typeCombo.setSelectedItem(currentType);
        typeCombo.setEnabled(typeEditable);
        dialog.add(typeCombo);

        JButton confirmBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        stylePrimaryButton(confirmBtn);
        dialog.add(confirmBtn);
        dialog.add(cancelBtn);

        confirmBtn.addActionListener(e -> {
            int floor = (int) floorSpinner.getValue();
            VehicleType selectedType = (VehicleType) typeCombo.getSelectedItem();
            clearActiveEditDialog();
            if (controller != null) controller.editSpace(code, floor, selectedType);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> {
            clearActiveEditDialog();
            dialog.dispose();
        });

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            /** Clears the tracked edit dialog when it closes. */
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (activeEditDialog == dialog) {
                    clearActiveEditDialog();
                }
            }
        });

        dialog.setVisible(true);
    }

    /** Clears the tracked edit dialog state. */
    private void clearActiveEditDialog() {
        activeEditDialog = null;
        activeEditSpaceCode = null;
        activeEditAllowsTypeChange = false;
    }

    /** Starts the delete flow for the selected parking space. */
    private void handleDelete() {
        int row = getSelectedModelRow();
        if (row < 0) return;

        String code = (String) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 3);
        String reservation = (String) tableModel.getValueAt(row, 4);

        if ("Occupied".equals(status)) {
            showError("Cannot delete parking space \"" + code + "\" because a vehicle is currently parked there.");
            return;
        }

        String message = "Delete parking space \"" + code + "\"? This cannot be undone.";
        if ("Reserved".equals(reservation)) {
            message = message
                    + "\n\nThis space has an active reservation. The system will try to move it to a similar vacant space."
                    + "\nIf no similar vacant space exists, the reservation will be cancelled and the user will be notified.";
        }

        showDeleteConfirmationDialog(code, message);
    }

    /** Opens the delete confirmation dialog for a parking space. */
    private void showDeleteConfirmationDialog(String code, String message) {
        JDialog dialog = new JDialog(this, "Confirm Delete", true);
        activeDeleteDialog = dialog;
        activeDeleteSpaceCode = code;

        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setColumns(42);
        messageArea.setRows(5);
        messageArea.setFont(new Font("SansSerif", Font.BOLD, 14));
        dialog.add(messageArea, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");
        stylePrimaryButton(yesButton);
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        yesButton.addActionListener(e -> {
            clearActiveDeleteDialog();
            dialog.dispose();
            if (controller != null) {
                controller.deleteSpace(code);
            }
        });
        noButton.addActionListener(e -> {
            clearActiveDeleteDialog();
            dialog.dispose();
        });

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            /** Clears the tracked delete dialog when it closes. */
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (activeDeleteDialog == dialog) {
                    clearActiveDeleteDialog();
                }
            }
        });

        dialog.pack();
        dialog.setSize(new Dimension(Math.max(dialog.getWidth(), 540), dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** Clears the tracked delete dialog state. */
    private void clearActiveDeleteDialog() {
        activeDeleteDialog = null;
        activeDeleteSpaceCode = null;
    }

    /** Replaces the table with the given parking spaces. */
    public void updateSpaces(List<ParkingSpace> spaces) {
        clearSpacesTable();
        for (ParkingSpace space : spaces) {
            addSpaceToTable(space);
        }
    }

    /** Clears the parking-space table. */
    public void clearSpacesTable() {
        tableModel.setRowCount(0);
    }

    /** Adds or updates one parking-space row. */
    public void addSpaceToTable(ParkingSpace space) {
        Object[] rowData = buildSpaceRow(space);
        int existingRow = findSpaceRow(space.getId());

        if (existingRow == -1) {
            tableModel.addRow(rowData);
            updateActionButtons();
            return;
        }

        for (int column = 0; column < rowData.length; column++) {
            Object currentValue = tableModel.getValueAt(existingRow, column);
            Object newValue = rowData[column];
            if (currentValue == null && newValue != null) {
                tableModel.setValueAt(newValue, existingRow, column);
            } else if (currentValue != null && !currentValue.equals(newValue)) {
                tableModel.setValueAt(newValue, existingRow, column);
            }
        }

        updateActionButtons();
    }

    /** Builds a table row for a parking space. */
    private Object[] buildSpaceRow(ParkingSpace space) {
        return new Object[]{
            space.getId(),
            space.getFloor(),
            space.getVehicleType().name(),
            space.isOccupied() ? "Occupied" : "Vacant",
            space.isReserved() ? "Reserved" : "Available",
            getLicensePlate(space)
        };
    }

    /** Finds the table row for a parking space code. */
    private int findSpaceRow(String code) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String currentCode = String.valueOf(tableModel.getValueAt(row, 0));
            if (currentCode.equals(code)) {
                return row;
            }
        }

        return -1;
    }

    /** Removes rows for spaces that are no longer present. */
    public void removeSpacesNotIn(java.util.Set<String> visibleCodes) {
        for (int row = tableModel.getRowCount() - 1; row >= 0; row--) {
            String currentCode = String.valueOf(tableModel.getValueAt(row, 0));
            if (!visibleCodes.contains(currentCode)) {
                tableModel.removeRow(row);
            }
        }

        updateActionButtons();
    }

    /** Closes the delete confirmation if the selected space can no longer be deleted. */
    public void closeActiveDeleteDialogIfTargetUnavailable() {
        if (activeDeleteDialog == null
                || !activeDeleteDialog.isVisible()
                || activeDeleteSpaceCode == null) {
            return;
        }

        int row = findSpaceRow(activeDeleteSpaceCode);
        String message = null;

        if (row == -1) {
            message = "Parking space \"" + activeDeleteSpaceCode
                    + "\" no longer exists. The delete confirmation was closed.";
        } else {
            String status = String.valueOf(tableModel.getValueAt(row, 3));
            if ("Occupied".equals(status)) {
                message = "Parking space \"" + activeDeleteSpaceCode
                        + "\" is now occupied, so it cannot be deleted.";
            }
        }

        if (message != null) {
            JDialog dialog = activeDeleteDialog;
            clearActiveDeleteDialog();
            dialog.dispose();
            showError(message);
        }
    }

    /** Closes the edit dialog if the selected space changes state. */
    public void closeActiveEditDialogIfTargetUnavailable() {
        if (activeEditDialog == null
                || !activeEditDialog.isVisible()
                || activeEditSpaceCode == null) {
            return;
        }

        int row = findSpaceRow(activeEditSpaceCode);
        String message = null;

        if (row == -1) {
            message = "Parking space \"" + activeEditSpaceCode
                    + "\" no longer exists. The edit dialog was closed.";
        } else if (activeEditAllowsTypeChange) {
            String status = String.valueOf(tableModel.getValueAt(row, 3));
            String reservation = String.valueOf(tableModel.getValueAt(row, 4));

            if ("Occupied".equals(status)) {
                message = "Parking space \"" + activeEditSpaceCode
                        + "\" is now occupied, so its vehicle type cannot be edited.";
            } else if ("Reserved".equals(reservation)) {
                message = "Parking space \"" + activeEditSpaceCode
                        + "\" is now reserved, so its vehicle type cannot be edited.";
            }
        }

        if (message != null) {
            JDialog dialog = activeEditDialog;
            clearActiveEditDialog();
            dialog.dispose();
            showError(message);
        }
    }

    /** Shows an error message owned by this dialog. */
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Shows an information message owned by this dialog. */
    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Enables or disables controls while work is running. */
    public void setLoading(boolean loading) {
        this.loading = loading;
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        spacesTable.setEnabled(!loading);
        updateActionButtons();
    }

    /** Sets the controller used by this dialog. */
    public void setController(AdminController controller) {
        this.controller = controller;
    }

    /** Enables or disables buttons based on the current selection. */
    private void updateActionButtons() {
        boolean selected = spacesTable.getSelectedRow() >= 0;
        boolean occupied = isSelectedSpaceOccupied();
        addButton.setEnabled(!loading);
        editButton.setEnabled(!loading && selected);
        deleteButton.setEnabled(!loading && selected && !occupied);
        refreshButton.setEnabled(!loading);
    }

    /** Checks whether the selected table row is occupied. */
    private boolean isSelectedSpaceOccupied() {
        int row = getSelectedModelRow();
        if (row < 0) return false;

        String status = String.valueOf(tableModel.getValueAt(row, 3));
        return "Occupied".equals(status);
    }

    /** Gets the selected row index in the table model. */
    private int getSelectedModelRow() {
        int row = spacesTable.getSelectedRow();
        if (row < 0) return -1;
        return spacesTable.convertRowIndexToModel(row);
    }

    /** Gets the plate displayed for a parking space. */
    private String getLicensePlate(ParkingSpace space) {
        if (space.getParkedVehicle() != null) {
            return space.getParkedVehicle().getLicensePlate();
        } else if (space.getReservation() != null && space.getReservation().getVehicle() != null) {
            return space.getReservation().getVehicle().getLicensePlate();
        }
        return "";
    }

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        private static final Color VACANT_COLOR = new Color(232, 248, 238);
        private static final Color OCCUPIED_COLOR = new Color(253, 235, 235);

        /** Colors the status column according to vacancy. */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String status = String.valueOf(value);
                if ("Vacant".equals(status)) {
                    cell.setBackground(VACANT_COLOR);
                } else if ("Occupied".equals(status)) {
                    cell.setBackground(OCCUPIED_COLOR);
                } else {
                    cell.setBackground(Color.WHITE);
                }
            }

            return cell;
        }
    }
}
