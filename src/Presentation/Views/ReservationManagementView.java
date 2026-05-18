package Presentation.Views;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;

import Business.Entities.Reservation;
import Presentation.Controllers.ReservationController;

public class ReservationManagementView {
	private JFrame frame;
	private JTable reservationTable;
	private JButton cancelReservationButton;
	private JButton backButton;
	private ReservationController controller;

	/**
	 *  
	 */
	public ReservationManagementView(JFrame frame, JTable reservationTable,
			JButton cancelReservationButton, JButton backButton, ReservationController controller) {
		this.frame = frame;
		this.reservationTable = reservationTable;
		this.cancelReservationButton = cancelReservationButton;
		this.backButton = backButton;
		this.controller = controller;
	}

	public void updateReservations(List<Reservation> reservations) {

	}

	public String getSelectedReservationPlate() {
		return null;
	}

	public void setController(ReservationController controller) {

	}

}
