package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Presentation.Controllers.ReservationController;

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextField;

public class ReservationView {
    private JFrame frame;
    private JTextField vehiclePlateField;
    private JComboBox<VehicleType> vehicleTypeCombo;
    private JList<String> availableSpacesList;
    private JButton reserveButton;
    private JButton backButton;
    private ReservationController controller;

    /**
     *
     */
    public ReservationView(JFrame frame, JTextField vehiclePlateField,
                           JComboBox<VehicleType> vehicleTypeCombo, JList<String> availableSpacesList, JButton reserveButton,
                           JButton backButton, ReservationController controller) {
        this.frame = frame;
        this.vehiclePlateField = vehiclePlateField;
        this.vehicleTypeCombo = vehicleTypeCombo;
        this.availableSpacesList = availableSpacesList;
        this.reserveButton = reserveButton;
        this.backButton = backButton;
        this.controller = controller;
    }

    public void initComponents() {

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

    public JFrame getFrame() {
        return frame;
    }
}
