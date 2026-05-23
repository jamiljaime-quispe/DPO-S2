package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;
import Presentation.Controllers.AdminSlotBookingController;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminSlotBookingManagementView extends JDialog {
    private static final int ADMIN_MODE = 1;
    private static final int USER_MODE = 2;
    private static final int MY_BOOKING_COLUMN = 7;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private JTable bookingsTable;
    private DefaultTableModel tableModel;
    private JTabbedPane tabbedPane;
    private JTable reservationsTable;
    private DefaultTableModel reservationsTableModel;
    private JPanel reservationsPanel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;
    private AdminSlotBookingController controller;
    private int currentMode = ADMIN_MODE;
    private boolean loading;
    private JDialog activeBookingDialog;
    private JTextField activeBookingSpaceCodeField;
    private int activeBookingDialogMode;
    private JDialog activeCancelBookingDialog;
    private String activeCancelBookingSpaceCode;
    private String activeCancelBookingPlate;
    private int activeCancelBookingMode;

    public AdminSlotBookingManagementView(Frame parent) {
        super(parent, "Manage Slot Bookings", true);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 247, 250));
        setSize(900, 500);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] columns = {"Code", "Floor", "Type", "Parking Status", "Booking", "Booked Plate", "Booked At", "My Booking"};
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
        bookingsTable.getTableHeader().setBackground(new Color(33, 99, 168));
        bookingsTable.getTableHeader().setForeground(Color.WHITE);
        bookingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        bookingsTable.removeColumn(bookingsTable.getColumnModel().getColumn(MY_BOOKING_COLUMN));
        BookingCellRenderer bookingCellRenderer = new BookingCellRenderer();
        for (int i = 0; i < bookingsTable.getColumnModel().getColumnCount(); i++) {
            bookingsTable.getColumnModel().getColumn(i).setCellRenderer(bookingCellRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(bookingsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        String[] reservationColumns = {"Space Code", "Floor", "Type", "License Plate", "Booked At", "Status"};
        reservationsTableModel = new DefaultTableModel(reservationColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reservationsTable = new JTable(reservationsTableModel);
        reservationsTable.setRowHeight(24);
        reservationsTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        reservationsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        reservationsTable.getTableHeader().setBackground(new Color(33, 99, 168));
        reservationsTable.getTableHeader().setForeground(Color.WHITE);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane reservationsScrollPane = new JScrollPane(reservationsTable);
        reservationsScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        reservationsPanel = new JPanel(new BorderLayout());
        reservationsPanel.setOpaque(false);
        reservationsPanel.add(reservationsScrollPane, BorderLayout.CENTER);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Slot bookings", scrollPane);
        tabbedPane.addChangeListener(e -> updateActionButtons());
        add(tabbedPane, BorderLayout.CENTER);

        addButton = new JButton("Add Booking");
        editButton = new JButton("Edit Booking");
        deleteButton = new JButton("Delete Booking");
        refreshButton = new JButton("Refresh");
        stylePrimaryButton(addButton);
        stylePrimaryButton(editButton);
        stylePrimaryButton(refreshButton);
        deleteButton.setForeground(new Color(180, 30, 30));

        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        bookingsTable.getSelectionModel().addListSelectionListener(e -> {
            updateActionButtons();
        });
        reservationsTable.getSelectionModel().addListSelectionListener(e -> {
            updateActionButtons();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setOpaque(false);
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
        String currentStatus = String.valueOf(tableModel.getValueAt(modelRow, 3));
        String currentReservation = String.valueOf(tableModel.getValueAt(modelRow, 4));

        if ("Occupied".equals(currentStatus)) {
            showError("Space \"" + currentCode + "\" cannot be booked because a vehicle is parked there.");
            return;
        }
        if ("Reserved".equals(currentReservation)) {
            showError("Space \"" + currentCode + "\" is already booked.");
            return;
        }

        JDialog dialog = createBookingDialog("Book Selected Slot");

        JTextField plateField = new JTextField();
        JTextField spaceCodeField = new JTextField(currentCode);
        spaceCodeField.setEditable(false);

        addBookingFields(dialog, plateField, spaceCodeField);
        trackActiveBookingDialog(dialog, spaceCodeField);

        JButton confirmBtn = new JButton("Create");
        JButton cancelBtn = new JButton("Cancel");
        stylePrimaryButton(confirmBtn);
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
        trackActiveBookingDialog(dialog, spaceCodeField);

        JButton confirmBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        stylePrimaryButton(confirmBtn);
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

    private void stylePrimaryButton(JButton b) {
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(33, 99, 168));
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void showDeleteConfirmation() {
        String spaceCode;
        String plate;
        boolean reservationsTab = currentMode == USER_MODE && tabbedPane != null && tabbedPane.getSelectedIndex() == 1;

        if (reservationsTab) {
            int row = reservationsTable.getSelectedRow();
            if (row < 0) return;

            int modelRow = reservationsTable.convertRowIndexToModel(row);
            spaceCode = String.valueOf(reservationsTableModel.getValueAt(modelRow, 0));
            plate = String.valueOf(reservationsTableModel.getValueAt(modelRow, 3));
        } else {
            int row = bookingsTable.getSelectedRow();
            if (row < 0) return;

            int modelRow = bookingsTable.convertRowIndexToModel(row);
            spaceCode = String.valueOf(tableModel.getValueAt(modelRow, 0));
            plate = String.valueOf(tableModel.getValueAt(modelRow, 5));
        }

        if (currentMode == USER_MODE) {
            showUserCancelBookingDialog(spaceCode, plate);
        } else {
            showAdminCancelBookingDialog(spaceCode, plate);
        }
    }

    private void showUserCancelBookingDialog(String spaceCode, String plate) {
        JDialog dialog = createCancelDialog("Confirm Cancellation");
        trackActiveCancelBookingDialog(dialog, spaceCode, plate);

        JTextField plateField = new JTextField();
        dialog.add(new JLabel("<html>Enter your license plate to confirm cancellation of the booking for space \""
                + spaceCode + "\":</html>"));
        dialog.add(plateField);

        JButton confirmBtn = new JButton("Cancel Booking");
        JButton closeBtn = new JButton("Close");
        stylePrimaryButton(confirmBtn);
        dialog.add(confirmBtn);
        dialog.add(closeBtn);

        confirmBtn.addActionListener(e -> {
            String input = plateField.getText().trim().toUpperCase();
            if (!input.equals(plate.toUpperCase())) {
                showError("License plate does not match. Cancellation aborted.");
                return;
            }

            clearActiveCancelBookingDialog();
            dialog.dispose();
            if (controller != null) {
                controller.deleteBooking(spaceCode, plate);
            }
        });
        closeBtn.addActionListener(e -> {
            clearActiveCancelBookingDialog();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private void showAdminCancelBookingDialog(String spaceCode, String plate) {
        JDialog dialog = createCancelDialog("Confirm Cancel");
        trackActiveCancelBookingDialog(dialog, spaceCode, plate);

        dialog.add(new JLabel("Cancel the booking for space \"" + spaceCode + "\"?"));
        dialog.add(new JLabel("License plate: " + plate));

        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");
        stylePrimaryButton(yesButton);
        dialog.add(yesButton);
        dialog.add(noButton);

        yesButton.addActionListener(e -> {
            clearActiveCancelBookingDialog();
            dialog.dispose();
            if (controller != null) {
                controller.deleteBooking(spaceCode, plate);
            }
        });
        noButton.addActionListener(e -> {
            clearActiveCancelBookingDialog();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private JDialog createCancelDialog(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new GridLayout(2, 2, 10, 10));
        dialog.setSize(420, 160);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return dialog;
    }

    private void trackActiveCancelBookingDialog(JDialog dialog, String spaceCode, String plate) {
        activeCancelBookingDialog = dialog;
        activeCancelBookingSpaceCode = spaceCode;
        activeCancelBookingPlate = plate;
        activeCancelBookingMode = currentMode;

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (activeCancelBookingDialog == dialog) {
                    clearActiveCancelBookingDialog();
                }
            }
        });
    }

    private void clearActiveCancelBookingDialog() {
        activeCancelBookingDialog = null;
        activeCancelBookingSpaceCode = null;
        activeCancelBookingPlate = null;
    }

    private JDialog createBookingDialog(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(340, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return dialog;
    }

    private void trackActiveBookingDialog(JDialog dialog, JTextField spaceCodeField) {
        activeBookingDialog = dialog;
        activeBookingSpaceCodeField = spaceCodeField;
        activeBookingDialogMode = currentMode;

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (activeBookingDialog == dialog) {
                    clearActiveBookingDialog();
                }
            }
        });
    }

    private void clearActiveBookingDialog() {
        activeBookingDialog = null;
        activeBookingSpaceCodeField = null;
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
            addBookingToTable(space, false);
        }
    }

    public void clearBookingsTable() {
        tableModel.setRowCount(0);
        if (reservationsTableModel != null) {
            reservationsTableModel.setRowCount(0);
        }
    }

    public void addBookingToTable(ParkingSpace space) {
        addBookingToTable(space, false);
    }

    public void addBookingToTable(ParkingSpace space, boolean myBooking) {
        Object[] rowData = buildBookingRow(space, myBooking);
        int existingRow = findBookingRow(space.getId());

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

    private Object[] buildBookingRow(ParkingSpace space, boolean myBooking) {
        return new Object[]{
                space.getId(),
                space.getFloor(),
                space.getVehicleType().name(),
                space.isOccupied() ? "Occupied" : "Vacant",
                space.isReserved() ? "Reserved" : "Available",
                getLicensePlate(space),
                getReservationDate(space),
                myBooking
        };
    }

    private int findBookingRow(String code) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String currentCode = String.valueOf(tableModel.getValueAt(row, 0));
            if (currentCode.equals(code)) {
                return row;
            }
        }

        return -1;
    }

    public void removeBookingSpacesNotIn(java.util.Set<String> visibleCodes) {
        for (int row = tableModel.getRowCount() - 1; row >= 0; row--) {
            String currentCode = String.valueOf(tableModel.getValueAt(row, 0));
            if (!visibleCodes.contains(currentCode)) {
                tableModel.removeRow(row);
            }
        }

        updateActionButtons();
    }

    public void closeActiveBookingDialogIfTargetUnavailable() {
        if (activeBookingDialog == null
                || !activeBookingDialog.isVisible()
                || activeBookingSpaceCodeField == null) {
            return;
        }

        String code = activeBookingSpaceCodeField.getText().trim();
        if (code.isEmpty()) return;

        int row = findBookingRow(code);
        String message = null;

        if (row == -1 && activeBookingDialogMode == USER_MODE) {
            message = "Space \"" + code + "\" is no longer available. The booking dialog was closed.";
        } else if (row != -1) {
            String status = String.valueOf(tableModel.getValueAt(row, 3));
            String reservation = String.valueOf(tableModel.getValueAt(row, 4));

            if (activeBookingDialogMode == USER_MODE
                    && (!"Vacant".equals(status) || !"Available".equals(reservation))) {
                message = "Space \"" + code + "\" is no longer available for booking. The booking dialog was closed.";
            } else if (activeBookingDialogMode == ADMIN_MODE && "Occupied".equals(status)) {
                message = "Space \"" + code + "\" is now occupied. The booking dialog was closed.";
            }
        }

        if (message != null) {
            JDialog dialog = activeBookingDialog;
            clearActiveBookingDialog();
            dialog.dispose();
            showError(message);
        }
    }

    public void closeActiveCancelDialogIfTargetUnavailable() {
        if (activeCancelBookingDialog == null
                || !activeCancelBookingDialog.isVisible()
                || activeCancelBookingSpaceCode == null
                || activeCancelBookingPlate == null) {
            return;
        }

        int row = findBookingRow(activeCancelBookingSpaceCode);
        String message = null;

        if (row == -1) {
            message = "Space \"" + activeCancelBookingSpaceCode
                    + "\" no longer exists. The cancellation confirmation was closed.";
        } else {
            String reservation = String.valueOf(tableModel.getValueAt(row, 4));
            String plate = String.valueOf(tableModel.getValueAt(row, 5));
            boolean samePlate = activeCancelBookingPlate.equalsIgnoreCase(plate);

            if (!"Reserved".equals(reservation) || !samePlate) {
                message = "The booking for space \"" + activeCancelBookingSpaceCode
                        + "\" is no longer available to cancel.";
            } else if (activeCancelBookingMode == USER_MODE
                    && !Boolean.TRUE.equals(tableModel.getValueAt(row, MY_BOOKING_COLUMN))) {
                message = "This is no longer one of your active bookings. The cancellation confirmation was closed.";
            }
        }

        if (message != null) {
            JDialog dialog = activeCancelBookingDialog;
            clearActiveCancelBookingDialog();
            dialog.dispose();
            showError(message);
        }
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateReservationsTable(List<Reservation> reservations) {
        if (reservationsTableModel == null) return;
        reservationsTableModel.setRowCount(0);
        for (Reservation reservation : reservations) {
            ParkingSpace space = reservation.getParkingSpace();
            String code = space != null ? space.getId() : "";
            Object floor = space != null ? space.getFloor() : "";
            String type = space != null ? space.getVehicleType().name() : "";
            String plate = reservation.getVehicle() != null ? reservation.getVehicle().getLicensePlate() : "";
            String date = reservation.getReservationDate() != null
                    ? reservation.getReservationDate().format(DATE_FORMAT) : "";
            String status;
            if (reservation.isActive()) {
                status = "Active";
            } else if (reservation.isCancelledByAdmin()) {
                status = "Cancelled by admin";
            } else {
                status = "Cancelled";
            }
            reservationsTableModel.addRow(new Object[]{code, floor, type, plate, date, status});
        }
    }

    public void setMode(int mode) {
        currentMode = mode;

        if (currentMode == USER_MODE) {
            setTitle("Manage My Bookings");
            if (tabbedPane.indexOfComponent(reservationsPanel) < 0) {
                tabbedPane.addTab("My reservations", reservationsPanel);
            }
            addButton.setText("Book Slot");
            addButton.setVisible(true);
            editButton.setVisible(false);
            deleteButton.setVisible(true);
            deleteButton.setText("Cancel Booking");
        } else {
            setTitle("Manage Slot Bookings");
            if (tabbedPane.indexOfComponent(reservationsPanel) >= 0) {
                tabbedPane.remove(reservationsPanel);
            }
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
        if (reservationsTable != null) {
            reservationsTable.setEnabled(!loading);
        }
        updateActionButtons();
    }

    public void setController(AdminSlotBookingController controller) {
        this.controller = controller;
    }

    private void updateActionButtons() {
        boolean admin = currentMode == ADMIN_MODE;
        boolean user = currentMode == USER_MODE;
        boolean reservationsTab = user && tabbedPane != null && tabbedPane.getSelectedIndex() == 1;

        if (reservationsTab) {
            int row = reservationsTable.getSelectedRow();
            boolean activeReservation = row >= 0
                    && "Active".equals(String.valueOf(reservationsTableModel.getValueAt(
                    reservationsTable.convertRowIndexToModel(row), 5)));

            addButton.setEnabled(false);
            editButton.setEnabled(false);
            deleteButton.setEnabled(!loading && activeReservation);
            refreshButton.setEnabled(!loading);
            return;
        }

        int row = bookingsTable.getSelectedRow();
        boolean selected = row >= 0;
        boolean availableSlot = selected && isSelectedSlotAvailableForBooking(row);
        boolean reservedSlot = selected && isSelectedSlotReserved(row);
        boolean userBooking = selected && isSelectedUserBooking(row);

        addButton.setEnabled(!loading && user && availableSlot);
        editButton.setEnabled(!loading && admin && reservedSlot);
        deleteButton.setEnabled(!loading && ((admin && reservedSlot) || (user && userBooking)));
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

    private boolean isSelectedUserBooking(int selectedRow) {
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        return Boolean.TRUE.equals(tableModel.getValueAt(modelRow, MY_BOOKING_COLUMN));
    }

    private String getLicensePlate(ParkingSpace space) {
        if (space.getParkedVehicle() != null) {
            return space.getParkedVehicle().getLicensePlate();
        } else if (space.getReservation() != null && space.getReservation().getVehicle() != null) {
            return space.getReservation().getVehicle().getLicensePlate();
        }
        return "";
    }

    private String getReservationDate(ParkingSpace space) {
        if (space.getReservation() != null && space.getReservation().getReservationDate() != null) {
            return space.getReservation().getReservationDate().format(DATE_FORMAT);
        }
        return "";
    }

    private class BookingCellRenderer extends DefaultTableCellRenderer {
        private static final Color VACANT_COLOR = new Color(232, 248, 238);
        private static final Color OCCUPIED_COLOR = new Color(253, 235, 235);
        private static final Color MY_BOOKING_COLOR = new Color(225, 240, 255);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                int modelRow = table.convertRowIndexToModel(row);
                int modelColumn = table.convertColumnIndexToModel(column);
                boolean myBooking = Boolean.TRUE.equals(tableModel.getValueAt(modelRow, MY_BOOKING_COLUMN));
                String status = String.valueOf(tableModel.getValueAt(modelRow, 3));

                if (myBooking) {
                    cell.setBackground(MY_BOOKING_COLOR);
                } else {
                    cell.setBackground(Color.WHITE);
                }

                if (modelColumn == 3 && "Vacant".equals(status)) {
                    cell.setBackground(VACANT_COLOR);
                } else if (modelColumn == 3 && "Occupied".equals(status)) {
                    cell.setBackground(OCCUPIED_COLOR);
                }
            }

            return cell;
        }
    }
}
