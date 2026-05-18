package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.UserService;
import Presentation.Views.MainMenuView;
import Presentation.Views.ParkingStatusView;
import Presentation.Views.EntryExitView;
import Presentation.Views.ParkingSpaceDetailsView;
import Business.Entities.VehicleType;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.JTextField;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParkingController {
	private static final int BACKGROUND_TEST_DELAY_MS = 300;

	private ParkingStatusView parkingStatusView;
	private EntryExitView entryExitView;
	private ParkingService parkingService;
	private ParkingSpaceDetailsView parkingSpaceDetailsView;
	private MainMenuView mainMenuView;
	private UserService userService;
	private AdminService adminService;
	private boolean exitAllowed;
	private volatile int parkingStatusLoadId;

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

		parkingStatusLoadId++;
		int loadId = parkingStatusLoadId;

		mainMenuView.clearParkingSlotsTable();
		mainMenuView.showParkingSlotsTable();

		SwingWorker<Void, StatusRow> worker = new SwingWorker<Void, StatusRow>() {
			@Override
			protected Void doInBackground() {
				List<ParkingSpace> spaces = parkingService.getParkingStatus();
				Set<String> userParkedCodes = new HashSet<>();

				if (userService != null && userService.getLastLoggedInUserId() > 0) {
					List<ParkingSpace> userParkedSpaces = parkingService.getParkedSpacesByUser(
							userService.getLastLoggedInUserId());
					for (ParkingSpace parkedSpace : userParkedSpaces) {
						userParkedCodes.add(parkedSpace.getId());
					}
				}

				for (ParkingSpace space : spaces) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}

					if (loadId == parkingStatusLoadId) {
						publish(new StatusRow(space, userParkedCodes.contains(space.getId())));
					}
				}

				return null;
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
						parkingSpaceDetailsView.setCancelReservationListener(e -> cancelReservationFromDetails());
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
			protected EntryResult doInBackground() {
				try {
					simulateDatabaseDelay();

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
				} catch (Exception e) {
					return EntryResult.error("Failed to process parking entry: " + e.getMessage());
				}
			}

			@Override
			protected void done() {
				setParkingEntryLoading(false);
				try {
					handleEntryResult(plate, get());
				} catch (Exception e) {
					showEntryError("Failed to process parking entry: " + e.getMessage());
				}
			}
		}.execute();
	}

	private void enterWithoutReservation(String plate, VehicleType type) {
		setParkingEntryLoading(true);

		new SwingWorker<EntryResult, Void>() {
			@Override
			protected EntryResult doInBackground() {
				try {
					simulateDatabaseDelay();

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
				} catch (Exception e) {
					return EntryResult.error("Failed to process parking entry: " + e.getMessage());
				}
			}

			@Override
			protected void done() {
				setParkingEntryLoading(false);
				try {
					handleEntryResult(plate, get());
				} catch (Exception e) {
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

	public void refreshExitButtonState() {
		if (mainMenuView == null || userService == null) return;

		mainMenuView.getParkingExitButton().setEnabled(false);
		new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() {
				simulateDatabaseDelay();
				List<ParkingSpace> parkedSpaces = parkingService.getParkedSpacesByUser(
						userService.getLastLoggedInUserId());
				return !parkedSpaces.isEmpty();
			}

			@Override
			protected void done() {
				try {
					exitAllowed = get();
				} catch (Exception e) {
					exitAllowed = false;
				}

				mainMenuView.getParkingExitButton().setEnabled(exitAllowed);
			}
		}.execute();
	}

	public void showVehicleExitDialog() {
		if (mainMenuView == null || userService == null) return;

		setExitLoading(true);
		new SwingWorker<List<ParkingSpace>, Void>() {
			private String errorMessage;

			@Override
			protected List<ParkingSpace> doInBackground() {
				try {
					simulateDatabaseDelay();
					return parkingService.getParkedSpacesByUser(userService.getLastLoggedInUserId());
				} catch (Exception e) {
					errorMessage = e.getMessage();
					return null;
				}
			}

			@Override
			protected void done() {
				setExitLoading(false);
				try {
					List<ParkingSpace> parkedSpaces = get();
					if (parkedSpaces == null) {
						showExitError(errorMessage != null
								? errorMessage
								: "Failed to load parked vehicles.");
						refreshExitButtonState();
						return;
					}

					if (parkedSpaces.isEmpty()) {
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
				} catch (Exception e) {
					showExitError("Failed to load parked vehicles: " + e.getMessage());
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
			private String errorMessage;

			@Override
			protected ParkingSpace doInBackground() {
				try {
					simulateDatabaseDelay();
					return parkingService.handleUserVehicleExit(
							userService.getLastLoggedInUserId(),
							selectedOption.getPlate());
				} catch (Exception e) {
					errorMessage = e.getMessage();
					return null;
				}
			}

			@Override
			protected void done() {
				setExitLoading(false);
				try {
					ParkingSpace freedSpace = get();
					if (freedSpace == null) {
						showExitError(errorMessage != null
								? errorMessage
								: "That vehicle is not currently parked.");
						refreshExitButtonState();
						return;
					}

					JOptionPane.showMessageDialog(mainMenuView,
							"Vehicle " + selectedOption.getPlate()
									+ " has left space " + freedSpace.getId() + ".",
							"Parking exit",
							JOptionPane.INFORMATION_MESSAGE);
					refreshExitButtonState();
				} catch (Exception e) {
					showExitError("Failed to process parking exit: " + e.getMessage());
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

	private void simulateDatabaseDelay() {
		try {
			Thread.sleep(BACKGROUND_TEST_DELAY_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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
			private String errorMessage;

			@Override
			protected Boolean doInBackground() {
				try {
					simulateDatabaseDelay();
					return adminService.cancelReservationByPlate(plate);
				} catch (Exception e) {
					errorMessage = e.getMessage();
					return false;
				}
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
						detailsView.showError(errorMessage != null
								? errorMessage
								: "No active reservation was found for plate \"" + plate + "\".");
					}
				} catch (Exception e) {
					detailsView.showError("Failed to cancel reservation: " + e.getMessage());
				}
			}
		}.execute();
	}

	public void vehicleExit(String plate) {

	}

	public void setMainMenuView(MainMenuView mainMenuView) {
		this.mainMenuView = mainMenuView;
		this.parkingSpaceDetailsView = new ParkingSpaceDetailsView(mainMenuView);
		this.parkingSpaceDetailsView.setCancelReservationListener(e -> cancelReservationFromDetails());
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}
}
