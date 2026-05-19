package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Presentation.Controllers.ReservationController;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.Frame;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

public class ReservationView {
    private Frame parent;
    private JDialog reservationDialog;
    private JTextField vehiclePlateField;
    private JComboBox<VehicleType> vehicleTypeCombo;
    private JList<String> availableSpacesList;
    private JButton reserveButton;
    private JButton backButton;
    private ReservationController controller;

    public ReservationView(Frame parent) {
        this.parent = parent;
    }

    public void initComponents(ReservationManagementView managementView) {
        vehiclePlateField = new JTextField(12);
        vehicleTypeCombo = new JComboBox<>(VehicleType.values());
        availableSpacesList = new JList<>(new DefaultListModel<>());
        reserveButton = new JButton("Reserve");
        backButton = new JButton("Close");

        managementView.initComponents();

        JPanel bookPanel = new JPanel(new BorderLayout(8, 8));
        JPanel bookNorth = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bookNorth.add(new JLabel("License plate:"));
        bookNorth.add(vehiclePlateField);
        bookNorth.add(new JLabel("Vehicle type:"));
        bookNorth.add(vehicleTypeCombo);
        bookPanel.add(bookNorth, BorderLayout.NORTH);
        bookPanel.add(new JScrollPane(availableSpacesList), BorderLayout.CENTER);
        JPanel bookSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bookSouth.add(reserveButton);
        bookSouth.add(backButton);
        bookPanel.add(bookSouth, BorderLayout.SOUTH);

        JTabbedPane reservationTabs = new JTabbedPane();
        reservationTabs.addTab("Book space", bookPanel);
        reservationTabs.addTab("My reservations", managementView.getContentPanel());

        reservationDialog = new JDialog(parent, "Reservations", false);
        reservationDialog.setContentPane(reservationTabs);
        reservationDialog.setSize(560, 420);
        reservationDialog.setLocationRelativeTo(parent);

        JButton mgmtCloseBtn = managementView.getBackButton();
        if (mgmtCloseBtn != null) {
            mgmtCloseBtn.addActionListener(ev -> reservationDialog.setVisible(false));
        }
        backButton.addActionListener(ev -> reservationDialog.setVisible(false));
    }

    public String getVehiclePlate() {
        if (vehiclePlateField == null)
            return "";
        return vehiclePlateField.getText() != null ? vehiclePlateField.getText().trim() : "";
    }

    public VehicleType getVehicleType() {
        if (vehicleTypeCombo == null || vehicleTypeCombo.getSelectedItem() == null)
            return VehicleType.CAR;
        return (VehicleType) vehicleTypeCombo.getSelectedItem();
    }

    public void updateAvailableSpaces(List<ParkingSpace> spaces) {
        if (availableSpacesList == null)
            return;
        DefaultListModel<String> model = new DefaultListModel<>();
        if (spaces != null) {
            for (ParkingSpace s : spaces) {
                model.addElement(s.getId() + "  (floor " + s.getFloor() + ", " + s.getVehicleType() + ")");
            }
        }
        availableSpacesList.setModel(model);
    }

    public String getSelectedSpace() {
        if (availableSpacesList == null)
            return null;
        String sel = availableSpacesList.getSelectedValue();
        if (sel == null || sel.isEmpty())
            return null;
        int cut = sel.indexOf("  (");
        if (cut < 0)
            return sel.trim();
        return sel.substring(0, cut).trim();
    }

    public void setController(ReservationController controller) {
        this.controller = controller;
    }

    public JComboBox<VehicleType> getVehicleTypeCombo() {
        return vehicleTypeCombo;
    }

    public JButton getReserveButton() {
        return reserveButton;
    }

    public JDialog getDialog() {
        return reservationDialog;
    }

    /** Parent for JOptionPane dialogs (dialog when open, otherwise main window). */
    public Component getFrame() {
        if (reservationDialog != null && reservationDialog.isShowing()) {
            return reservationDialog;
        }
        return parent;
    }
}
