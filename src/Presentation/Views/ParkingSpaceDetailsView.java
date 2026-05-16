package Presentation.Views;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Presentation.Controllers.ParkingController;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

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

	/**
	 *  
	 */
	public ParkingSpaceDetailsView(JDialog frame, JLabel codeLabel, JLabel floorLabel,
			JLabel typeLabel, JLabel reservedUserLabel, JButton cancelReservationButton, JButton backButton,
			ParkingController controller) {
		this.frame = frame;
		this.codeLabel = codeLabel;
		this.floorLabel = floorLabel;
		this.typeLabel = typeLabel;
		this.reservedUserLabel = reservedUserLabel;
		this.cancelReservationButton = cancelReservationButton;
		this.backButton = backButton;
		this.controller = controller;
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
