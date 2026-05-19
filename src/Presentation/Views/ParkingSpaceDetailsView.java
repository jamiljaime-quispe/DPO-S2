package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Presentation.Controllers.ParkingController;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ParkingSpaceDetailsView {
	private JDialog frame;
	private JLabel codeLabel;
	private JLabel floorLabel;
	private JLabel typeLabel;
	private JLabel reservedUserLabel;
	private JButton cancelReservationButton;
	private JButton backButton;
	private ParkingController controller;
	private int currentReservationId;

	public ParkingSpaceDetailsView(Frame parent) {
		frame = new JDialog(parent, "Parking space details", false);
		initComponents();
	}

	private void initComponents() {
		JPanel detailsPanel = new JPanel(new GridLayout(0, 2, 8, 8));
		detailsPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		codeLabel = new JLabel();
		floorLabel = new JLabel();
		typeLabel = new JLabel();
		reservedUserLabel = new JLabel();
		detailsPanel.add(new JLabel("Code:"));
		detailsPanel.add(codeLabel);
		detailsPanel.add(new JLabel("Floor:"));
		detailsPanel.add(floorLabel);
		detailsPanel.add(new JLabel("Vehicle type:"));
		detailsPanel.add(typeLabel);
		detailsPanel.add(new JLabel("Reserved by:"));
		detailsPanel.add(reservedUserLabel);

		cancelReservationButton = new JButton("Cancel reservation");
		backButton = new JButton("Back");
		JPanel detailsButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		detailsButtons.add(cancelReservationButton);
		detailsButtons.add(backButton);

		JPanel root = new JPanel(new BorderLayout());
		root.add(detailsPanel, BorderLayout.CENTER);
		root.add(detailsButtons, BorderLayout.SOUTH);

		frame.setContentPane(root);
		frame.setSize(420, 220);
		frame.setLocationRelativeTo(frame.getParent());
	}

	public void displaySpaceDetails(ParkingSpace space) {
		if (space == null)
			return;

		codeLabel.setText(space.getId() != null ? space.getId() : "");
		floorLabel.setText(String.valueOf(space.getFloor()));
		typeLabel.setText(space.getVehicleType() != null ? space.getVehicleType().name() : "");

		Reservation reservation = space.getReservation();
		boolean hasActiveReservation = reservation != null && reservation.isActive();
		currentReservationId = hasActiveReservation ? reservation.getId() : 0;

		if (hasActiveReservation && reservation.getUser() != null) {
			reservedUserLabel.setText(reservation.getUser().getUsername()
					+ " (" + reservation.getUser().getEmail() + ")");
		} else if (hasActiveReservation && reservation.getVehicle() != null) {
			reservedUserLabel.setText(reservation.getVehicle().getOwner());
		} else {
			reservedUserLabel.setText("—");
		}

		cancelReservationButton.setVisible(hasActiveReservation);
		cancelReservationButton.setEnabled(hasActiveReservation);

		if (frame != null) {
			frame.setVisible(true);
		}
	}

	public void setController(ParkingController controller) {
		this.controller = controller;
	}

	public int getCurrentReservationId() {
		return currentReservationId;
	}

	public JButton getCancelReservationButton() {
		return cancelReservationButton;
	}

	public JButton getBackButton() {
		return backButton;
	}

	public JDialog getFrame() {
		return frame;
	}
}
