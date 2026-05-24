package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modal dialog for slot booking management. Used by both admins (manage all bookings) and regular users
 * (manage their own bookings). Shows all parking spaces with colour-coded rows and provides Add, Edit, and
 * Cancel booking controls.
 * <p>
 * The view builds or updates Swing components and leaves the decisions to controllers and services. This
 * keeps the screen code focused on what the user sees.
 * </p>
 */
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
    private JButton logoutButton;
    private SlotBookingActions actions;
    private int currentMode = ADMIN_MODE;
    private boolean loading;
    private JDialog activeBookingDialog;
    private JTextField activeBookingSpaceCodeField;
    private int activeBookingDialogMode;
    private JDialog activeCancelBookingDialog;
    private String activeCancelBookingSpaceCode;
    private String activeCancelBookingPlate;
    private int activeCancelBookingMode;
    private String userBookingPlate;
    private VehicleType userBookingType;

    /**
     * Creates the booking management dialog.
     * <p>
     * The constructor receives the objects or values this class needs and stores them before the rest of
     * the methods are used.
     * </p>
     *
     * @param parent parent used by this operation
     */
    public AdminSlotBookingManagementView(Frame parent) {
        super(parent, "Manage Slot Bookings", true);
        initComponents();
    }

    /**
     * Handles init components.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void initComponents() {
        configureDialog();
        JScrollPane bookingsScrollPane = createBookingsTable();
        createReservationsPanel();
        createTabbedPane(bookingsScrollPane);
        createActionButtons();
        wireSelectionListeners();
        addActionButtonPanel();
        wireButtonActions();
    }

    /**
     * Applies the base dialog layout and size.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void configureDialog() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 247, 250));
        setSize(900, 500);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Creates bookings table.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @return the created bookings table
     */
    private JScrollPane createBookingsTable() {
        String[] columns = {"Code", "Floor", "Type", "Parking Status", "Booking", "Booked Plate", "Booked At", "My Booking"};
        tableModel = new NonEditableTableModel(columns, 0);

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
        return scrollPane;
    }

    /**
     * Creates reservations panel.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     */
    private void createReservationsPanel() {
        String[] reservationColumns = {"Space Code", "Floor", "Type", "License Plate", "Booked At", "Status"};
        reservationsTableModel = new NonEditableTableModel(reservationColumns, 0);
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
    }

    /**
     * Creates the tabs used by the booking dialog.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @param bookingsScrollPane bookings scroll pane used by this operation
     */
    private void createTabbedPane(JScrollPane bookingsScrollPane) {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Slot bookings", bookingsScrollPane);
        tabbedPane.addChangeListener(e -> updateActionButtons());
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Creates action buttons.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     */
    private void createActionButtons() {
        addButton = new JButton("Add Booking");
        editButton = new JButton("Edit Booking");
        deleteButton = new JButton("Delete Booking");
        refreshButton = new JButton("Refresh");
        logoutButton = new JButton("Log out");
        stylePrimaryButton(addButton);
        stylePrimaryButton(editButton);
        stylePrimaryButton(refreshButton);
        stylePrimaryButton(logoutButton);
        deleteButton.setForeground(new Color(180, 30, 30));

        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    /**
     * Handles wire selection listeners.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void wireSelectionListeners() {
        bookingsTable.getSelectionModel().addListSelectionListener(e -> {
            updateActionButtons();
        });
        reservationsTable.getSelectionModel().addListSelectionListener(e -> {
            updateActionButtons();
        });
    }

    /**
     * Adds action button panel.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void addActionButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(logoutButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Handles wire button actions.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void wireButtonActions() {
        addButton.addActionListener(e -> showAddBookingDialog());
        editButton.addActionListener(e -> showEditBookingDialog());
        deleteButton.addActionListener(e -> showDeleteConfirmation());
        refreshButton.addActionListener(e -> {
            if (actions != null) actions.loadBookings();
        });
    }

    /**
     * Opens the dialog used to book the selected slot.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     */
    private void showAddBookingDialog() {
        if (currentMode != USER_MODE) return;

        if (userBookingPlate == null || userBookingPlate.isBlank() || userBookingType == null) {
            BookingVehicleInput selection = promptForBookingVehicle();
            if (selection == null) return;
            if (actions != null) {
                actions.prepareUserBooking(selection.getPlate(), selection.getType());
            }
            return;
        }

        int row = bookingsTable.getSelectedRow();
        if (row < 0) {
            showError("Select an available space to book.");
            return;
        }

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
        if (VehicleType.valueOf(currentType) != userBookingType) {
            showError("Space \"" + currentCode + "\" does not accept " + userBookingType.name() + " vehicles.");
            return;
        }

        JDialog dialog = createBookingDialog("Book Selected Slot");

        JTextField plateField = new JTextField(userBookingPlate);
        plateField.setEditable(false);
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
            String spaceCode = spaceCodeField.getText().trim();

            if (spaceCode.isEmpty()) {
                showError("License plate and space code cannot be empty.");
                return;
            }

            if (actions != null) actions.createBooking(userBookingPlate, userBookingType, spaceCode);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    /**
     * Opens the dialog used by admins to reassign a booking.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     */
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

            if (actions != null) actions.editBooking(currentCode, plate, type, spaceCode);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    /**
     * Handles style primary button.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param b b used by this operation
     */
    private void stylePrimaryButton(JButton b) {
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(33, 99, 168));
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Shows delete confirmation.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     */
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

    /**
     * Opens the user confirmation dialog for cancelling a booking.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     *
     * @param spaceCode parking space code involved in the operation
     * @param plate license plate involved in the operation
     */
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
            if (actions != null) {
                actions.deleteBooking(spaceCode, plate);
            }
        });
        closeBtn.addActionListener(e -> {
            clearActiveCancelBookingDialog();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    /**
     * Opens the admin confirmation dialog for cancelling a booking.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     *
     * @param spaceCode parking space code involved in the operation
     * @param plate license plate involved in the operation
     */
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
            if (actions != null) {
                actions.deleteBooking(spaceCode, plate);
            }
        });
        noButton.addActionListener(e -> {
            clearActiveCancelBookingDialog();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    /**
     * Creates a small confirmation dialog.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @param title title used by this operation
     * @return the created cancel dialog
     */
    private JDialog createCancelDialog(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new GridLayout(2, 2, 10, 10));
        dialog.setSize(420, 160);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return dialog;
    }

    /**
     * Stores the currently open cancellation dialog.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param dialog dialog used by this operation
     * @param spaceCode parking space code involved in the operation
     * @param plate license plate involved in the operation
     */
    private void trackActiveCancelBookingDialog(JDialog dialog, String spaceCode, String plate) {
        activeCancelBookingDialog = dialog;
        activeCancelBookingSpaceCode = spaceCode;
        activeCancelBookingPlate = plate;
        activeCancelBookingMode = currentMode;

        dialog.addWindowListener(new WindowClosedAction(() -> clearActiveCancelBookingDialogIfMatches(dialog)));
    }

    /**
     * Clears the tracked cancellation dialog state.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     */
    private void clearActiveCancelBookingDialog() {
        activeCancelBookingDialog = null;
        activeCancelBookingSpaceCode = null;
        activeCancelBookingPlate = null;
    }

    /**
     * Closes the tracked cancellation dialog if one is open.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void closeActiveCancelBookingDialog() {
        if (activeCancelBookingDialog != null) {
            activeCancelBookingDialog.dispose();
        }
        clearActiveCancelBookingDialog();
    }

    /**
     * Creates the dialog used for booking or reassignment.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @param title title used by this operation
     * @return the created booking dialog
     */
    private JDialog createBookingDialog(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(340, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return dialog;
    }

    /**
     * Stores the currently open booking dialog.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param dialog dialog used by this operation
     * @param spaceCodeField parking space code involved in the operation
     */
    private void trackActiveBookingDialog(JDialog dialog, JTextField spaceCodeField) {
        activeBookingDialog = dialog;
        activeBookingSpaceCodeField = spaceCodeField;
        activeBookingDialogMode = currentMode;

        dialog.addWindowListener(new WindowClosedAction(() -> clearActiveBookingDialogIfMatches(dialog)));
    }

    /**
     * Clears the tracked booking dialog state.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     */
    private void clearActiveBookingDialog() {
        activeBookingDialog = null;
        activeBookingSpaceCodeField = null;
    }

    /**
     * Closes the tracked booking dialog if one is open.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void closeActiveBookingDialog() {
        if (activeBookingDialog != null) {
            activeBookingDialog.dispose();
        }
        clearActiveBookingDialog();
    }

    /**
     * Adds the common booking fields to a dialog.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param dialog dialog used by this operation
     * @param plateField plate field used by this operation
     * @param spaceCodeField parking space code involved in the operation
     */
    private void addBookingFields(JDialog dialog, JTextField plateField, JTextField spaceCodeField) {
        dialog.add(new JLabel("License Plate:"));
        dialog.add(plateField);

        dialog.add(new JLabel("Space Code:"));
        dialog.add(spaceCodeField);
    }


    /**
     * Handles clear bookings table.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     */
    public void clearBookingsTable() {
        tableModel.setRowCount(0);
        if (reservationsTableModel != null) {
            reservationsTableModel.setRowCount(0);
        }
    }

    /**
     * Clears booking data and closes child dialogs when a user session ends.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     */
    public void clearSessionViewState() {
        closeActiveBookingDialog();
        closeActiveCancelBookingDialog();
        resetModeAfterSession();
        setUserBookingVehicle(null, null);
        clearBookingsTable();
        setLoading(false);
        setVisible(false);
    }


    /**
     * Adds booking to table.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param space space used by this operation
     * @param myBooking my booking used by this operation
     */
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

    /**
     * Builds booking row.
     * <p>
     * This helper builds one Swing component used by the screen, keeping layout code separate from event
     * logic.
     * </p>
     *
     * @param space space used by this operation
     * @param myBooking my booking used by this operation
     * @return the built booking row
     */
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

    /**
     * Finds booking row.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param code parking space code involved in the operation
     * @return the matching booking row, or null when it is not found
     */
    private int findBookingRow(String code) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String currentCode = String.valueOf(tableModel.getValueAt(row, 0));
            if (currentCode.equals(code)) {
                return row;
            }
        }

        return -1;
    }

    /**
     * Handles remove booking spaces not in.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param visibleCodes visible codes used by this operation
     */
    public void removeBookingSpacesNotIn(java.util.Set<String> visibleCodes) {
        for (int row = tableModel.getRowCount() - 1; row >= 0; row--) {
            String currentCode = String.valueOf(tableModel.getValueAt(row, 0));
            if (!visibleCodes.contains(currentCode)) {
                tableModel.removeRow(row);
            }
        }

        updateActionButtons();
    }

    /**
     * Closes an open booking dialog if the selected space changed.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
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

    /**
     * Closes an open cancellation dialog if the booking changed.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
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

    /**
     * Shows an error message owned by this dialog.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     *
     * @param message message shown to the user or written to the log
     */
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an information message owned by this dialog.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     *
     * @param message message shown to the user or written to the log
     */
    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Asks the user to confirm logout from this dialog.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     *
     * @return the answer chosen by the user
     */
    public boolean confirmLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?",
                "Log out",
                JOptionPane.YES_NO_OPTION);
        return confirm == JOptionPane.YES_OPTION;
    }

    /**
     * Adds logout listener.
     * <p>
     * This connects a Swing action with the code that should run when the user clicks a button or interacts
     * with the screen.
     * </p>
     *
     * @param listener action to run when logout is clicked
     */
    public void addLogoutListener(ActionListener listener) {
        logoutButton.addActionListener(listener);
    }

    /**
     * Updates reservations table.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     *
     * @param reservations reservations used by this operation
     */
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

    /**
     * Sets the dialog mode: admin or regular user.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param mode mode used by this operation
     */
    public void setMode(int mode) {
        currentMode = mode;

        if (currentMode == USER_MODE) {
            setTitle("Manage My Bookings");
            if (tabbedPane.indexOfComponent(reservationsPanel) < 0) {
                tabbedPane.addTab("My reservations", reservationsPanel);
            }
            tabbedPane.setSelectedComponent(reservationsPanel);
            addButton.setText("Choose Vehicle");
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

    /**
     * Sets the loading.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param loading true while the screen is waiting for an operation to finish
     */
    public void setLoading(boolean loading) {
        this.loading = loading;
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        bookingsTable.setEnabled(!loading);
        if (reservationsTable != null) {
            reservationsTable.setEnabled(!loading);
        }
        updateActionButtons();
    }

    /**
     * Sets the actions used by this dialog.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param actions controller-backed actions for this dialog
     */
    public void setActions(SlotBookingActions actions) {
        this.actions = actions;
    }

    /**
     * Sets the user booking vehicle.
     * <p>
     * The setter keeps the field change inside this object instead of letting other classes touch the field
     * directly.
     * </p>
     *
     * @param plate license plate involved in the operation
     * @param type vehicle type involved in the operation
     */
    public void setUserBookingVehicle(String plate, VehicleType type) {
        userBookingPlate = plate;
        userBookingType = type;
        updateActionButtons();
    }

    /**
     * Shows slot bookings tab.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     */
    public void showSlotBookingsTab() {
        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(0);
        }
    }

    /**
     * Prompts for for booking vehicle.
     * <p>
     * This method shows a dialog or message to the user while keeping direct Swing work inside the view.
     * </p>
     *
     * @return selected plate and type, or null if cancelled
     */
    public BookingVehicleInput promptForBookingVehicle() {
        while (true) {
            JTextField plateField = new JTextField(18);
            JComboBox<VehicleType> typeCombo = new JComboBox<>(VehicleType.values());
            JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
            panel.add(new JLabel("License plate"));
            panel.add(plateField);
            panel.add(new JLabel("Vehicle type"));
            panel.add(typeCombo);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Choose vehicle",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            String plate = plateField.getText().trim().toUpperCase();
            VehicleType type = (VehicleType) typeCombo.getSelectedItem();
            if (!plate.isEmpty() && type != null) {
                return new BookingVehicleInput(plate, type);
            }

            showError("License plate and vehicle type are required.");
        }
    }

    /**
     * Updates action buttons.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     */
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
            logoutButton.setEnabled(!loading);
            return;
        }

        int row = bookingsTable.getSelectedRow();
        boolean selected = row >= 0;
        boolean availableSlot = selected && isSelectedSlotAvailableForBooking(row);
        boolean reservedSlot = selected && isSelectedSlotReserved(row);
        boolean userBooking = selected && isSelectedUserBooking(row);
        boolean needsVehicleSelection = user && (userBookingPlate == null
                || userBookingPlate.isBlank()
                || userBookingType == null);

        if (user) {
            addButton.setText(needsVehicleSelection ? "Choose Vehicle" : "Book Slot");
        }

        addButton.setEnabled(!loading && user && (needsVehicleSelection || availableSlot));
        editButton.setEnabled(!loading && admin && reservedSlot);
        deleteButton.setEnabled(!loading && ((admin && reservedSlot) || (user && userBooking)));
        refreshButton.setEnabled(!loading);
        logoutButton.setEnabled(!loading);
    }

    /**
     * Checks whether selected slot available for booking.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param selectedRow selected row used by this operation
     * @return true when the condition is met, false otherwise
     */
    private boolean isSelectedSlotAvailableForBooking(int selectedRow) {
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        String status = String.valueOf(tableModel.getValueAt(modelRow, 3));
        String reservation = String.valueOf(tableModel.getValueAt(modelRow, 4));
        return "Vacant".equals(status) && "Available".equals(reservation);
    }

    /**
     * Checks whether selected slot reserved.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param selectedRow selected row used by this operation
     * @return true when the condition is met, false otherwise
     */
    private boolean isSelectedSlotReserved(int selectedRow) {
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        String reservation = String.valueOf(tableModel.getValueAt(modelRow, 4));
        return "Reserved".equals(reservation);
    }

    /**
     * Checks whether selected user booking.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     *
     * @param selectedRow selected row used by this operation
     * @return true when the condition is met, false otherwise
     */
    private boolean isSelectedUserBooking(int selectedRow) {
        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        return Boolean.TRUE.equals(tableModel.getValueAt(modelRow, MY_BOOKING_COLUMN));
    }

    /**
     * Gets the plate shown for a parking space.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @param space space used by this operation
     * @return the current license plate
     */
    private String getLicensePlate(ParkingSpace space) {
        if (space.getParkedVehicle() != null) {
            return space.getParkedVehicle().getLicensePlate();
        } else if (space.getReservation() != null && space.getReservation().getVehicle() != null) {
            return space.getReservation().getVehicle().getLicensePlate();
        }
        return "";
    }

    /**
     * Gets the reservation date shown for a parking space.
     * <p>
     * The getter keeps the field private while still giving the rest of the project a clear way to read it.
     * </p>
     *
     * @param space space used by this operation
     * @return the current reservation date
     */
    private String getReservationDate(ParkingSpace space) {
        if (space.getReservation() != null && space.getReservation().getReservationDate() != null) {
            return space.getReservation().getReservationDate().format(DATE_FORMAT);
        }
        return "";
    }

    /**
     * Resets the hidden dialog mode so it keeps no role from the previous session.
     * <p>
     * This helper keeps a small part of the Swing screen named so the larger view method stays easier to
     * read.
     * </p>
     */
    private void resetModeAfterSession() {
        currentMode = ADMIN_MODE;
    }

    /**
     * Clears the booking dialog state if the closed dialog is still the tracked dialog.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     *
     * @param dialog dialog that has just closed
     */
    private void clearActiveBookingDialogIfMatches(JDialog dialog) {
        if (activeBookingDialog == dialog) {
            clearActiveBookingDialog();
        }
    }

    /**
     * Clears the cancellation dialog state if the closed dialog is still the tracked dialog.
     * <p>
     * This method changes visible fields, buttons, or table rows after a controller provides new data.
     * </p>
     *
     * @param dialog dialog that has just closed
     */
    private void clearActiveCancelBookingDialogIfMatches(JDialog dialog) {
        if (activeCancelBookingDialog == dialog) {
            clearActiveCancelBookingDialog();
        }
    }
}
