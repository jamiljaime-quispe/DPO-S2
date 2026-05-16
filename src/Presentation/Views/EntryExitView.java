package Presentation.Views;

import Business.Entities.VehicleType;
import Presentation.Controllers.ParkingController;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextField;

public class EntryExitView {
	private JFrame frame;
	private JTextField licensePlateField;
	private JComboBox<VehicleType> vehicleTypeCombo;
	private JButton entryButton;
	private JButton exitButton;
	private JButton backButton;
	private ParkingController controller;

	/**
	 *  
	 */
	public EntryExitView(JFrame frame, JTextField licencePlateField,
			JComboBox<VehicleType> vehicleTypeCombo, JButton entryButton, JButton exitButton,
			ParkingController controller) {
		this.frame = frame;
		this.licensePlateField = licencePlateField;
		this.vehicleTypeCombo = vehicleTypeCombo;
		this.entryButton = entryButton;
		this.exitButton = exitButton;
		this.controller = controller;
	}

	public String getLicensePlate() {
		return null;
	}

	public VehicleType getVehicleType() {
		return null;
	}

	public void setController(ParkingController controller) {

	}

	public void showAssignedSpace(String spaceCode) {

	}
}
