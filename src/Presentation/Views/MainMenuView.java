package Presentation.Views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import Business.Entities.ParkingSpace;
import Presentation.Controllers.MainController;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class MainMenuView extends JFrame {
    private MainController controller;

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

    private JPanel parkingSlotsPanel;
    private JTable parkingSlotsTable;
    private java.util.List<MouseListener> parkingSlotsTableMouseListeners = new java.util.ArrayList<>();

    private OccupancyChartView occupancyChartPanel;
    private JButton parkingSlotsBackButton;
    private java.util.List<ActionListener> parkingSlotsBackListeners = new java.util.ArrayList<>();

    public MainMenuView() {
        setTitle("Main screen");
    }

    public void setController(MainController controller) {
        this.controller = controller;
    }

    public void initComponents() {
        mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBackground(new Color(245, 247, 250));

        JPanel brand = new JPanel();
        brand.setLayout(null);
        brand.setBackground(new Color(33, 99, 168));
        brand.setBounds(0, 0, 360, 620);

        JLabel logo = new JLabel("P", SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 64));
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 90), 3, true));
        logo.setBounds(120, 120, 120, 110);
        brand.add(logo);

        JLabel brandTitle = new JLabel("Parking System", SwingConstants.CENTER);
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setBounds(20, 255, 320, 40);
        brand.add(brandTitle);

        brandSubtitleLabel = new JLabel("Your dashboard", SwingConstants.CENTER);
        brandSubtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        brandSubtitleLabel.setForeground(new Color(220, 230, 245));
        brandSubtitleLabel.setBounds(20, 300, 320, 30);
        brand.add(brandSubtitleLabel);

        mainPanel.add(brand);

        titleLabel = new JLabel("Welcome");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(40, 40, 50));
        titleLabel.setBounds(390, 35, 500, 40);
        mainPanel.add(titleLabel);

        navPanel = new JPanel();
        navPanel.setLayout(null);
        navPanel.setOpaque(false);
        navPanel.setBounds(390, 90, 510, 540);
        mainPanel.add(navPanel);

        statusButton = new JButton();
        reservationButton = new JButton();
        entryExitButton = new JButton();
        parkingEntryButton = new JButton();
        parkingExitButton = new JButton();
        occupancyChartButton = new JButton();
        logoutButton = new JButton();
        deleteAccountButton = new JButton();

        mgmtGroup = createGroupPanel("Parking management", 0, 0);
        mgmtGroup.add(styleButton(entryExitButton, "Manage parking slots"));
        mgmtGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        mgmtGroup.add(styleButton(reservationButton, "Manage slot booking"));
        mgmtGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        parkingEntryExitButtonRow = createEntryExitButtonRow();
        mgmtGroup.add(parkingEntryExitButtonRow);
        navPanel.add(mgmtGroup);

        visGroup = createGroupPanel("Parking visualization", 0, 200);
        visGroup.add(styleButton(occupancyChartButton, "Display last hour occupancy"));
        visGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        visGroup.add(styleButton(statusButton, "Display current parking status"));
        navPanel.add(visGroup);

        accountGroup = createGroupPanel("Account", 270, 0);
        accountGroup.add(styleButton(logoutButton, "Log out"));
        accountGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        styleButton(deleteAccountButton, "Delete Account");
        deleteAccountButton.setForeground(new Color(180, 30, 30));
        accountGroup.add(deleteAccountButton);
        navPanel.add(accountGroup);

        setContentPane(mainPanel);
        setSize(930, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        parkingSlotsPanel = createParkingSlotsPanel();
        parkingSlotsPanel.setVisible(false);
        mainPanel.add(parkingSlotsPanel);

        occupancyChartPanel = new OccupancyChartView();
        occupancyChartPanel.initComponents();
        occupancyChartPanel.setBounds(390, 90, 510, 540);
        occupancyChartPanel.setVisible(false);
        mainPanel.add(occupancyChartPanel);
    }

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
            parkingExitButton.setVisible(true);
            parkingExitButton.setEnabled(false);
            parkingEntryExitButtonRow.setVisible(true);
            reservationButton.setText("Manage my bookings");
            deleteAccountButton.setVisible(true);
        }

        showNavigation();

        if (controller != null)
            controller.setMode(mode);

        revalidate();
        repaint();
    }

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
        panel.add(header, BorderLayout.NORTH);

        for (ActionListener listener : parkingSlotsBackListeners) {
            parkingSlotsBackButton.addActionListener(listener);
        }

        String[] columns = {
                "Code",
                "Floor",
                "Status",
                "Reservation",
                "License plate",
                "My Parked Vehicle"
        };

        Object[][] rows = {};

        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        parkingSlotsTable = new JTable(model);
        parkingSlotsTable.setRowHeight(23);
        parkingSlotsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        parkingSlotsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        parkingSlotsTable.getTableHeader().setBackground(new Color(33, 99, 168));
        parkingSlotsTable.getTableHeader().setForeground(Color.WHITE);
        parkingSlotsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        parkingSlotsTable.removeColumn(parkingSlotsTable.getColumnModel().getColumn(5));

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

    private void showNavigation() {
        navPanel.setVisible(true);
        titleLabel.setVisible(true);
    }

    private void hideNavigation() {
        navPanel.setVisible(false);
    }

    public void showParkingSlotsTable() {
        hideNavigation();
        occupancyChartPanel.setVisible(false);
        parkingSlotsPanel.setVisible(true);
        parkingSlotsPanel.revalidate();
        parkingSlotsPanel.repaint();
    }

    public boolean isParkingSlotsTableVisible() {
        return parkingSlotsPanel != null && parkingSlotsPanel.isVisible();
    }

    public void addParkingSlotsBackListener(ActionListener listener) {
        parkingSlotsBackListeners.add(listener);
        if (parkingSlotsBackButton != null) {
            parkingSlotsBackButton.addActionListener(listener);
        }
    }

    public void addParkingSpaceToTable(ParkingSpace space) {
        addParkingSpaceToTable(space, false);
    }

    public void addParkingSpaceToTable(ParkingSpace space, boolean myParkedVehicle) {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        Object[] rowData = buildParkingSpaceRow(space, myParkedVehicle);
        int existingRow = findParkingSpaceRow(space.getId());

        if (existingRow == -1) {
            model.addRow(rowData);
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
    }

    private Object[] buildParkingSpaceRow(ParkingSpace space, boolean myParkedVehicle) {
        String licensePlate = "";

        if (space.getParkedVehicle() != null) {
            licensePlate = space.getParkedVehicle().getLicensePlate();
        } else if (space.getReservation() != null && space.getReservation().getVehicle() != null) {
            licensePlate = space.getReservation().getVehicle().getLicensePlate();
        }

        return new Object[] {
                space.getId(),
                space.getFloor(),
                space.isOccupied() ? "Occupied" : "Vacant",
                space.isReserved() ? "Reserved" : "Available",
                licensePlate,
                myParkedVehicle
        };
    }

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

    public void removeParkingSpacesNotIn(java.util.Set<String> visibleCodes) {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        for (int row = model.getRowCount() - 1; row >= 0; row--) {
            String currentCode = String.valueOf(model.getValueAt(row, 0));
            if (!visibleCodes.contains(currentCode)) {
                model.removeRow(row);
            }
        }
    }

    private class ParkingStatusCellRenderer extends DefaultTableCellRenderer {
        private static final Color VACANT_COLOR = new Color(232, 248, 238);
        private static final Color OCCUPIED_COLOR = new Color(253, 235, 235);
        private static final Color MY_PARKED_COLOR = new Color(225, 240, 255);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                int modelRow = table.convertRowIndexToModel(row);
                int modelColumn = table.convertColumnIndexToModel(column);
                boolean myParkedVehicle = Boolean.TRUE.equals(table.getModel().getValueAt(modelRow, 5));
                String status = String.valueOf(table.getModel().getValueAt(modelRow, 2));

                if (myParkedVehicle) {
                    cell.setBackground(MY_PARKED_COLOR);
                } else if (modelColumn == 2 && "Vacant".equals(status)) {
                    cell.setBackground(VACANT_COLOR);
                } else if (modelColumn == 2 && "Occupied".equals(status)) {
                    cell.setBackground(OCCUPIED_COLOR);
                } else {
                    cell.setBackground(Color.WHITE);
                }
            }

            return cell;
        }
    }

    public JButton getStatusButton() {
        return statusButton;
    }

    public JButton getReservationButton() {
        return reservationButton;
    }

    public JButton getEntryExitButton() {
        return entryExitButton;
    }

    public JButton getParkingEntryButton() {
        return parkingEntryButton;
    }

    public JButton getParkingExitButton() {
        return parkingExitButton;
    }

    public JButton getOccupancyChartButton() {
        return occupancyChartButton;
    }

    public JButton getLogoutButton() {
        return logoutButton;
    }

    public JButton getDeleteAccountButton() {
        return deleteAccountButton;
    }

    public void clearParkingSlotsTable() {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        model.setRowCount(0);
    }

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

    public void addParkingSlotsTableMouseListener(MouseListener listener) {
        parkingSlotsTableMouseListeners.add(listener);
        if (parkingSlotsTable != null) {
            parkingSlotsTable.addMouseListener(listener);
        }
    }

    public String getParkingSpaceCodeAtPoint(Point point) {
        int row = parkingSlotsTable.rowAtPoint(point);
        if (row < 0) return null;

        int modelRow = parkingSlotsTable.convertRowIndexToModel(row);
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        return String.valueOf(model.getValueAt(modelRow, 0));
    }

    public void resetDisplayedContent() {
        clearParkingSlotsTable();
        parkingSlotsPanel.setVisible(false);
        occupancyChartPanel.setVisible(false);
        showNavigation();
        revalidate();
        repaint();
    }

    public void showOccupancyChart() {
        hideNavigation();
        parkingSlotsPanel.setVisible(false);
        occupancyChartPanel.setVisible(true);
        occupancyChartPanel.revalidate();
        occupancyChartPanel.repaint();
    }

    public JButton getBackToMenuButton() {
        return occupancyChartPanel.getBackButton();
    }

    public OccupancyChartView getOccupancyChartView() {
        return occupancyChartPanel;
    }
}
