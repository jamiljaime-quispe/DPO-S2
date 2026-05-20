package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;

public class ParkingSpaceDetailsView extends JDialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private JLabel codeValue;
    private JLabel floorValue;
    private JLabel typeValue;
    private JLabel statusValue;
    private JLabel reservationValue;
    private JLabel licensePlateValue;
    private JLabel reservedUserValue;
    private JLabel reservedEmailValue;
    private JLabel reservationDateValue;
    private JButton cancelReservationButton;
    private JButton closeButton;
    private ParkingSpace displayedSpace;

    public ParkingSpaceDetailsView(Frame parent) {
        super(parent, "Parking Space Details", true);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 247, 250));
        setSize(440, 350);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new GridLayout(9, 2, 10, 8));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 240), 1, true),
                BorderFactory.createEmptyBorder(18, 20, 10, 20)));

        codeValue = new JLabel();
        floorValue = new JLabel();
        typeValue = new JLabel();
        statusValue = new JLabel();
        reservationValue = new JLabel();
        licensePlateValue = new JLabel();
        reservedUserValue = new JLabel();
        reservedEmailValue = new JLabel();
        reservationDateValue = new JLabel();

        addRow(content, "Code:", codeValue);
        addRow(content, "Floor:", floorValue);
        addRow(content, "Type:", typeValue);
        addRow(content, "Status:", statusValue);
        addRow(content, "Reservation:", reservationValue);
        addRow(content, "License Plate:", licensePlateValue);
        addRow(content, "Booked By:", reservedUserValue);
        addRow(content, "User Email:", reservedEmailValue);
        addRow(content, "Booked At:", reservationDateValue);

        cancelReservationButton = new JButton("Cancel Reservation");
        cancelReservationButton.setForeground(new Color(180, 30, 30));
        cancelReservationButton.setVisible(false);

        closeButton = new JButton("Close");
        stylePrimaryButton(closeButton);
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelReservationButton);
        buttonPanel.add(closeButton);

        add(content, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, String label, JLabel value) {
        JLabel name = new JLabel(label);
        name.setFont(new Font("SansSerif", Font.BOLD, 13));
        name.setForeground(new Color(40, 40, 50));
        value.setFont(new Font("SansSerif", Font.PLAIN, 13));
        value.setForeground(new Color(40, 40, 50));
        panel.add(name);
        panel.add(value);
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

    public void displaySpaceDetails(ParkingSpace space) {
        updateSpaceDetails(space);
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    public void updateSpaceDetails(ParkingSpace space) {
        displayedSpace = space;
        Reservation reservation = space.getReservation();

        codeValue.setText(space.getId());
        floorValue.setText(String.valueOf(space.getFloor()));
        typeValue.setText(space.getVehicleType().name());
        statusValue.setText(space.isOccupied() ? "Occupied" : "Vacant");
        reservationValue.setText(space.isReserved() ? "Reserved" : "Available");
        licensePlateValue.setText(resolveLicensePlate(space));

        if (reservation != null && reservation.isActive()) {
            User user = reservation.getUser();
            reservedUserValue.setText(user != null ? user.getUsername() + " (ID " + user.getId() + ")" : "Unknown");
            reservedEmailValue.setText(user != null ? user.getEmail() : "Unknown");
            reservationDateValue.setText(reservation.getReservationDate() != null
                    ? reservation.getReservationDate().format(DATE_FORMAT)
                    : "Unknown");
            cancelReservationButton.setVisible(true);
            cancelReservationButton.setEnabled(true);
        } else {
            reservedUserValue.setText("No active reservation");
            reservedEmailValue.setText("-");
            reservationDateValue.setText("-");
            cancelReservationButton.setVisible(false);
            cancelReservationButton.setEnabled(false);
        }
    }

    public String getDisplayedSpaceCode() {
        if (displayedSpace == null) return "";
        return displayedSpace.getId();
    }

    public String getDisplayedReservationPlate() {
        if (displayedSpace == null
                || displayedSpace.getReservation() == null
                || displayedSpace.getReservation().getVehicle() == null) {
            return "";
        }
        return displayedSpace.getReservation().getVehicle().getLicensePlate();
    }

    public void setCancelReservationListener(ActionListener listener) {
        cancelReservationButton.addActionListener(listener);
    }

    public void setLoading(boolean loading) {
        setCursor(Cursor.getPredefinedCursor(loading ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        cancelReservationButton.setEnabled(!loading);
        closeButton.setEnabled(!loading);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private String resolveLicensePlate(ParkingSpace space) {
        if (space.getParkedVehicle() != null) {
            return space.getParkedVehicle().getLicensePlate();
        }
        if (space.getReservation() != null && space.getReservation().getVehicle() != null) {
            return space.getReservation().getVehicle().getLicensePlate();
        }
        return "-";
    }
}
