package Presentation.Views;

import Business.Entities.ParkingSpace;
import Presentation.Controllers.MainController;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MainMenuView extends JFrame {
    public static final int MODE_ADMIN = 1;
    public static final int MODE_CLIENT = 2;

    private MainController controller;

    // Title label
    private JLabel titleLabel;

    // Buttons are now internal members
    private JButton statusButton;
    private JButton reservationButton;
    private JButton entryExitButton;
    private JButton occupancyChartButton;
    private JButton logoutButton;
    private JButton deleteAccountButton;

    // Panels kept as members so showAdmin/UserOptions can toggle them
    private JPanel mgmtGroup;
    private JPanel visGroup;
    private JPanel accountGroup;

    // BUTTON 1: Parking slots panel and table
    private JPanel parkingSlotsPanel;
    private JTable parkingSlotsTable;

    // Occupancy chart panel
    private OccupancyChartView occupancyChartPanel;

    public MainMenuView() {
        // Sets the title of the window directly
        setTitle("Main screen");
    }

    public void setController(MainController controller) {
        this.controller = controller;
    }

    public void initComponents() {
        // Custom panel to draw the background image
        // Replace with your actual image path
        BackgroundPanel mainPanel = new BackgroundPanel("src/resources/parking_bg.jpg");
        mainPanel.setLayout(null);

        // 1. MUST INITIALIZE BUTTONS FIRST
        statusButton = new JButton();
        reservationButton = new JButton();
        entryExitButton = new JButton();
        occupancyChartButton = new JButton();
        logoutButton = new JButton();
        deleteAccountButton = new JButton();

        // Title
        titleLabel = new JLabel("Welcome - admin");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        titleLabel.setBounds(40, 30, 500, 50);
        mainPanel.add(titleLabel);

        // Parking Management Card
        mgmtGroup = createGroupPanel("Parking management", 40, 100);
        mgmtGroup.add(styleButton(entryExitButton, "Manage parking slots"));
        mgmtGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        mgmtGroup.add(styleButton(reservationButton, "Manage slot booking"));
        mainPanel.add(mgmtGroup);

        // Parking Visualization Card
        visGroup = createGroupPanel("Parking visualization", 40, 300);
        visGroup.add(styleButton(occupancyChartButton, "Display last hour occupancy"));
        visGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        visGroup.add(styleButton(statusButton, "Display current parking status"));
        mainPanel.add(visGroup);

        // Account Card (logout + delete account)
        accountGroup = createGroupPanel("Account", 40, 480);
        accountGroup.add(styleButton(logoutButton, "Log out"));
        accountGroup.add(Box.createRigidArea(new Dimension(0, 10)));
        styleButton(deleteAccountButton, "Delete Account");
        deleteAccountButton.setForeground(new Color(180, 30, 30));
        accountGroup.add(deleteAccountButton);
        mainPanel.add(accountGroup);

        // Apply settings directly to THIS class instead of a separate frame variable
        setContentPane(mainPanel);
        setSize(930, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Tables:

        // Table 1: Manage parking slots
        parkingSlotsPanel = createParkingSlotsPanel();
        parkingSlotsPanel.setVisible(false);
        mainPanel.add(parkingSlotsPanel);

        // Occupancy chart
        occupancyChartPanel = new OccupancyChartView();
        occupancyChartPanel.initComponents();
        occupancyChartPanel.setBounds(360, 90, 540, 440);
        occupancyChartPanel.setVisible(false);
        mainPanel.add(occupancyChartPanel);

    }

    /**
     * Called by AuthController to configure the UI before displaying it.
     * Mode 1 = Admin, Mode 2 = Regular User.
     */
    public void setMode(int mode, String username) {
        if (mode == MODE_ADMIN) {
            titleLabel.setText("Welcome - " + username);
            mgmtGroup.setVisible(true);
            entryExitButton.setVisible(true);
            reservationButton.setVisible(false);
            visGroup.setBounds(40, 300, 300, 160);
            accountGroup.setBounds(40, 480, 300, 160);
            deleteAccountButton.setVisible(false);
        } else if (mode == MODE_CLIENT) {
            titleLabel.setText("Welcome - " + username);
            mgmtGroup.setVisible(true);
            entryExitButton.setVisible(false);
            reservationButton.setVisible(true);
            visGroup.setBounds(40, 300, 300, 160);
            accountGroup.setBounds(40, 480, 300, 160);
            deleteAccountButton.setVisible(true);
        }

        if (controller != null)
            controller.setMode(mode);

        this.revalidate();
        this.repaint();
    }

    private JPanel createGroupPanel(String title, int x, int y) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(255, 255, 255, 180)); // Transparency
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBounds(x, y, 300, 160);

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        return panel;
    }

    private JButton styleButton(JButton btn, String text) {
        btn.setText(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(260, 40));
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    // Inner class for background
    class BackgroundPanel extends JPanel {
        private Image img;

        public BackgroundPanel(String path) {
            this.img = new ImageIcon(path).getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private JPanel createParkingSlotsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(255, 255, 255, 190));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Adjust these numbers depending on where you want the box
        panel.setBounds(370, 150, 520, 300);

        JLabel title = new JLabel("Manage parking slots");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {
                "Code",
                "Floor",
                "Status",
                "Reservation",
                "License plate"
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
        parkingSlotsTable.getTableHeader().setBackground(new Color(60, 60, 60));
        parkingSlotsTable.getTableHeader().setForeground(Color.WHITE);
        parkingSlotsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(parkingSlotsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    public void showParkingSlotsTable() {
        occupancyChartPanel.setVisible(false);
        parkingSlotsPanel.setVisible(true);
        parkingSlotsPanel.revalidate();
        parkingSlotsPanel.repaint();
    }

    public void addParkingSpaceToTable(ParkingSpace space) {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();

        String licensePlate = "";

        if (space.getParkedVehicle() != null) {
            licensePlate = space.getParkedVehicle().getLicensePlate();
        } else if (space.getReservation() != null && space.getReservation().getVehicle() != null) {
            licensePlate = space.getReservation().getVehicle().getLicensePlate();
        }

        model.addRow(new Object[] {
                space.getId(),
                space.getFloor(),
                space.isOccupied() ? "Occupied" : "Vacant",
                space.isReserved() ? "Reserved" : "Available",
                licensePlate
        });
    }

    // ==========================================
    // GETTERS FOR THE CONTROLLER
    // ==========================================

    public JButton getStatusButton() {
        return statusButton;
    }

    public JButton getReservationButton() {
        return reservationButton;
    }

    public JButton getEntryExitButton() {
        return entryExitButton;
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

    public JTable getParkingSlotsTable() {
        return parkingSlotsTable;
    }

    public void clearParkingSlotsTable() {
        DefaultTableModel model = (DefaultTableModel) parkingSlotsTable.getModel();
        model.setRowCount(0);
    }

    public void showOccupancyChart() {
        parkingSlotsPanel.setVisible(false);
        occupancyChartPanel.setVisible(true);
        occupancyChartPanel.revalidate();
        occupancyChartPanel.repaint();
    }

    public OccupancyChartView getOccupancyChartView() {
        return occupancyChartPanel;
    }

    /**
     * Clears embedded tables and hides secondary panels so the next login starts clean.
     */
    public void resetDisplayedContent() {
        if (parkingSlotsPanel != null) {
            parkingSlotsPanel.setVisible(false);
        }
        if (occupancyChartPanel != null) {
            occupancyChartPanel.setVisible(false);
        }
        clearParkingSlotsTable();
    }
}
