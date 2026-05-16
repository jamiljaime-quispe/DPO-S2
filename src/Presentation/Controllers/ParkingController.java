package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Services.ParkingService;
import Presentation.Views.MainMenuView;
import Presentation.Views.ParkingStatusView;
import Presentation.Views.EntryExitView;
import Presentation.Views.ParkingSpaceDetailsView;
import Business.Entities.VehicleType;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.util.List;

public class ParkingController {
	private ParkingStatusView parkingStatusView;
	private EntryExitView entryExitView;
	private ParkingService parkingService;
	private ParkingSpaceDetailsView parkingSpaceDetailsView;
	private MainMenuView mainMenuView;

	/**
	 *  
	 */
	public ParkingController(ParkingStatusView parkingStatusView, EntryExitView entryExitView,
			ParkingService parkingService) {
		this.parkingStatusView = parkingStatusView;
		this.entryExitView = entryExitView;
		this.parkingService = parkingService;
	}

	public void loadParkingStatus() {
		if (mainMenuView == null) return;

		mainMenuView.clearParkingSlotsTable();
		mainMenuView.showParkingSlotsTable();

		SwingWorker<Void, ParkingSpace> worker = new SwingWorker<Void, ParkingSpace>() {
			@Override
			protected Void doInBackground() {
				List<ParkingSpace> spaces = parkingService.getParkingStatus();

				for (ParkingSpace space : spaces) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}

					publish(space);
				}

				return null;
			}

			@Override
			protected void process(List<ParkingSpace> chunks) {
				for (ParkingSpace space : chunks) {
					mainMenuView.addParkingSpaceToTable(space);
				}
			}

			@Override
			protected void done() {
				System.out.println("Parking spaces loaded successfully.");
			}
		};

		worker.execute();
	}

	public void showSpaceDetails(String code) {
		if (mainMenuView == null) return;

		mainMenuView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		new SwingWorker<ParkingSpace, Void>() {
			@Override
			protected ParkingSpace doInBackground() {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return parkingService.findByCode(code);
			}

			@Override
			protected void done() {
				try {
					ParkingSpace space = get();
					if (space == null) {
						JOptionPane.showMessageDialog(mainMenuView,
								"Parking space not found: " + code,
								"Details unavailable",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					if (parkingSpaceDetailsView == null) {
						parkingSpaceDetailsView = new ParkingSpaceDetailsView(mainMenuView);
					}
					parkingSpaceDetailsView.displaySpaceDetails(space);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(mainMenuView,
							"Failed to load parking space details: " + e.getMessage(),
							"Details unavailable",
							JOptionPane.ERROR_MESSAGE);
				} finally {
					mainMenuView.setCursor(Cursor.getDefaultCursor());
				}
			}
		}.execute();

	}

	public void vehicleEntry(String plate, VehicleType type) {

	}

	public void vehicleExit(String plate) {

	}

	public void setMainMenuView(MainMenuView mainMenuView) {
		this.mainMenuView = mainMenuView;
		this.parkingSpaceDetailsView = new ParkingSpaceDetailsView(mainMenuView);
	}
}
