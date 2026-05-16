package Presentation.Views;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;

import Business.Entities.ParkingSpace;

public class ParkingStatusView {
	private JFrame frame;
	private JTable parkingTable;
	private JButton refreshButton;
	private JButton backButton;
	private ParkingController controller;

	/**
	 *  
	 */
	public ParkingStatusView(JFrame frame, JTable parkingTable,
			JButton refreshButton, JButton backButton, ParkingController controller) {
		this.frame = frame;
		this.parkingTable = parkingTable;
		this.refreshButton = refreshButton;
		this.backButton = backButton;
		this.controller = controller;
	}

	public void initComponents() {

	}

	public void updateTable(List<ParkingSpace> spaces) {

	}

	public String getSelectedSpaceCode() {
		return null;
	}

	public void setController(ParkingController controller) {

	}
}
