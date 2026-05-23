package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Listeners.ParkingStatusChangeListener;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.UserService;
import Presentation.Views.MainMenuView;
import Presentation.Views.ParkingStatusView;
import Presentation.Views.EntryExitView;
import Presentation.Views.ParkingSpaceDetailsView;
import Business.Entities.VehicleType;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Controller for parking status, vehicle entry/exit, and space details.
 * Implements {@link ParkingStatusChangeListener} to react to simulation events.
 */
public class ParkingController implements ParkingStatusChangeListener {

	private ParkingStatusView parkingStatusView;
	private EntryExitView entryExitView;
	private ParkingService parkingService;
	private ParkingSpaceDetailsView parkingSpaceDetailsView;
	private MainMenuView mainMenuView;
	private UserService userService;
	private AdminService adminService;
	private AdminController adminController;
	private AdminSlotBookingController slotBookingController;
	private boolean exitAllowed;
	private volatile int parkingStatusLoadId;

	@Override
	public void parkingStatusChanged() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (mainMenuView != null && mainMenuView.isParkingSlotsTableVisible()) {
					loadParkingStatus();
				}
				refreshSpaceDetailsIfVisible();
				if (adminController != null) {
					adminController.refreshIfVisible();
				}
				if (slotBookingController != null) {
					slotBookingController.refreshIfVisible();
				}
			}
		});
	}

	private static class ExitOption {
		private ParkingSpace space;

		private ExitOption(ParkingSpace space) {
			this.space = space;
		}

		private String getPlate() {
			return space.getParkedVehicle().getLicensePlate();
		}

		@Override
		public String toString() {
			return "Plate " + getPlate() + " - Space " + space.getId() + " - Floor " + space.getFloor();
		}
	}

	private static class StatusRow {
		private ParkingSpace space;
		private boolean userParkedVehicle;

		private StatusRow(ParkingSpace space, boolean userParkedVehicle) {
			this.space = space;
			this.userParkedVehicle = userParkedVehicle;
		}
	}

	private enum EntryStatus {
		NEEDS_VEHICLE_TYPE,
		ASSIGNED_FROM_RESERVATION,
		ASSIGNED_WITHOUT_RESERVATION,
		ALREADY_PARKED,
		NO_SPACE,
		ERROR
	}

	private static class EntryResult {
		private final EntryStatus status;
		private final ParkingSpace space;
		private final String message;

		private EntryResult(EntryStatus status, ParkingSpace space, String message) {
			this.status = status;
			this.space = space;
			this.message = message;
		}

		private static EntryResult needsVehicleType() {
			return new EntryResult(EntryStatus.NEEDS_VEHICLE_TYPE, null, null);
		}

		private static EntryResult assignedFromReservation(ParkingSpace space) {
			return new EntryResult(EntryStatus.ASSIGNED_FROM_RESERVATION, space, null);
		}

		private static EntryResult assignedWithoutReservation(ParkingSpace space) {
			return new EntryResult(EntryStatus.ASSIGNED_WITHOUT_RESERVATION, space, null);
		}

		private static EntryResult alreadyParked(ParkingSpace space) {
			return new EntryResult(EntryStatus.ALREADY_PARKED, space, null);
		}

		private static EntryResult noSpace(VehicleType type) {
			return new EntryResult(EntryStatus.NO_SPACE, null,
					"No vacant unreserved " + type.name() + " spaces are available.");
		}

		private static EntryResult error(String message) {
			return new EntryResult(EntryStatus.ERROR, null, message);
		}
	}

	/**
	 * Constructs the controller.
	 *
	 * @param parkingStatusView the parking status view (may be null)
	 * @param entryExitView     the entry/exit view (may be null)
	 * @param parkingService    the parking service
	 */
	public ParkingController(ParkingStatusView parkingStatusView, EntryExitView entryExitView,
			ParkingService parkingService) {
		this.parkingStatusView = parkingStatusView;
		this.entryExitView = entryExitView;
		this.parkingService = parkingService;
	}

	/**
	 * Loads all parking spaces and updates the main menu table.
	 * Uses a generation counter to discard results from stale background tasks.
	 */
	public void loadParkingStatus() {
		if (mainMenuView == null) return;

		parkingStatusLoadId++;
		int loadId = parkingStatusLoadId;

		mainMenuView.showParkingSlotsTable();

		SwingWorker<Set<String>, StatusRow> worker = new SwingWorker<Set<String>, StatusRow>() {
			@Override
			protected Set<String> doInBackground() {
				List<ParkingSpace> spaces = parkingService.getParkingStatus();
				Set<String> userParkedCodes = new HashSet<>();
				Set<String> loadedCodes = new HashSet<>();

				if (userService != null && userService.getLastLoggedInUserId() > 0) {
					List<ParkingSpace> userParkedSpaces = parkingService.getParkedSpacesByUser(
							userService.getLastLoggedInUserId());
					for (ParkingSpace parkedSpace : userParkedSpaces) {
						userParkedCodes.add(parkedSpace.getId());
					}
				}

				for (ParkingSpace space : spaces) {
					loadedCodes.add(space.getId());
					if (loadId == parkingStatusLoadId) {
						publish(new StatusRow(space, userParkedCodes.contains(space.getId())));
					}
				}

				return loadedCodes;
			}

			@Override
			protected void process(List<StatusRow> chunks) {
				if (loadId != parkingStatusLoadId) return;

				for (StatusRow row : chunks) {
					mainMenuView.addParkingSpaceToTable(row.space, row.userParkedVehicle);
				}
			}

			@Override
			protected void done() {
				if (loadId != parkingStatusLoadId) return;

				try {
					Set<String> loadedCodes = get();
					mainMenuView.removeParkingSpacesNotIn(loadedCodes);
					System.out.println("Parking spaces loaded successfully.");
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(mainMenuView,
							"Failed to load parking spaces: " + e.getMessage(),
							"Parking status",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		worker.execute();
	}

	/**
	 * Loads and displays the details dialog for the given parking space code.
	 *
	 * @param code the parking space code
	 */
	public void showSpaceDetails(String code) {
		if (mainMenuView == null) return;

		mainMenuView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		new SwingWorker<ParkingSpace, Void>() {
			@Override
			protected ParkingSpace doInBackground() {
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
						parkingSpaceDetailsView.setCancelReservationListener(e -> cancelReservationFromDetails());
					}
					parkingSpaceDetailsView.displaySpaceDetails(space);
				} catch (InterruptedException | ExecutionException e) {
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

	private void refreshSpaceDetailsIfVisible() {
		if (parkingSpaceDetailsView == null || !parkingSpaceDetailsView.isVisible()) return;

		String code = parkingSpaceDetailsView.getDisplayedSpaceCode();
		if (code == null || code.isBlank()) return;

		new SwingWorker<ParkingSpace, Void>() {
			@Override
			protected ParkingSpace doInBackground() {
				return parkingService.findByCode(code);
			}

			@Override
			protected void done() {
				if (parkingSpaceDetailsView == null || !parkingSpaceDetailsView.isVisible()) return;

				try {
					ParkingSpace space = get();
					if (space == null) {
						parkingSpaceDetailsView.dispose();
						JOptionPane.showMessageDialog(mainMenuView,
								"Parking space \"" + code + "\" is no longer available.",
								"Parking space updated",
								JOptionPane.INFORMATION_MESSAGE);
						return;
					}

					parkingSpaceDetailsView.updateSpaceDetails(space);
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(mainMenuView,
							"Failed to refresh parking space details: " + e.getMessage(),
							"Parking space updated",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	/** Opens the vehicle entry dialog. Prompts for a license plate, then processes entry. */
	public void showVehicleEntryDialog() {
		if (mainMenuView == null) return;

		String plate = promptForLicensePlate();
		if (plate == null) return;

		checkReservationAndEnter(plate);
	}

	private String promptForLicensePlate() {
		while (true) {
			JTextField plateField = new JTextField(18);
			JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
			panel.add(new JLabel("License plate"));
			panel.add(plateField);

			int result = JOptionPane.showConfirmDialog(mainMenuView,
					panel,
					"Parking entry",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);

			if (result != JOptionPane.OK_OPTION) {
				return null;
			}

			String plate = normalizeLicensePlate(plateField.getText());
			if (!plate.isEmpty()) {
				return plate;
			}

			JOptionPane.showMessageDialog(mainMenuView,
					"License plate cannot be empty.",
					"Parking entry",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void checkReservationAndEnter(String plate) {
		setParkingEntryLoading(true);

		new SwingWorker<EntryResult, Void>() {
			@Override
			protected EntryResult doInBackground() throws Exception {
				ParkingSpace alreadyParked = parkingService.findOccupiedSpaceByPlate(plate);
				if (alreadyParked != null) {
					return EntryResult.alreadyParked(alreadyParked);
				}

				Reservation reservation = parkingService.findActiveReservationByPlate(plate);
				if (reservation == null) {
					return EntryResult.needsVehicleType();
				}

				ParkingSpace reservedSpace = reservation.getParkingSpace();
				if (reservedSpace != null && reservedSpace.isOccupied()) {
					return EntryResult.error("The reserved space is already occupied.");
				}

				ParkingSpace assignedSpace = parkingService.handleVehicleEntry(
						plate,
						resolveReservationVehicleType(reservation));
				if (assignedSpace == null) {
					return EntryResult.error("Could not occupy the reserved parking space.");
				}

				return EntryResult.assignedFromReservation(assignedSpace);
			}

			@Override
			protected void done() {
				setParkingEntryLoading(false);
				try {
					handleEntryResult(plate, get());
				} catch (InterruptedException | ExecutionException e) {
					showEntryError("Failed to process parking entry: " + e.getMessage());
				}
			}
		}.execute();
	}

	private void enterWithoutReservation(String plate, VehicleType type) {
		setParkingEntryLoading(true);

		new SwingWorker<EntryResult, Void>() {
			@Override
			protected EntryResult doInBackground() throws Exception {
				ParkingSpace alreadyParked = parkingService.findOccupiedSpaceByPlate(plate);
				if (alreadyParked != null) {
					return EntryResult.alreadyParked(alreadyParked);
				}

				ParkingSpace assignedSpace = parkingService.handleUserVehicleEntry(
						userService.getLastLoggedInUserId(),
						plate,
						type);
				if (assignedSpace == null) {
					return EntryResult.noSpace(type);
				}

				return EntryResult.assignedWithoutReservation(assignedSpace);
			}

			@Override
			protected void done() {
				setParkingEntryLoading(false);
				try {
					handleEntryResult(plate, get());
				} catch (InterruptedException | ExecutionException e) {
					showEntryError("Failed to process parking entry: " + e.getMessage());
				}
			}
		}.execute();
	}

	private void handleEntryResult(String plate, EntryResult result) {
		if (result.status == EntryStatus.NEEDS_VEHICLE_TYPE) {
			VehicleType type = promptForVehicleType(plate);
			if (type != null) {
				enterWithoutReservation(plate, type);
			}
			return;
		}

		if (result.status == EntryStatus.ASSIGNED_FROM_RESERVATION) {
			showAssignedSpace("Access granted using your reservation.", result.space);
			return;
		}

		if (result.status == EntryStatus.ASSIGNED_WITHOUT_RESERVATION) {
			showAssignedSpace("Access granted.", result.space);
			return;
		}

		if (result.status == EntryStatus.ALREADY_PARKED) {
			showEntryError("Vehicle " + plate + " is already parked in space "
					+ result.space.getId() + ".");
			return;
		}

		showEntryError(result.message);
	}

	private VehicleType promptForVehicleType(String plate) {
		JComboBox<VehicleType> typeCombo = new JComboBox<>(VehicleType.values());
		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
		panel.add(new JLabel("No active reservation was found for " + plate + "."));
		panel.add(new JLabel("Select the vehicle type:"));
		panel.add(typeCombo);

		int result = JOptionPane.showConfirmDialog(mainMenuView,
				panel,
				"Parking entry",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (result != JOptionPane.OK_OPTION) {
			return null;
		}

		return (VehicleType) typeCombo.getSelectedItem();
	}

	private void showAssignedSpace(String message, ParkingSpace space) {
		JOptionPane.showMessageDialog(mainMenuView,
				message
						+ "\nAssigned space: " + space.getId()
						+ "\nFloor: " + space.getFloor()
						+ "\nVehicle type: " + space.getVehicleType().name(),
				"Parking entry",
				JOptionPane.INFORMATION_MESSAGE);
		refreshExitButtonState();
	}

	private void showEntryError(String message) {
		JOptionPane.showMessageDialog(mainMenuView,
				message,
				"Parking entry",
				JOptionPane.ERROR_MESSAGE);
	}

	private VehicleType resolveReservationVehicleType(Reservation reservation) {
		if (reservation.getVehicle() != null && reservation.getVehicle().getType() != null) {
			return reservation.getVehicle().getType();
		}

		if (reservation.getParkingSpace() != null && reservation.getParkingSpace().getVehicleType() != null) {
			return reservation.getParkingSpace().getVehicleType();
		}

		return VehicleType.CAR;
	}

	private String normalizeLicensePlate(String plate) {
		if (plate == null) return "";
		return plate.trim().toUpperCase();
	}

	private void setParkingEntryLoading(boolean loading) {
		if (mainMenuView == null) return;

		mainMenuView.getParkingEntryButton().setEnabled(!loading);
		mainMenuView.getParkingExitButton().setEnabled(!loading && exitAllowed);
		mainMenuView.setCursor(loading
				? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
				: Cursor.getDefaultCursor());
	}

	/** Checks whether the current user has any parked vehicles and updates the Exit button state. */
	public void refreshExitButtonState() {
		if (mainMenuView == null || userService == null) return;

		mainMenuView.getParkingExitButton().setEnabled(false);
		new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() {
				List<ParkingSpace> parkedSpaces = parkingService.getParkedSpacesByUser(
						userService.getLastLoggedInUserId());
				return !parkedSpaces.isEmpty();
			}

			@Override
			protected void done() {
				try {
					exitAllowed = get();
				} catch (InterruptedException | ExecutionException e) {
					exitAllowed = false;
				}

				mainMenuView.getParkingExitButton().setEnabled(exitAllowed);
			}
		}.execute();
	}

	/** Opens the vehicle exit dialog, listing the user's currently parked vehicles. */
	public void showVehicleExitDialog() {
		if (mainMenuView == null || userService == null) return;

		setExitLoading(true);
		new SwingWorker<List<ParkingSpace>, Void>() {
			@Override
			protected List<ParkingSpace> doInBackground() throws Exception {
				return parkingService.getParkedSpacesByUser(userService.getLastLoggedInUserId());
			}

			@Override
			protected void done() {
				setExitLoading(false);
				try {
					List<ParkingSpace> parkedSpaces = get();
					if (parkedSpaces == null || parkedSpaces.isEmpty()) {
						JOptionPane.showMessageDialog(mainMenuView,
								"You do not have any parked vehicles right now.",
								"Parking exit",
								JOptionPane.INFORMATION_MESSAGE);
						refreshExitButtonState();
						return;
					}

					ExitOption selectedOption = promptForExitVehicle(parkedSpaces);
					if (selectedOption != null) {
						exitWithVehicle(selectedOption);
					}
				} catch (InterruptedException | ExecutionException e) {
					Throwable cause = e.getCause();
					showExitError(cause != null ? cause.getMessage() : "Failed to load parked vehicles.");
					refreshExitButtonState();
				}
			}
		}.execute();
	}

	private ExitOption promptForExitVehicle(List<ParkingSpace> parkedSpaces) {
		JComboBox<ExitOption> vehicleCombo = new JComboBox<>();
		for (ParkingSpace space : parkedSpaces) {
			vehicleCombo.addItem(new ExitOption(space));
		}

		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
		panel.add(new JLabel("Choose the vehicle that is leaving:"));
		panel.add(vehicleCombo);

		int result = JOptionPane.showConfirmDialog(mainMenuView,
				panel,
				"Parking exit",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (result != JOptionPane.OK_OPTION) {
			return null;
		}

		return (ExitOption) vehicleCombo.getSelectedItem();
	}

	private void exitWithVehicle(ExitOption selectedOption) {
		setExitLoading(true);
		new SwingWorker<ParkingSpace, Void>() {
			@Override
			protected ParkingSpace doInBackground() throws Exception {
				return parkingService.handleUserVehicleExit(
						userService.getLastLoggedInUserId(),
						selectedOption.getPlate());
			}

			@Override
			protected void done() {
				setExitLoading(false);
				try {
					ParkingSpace freedSpace = get();
					if (freedSpace == null) {
						showExitError("That vehicle is not currently parked.");
						refreshExitButtonState();
						return;
					}

					JOptionPane.showMessageDialog(mainMenuView,
							"Vehicle " + selectedOption.getPlate()
									+ " has left space " + freedSpace.getId() + ".",
							"Parking exit",
							JOptionPane.INFORMATION_MESSAGE);
					refreshExitButtonState();
				} catch (InterruptedException | ExecutionException e) {
					Throwable cause = e.getCause();
					showExitError(cause != null ? cause.getMessage() : "Failed to process parking exit.");
					refreshExitButtonState();
				}
			}
		}.execute();
	}

	private void setExitLoading(boolean loading) {
		if (mainMenuView == null) return;

		mainMenuView.getParkingEntryButton().setEnabled(!loading);
		mainMenuView.getParkingExitButton().setEnabled(!loading && exitAllowed);
		mainMenuView.setCursor(loading
				? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
				: Cursor.getDefaultCursor());
	}

	private void showExitError(String message) {
		JOptionPane.showMessageDialog(mainMenuView,
				message,
				"Parking exit",
				JOptionPane.ERROR_MESSAGE);
	}

	public void vehicleEntry(String plate, VehicleType type) {

	}

	private void cancelReservationFromDetails() {
		if (parkingSpaceDetailsView == null || adminService == null) return;

		ParkingSpaceDetailsView detailsView = parkingSpaceDetailsView;
		String spaceCode = detailsView.getDisplayedSpaceCode();
		String plate = detailsView.getDisplayedReservationPlate();
		if (plate == null || plate.isBlank()) {
			detailsView.showError("There is no active reservation to cancel.");
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(detailsView,
				"Cancel the reservation for plate \"" + plate + "\" in space \"" + spaceCode + "\"?",
				"Cancel Reservation",
				JOptionPane.YES_NO_OPTION);

		if (confirm != JOptionPane.YES_OPTION) return;

		detailsView.setLoading(true);
		new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				return adminService.cancelReservationByPlate(plate);
			}

			@Override
			protected void done() {
				detailsView.setLoading(false);
				try {
					boolean cancelled = get();
					if (cancelled) {
						detailsView.setVisible(false);
						detailsView.dispose();
						if (parkingSpaceDetailsView == detailsView) {
							parkingSpaceDetailsView = null;
						}
						JOptionPane.showMessageDialog(mainMenuView,
								"Reservation for plate \"" + plate + "\" has been cancelled.",
								"Info",
								JOptionPane.INFORMATION_MESSAGE);
						mainMenuView.rebuildParkingSlotsPanel();
						loadParkingStatus();
					} else {
						detailsView.showError("No active reservation was found for plate \"" + plate + "\".");
					}
				} catch (InterruptedException | ExecutionException e) {
					Throwable cause = e.getCause();
					detailsView.showError(cause != null ? cause.getMessage() : "Failed to cancel reservation.");
				}
			}
		}.execute();
	}

	public void vehicleExit(String plate) {

	}

	/** Sets the main menu view and initialises the space details view. */
	public void setMainMenuView(MainMenuView mainMenuView) {
		this.mainMenuView = mainMenuView;
		this.parkingSpaceDetailsView = new ParkingSpaceDetailsView(mainMenuView);
		this.parkingSpaceDetailsView.setCancelReservationListener(e -> cancelReservationFromDetails());
	}

	/** Sets the user service. */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	/** Sets the admin service. */
	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}

	/** Sets the admin controller to refresh when parking status changes. */
	public void setAdminController(AdminController adminController) {
		this.adminController = adminController;
	}

	/** Sets the slot booking controller to refresh when parking status changes. */
	public void setSlotBookingController(AdminSlotBookingController slotBookingController) {
		this.slotBookingController = slotBookingController;
	}
}
