package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Presentation.Controllers.AdminController;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminParkingManagementView extends JDialog {
    private JTable spacesTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private AdminController controller;
    private boolean loading;

    public AdminParkingManagementView(Frame parent) {
        super(parent, "Manage Parking Slots", true);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(750, 480);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] columns = {"Code", "Floor", "Type", "Status", "Reservation", "License Plate"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        spacesTable = new JTable(tableModel);
        spacesTable.setRowHeight(24);
        spacesTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        spacesTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        spacesTable.getTableHeader().setBackground(new Color(60, 60, 60));
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

        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        spacesTable.getSelectionModel().addListSelectionListener(e -> {
            updateActionButtons();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
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

    private void showAddDialog() {
        JDialog dialog = new JDialog(this, "Add Parking Space", true);
        dialog.setLayout(new GridLayout(4, 2, 10, 10));
        dialog.setSize(320, 180);
        dialog.setLocationRelativeTo(this);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        dialog.add(new JLabel("Code:"));
        JTextField codeField = new JTextField();
        dialog.add(codeField);

        dialog.add(new JLabel("Floor:"));
        JSpinner floorSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
        dialog.add(floorSpinner);

        dialog.add(new JLabel("Vehicle Type:"));
        JComboBox<VehicleType> typeCombo = new JComboBox<>(VehicleType.values());
        dialog.add(typeCombo);

        JButton confirmBtn = new JButton("Create");
        JButton cancelBtn = new JButton("Cancel");
        dialog.add(confirmBtn);
        dialog.add(cancelBtn);

        confirmBtn.addActionListener(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Code cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int floor = (int) floorSpinner.getValue();
            VehicleType type = (VehicleType) typeCombo.getSelectedItem();
            if (controller != null) controller.createSpace(code, floor, type);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditDialog() {
        int row = spacesTable.getSelectedRow();
        if (row < 0) return;

        String code = (String) tableModel.getValueAt(row, 0);
        int currentFloor = (int) tableModel.getValueAt(row, 1);
        VehicleType currentType = VehicleType.valueOf((String) tableModel.getValueAt(row, 2));

        JDialog dialog = new JDialog(this, "Edit Parking Space", true);
        dialog.setLayout(new GridLayout(4, 2, 10, 10));
        dialog.setSize(320, 180);
        dialog.setLocationRelativeTo(this);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        dialog.add(new JLabel("Code:"));
        JTextField codeField = new JTextField(code);
        codeField.setEditable(false);
        dialog.add(codeField);

        dialog.add(new JLabel("Floor:"));
        JSpinner floorSpinner = new JSpinner(new SpinnerNumberModel(currentFloor, 0, 99, 1));
        dialog.add(floorSpinner);

        dialog.add(new JLabel("Vehicle Type:"));
        JComboBox<VehicleType> typeCombo = new JComboBox<>(VehicleType.values());
        typeCombo.setSelectedItem(currentType);
        dialog.add(typeCombo);

        JButton confirmBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        dialog.add(confirmBtn);
        dialog.add(cancelBtn);

        confirmBtn.addActionListener(e -> {
            int floor = (int) floorSpinner.getValue();
            VehicleType type = (VehicleType) typeCombo.getSelectedItem();
            if (controller != null) controller.editSpace(code, floor, type);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void handleDelete() {
        int row = spacesTable.getSelectedRow();
        if (row < 0) return;

        String code = (String) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete parking space \"" + code + "\"? This cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION && controller != null) {
            controller.deleteSpace(code);
        }
    }

    public void updateSpaces(List<ParkingSpace> spaces) {
        clearSpacesTable();
        for (ParkingSpace space : spaces) {
            addSpaceToTable(space);
        }
    }

    public void clearSpacesTable() {
        tableModel.setRowCount(0);
    }

    public void addSpaceToTable(ParkingSpace space) {
        tableModel.addRow(new Object[]{
            space.getId(),
            space.getFloor(),
            space.getVehicleType().name(),
            space.isOccupied() ? "Occupied" : "Vacant",
            space.isReserved() ? "Reserved" : "Available",
            getLicensePlate(space)
        });
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        spacesTable.setEnabled(!loading);
        updateActionButtons();
    }

    public void setController(AdminController controller) {
        this.controller = controller;
    }

    private void updateActionButtons() {
        boolean selected = spacesTable.getSelectedRow() >= 0;
        addButton.setEnabled(!loading);
        editButton.setEnabled(!loading && selected);
        deleteButton.setEnabled(!loading && selected);
        refreshButton.setEnabled(!loading);
    }

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
