package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Presentation.Views.EntryExitView;
import Presentation.Views.ParkingSpaceDetailsView;
import Presentation.Views.ParkingStatusView;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class ParkingController {
	private ParkingStatusView parkingStatusView;
	private EntryExitView entryExitView;
	private ParkingService parkingService;
	private ParkingSpaceDetailsView parkingSpaceDetailsView;
	private AdminService adminService;
	MainController mainController;
	private int selectedReservationId;

	/**
	 *  
	 */
	public ParkingController(ParkingStatusView parkingStatusView, EntryExitView entryExitView,
			ParkingService parkingService, AdminService adminService,
			ParkingSpaceDetailsView parkingSpaceDetailsView) {
		this.parkingStatusView = parkingStatusView;
		this.entryExitView = entryExitView;
		this.parkingService = parkingService;
		this.adminService = adminService;
		this.parkingSpaceDetailsView = parkingSpaceDetailsView;

		if (parkingStatusView != null) {
			parkingStatusView.setController(this);
		}
		if (parkingSpaceDetailsView != null) {
			parkingSpaceDetailsView.setController(this);
			if (parkingSpaceDetailsView.getCancelReservationButton() != null) {
				parkingSpaceDetailsView.getCancelReservationButton().addActionListener(e -> {
					int reservationId = selectedReservationId > 0
							? selectedReservationId
							: parkingSpaceDetailsView.getCurrentReservationId();
					if (reservationId <= 0 || adminService == null)
						return;
					int confirm = JOptionPane.showConfirmDialog(parkingSpaceDetailsView.getFrame(),
							"Cancel this user's reservation on this space?",
							"Cancel reservation",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE);
					if (confirm != JOptionPane.YES_OPTION)
						return;
					adminService.cancelReservation(reservationId);
					selectedReservationId = 0;
					JOptionPane.showMessageDialog(parkingSpaceDetailsView.getFrame(),
							"Reservation cancelled. The user will be notified at next login.",
							"Done",
							JOptionPane.INFORMATION_MESSAGE);
					if (parkingSpaceDetailsView.getFrame() != null)
						parkingSpaceDetailsView.getFrame().setVisible(false);
					if (mainController != null) {
						mainController.executeParkingSpace();
					}
				});
			}
			if (parkingSpaceDetailsView.getBackButton() != null) {
				parkingSpaceDetailsView.getBackButton().addActionListener(e -> {
					if (parkingSpaceDetailsView.getFrame() != null)
						parkingSpaceDetailsView.getFrame().setVisible(false);
				});
			}
		}
	}

	public void loadParkingStatus() {
		new SwingWorker<java.util.List<ParkingSpace>, Void>() {
			@Override
			protected java.util.List<ParkingSpace> doInBackground() {
				if (adminService != null)
					return adminService.getFullParkingStatus();
				return parkingService.getAllSpaces();
			}

			@Override
			protected void done() {
				try {
					java.util.List<ParkingSpace> spaces = get();
					if (parkingStatusView != null) {
						parkingStatusView.updateTable(spaces);
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(null,
							"Failed to load parking status: " + ex.getMessage(),
							"Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	public void showSpaceDetails(String code) {
		if (code == null || code.isBlank() || parkingService == null)
			return;
		ParkingSpace space = parkingService.findByCode(code.trim());
		if (space == null) {
			JOptionPane.showMessageDialog(parkingSpaceDetailsView != null ? parkingSpaceDetailsView.getFrame() : null,
					"Space not found: " + code,
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		Reservation reservation = space.getReservation();
		selectedReservationId = (reservation != null && reservation.isActive()) ? reservation.getId() : 0;
		if (parkingSpaceDetailsView != null) {
			parkingSpaceDetailsView.displaySpaceDetails(space);
		}
	}

	public void vehicleEntry(String plate, VehicleType type) {

	}

	public void vehicleExit(String plate) {

	}
}
