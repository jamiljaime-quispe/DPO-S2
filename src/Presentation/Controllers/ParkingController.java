package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Listeners.ParkingStatusChangeListener;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Business.Services.UserService;
import Presentation.Views.MainMenuView;
import Presentation.Views.ParkingSpaceDetailsView;
import Business.Entities.VehicleType;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Controller for parking status, vehicle entry/exit, and space details.
 * Implements {@link ParkingStatusChangeListener} to react to simulation events.
 */
public class ParkingController implements ParkingStatusChangeListener {
	private static final Logger LOGGER = Logger.getLogger(ParkingController.class.getName());

	private ParkingService parkingService;
	private ParkingSpaceDetailsView parkingSpaceDetailsView;
	private MainMenuView mainMenuView;
	private UserService userService;
	private AdminService adminService;
	private AdminController adminController;
	private AdminSlotBookingController slotBookingController;
	private boolean exitAllowed;
	private volatile int parkingStatusLoadId;

	/** Refreshes visible parking screens after a parking change. */
	@Override
	public void parkingStatusChanged() {
		parkingStatusChanged(null);
	}

	/** Refreshes visible parking screens and logs a simulation message. */
	@Override
	public void parkingStatusChanged(String message) {
		SwingUtilities.invokeLater(new Runnable() {
			/** Refreshes visible parking screens on the EDT. */
			@Override
			public void run() {
				if (message != null && !message.isBlank()) {
					LOGGER.info(message);
				}
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

	private static class StatusRow {
		private ParkingSpace space;
		private boolean userParkedVehicle;

		/** Stores one parking status row loaded for the table. */
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

		/** Stores the result of a parking entry attempt. */
		private EntryResult(EntryStatus status, ParkingSpace space, String message) {
			this.status = status;
			this.space = space;
			this.message = message;
		}

		/** Creates a result that asks the user for vehicle type. */
		private static EntryResult needsVehicleType() {
			return new EntryResult(EntryStatus.NEEDS_VEHICLE_TYPE, null, null);
		}

		/** Creates a result for a reserved-space entry. */
		private static EntryResult assignedFromReservation(ParkingSpace space) {
			return new EntryResult(EntryStatus.ASSIGNED_FROM_RESERVATION, space, null);
		}

		/** Creates a result for an entry without reservation. */
		private static EntryResult assignedWithoutReservation(ParkingSpace space) {
			return new EntryResult(EntryStatus.ASSIGNED_WITHOUT_RESERVATION, space, null);
		}

		/** Creates a result for a vehicle that is already parked. */
		private static EntryResult alreadyParked(ParkingSpace space) {
			return new EntryResult(EntryStatus.ALREADY_PARKED, space, null);
		}

		/** Creates a result for a full compatible parking area. */
		private static EntryResult noSpace(VehicleType type) {
			return new EntryResult(EntryStatus.NO_SPACE, null,
					"No vacant unreserved " + type.name() + " spaces are available.");
		}

		/** Creates a result for an entry error. */
		private static EntryResult error(String message) {
			return new EntryResult(EntryStatus.ERROR, null, message);
		}
	}

	/**
	 * Constructs the controller.
	 *
	 * @param parkingService the parking service
	 */
	public ParkingController(ParkingService parkingService) {
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
			/** Loads parking status rows away from the EDT. */
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

			/** Adds parking rows to the table on the EDT. */
			@Override
			protected void process(List<StatusRow> chunks) {
				if (loadId != parkingStatusLoadId) return;

				for (StatusRow row : chunks) {
					mainMenuView.addParkingSpaceToTable(row.space, row.userParkedVehicle);
				}
			}

			/** Finishes the parking status refresh. */
			@Override
			protected void done() {
				if (loadId != parkingStatusLoadId) return;

				try {
					Set<String> loadedCodes = get();
					mainMenuView.removeParkingSpacesNotIn(loadedCodes);
					LOGGER.fine("Parking spaces loaded successfully.");
				} catch (InterruptedException | ExecutionException e) {
					mainMenuView.showError("Parking status",
							"Failed to load parking spaces: " + e.getMessage());
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
			/** Loads the selected parking space away from the EDT. */
			@Override
			protected ParkingSpace doInBackground() {
				return parkingService.findByCode(code);
			}

			/** Shows the details dialog after loading the space. */
			@Override
			protected void done() {
				try {
					ParkingSpace space = get();
					if (space == null) {
						mainMenuView.showError("Details unavailable",
								"Parking space not found: " + code);
						return;
					}

					if (parkingSpaceDetailsView == null) {
						parkingSpaceDetailsView = new ParkingSpaceDetailsView(mainMenuView);
						parkingSpaceDetailsView.setCancelReservationListener(e -> cancelReservationFromDetails());
					}
					parkingSpaceDetailsView.displaySpaceDetails(space);
				} catch (InterruptedException | ExecutionException e) {
					mainMenuView.showError("Details unavailable",
							"Failed to load parking space details: " + e.getMessage());
				} finally {
					mainMenuView.setCursor(Cursor.getDefaultCursor());
				}
			}
		}.execute();

	}

	/** Refreshes the open parking-space details dialog, if any. */
	private void refreshSpaceDetailsIfVisible() {
		if (parkingSpaceDetailsView == null || !parkingSpaceDetailsView.isVisible()) return;

		String code = parkingSpaceDetailsView.getDisplayedSpaceCode();
		if (code == null || code.isBlank()) return;

		new SwingWorker<ParkingSpace, Void>() {
			/** Reloads the displayed parking space away from the EDT. */
			@Override
			protected ParkingSpace doInBackground() {
				return parkingService.findByCode(code);
			}

			/** Updates or closes the details dialog after refresh. */
			@Override
			protected void done() {
				if (parkingSpaceDetailsView == null || !parkingSpaceDetailsView.isVisible()) return;

				try {
					ParkingSpace space = get();
					if (space == null) {
						parkingSpaceDetailsView.dispose();
						mainMenuView.showInfo("Parking space updated",
								"Parking space \"" + code + "\" is no longer available.");
						return;
					}

					parkingSpaceDetailsView.updateSpaceDetails(space);
				} catch (InterruptedException | ExecutionException e) {
					mainMenuView.showError("Parking space updated",
							"Failed to refresh parking space details: " + e.getMessage());
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

	/** Asks the main menu view for a license plate. */
	private String promptForLicensePlate() {
		return mainMenuView.promptLicensePlate("Parking entry");
	}

	/** Checks whether a plate has a reservation and continues the entry flow. */
	private void checkReservationAndEnter(String plate) {
		setParkingEntryLoading(true);

		new SwingWorker<EntryResult, Void>() {
			/** Processes reserved entry rules away from the EDT. */
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

				ParkingSpace assignedSpace = parkingService.handleUserVehicleEntry(
						userService.getLastLoggedInUserId(),
						plate,
						resolveReservationVehicleType(reservation));
				if (assignedSpace == null) {
					return EntryResult.error("Could not occupy the reserved parking space.");
				}

				return EntryResult.assignedFromReservation(assignedSpace);
			}

			/** Handles the reserved entry result on the EDT. */
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

	/** Parks a vehicle without an active reservation. */
	private void enterWithoutReservation(String plate, VehicleType type) {
		setParkingEntryLoading(true);

		new SwingWorker<EntryResult, Void>() {
			/** Processes unreserved entry rules away from the EDT. */
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

			/** Handles the unreserved entry result on the EDT. */
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

	/** Shows the correct next step for a parking entry result. */
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

	/** Asks the main menu view for a vehicle type. */
	private VehicleType promptForVehicleType(String plate) {
		return mainMenuView.promptVehicleTypeForEntry(plate);
	}

	/** Shows the assigned entry space to the user. */
	private void showAssignedSpace(String message, ParkingSpace space) {
		mainMenuView.showAssignedParkingEntry(message, space);
		refreshExitButtonState();
	}

	/** Shows a parking entry error. */
	private void showEntryError(String message) {
		mainMenuView.showError("Parking entry", message);
	}

	/** Finds the vehicle type that should be used for a reservation entry. */
	private VehicleType resolveReservationVehicleType(Reservation reservation) {
		if (reservation.getVehicle() != null && reservation.getVehicle().getType() != null) {
			return reservation.getVehicle().getType();
		}

		if (reservation.getParkingSpace() != null && reservation.getParkingSpace().getVehicleType() != null) {
			return reservation.getParkingSpace().getVehicleType();
		}

		return VehicleType.CAR;
	}

	/** Enables or disables entry controls while work is running. */
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
			/** Checks parked vehicles for the current user away from the EDT. */
			@Override
			protected Boolean doInBackground() {
				List<ParkingSpace> parkedSpaces = parkingService.getParkedSpacesByUser(
						userService.getLastLoggedInUserId());
				return !parkedSpaces.isEmpty();
			}

			/** Updates the exit button after the parked-vehicle check. */
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
			/** Loads the current user's parked vehicles away from the EDT. */
			@Override
			protected List<ParkingSpace> doInBackground() throws Exception {
				return parkingService.getParkedSpacesByUser(userService.getLastLoggedInUserId());
			}

			/** Shows the exit choice dialog after parked vehicles load. */
			@Override
			protected void done() {
				setExitLoading(false);
				try {
					List<ParkingSpace> parkedSpaces = get();
					if (parkedSpaces == null || parkedSpaces.isEmpty()) {
						mainMenuView.showInfo("Parking exit",
								"You do not have any parked vehicles right now.");
						refreshExitButtonState();
						return;
					}

					ParkingSpace selectedSpace = mainMenuView.promptExitVehicle(parkedSpaces);
					if (selectedSpace != null) {
						exitWithVehicle(selectedSpace);
					}
				} catch (InterruptedException | ExecutionException e) {
					Throwable cause = e.getCause();
					showExitError(cause != null ? cause.getMessage() : "Failed to load parked vehicles.");
					refreshExitButtonState();
				}
			}
		}.execute();
	}

	/** Processes exit for the selected parked vehicle. */
	private void exitWithVehicle(ParkingSpace selectedSpace) {
		String plate = selectedSpace.getParkedVehicle().getLicensePlate();
		setExitLoading(true);
		new SwingWorker<ParkingSpace, Void>() {
			/** Frees the selected vehicle's space away from the EDT. */
			@Override
			protected ParkingSpace doInBackground() throws Exception {
				return parkingService.handleUserVehicleExit(
						userService.getLastLoggedInUserId(),
						plate);
			}

			/** Shows the exit result on the EDT. */
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

					mainMenuView.showInfo("Parking exit",
							"Vehicle " + plate + " has left space " + freedSpace.getId() + ".");
					refreshExitButtonState();
				} catch (InterruptedException | ExecutionException e) {
					Throwable cause = e.getCause();
					showExitError(cause != null ? cause.getMessage() : "Failed to process parking exit.");
					refreshExitButtonState();
				}
			}
		}.execute();
	}

	/** Enables or disables exit controls while work is running. */
	private void setExitLoading(boolean loading) {
		if (mainMenuView == null) return;

		mainMenuView.getParkingEntryButton().setEnabled(!loading);
		mainMenuView.getParkingExitButton().setEnabled(!loading && exitAllowed);
		mainMenuView.setCursor(loading
				? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
				: Cursor.getDefaultCursor());
	}

	/** Shows a parking exit error. */
	private void showExitError(String message) {
		mainMenuView.showError("Parking exit", message);
	}


	/** Cancels the reservation shown in the admin space-details dialog. */
	private void cancelReservationFromDetails() {
		if (parkingSpaceDetailsView == null || adminService == null) return;

		ParkingSpaceDetailsView detailsView = parkingSpaceDetailsView;
		String spaceCode = detailsView.getDisplayedSpaceCode();
		String plate = detailsView.getDisplayedReservationPlate();
		if (plate == null || plate.isBlank()) {
			detailsView.showError("There is no active reservation to cancel.");
			return;
		}

		if (!detailsView.confirmCancelReservation(spaceCode, plate)) return;

		detailsView.setLoading(true);
		new SwingWorker<Boolean, Void>() {
			/** Cancels the reservation away from the EDT. */
			@Override
			protected Boolean doInBackground() throws Exception {
				return adminService.cancelReservationByPlate(plate);
			}

			/** Updates the UI after reservation cancellation. */
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
						mainMenuView.showInfo("Info",
								"Reservation for plate \"" + plate + "\" has been cancelled.");
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
