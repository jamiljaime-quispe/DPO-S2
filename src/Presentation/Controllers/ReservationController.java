package Presentation.Controllers;

import Business.Services.ReservationService;
import Presentation.Views.ReservationView;
import Presentation.Views.ReservationManagementView;
import Business.Entities.VehicleType;

public class ReservationController {
	private ReservationView reservationView;
	private ReservationManagementView reservationManagementView;
	private ReservationService reservationService;

	/**
	 *  
	 */
	public ReservationController(ReservationView reservationView,
			ReservationManagementView reservationManagementView, ReservationService reservationService) {
		this.reservationView = reservationView;
		this.reservationManagementView = reservationManagementView;
		this.reservationService = reservationService;
	}

	public void searchAvailableSpaces(VehicleType type) {

	}

	public void reserveSpace(String plate, VehicleType type, String spaceCode) {

	}

	public void cancelReservation(String plate) {

	}

	public void loadUserReservations(int userId) {

	}
}
