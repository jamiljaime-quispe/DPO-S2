package Presentation.Views;

import Business.Entities.VehicleType;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextField;

import Business.Entities.ParkingSpace;

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
		return null;
	}

	public VehicleType getVehicleType() {
		return null;
	}

	public void updateAvailableSpaces(List<ParkingSpace> spaces) {

	}

	public String getSelectedSpace() {
		return null;
	}

	public void setController(ReservationController controller) {

	}
}
