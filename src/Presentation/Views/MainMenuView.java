package Presentation.Views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import javax.swing.table.DefaultTableModel;

/**
 * Main application window shown after a successful login.
 * Adapts its navigation options depending on whether the logged-in user is an admin or a regular client.
 */
public class MainMenuView extends JFrame {
    private JLabel titleLabel;
    private JLabel brandSubtitleLabel;

    private JButton statusButton;
    private JButton reservationButton;
    private JButton entryExitButton;
    private JButton parkingEntryButton;
    private JButton parkingExitButton;
    private JButton occupancyChartButton;
    private JButton logoutButton;
    private JButton deleteAccountButton;

    private JPanel mainPanel;
    private JPanel navPanel;
    private JPanel mgmtGroup;
    private JPanel visGroup;
    private JPanel accountGroup;
    private JPanel parkingEntryExitButtonRow;
    private JPanel brand;

    private JPanel parkingSlotsPanel;
    private JTable parkingSlotsTable;
    private JLabel parkingSlotsCountLabel;
    private java.util.List<MouseListener> parkingSlotsTableMouseListeners = new java.util.ArrayList<>();

    private OccupancyChartView occupancyChartPanel;
    private JButton parkingSlotsBackButton;
    private java.util.List<ActionListener> parkingSlotsBackListeners = new java.util.ArrayList<>();

    /** Creates the main menu window. */
    public MainMenuView() {
        this(new OccupancyChartView());
    }

    /**
     * Creates the main menu window with the chart panel it should display.
     *
     * @param occupancyChartPanel chart panel owned by the main menu
     */
    public MainMenuView(OccupancyChartView occupancyChartPanel) {
        this.occupancyChartPanel = occupancyChartPanel;
        setTitle("Main screen");
    }

    /** Builds the main menu components. */
    public void initComponents() {
        configureMainPanel();
        addBrandPanel();
        addTitleLabel();
        addNavigationPanel();
        createMenuButtons();
        addManagementGroup();
        addVisualizationGroup();
        addAccountGroup();
        configureMainWindow();
        addParkingSlotsPanel();
        addOccupancyChartPanel();
    }

    /** Creates the main content panel. */
    private void configureMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBackground(new Color(245, 247, 250));
    }

    /** Creates and adds the left brand panel. */
    private void addBrandPanel() {
        brand = new JPanel();
        brand.setLayout(null);
        brand.setBackground(new Color(33, 99, 168));
        brand.setBounds(0, 0, 360, 620);
        brand.add(createBrandLogo());
        brand.add(createBrandTitle());
        brand.add(createBrandSubtitle());
        mainPanel.add(brand);
    }

    /** Creates the logo shown in the brand panel. */
    private JLabel createBrandLogo() {
        JLabel logo = new JLabel("P", SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 64));
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 90), 3, true));
        logo.setBounds(120, 120, 120, 110);
        return logo;
    }

    /** Creates the title shown in the brand panel. */
    private JLabel createBrandTitle() {
        JLabel brandTitle = new JLabel("Parking System", SwingConstants.CENTER);
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setBounds(20, 255, 320, 40);
        return brandTitle;
    }

    /** Creates the subtitle updated by the current user mode. */
    private JLabel createBrandSubtitle() {
        brandSubtitleLabel = new JLabel("Your dashboard", SwingConstants.CENTER);
        brandSubtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        brandSubtitleLabel.setForeground(new Color(220, 230, 245));
        brandSubtitleLabel.setBounds(20, 300, 320, 30);
        return brandSubtitleLabel;
    }

    /** Adds the welcome label above the navigation panel. */
    private void addTitleLabel() {
        titleLabel = new JLabel("Welcome");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(40, 40, 50));
        titleLabel.setBounds(390, 35, 500, 40);
        mainPanel.add(titleLabel);
    }

    /** Adds the panel that contains the main navigation groups. */
    private void addNavigationPanel() {
        navPanel = new JPanel();
        navPanel.setLayout(null);
        navPanel.setOpaque(false);
        navPanel.setBounds(390, 90, 510, 540);
        mainPanel.add(navPanel);
    }

    /** Creates all main menu buttons before placing them in groups. */
    private void createMenuButtons() {
        statusButton = new JButton();
        reservationButton = new JButton();
        entryExitButton = new JButton();
        parkingEntryButton = new JButton();
        parkingExitButton = new JButton();
        occupancyChartButton = new JButton();
        logoutButton = new JButton();
        deleteAccountButton = new JButton();
    }

    /** Adds the parking management group. */
    private void addManagementGroup() {
        mgmtGroup = createGroupPanel("Parking management", 0, 0);
        mgmtGroup.add(styleButton(entryExitButton, "Manage parking slots"));
        mgmtGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        mgmtGroup.add(styleButton(reservationButton, "Manage slot booking"));
        mgmtGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        parkingEntryExitButtonRow = createEntryExitButtonRow();
        mgmtGroup.add(parkingEntryExitButtonRow);
        navPanel.add(mgmtGroup);
    }

    /** Adds the parking visualization group. */
    private void addVisualizationGroup() {
        visGroup = createGroupPanel("Parking visualization", 0, 200);
        visGroup.add(styleButton(occupancyChartButton, "Display last hour occupancy"));
        visGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        visGroup.add(styleButton(statusButton, "Display current parking status"));
        navPanel.add(visGroup);
    }

    /** Adds the account group inside the brand panel. */
    private void addAccountGroup() {
        accountGroup = createBrandGroupPanel("Account", 40, 430);
        accountGroup.add(styleBrandButton(logoutButton, "Log out"));
        accountGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        styleBrandButton(deleteAccountButton, "Delete Account");
        deleteAccountButton.setForeground(new Color(200, 60, 60));
        accountGroup.add(deleteAccountButton);
        brand.add(accountGroup);
    }

    /** Applies the main window settings. */
    private void configureMainWindow() {
        setContentPane(mainPanel);
        setSize(930, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /** Creates and adds the parking status panel. */
    private void addParkingSlotsPanel() {
        parkingSlotsPanel = createParkingSlotsPanel();
        parkingSlotsPanel.setVisible(false);
        mainPanel.add(parkingSlotsPanel);
    }

    /** Creates and adds the occupancy chart panel. */
    private void addOccupancyChartPanel() {
        occupancyChartPanel.initComponents();
        occupancyChartPanel.setBounds(390, 90, 510, 540);
        occupancyChartPanel.setVisible(false);
        mainPanel.add(occupancyChartPanel);
    }

    /** Applies the admin or user mode after login. */
    public void setMode(int mode, String username) {
        resetDisplayedContent();

        if (mode == 1) {
            titleLabel.setText("Welcome - " + username);
            brandSubtitleLabel.setText("Admin dashboard");
            mgmtGroup.setVisible(true);
            entryExitButton.setVisible(true);
            reservationButton.setVisible(true);
            parkingEntryButton.setVisible(false);
            parkingExitButton.setVisible(false);
            parkingEntryExitButtonRow.setVisible(false);
            reservationButton.setText("Manage slot booking");
            deleteAccountButton.setVisible(false);
        } else if (mode == 2) {
            titleLabel.setText("Welcome - " + username);
            brandSubtitleLabel.setText("User dashboard");
            mgmtGroup.setVisible(true);
            entryExitButton.setVisible(false);
            reservationButton.setVisible(true);
            parkingEntryButton.setVisible(true);
            parkingEntryButton.setEnabled(true);
            parkingExitButton.setVisible(true);
            parkingExitButton.setEnabled(false);
            parkingEntryExitButtonRow.setVisible(true);
            reservationButton.setText("Manage my bookings");
            deleteAccountButton.setVisible(true);
        }

        showNavigation();

        revalidate();
        repaint();
    }

    /** Creates a white navigation group. */
    private JPanel createGroupPanel(String title, int x, int y) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        panel.setBounds(x, y, 250, 175);

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(new Color(40, 40, 50));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        return panel;
    }

    /** Creates a navigation group inside the blue brand panel. */
    private JPanel createBrandGroupPanel(String title, int x, int y) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(33, 99, 168));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 90), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        panel.setBounds(x, y, 280, 150);

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        return panel;
    }

    /** Applies the brand-panel button style. */
    private JButton styleBrandButton(JButton btn, String text) {
        btn.setText(text);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(260, 40));
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(33, 99, 168));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Applies the standard menu button style. */
    private JButton styleButton(JButton btn, String text) {
        btn.setText(text);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(220, 40));
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(33, 99, 168));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 99, 168), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Creates the row with user entry and exit buttons. */
    private JPanel createEntryExitButtonRow() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(220, 40));

        styleSmallButton(parkingEntryButton, "Entry");
        styleSmallButton(parkingExitButton, "Exit");

        panel.add(parkingEntryButton);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        panel.add(parkingExitButton);
        return panel;
    }

    /** Creates the compact back button for table panels. */
    private JButton createBackBoxButton() {
        JButton b = new JButton("<-");
        b.setPreferredSize(new Dimension(42, 34));
        b.setMinimumSize(new Dimension(42, 34));
        b.setMaximumSize(new Dimension(42, 34));
        b.setForeground(new Color(33, 99, 168));
        b.setBackground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(33, 99, 168), 1, true));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText("Back to main menu");
        return b;
    }

    /** Applies the compact entry/exit button style. */
    private JButton styleSmallButton(JButton btn, String text) {
        btn.setText(text);
        btn.setMaximumSize(new Dimension(105, 40));
        btn.setPreferredSize(new Dimension(105, 40));
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(33, 99, 168));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 99, 168), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Creates the current parking status panel. */
    private JPanel createParkingSlotsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        panel.setBounds(390, 90, 510, 540);

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        parkingSlotsBackButton = createBackBoxButton();
        JPanel backCorner = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backCorner.setOpaque(false);
        backCorner.add(parkingSlotsBackButton);
        header.add(backCorner, BorderLayout.WEST);

        JLabel title = new JLabel("Current parking status");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(40, 40, 50));
        header.add(title, BorderLayout.CENTER);

        parkingSlotsCountLabel = new JLabel("Total slots: 0", SwingConstants.RIGHT);
        parkingSlotsCountLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        parkingSlotsCountLabel.setForeground(new Color(80, 90, 105));
        header.add(parkingSlotsCountLabel, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        for (ActionListener listener : parkingSlotsBackListeners) {
            parkingSlotsBackButton.addActionListener(listener);
        }

        String[] columns = {
                "Code",
                "Floor",
                "Status",
                "Reservation",
                "My Parked Vehicle"
        };

        Object[][] rows = {};

        DefaultTableModel model = new NonEditableTableModel(rows, columns);

        parkingSlotsTable = new JTable(model);
        parkingSlotsTable.setRowHeight(23);
        parkingSlotsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        parkingSlotsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        parkingSlotsTable.getTableHeader().setBackground(new Color(33, 99, 168));
        parkingSlotsTable.getTableHeader().setForeground(Color.WHITE);
        parkingSlotsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        parkingSlotsTable.removeColumn(parkingSlotsTable.getColumnModel().getColumn(4));

        for (MouseListener listener : parkingSlotsTableMouseListeners) {
            parkingSlotsTable.addMouseListener(listener);
        }

        ParkingStatusCellRenderer renderer = new ParkingStatusCellRenderer();
        for (int i = 0; i < parkingSlotsTable.getColumnModel().getColumnCount(); i++) {
            parkingSlotsTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane scrollPane = new JScrollPane(parkingSlotsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 215, 225), 1, true));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /** Shows the main navigation controls. */
    private void showNavigation() {
        navPanel.setVisible(true);
        titleLabel.setVisible(true);
    }

    /** Hides the main navigation controls. */
    private void hideNavigation() {
        navPanel.setVisible(false);
    }

    /** Shows the current parking status table. */
    public void showParkingSlotsTable() {
        hideNavigation();
        occupancyChartPanel.setVisible(false);
        parkingSlotsPanel.setVisible(true);
        parkingSlotsPanel.revalidate();
        parkingSlotsPanel.repaint();
    }

    /** Checks whether the parking status table is currently visible. */
    public boolean isParkingSlotsTableVisible() {
        return parkingSlotsPanel != null && parkingSlotsPanel.isVisible();
    }

    /** Adds a listener for the parking status back button. */
    public void addParkingSlotsBackListener(ActionListener listener) {
        parkingSlotsBackListeners.add(listener);
        if (parkingSlotsBackButton != null) {
            parkingSlotsBackButton.addActionListener(listener);
        }
    }

    /** Adds or updates one parking space row. */
    public void addParkingSpaceToTable(ParkingSpace space) {
        addParkingSpaceToTable(space, false);
    }

    /** Adds or updates one parking space row and marks user-owned parked vehicles. */
    public void addParkingSpaceToTable(ParkingSpace space, boolean myParkedVehicle) {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        Object[] rowData = buildParkingSpaceRow(space, myParkedVehicle);
        int existingRow = findParkingSpaceRow(space.getId());

        if (existingRow == -1) {
            model.addRow(rowData);
            updateParkingSlotsCount();
            return;
        }

        for (int column = 0; column < rowData.length; column++) {
            Object currentValue = model.getValueAt(existingRow, column);
            Object newValue = rowData[column];
            if (currentValue == null && newValue != null) {
                model.setValueAt(newValue, existingRow, column);
            } else if (currentValue != null && !currentValue.equals(newValue)) {
                model.setValueAt(newValue, existingRow, column);
            }
        }
        updateParkingSlotsCount();
    }

    /** Builds a table row for a parking space. */
    private Object[] buildParkingSpaceRow(ParkingSpace space, boolean myParkedVehicle) {
        String occupiedPlate = space.getParkedVehicle() != null
                ? space.getParkedVehicle().getLicensePlate()
                : null;
        String reservedPlate = (space.getReservation() != null && space.getReservation().getVehicle() != null)
                ? space.getReservation().getVehicle().getLicensePlate()
                : null;

        String statusCell;
        if (space.isOccupied() && occupiedPlate != null && !occupiedPlate.isEmpty()) {
            statusCell = "Occupied (" + occupiedPlate + ")";
        } else if (space.isOccupied()) {
            statusCell = "Occupied";
        } else {
            statusCell = "Vacant";
        }

        String reservationCell;
        if (space.isReserved() && reservedPlate != null && !reservedPlate.isEmpty()) {
            reservationCell = "Reserved (" + reservedPlate + ")";
        } else if (space.isReserved()) {
            reservationCell = "Reserved";
        } else {
            reservationCell = "Available";
        }

        return new Object[] {
                space.getId(),
                space.getFloor(),
                statusCell,
                reservationCell,
                myParkedVehicle
        };
    }

    /** Finds the parking status table row for a space code. */
    private int findParkingSpaceRow(String code) {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            String currentCode = String.valueOf(model.getValueAt(row, 0));
            if (currentCode.equals(code)) {
                return row;
            }
        }

        return -1;
    }

    /** Removes parking status rows that are no longer present. */
    public void removeParkingSpacesNotIn(java.util.Set<String> visibleCodes) {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        for (int row = model.getRowCount() - 1; row >= 0; row--) {
            String currentCode = String.valueOf(model.getValueAt(row, 0));
            if (!visibleCodes.contains(currentCode)) {
                model.removeRow(row);
            }
        }
        updateParkingSlotsCount();
    }

    /** Updates the total slot count shown above the parking status table. */
    private void updateParkingSlotsCount() {
        if (parkingSlotsCountLabel == null || parkingSlotsTable == null) return;

        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        parkingSlotsCountLabel.setText("Total slots: " + model.getRowCount());
    }

    /** Adds a listener to the current parking status button. */
    public void addStatusListener(ActionListener listener) {
        statusButton.addActionListener(listener);
    }

    /** Adds a listener to the booking management button. */
    public void addReservationListener(ActionListener listener) {
        reservationButton.addActionListener(listener);
    }

    /** Adds a listener to the admin parking management button. */
    public void addEntryExitListener(ActionListener listener) {
        entryExitButton.addActionListener(listener);
    }

    /** Adds a listener to the user parking entry button. */
    public void addParkingEntryListener(ActionListener listener) {
        parkingEntryButton.addActionListener(listener);
    }

    /** Adds a listener to the user parking exit button. */
    public void addParkingExitListener(ActionListener listener) {
        parkingExitButton.addActionListener(listener);
    }

    /** Adds a listener to the occupancy chart button. */
    public void addOccupancyChartListener(ActionListener listener) {
        occupancyChartButton.addActionListener(listener);
    }

    /** Adds a listener to the logout button. */
    public void addLogoutListener(ActionListener listener) {
        logoutButton.addActionListener(listener);
    }

    /** Adds a listener to the delete-account button. */
    public void addDeleteAccountListener(ActionListener listener) {
        deleteAccountButton.addActionListener(listener);
    }

    /** Enables or disables the parking entry button. */
    public void setParkingEntryButtonEnabled(boolean enabled) {
        parkingEntryButton.setEnabled(enabled);
    }

    /** Enables or disables the parking exit button. */
    public void setParkingExitButtonEnabled(boolean enabled) {
        parkingExitButton.setEnabled(enabled);
    }

    /** Clears the parking status table. */
    public void clearParkingSlotsTable() {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        model.setRowCount(0);
        updateParkingSlotsCount();
    }

    /** Recreates the parking status panel and its listeners. */
    public void rebuildParkingSlotsPanel() {
        Container contentPane = getContentPane();
        if (parkingSlotsPanel != null) {
            contentPane.remove(parkingSlotsPanel);
        }

        parkingSlotsPanel = createParkingSlotsPanel();
        parkingSlotsPanel.setVisible(false);
        contentPane.add(parkingSlotsPanel);
        revalidate();
        repaint();
    }

    /** Adds a mouse listener to the parking status table. */
    public void addParkingSlotsTableMouseListener(MouseListener listener) {
        parkingSlotsTableMouseListeners.add(listener);
        if (parkingSlotsTable != null) {
            parkingSlotsTable.addMouseListener(listener);
        }
    }

    /** Gets the parking space code at a clicked table point. */
    public String getParkingSpaceCodeAtPoint(Point point) {
        int row = parkingSlotsTable.rowAtPoint(point);
        if (row < 0) return null;

        int modelRow = parkingSlotsTable.convertRowIndexToModel(row);
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        return String.valueOf(model.getValueAt(modelRow, 0));
    }

    /** Returns the menu to its default navigation view. */
    public void resetDisplayedContent() {
        clearParkingSlotsTable();
        parkingSlotsPanel.setVisible(false);
        occupancyChartPanel.setVisible(false);
        showNavigation();
        revalidate();
        repaint();
    }

    /** Clears user-specific text and data from the main menu when a session ends. */
    public void clearSessionViewState() {
        resetDisplayedContent();
        titleLabel.setText("Welcome");
        brandSubtitleLabel.setText("Your dashboard");
        parkingExitButton.setEnabled(false);
        deleteAccountButton.setVisible(false);
        revalidate();
        repaint();
    }

    /** Shows the occupancy chart panel. */
    public void showOccupancyChart() {
        hideNavigation();
        parkingSlotsPanel.setVisible(false);
        occupancyChartPanel.setVisible(true);
        occupancyChartPanel.revalidate();
        occupancyChartPanel.repaint();
    }

    /** Adds a listener to the occupancy chart back action. */
    public void addOccupancyChartBackListener(ActionListener listener) {
        occupancyChartPanel.addBackListener(listener);
    }

    /**
     * Shows an error message owned by the main menu window.
     *
     * @param title   dialog title
     * @param message message to show
     */
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an information message owned by the main menu window.
     *
     * @param title   dialog title
     * @param message message to show
     */
    public void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a warning message owned by the main menu window.
     *
     * @param title   dialog title
     * @param message message to show
     */
    public void showWarning(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /** Asks the user to confirm logout. */
    public boolean confirmLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?",
                "Log out",
                JOptionPane.YES_NO_OPTION);
        return confirm == JOptionPane.YES_OPTION;
    }

    /** Asks the user to confirm account deletion. */
    public boolean confirmDeleteAccount() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete your account?\nThis action cannot be undone.",
                "Delete Account",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return confirm == JOptionPane.YES_OPTION;
    }

    /**
     * Prompts for a license plate.
     *
     * @param title dialog title
     * @return normalized plate, or null if cancelled
     */
    public String promptLicensePlate(String title) {
        while (true) {
            JTextField plateField = new JTextField(18);
            JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
            panel.add(new JLabel("License plate"));
            panel.add(plateField);

            int result = JOptionPane.showConfirmDialog(this,
                    panel,
                    title,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            String plate = plateField.getText().trim().toUpperCase();
            if (!plate.isEmpty()) {
                return plate;
            }

            showError(title, "License plate cannot be empty.");
        }
    }

    /**
     * Prompts for the vehicle type when no active reservation exists.
     *
     * @param plate license plate entered by the user
     * @return selected type, or null if cancelled
     */
    public VehicleType promptVehicleTypeForEntry(String plate) {
        JComboBox<VehicleType> typeCombo = new JComboBox<>(VehicleType.values());
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.add(new JLabel("No active reservation was found for " + plate + "."));
        panel.add(new JLabel("Select the vehicle type:"));
        panel.add(typeCombo);

        int result = JOptionPane.showConfirmDialog(this,
                panel,
                "Parking entry",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        return (VehicleType) typeCombo.getSelectedItem();
    }

    /**
     * Prompts the user to choose one of their parked vehicles for exit.
     *
     * @param parkedSpaces spaces currently occupied by the user's vehicles
     * @return selected parking space, or null if cancelled
     */
    public ParkingSpace promptExitVehicle(java.util.List<ParkingSpace> parkedSpaces) {
        JComboBox<ExitSpaceOption> vehicleCombo = new JComboBox<>();
        for (ParkingSpace space : parkedSpaces) {
            vehicleCombo.addItem(new ExitSpaceOption(space));
        }

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.add(new JLabel("Choose the vehicle that is leaving:"));
        panel.add(vehicleCombo);

        int result = JOptionPane.showConfirmDialog(this,
                panel,
                "Parking exit",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        ExitSpaceOption selected = (ExitSpaceOption) vehicleCombo.getSelectedItem();
        return selected != null ? selected.getSpace() : null;
    }

    /**
     * Shows the assigned parking space after entry.
     *
     * @param message message prefix
     * @param space   assigned space
     */
    public void showAssignedParkingEntry(String message, ParkingSpace space) {
        showInfo("Parking entry",
                message
                        + "\nAssigned space: " + space.getId()
                        + "\nFloor: " + space.getFloor()
                        + "\nVehicle type: " + space.getVehicleType().name());
    }

}
