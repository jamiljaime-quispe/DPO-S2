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
	private StatisticsController statisticsController;
	private Runnable logoutAction;
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
		runOnEventDispatchThread(() -> refreshVisibleParkingScreens(message));
	}

	/**
	 * Refreshes only the parking screens that are visible after a parking change.
	 *
	 * @param message optional simulation message
	 */
	private void refreshVisibleParkingScreens(String message) {
		if (hasSimulationMessage(message)) {
			logSimulationMessage(message);
		}
		if (isParkingStatusTableVisible()) {
			loadParkingStatus();
		}
		refreshSpaceDetailsIfVisible();
		if (hasAdminController()) {
			refreshAdminControllerIfVisible();
		}
		if (hasSlotBookingController()) {
			refreshSlotBookingControllerIfVisible();
		}
		if (hasStatisticsController()) {
			recordAndRefreshVisibleChart();
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

		showParkingSlotsTable();

		SwingWorker<Set<String>, ParkingStatusRow> worker = new SwingWorker<Set<String>, ParkingStatusRow>() {
			/** Loads parking status rows away from the EDT. */
			@Override
			protected Set<String> doInBackground() {
				List<ParkingSpace> spaces = loadParkingStatusFromService();
				Set<String> userParkedCodes = new HashSet<>();
				Set<String> loadedCodes = new HashSet<>();

				if (hasLoggedInUser()) {
					List<ParkingSpace> userParkedSpaces = loadCurrentUserParkedSpaces();
					for (ParkingSpace parkedSpace : userParkedSpaces) {
						userParkedCodes.add(parkedSpace.getId());
					}
				}

				for (ParkingSpace space : spaces) {
					loadedCodes.add(space.getId());
					if (loadId == parkingStatusLoadId) {
						publish(new ParkingStatusRow(space, userParkedCodes.contains(space.getId())));
					}
				}

				return loadedCodes;
			}

			/** Adds parking rows to the table on the EDT. */
			@Override
			protected void process(List<ParkingStatusRow> chunks) {
				if (loadId != parkingStatusLoadId) return;

				for (ParkingStatusRow row : chunks) {
					addParkingStatusRow(row);
				}
			}

			/** Finishes the parking status refresh. */
			@Override
			protected void done() {
				if (loadId != parkingStatusLoadId) return;

				try {
					Set<String> loadedCodes = get();
					removeParkingSpacesNotIn(loadedCodes);
					logParkingSpacesLoaded();
				} catch (InterruptedException | ExecutionException e) {
					showMainError("Parking status", "Failed to load parking spaces: " + e.getMessage());
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

		setMainMenuWaitCursor();

		new SwingWorker<ParkingSpace, Void>() {
			/** Loads the selected parking space away from the EDT. */
			@Override
			protected ParkingSpace doInBackground() {
				return findParkingSpace(code);
			}

			/** Shows the details dialog after loading the space. */
			@Override
			protected void done() {
				try {
					ParkingSpace space = get();
					if (space == null) {
						showMainError("Details unavailable", "Parking space not found: " + code);
						return;
					}

					createDetailsViewIfNeeded();
					displaySpaceDetails(space);
				} catch (InterruptedException | ExecutionException e) {
					showMainError("Details unavailable", "Failed to load parking space details: " + e.getMessage());
				} finally {
					setMainMenuDefaultCursor();
				}
			}
		}.execute();

	}

	/** Refreshes the open parking-space details dialog, if any. */
	private void refreshSpaceDetailsIfVisible() {
		if (!isSpaceDetailsVisible()) return;

		String code = getDisplayedSpaceCode();
		if (code == null || code.isBlank()) return;

		new SwingWorker<ParkingSpace, Void>() {
			/** Reloads the displayed parking space away from the EDT. */
			@Override
			protected ParkingSpace doInBackground() {
				return findParkingSpace(code);
			}

			/** Updates or closes the details dialog after refresh. */
			@Override
			protected void done() {
				if (!isSpaceDetailsVisible()) return;

				try {
					ParkingSpace space = get();
					if (space == null) {
						disposeSpaceDetailsView();
						showMainInfo("Parking space updated",
								"Parking space \"" + code + "\" is no longer available.");
						return;
					}

					updateSpaceDetails(space);
				} catch (InterruptedException | ExecutionException e) {
					showMainError("Parking space updated",
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
		return promptLicensePlateFromMainMenu("Parking entry");
	}

	/** Checks whether a plate has a reservation and continues the entry flow. */
	private void checkReservationAndEnter(String plate) {
		setParkingEntryLoading(true);

		new SwingWorker<ParkingEntryResult, Void>() {
			/** Processes reserved entry rules away from the EDT. */
			@Override
			protected ParkingEntryResult doInBackground() throws Exception {
				ParkingSpace alreadyParked = findOccupiedSpaceByPlate(plate);
				if (alreadyParked != null) {
					return ParkingEntryResult.alreadyParked(alreadyParked);
				}

				Reservation reservation = findActiveReservationByPlate(plate);
				if (reservation == null) {
					return ParkingEntryResult.needsVehicleType();
				}

				ParkingSpace assignedSpace = handleUserVehicleEntry(
						getCurrentUserId(),
						plate,
						resolveReservationVehicleType(reservation));
				if (assignedSpace == null) {
					return ParkingEntryResult.error("Could not occupy the reserved parking space.");
				}

				return ParkingEntryResult.assignedFromReservation(assignedSpace);
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

		new SwingWorker<ParkingEntryResult, Void>() {
			/** Processes unreserved entry rules away from the EDT. */
			@Override
			protected ParkingEntryResult doInBackground() throws Exception {
				ParkingSpace alreadyParked = findOccupiedSpaceByPlate(plate);
				if (alreadyParked != null) {
					return ParkingEntryResult.alreadyParked(alreadyParked);
				}

				ParkingSpace assignedSpace = handleUserVehicleEntry(
						getCurrentUserId(),
						plate,
						type);
				if (assignedSpace == null) {
					return ParkingEntryResult.noSpace(type);
				}

				return ParkingEntryResult.assignedWithoutReservation(assignedSpace);
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
	private void handleEntryResult(String plate, ParkingEntryResult result) {
		if (result.getStatus() == ParkingEntryStatus.NEEDS_VEHICLE_TYPE) {
			VehicleType type = promptForVehicleType(plate);
			if (type != null) {
				enterWithoutReservation(plate, type);
			}
			return;
		}

		if (result.getStatus() == ParkingEntryStatus.ASSIGNED_FROM_RESERVATION) {
			showAssignedSpace("Access granted using your reservation.", result.getSpace());
			return;
		}

		if (result.getStatus() == ParkingEntryStatus.ASSIGNED_WITHOUT_RESERVATION) {
			showAssignedSpace("Access granted.", result.getSpace());
			return;
		}

		if (result.getStatus() == ParkingEntryStatus.ALREADY_PARKED) {
			showEntryError("Vehicle " + plate + " is already parked in space "
					+ result.getSpace().getId() + ".");
			return;
		}

		showEntryError(result.getMessage());
	}

	/** Asks the main menu view for a vehicle type. */
	private VehicleType promptForVehicleType(String plate) {
		return promptVehicleTypeForEntry(plate);
	}

	/** Shows the assigned entry space to the user. */
	private void showAssignedSpace(String message, ParkingSpace space) {
		showAssignedParkingEntry(message, space);
		refreshExitButtonState();
		parkingStatusChanged();
	}

	/** Shows a parking entry error. */
	private void showEntryError(String message) {
		showMainError("Parking entry", message);
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

		setParkingEntryButtonEnabled(!loading);
		setParkingExitButtonEnabled(!loading && exitAllowed);
		setMainMenuCursor(loading);
	}

	/** Checks whether the current user has any parked vehicles and updates the Exit button state. */
	public void refreshExitButtonState() {
		if (mainMenuView == null || userService == null) return;

		setParkingExitButtonEnabled(false);
		new SwingWorker<Boolean, Void>() {
			/** Checks parked vehicles for the current user away from the EDT. */
			@Override
			protected Boolean doInBackground() {
				List<ParkingSpace> parkedSpaces = loadCurrentUserParkedSpaces();
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

				setParkingExitButtonEnabled(exitAllowed);
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
				return loadCurrentUserParkedSpaces();
			}

			/** Shows the exit choice dialog after parked vehicles load. */
			@Override
			protected void done() {
				setExitLoading(false);
				try {
					List<ParkingSpace> parkedSpaces = get();
					if (parkedSpaces == null || parkedSpaces.isEmpty()) {
						showMainInfo("Parking exit", "You do not have any parked vehicles right now.");
						refreshExitButtonState();
						return;
					}

					ParkingSpace selectedSpace = promptExitVehicle(parkedSpaces);
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
				return handleUserVehicleExit(
						getCurrentUserId(),
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

					showMainInfo("Parking exit",
							"Vehicle " + plate + " has left space " + freedSpace.getId() + ".");
					parkingStatusChanged();
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

		setParkingEntryButtonEnabled(!loading);
		setParkingExitButtonEnabled(!loading && exitAllowed);
		setMainMenuCursor(loading);
	}

	/** Shows a parking exit error. */
	private void showExitError(String message) {
		showMainError("Parking exit", message);
	}


	/** Cancels the reservation shown in the admin space-details dialog. */
	private void cancelReservationFromDetails() {
		if (parkingSpaceDetailsView == null || adminService == null) return;

		ParkingSpaceDetailsView detailsView = parkingSpaceDetailsView;
		String spaceCode = getDisplayedSpaceCode(detailsView);
		String plate = getDisplayedReservationPlate(detailsView);
		if (plate == null || plate.isBlank()) {
			showDetailsError(detailsView, "There is no active reservation to cancel.");
			return;
		}

		if (!confirmCancelReservation(detailsView, spaceCode, plate)) return;

		setDetailsLoading(detailsView, true);
		new SwingWorker<Boolean, Void>() {
			/** Cancels the reservation away from the EDT. */
			@Override
			protected Boolean doInBackground() throws Exception {
				return cancelReservationByPlate(plate);
			}

			/** Updates the UI after reservation cancellation. */
			@Override
			protected void done() {
				setDetailsLoading(detailsView, false);
				try {
					boolean cancelled = get();
					if (cancelled) {
						hideAndDisposeDetailsView(detailsView);
						if (parkingSpaceDetailsView == detailsView) {
							parkingSpaceDetailsView = null;
						}
						showMainInfo("Info", "Reservation for plate \"" + plate + "\" has been cancelled.");
						rebuildParkingSlotsPanel();
						loadParkingStatus();
					} else {
						showDetailsError(detailsView, "No active reservation was found for plate \"" + plate + "\".");
					}
				} catch (InterruptedException | ExecutionException e) {
					Throwable cause = e.getCause();
					showDetailsError(detailsView, cause != null ? cause.getMessage() : "Failed to cancel reservation.");
				}
			}
		}.execute();
	}


	/** Sets the main menu view and initialises the space details view. */
	public void setMainMenuView(MainMenuView mainMenuView) {
		this.mainMenuView = mainMenuView;
		createFreshDetailsView();
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

	/** Sets the statistics controller to refresh visible chart data when parking changes. */
	public void setStatisticsController(StatisticsController statisticsController) {
		this.statisticsController = statisticsController;
	}

	/** Clears parking state when the active user session ends. */
	public void clearSessionState() {
		exitAllowed = false;
		parkingStatusLoadId++;
		if (mainMenuView != null) {
			setParkingEntryButtonEnabled(false);
			setParkingExitButtonEnabled(false);
		}
		clearSpaceDetailsSessionState();
	}

	/** Sets the action used when the user logs out from a parking dialog. */
	public void setLogoutAction(Runnable logoutAction) {
		this.logoutAction = logoutAction;
	}

	/** Runs a task on the Swing event thread. */
	private void runOnEventDispatchThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}

	/** Checks whether a simulation message should be shown in the log. */
	private boolean hasSimulationMessage(String message) {
		return message != null && !message.isBlank();
	}

	/** Writes a simulation message through the controller logger. */
	private void logSimulationMessage(String message) {
		LOGGER.info(message);
	}

	/** Checks whether the parking status table is currently visible. */
	private boolean isParkingStatusTableVisible() {
		return mainMenuView != null && mainMenuView.isParkingSlotsTableVisible();
	}

	/** Checks whether the admin controller was connected in Main. */
	private boolean hasAdminController() {
		return adminController != null;
	}

	/** Asks the admin controller to refresh its visible screen. */
	private void refreshAdminControllerIfVisible() {
		adminController.refreshIfVisible();
	}

	/** Checks whether the booking controller was connected in Main. */
	private boolean hasSlotBookingController() {
		return slotBookingController != null;
	}

	/** Asks the booking controller to refresh its visible screen. */
	private void refreshSlotBookingControllerIfVisible() {
		slotBookingController.refreshIfVisible();
	}

	/** Checks whether the statistics controller was connected in Main. */
	private boolean hasStatisticsController() {
		return statisticsController != null;
	}

	/** Records the latest occupancy and refreshes the chart if it is visible. */
	private void recordAndRefreshVisibleChart() {
		statisticsController.recordAndRefreshVisibleChart();
	}

	/** Shows the parking status table in the main menu. */
	private void showParkingSlotsTable() {
		mainMenuView.showParkingSlotsTable();
	}

	/** Loads the parking status from the business layer. */
	private List<ParkingSpace> loadParkingStatusFromService() {
		return parkingService.getParkingStatus();
	}

	/** Checks whether a user is logged in for user-specific parking highlights. */
	private boolean hasLoggedInUser() {
		return userService != null && getCurrentUserId() > 0;
	}

	/** Gets the id of the logged-in user from the user service. */
	private int getCurrentUserId() {
		return userService.getLastLoggedInUserId();
	}

	/** Loads the spaces occupied by the current user. */
	private List<ParkingSpace> loadCurrentUserParkedSpaces() {
		return parkingService.getParkedSpacesByUser(getCurrentUserId());
	}

	/** Adds one loaded status row to the main menu table. */
	private void addParkingStatusRow(ParkingStatusRow row) {
		mainMenuView.addParkingSpaceToTable(row.getSpace(), row.isUserParkedVehicle());
	}

	/** Removes table rows that were not returned by the latest load. */
	private void removeParkingSpacesNotIn(Set<String> loadedCodes) {
		mainMenuView.removeParkingSpacesNotIn(loadedCodes);
	}

	/** Writes a successful parking status load message to the logger. */
	private void logParkingSpacesLoaded() {
		LOGGER.fine("Parking spaces loaded successfully.");
	}

	/** Shows an error from the main menu view. */
	private void showMainError(String title, String message) {
		mainMenuView.showError(title, message);
	}

	/** Shows an information message from the main menu view. */
	private void showMainInfo(String title, String message) {
		mainMenuView.showInfo(title, message);
	}

	/** Sets the main menu cursor to waiting. */
	private void setMainMenuWaitCursor() {
		mainMenuView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	/** Restores the normal main menu cursor. */
	private void setMainMenuDefaultCursor() {
		mainMenuView.setCursor(Cursor.getDefaultCursor());
	}

	/** Sets the main menu cursor according to a loading state. */
	private void setMainMenuCursor(boolean loading) {
		if (loading) {
			setMainMenuWaitCursor();
		} else {
			setMainMenuDefaultCursor();
		}
	}

	/** Finds one parking space through the business layer. */
	private ParkingSpace findParkingSpace(String code) {
		return parkingService.findByCode(code);
	}

	/** Creates the space details view if it has not been created yet. */
	private void createDetailsViewIfNeeded() {
		if (parkingSpaceDetailsView == null) {
			createFreshDetailsView();
		}
	}

	/** Creates a new space details view and connects its cancel action. */
	private void createFreshDetailsView() {
		parkingSpaceDetailsView = new ParkingSpaceDetailsView(mainMenuView);
		setDetailsCancelReservationListener();
		setDetailsLogoutListener();
	}

	/** Connects the details cancel button to this controller. */
	private void setDetailsCancelReservationListener() {
		parkingSpaceDetailsView.setCancelReservationListener(e -> cancelReservationFromDetails());
	}

	/** Connects the details logout button to this controller. */
	private void setDetailsLogoutListener() {
		parkingSpaceDetailsView.setLogoutListener(e -> logoutFromDetailsIfConfirmed());
	}

	/** Logs out from the details dialog after confirmation. */
	private void logoutFromDetailsIfConfirmed() {
		if (logoutAction != null && parkingSpaceDetailsView.confirmLogout()) {
			parkingSpaceDetailsView.dispose();
			logoutAction.run();
		}
	}

	/** Displays details for a loaded parking space. */
	private void displaySpaceDetails(ParkingSpace space) {
		parkingSpaceDetailsView.displaySpaceDetails(space);
	}

	/** Checks whether the details dialog is currently open. */
	private boolean isSpaceDetailsVisible() {
		return parkingSpaceDetailsView != null && parkingSpaceDetailsView.isVisible();
	}

	/** Gets the parking space code currently shown in the details dialog. */
	private String getDisplayedSpaceCode() {
		return parkingSpaceDetailsView.getDisplayedSpaceCode();
	}

	/** Closes the current parking space details dialog. */
	private void disposeSpaceDetailsView() {
		parkingSpaceDetailsView.dispose();
	}

	/** Updates the current parking space details dialog. */
	private void updateSpaceDetails(ParkingSpace space) {
		parkingSpaceDetailsView.updateSpaceDetails(space);
	}

	/** Asks the main menu for a license plate. */
	private String promptLicensePlateFromMainMenu(String title) {
		return mainMenuView.promptLicensePlate(title);
	}

	/** Finds an occupied parking space by plate through the business layer. */
	private ParkingSpace findOccupiedSpaceByPlate(String plate) {
		return parkingService.findOccupiedSpaceByPlate(plate);
	}

	/** Finds an active reservation by plate through the business layer. */
	private Reservation findActiveReservationByPlate(String plate) {
		return parkingService.findActiveReservationByPlate(plate);
	}

	/** Handles a user vehicle entry through the business layer. */
	private ParkingSpace handleUserVehicleEntry(int userId, String plate, VehicleType type) {
		return parkingService.handleUserVehicleEntry(userId, plate, type);
	}

	/** Asks the main menu for the vehicle type used during entry. */
	private VehicleType promptVehicleTypeForEntry(String plate) {
		return mainMenuView.promptVehicleTypeForEntry(plate);
	}

	/** Shows the parking entry result with its assigned space. */
	private void showAssignedParkingEntry(String message, ParkingSpace space) {
		mainMenuView.showAssignedParkingEntry(message, space);
	}

	/** Enables or disables the entry button. */
	private void setParkingEntryButtonEnabled(boolean enabled) {
		mainMenuView.setParkingEntryButtonEnabled(enabled);
	}

	/** Enables or disables the exit button. */
	private void setParkingExitButtonEnabled(boolean enabled) {
		mainMenuView.setParkingExitButtonEnabled(enabled);
	}

	/** Asks the user which parked vehicle should leave. */
	private ParkingSpace promptExitVehicle(List<ParkingSpace> parkedSpaces) {
		return mainMenuView.promptExitVehicle(parkedSpaces);
	}

	/** Handles a user vehicle exit through the business layer. */
	private ParkingSpace handleUserVehicleExit(int userId, String plate) {
		return parkingService.handleUserVehicleExit(userId, plate);
	}

	/** Gets the space code currently shown in a details dialog. */
	private String getDisplayedSpaceCode(ParkingSpaceDetailsView detailsView) {
		return detailsView.getDisplayedSpaceCode();
	}

	/** Gets the reserved plate currently shown in a details dialog. */
	private String getDisplayedReservationPlate(ParkingSpaceDetailsView detailsView) {
		return detailsView.getDisplayedReservationPlate();
	}

	/** Shows an error in a details dialog. */
	private void showDetailsError(ParkingSpaceDetailsView detailsView, String message) {
		detailsView.showError(message);
	}

	/** Asks the admin to confirm cancelling a reservation from details. */
	private boolean confirmCancelReservation(ParkingSpaceDetailsView detailsView, String spaceCode, String plate) {
		return detailsView.confirmCancelReservation(spaceCode, plate);
	}

	/** Enables or disables loading state in a details dialog. */
	private void setDetailsLoading(ParkingSpaceDetailsView detailsView, boolean loading) {
		detailsView.setLoading(loading);
	}

	/** Cancels a reservation by plate through the admin service. */
	private boolean cancelReservationByPlate(String plate) {
		return adminService.cancelReservationByPlate(plate);
	}

	/** Hides and disposes a details dialog. */
	private void hideAndDisposeDetailsView(ParkingSpaceDetailsView detailsView) {
		detailsView.setVisible(false);
		detailsView.dispose();
	}

	/** Clears and closes parking-space details that may contain user data. */
	private void clearSpaceDetailsSessionState() {
		if (parkingSpaceDetailsView != null) {
			parkingSpaceDetailsView.clearSessionViewState();
			parkingSpaceDetailsView.dispose();
			parkingSpaceDetailsView = null;
		}
	}

	/** Rebuilds the main menu parking table panel. */
	private void rebuildParkingSlotsPanel() {
		mainMenuView.rebuildParkingSlotsPanel();
	}
}
