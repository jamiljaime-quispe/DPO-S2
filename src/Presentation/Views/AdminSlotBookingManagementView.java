package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Presentation.Controllers.AdminSlotBookingController;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminSlotBookingManagementView extends JDialog {
    private static final int ADMIN_MODE = 1;
    private static final int USER_MODE = 2;

    private JTable bookingsTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private AdminSlotBookingController controller;
    private int currentMode = ADMIN_MODE;
    private boolean loading;

    public AdminSlotBookingManagementView(Frame parent) {
        super(parent, "Manage Slot Bookings", true);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(760, 480);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] columns = {"Code", "Floor", "Type", "Parking Status", "Booking", "Booked Plate"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bookingsTable = new JTable(tableModel);
        bookingsTable.setRowHeight(24);
        bookingsTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        bookingsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        bookingsTable.getTableHeader().setBackground(new Color(60, 60, 60));
        bookingsTable.getTableHeader().setForeground(Color.WHITE);
        bookingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingsTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(bookingsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);

        addButton = new JButton("Add Booking");
        editButton = new JButton("Edit Booking");
        deleteButton = new JButton("Delete Booking");
        refreshButton = new JButton("Refresh");

        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        bookingsTable.getSelectionModel().addListSelectionListener(e -> {
            updateActionButtons();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> showAddBookingDialog());
        editButton.addActionListener(e -> showEditBookingDialog());
        deleteButton.addActionListener(e -> showDeleteConfirmation());
        refreshButton.addActionListener(e -> {
            if (controller != null) controller.loadBookings();
        });
    }

    private void showAddBookingDialog() {
        if (currentMode != USER_MODE) return;

        int row = bookingsTable.getSelectedRow();
        if (row < 0) return;

        int modelRow = bookingsTable.convertRowIndexToModel(row);
        String currentCode = String.valueOf(tableModel.getValueAt(modelRow, 0));
        String currentType = String.valueOf(tableModel.getValueAt(modelRow, 2));

        JDialog dialog = createBookingDialog(currentMode == USER_MODE ? "Book Selected Slot" : "Add Slot Booking");

        JTextField plateField = new JTextField();
        JTextField spaceCodeField = new JTextField(currentCode);
        spaceCodeField.setEditable(false);

        addBookingFields(dialog, plateField, spaceCodeField);

        JButton confirmBtn = new JButton("Create");
        JButton cancelBtn = new JButton("Cancel");
        dialog.add(confirmBtn);
        dialog.add(cancelBtn);

        confirmBtn.addActionListener(e -> {
            String plate = plateField.getText().trim();
            String spaceCode = spaceCodeField.getText().trim();
            VehicleType type = VehicleType.valueOf(currentType);

            if (plate.isEmpty() || spaceCode.isEmpty()) {
                showError("License plate and space code cannot be empty.");
                return;
            }

            if (controller != null) controller.createBooking(plate, type, spaceCode);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showEditBookingDialog() {
        if (currentMode != ADMIN_MODE) return;

        int row = bookingsTable.getSelectedRow();
        if (row < 0) return;

        int modelRow = bookingsTable.convertRowIndexToModel(row);
        String currentCode = String.valueOf(tableModel.getValueAt(modelRow, 0));
        String currentType = String.valueOf(tableModel.getValueAt(modelRow, 2));
        String currentPlate = String.valueOf(tableModel.getValueAt(modelRow, 5));

        JDialog dialog = createBookingDialog("Edit Slot Booking");

        JTextField plateField = new JTextField(currentPlate);
        JTextField spaceCodeField = new JTextField(currentCode);

        addBookingFields(dialog, plateField, spaceCodeField);

        JButton confirmBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        dialog.add(confirmBtn);
        dialog.add(cancelBtn);

        confirmBtn.addActionListener(e -> {
            String plate = plateField.getText().trim();
            String spaceCode = spaceCodeField.getText().trim();
            VehicleType type = VehicleType.valueOf(currentType);

            if (plate.isEmpty() || spaceCode.isEmpty()) {
                showError("License plate and space code cannot be empty.");
                return;
            }

            if (controller != null) controller.editBooking(currentCode, plate, type, spaceCode);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void showDeleteConfirmation() {
        int row = bookingsTable.getSelectedRow();
        if (row < 0) return;

        int modelRow = bookingsTable.convertRowIndexToModel(row);
        String spaceCode = String.valueOf(tableModel.getValueAt(modelRow, 0));
        String plate = String.valueOf(tableModel.getValueAt(modelRow, 5));

        String action = currentMode == USER_MODE ? "Cancel" : "Delete";
        int confirm = JOptionPane.showConfirmDialog(this,
                action + " the booking for space \"" + spaceCode + "\"?",
                "Confirm " + action,
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION && controller != null) {
            controller.deleteBooking(spaceCode, plate);
        }
    }

    private JDialog createBookingDialog(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(340, 150);
        dialog.setLocationRelativeTo(this);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return dialog;
    }

    private void addBookingFields(JDialog dialog, JTextField plateField, JTextField spaceCodeField) {
        dialog.add(new JLabel("License Plate:"));
        dialog.add(plateField);

        dialog.add(new JLabel("Space Code:"));
        dialog.add(spaceCodeField);
    }

    public void updateBookings(List<ParkingSpace> spaces) {
        clearBookingsTable();
        for (ParkingSpace space : spaces) {
            addBookingToTable(space);
        }
    }

    public void clearBookingsTable() {
        tableModel.setRowCount(0);
    }

    public void addBookingToTable(ParkingSpace space) {
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

    public void setMode(int mode) {
        currentMode = mode;

        if (currentMode == USER_MODE) {
            setTitle("Manage My Slot Bookings");
            addButton.setText("Book Slot");
            addButton.setVisible(true);
            editButton.setVisible(false);
            deleteButton.setVisible(false);
        } else {
            setTitle("Manage Slot Bookings");
            addButton.setVisible(false);
            editButton.setVisible(true);
            editButton.setText("Reassign Booking");
            deleteButton.setVisible(true);
            deleteButton.setText("Cancel Booking");
        }

        updateActionButtons();
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        bookingsTable.setEnabled(!loading);
        updateActionButtons();
    }

    public void setController(AdminSlotBookingController controller) {
        this.controller = controller;
    }

    private void updateActionButtons() {
        int row = bookingsTable.getSelectedRow();
        boolean selected = row >= 0;
        boolean availableSlot = selected && isSelectedSlotAvailableForBooking(row);
        boolean reservedSlot = selected && isSelectedSlotReserved(row);
        boolean admin = currentMode == ADMIN_MODE;
        boolean user = currentMode == USER_MODE;

        addButton.setEnabled(!loading && user && availableSlot);
        editButton.setEnabled(!loading && admin && reservedSlot);
        deleteButton.setEnabled(!loading && admin && reservedSlot);
        refreshButton.setEnabled(!loading);
    }

    private boolean isSelectedSlotAvailableForBooking(int selectedRow) {
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        String status = String.valueOf(tableModel.getValueAt(modelRow, 3));
        String reservation = String.valueOf(tableModel.getValueAt(modelRow, 4));
        return "Vacant".equals(status) && "Available".equals(reservation);
    }

    private boolean isSelectedSlotReserved(int selectedRow) {
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        String reservation = String.valueOf(tableModel.getValueAt(modelRow, 4));
        return "Reserved".equals(reservation);
    }

    private String getLicensePlate(ParkingSpace space) {
        if (space.getReservation() != null && space.getReservation().getVehicle() != null) {
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
