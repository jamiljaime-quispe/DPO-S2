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
 * Controller for parking status, vehicle entry/exit, and space details. Implements {@link
 * ParkingStatusChangeListener} to react to simulation events.
 * <p>
 * The controller receives actions from the view, calls the needed service, and then asks the view to show
 * the result. This keeps Swing code separate from the business rules.
 * </p>
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

	/**
	 * Refreshes visible parking screens after a parking change.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	@Override
	public void parkingStatusChanged() {
		parkingStatusChanged(null);
	}

	/**
	 * Refreshes visible parking screens and logs a simulation message.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 */
	@Override
	public void parkingStatusChanged(String message) {
		runOnEventDispatchThread(() -> refreshVisibleParkingScreens(message));
	}

	/**
	 * Refreshes only the parking screens that are visible after a parking change.
	 * <p>
	 * This method asks the service for fresh data and sends it back to the visible table or dialog when the
	 * screen needs to change.
	 * </p>
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
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 *
	 * @param parkingService the parking service
	 */
	public ParkingController(ParkingService parkingService) {
		this.parkingService = parkingService;
	}

	/**
	 * Loads all parking spaces and updates the main menu table.
	 * <p>
	 * This method asks the service for fresh data and sends it back to the visible table or dialog when the
	 * screen needs to change.
	 * </p>
	 */
	public void loadParkingStatus() {
		if (mainMenuView == null) return;

		parkingStatusLoadId++;
		int loadId = parkingStatusLoadId;

		showParkingSlotsTable();

		SwingWorker<Set<String>, ParkingStatusRow> worker = new SwingWorker<Set<String>, ParkingStatusRow>() {
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the set of values found for the operation
			 */
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

			/**
			 * Applies worker updates on the Swing screen thread.
			 * <p>
			 * This receives the values published by the worker on the Swing screen thread, which makes it safe to
			 * add rows or refresh visible components little by little.
			 * </p>
			 *
			 * @param chunks chunks used by this operation
			 */
			@Override
			protected void process(List<ParkingStatusRow> chunks) {
				if (loadId != parkingStatusLoadId) return;

				for (ParkingStatusRow row : chunks) {
					addParkingStatusRow(row);
				}
			}

			/**
			 * Finishes the worker task on the Swing screen thread.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param code the parking space code
	 */
	public void showSpaceDetails(String code) {
		if (mainMenuView == null) return;

		setMainMenuWaitCursor();

		new SwingWorker<ParkingSpace, Void>() {
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the result of the operation
			 */
			@Override
			protected ParkingSpace doInBackground() {
				return findParkingSpace(code);
			}

			/**
			 * Shows the details dialog after loading the space.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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

	/**
	 * Refreshes the open parking-space details dialog, if any.
	 * <p>
	 * This method asks the service for fresh data and sends it back to the visible table or dialog when the
	 * screen needs to change.
	 * </p>
	 */
	private void refreshSpaceDetailsIfVisible() {
		if (!isSpaceDetailsVisible()) return;

		String code = getDisplayedSpaceCode();
		if (code == null || code.isBlank()) return;

		new SwingWorker<ParkingSpace, Void>() {
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the result of the operation
			 */
			@Override
			protected ParkingSpace doInBackground() {
				return findParkingSpace(code);
			}

			/**
			 * Updates or closes the details dialog after refresh.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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

	/**
	 * Opens the vehicle entry dialog. Prompts for a license plate, then processes entry.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 */
	public void showVehicleEntryDialog() {
		if (mainMenuView == null) return;

		String plate = promptForLicensePlate();
		if (plate == null) return;

		checkReservationAndEnter(plate);
	}

	/**
	 * Asks the main menu view for a license plate.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @return the answer chosen by the user
	 */
	private String promptForLicensePlate() {
		return promptLicensePlateFromMainMenu("Parking entry");
	}

	/**
	 * Handles check reservation and enter.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 */
	private void checkReservationAndEnter(String plate) {
		setParkingEntryLoading(true);

		new SwingWorker<ParkingEntryResult, Void>() {
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the result of the operation
			 * @throws Exception if the operation cannot be completed correctly
			 */
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

			/**
			 * Finishes the worker task on the Swing screen thread.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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

	/**
	 * Handles enter without reservation.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @param type vehicle type involved in the operation
	 */
	private void enterWithoutReservation(String plate, VehicleType type) {
		setParkingEntryLoading(true);

		new SwingWorker<ParkingEntryResult, Void>() {
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the result of the operation
			 * @throws Exception if the operation cannot be completed correctly
			 */
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

			/**
			 * Finishes the worker task on the Swing screen thread.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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

	/**
	 * Handles entry result.
	 * <p>
	 * This method is called from a user action, gathers what the screen needs, and passes the real work to the
	 * service layer.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @param result result used by this operation
	 */
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

	/**
	 * Asks the main menu view for a vehicle type.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return the answer chosen by the user
	 */
	private VehicleType promptForVehicleType(String plate) {
		return promptVehicleTypeForEntry(plate);
	}

	/**
	 * Shows assigned space.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 * @param space space used by this operation
	 */
	private void showAssignedSpace(String message, ParkingSpace space) {
		showAssignedParkingEntry(message, space);
		refreshExitButtonState();
		parkingStatusChanged();
	}

	/**
	 * Shows entry error.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 */
	private void showEntryError(String message) {
		showMainError("Parking entry", message);
	}

	/**
	 * Handles resolve reservation vehicle type.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param reservation reservation used by this operation
	 * @return the result of the operation
	 */
	private VehicleType resolveReservationVehicleType(Reservation reservation) {
		if (reservation.getVehicle() != null && reservation.getVehicle().getType() != null) {
			return reservation.getVehicle().getType();
		}

		if (reservation.getParkingSpace() != null && reservation.getParkingSpace().getVehicleType() != null) {
			return reservation.getParkingSpace().getVehicleType();
		}

		return VehicleType.CAR;
	}

	/**
	 * Sets the parking entry loading.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param loading true while the screen is waiting for an operation to finish
	 */
	private void setParkingEntryLoading(boolean loading) {
		if (mainMenuView == null) return;

		setParkingEntryButtonEnabled(!loading);
		setParkingExitButtonEnabled(!loading && exitAllowed);
		setMainMenuCursor(loading);
	}

	/**
	 * Handles refresh exit button state.
	 * <p>
	 * This method asks the service for fresh data and sends it back to the visible table or dialog when the
	 * screen needs to change.
	 * </p>
	 */
	public void refreshExitButtonState() {
		if (mainMenuView == null || userService == null) return;

		setParkingExitButtonEnabled(false);
		new SwingWorker<Boolean, Void>() {
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the result of the operation
			 */
			@Override
			protected Boolean doInBackground() {
				List<ParkingSpace> parkedSpaces = loadCurrentUserParkedSpaces();
				return !parkedSpaces.isEmpty();
			}

			/**
			 * Finishes the worker task on the Swing screen thread.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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

	/**
	 * Opens the vehicle exit dialog, listing the user's currently parked vehicles.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 */
	public void showVehicleExitDialog() {
		if (mainMenuView == null || userService == null) return;

		setExitLoading(true);
		new SwingWorker<List<ParkingSpace>, Void>() {
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the list of values found for the operation
			 * @throws Exception if the operation cannot be completed correctly
			 */
			@Override
			protected List<ParkingSpace> doInBackground() throws Exception {
				return loadCurrentUserParkedSpaces();
			}

			/**
			 * Shows the exit choice dialog after parked vehicles load.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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

	/**
	 * Handles exit with vehicle.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param selectedSpace selected space used by this operation
	 */
	private void exitWithVehicle(ParkingSpace selectedSpace) {
		String plate = selectedSpace.getParkedVehicle().getLicensePlate();
		setExitLoading(true);
		new SwingWorker<ParkingSpace, Void>() {
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the result of the operation
			 * @throws Exception if the operation cannot be completed correctly
			 */
			@Override
			protected ParkingSpace doInBackground() throws Exception {
				return handleUserVehicleExit(
						getCurrentUserId(),
						plate);
			}

			/**
			 * Finishes the worker task on the Swing screen thread.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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

	/**
	 * Sets the exit loading.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param loading true while the screen is waiting for an operation to finish
	 */
	private void setExitLoading(boolean loading) {
		if (mainMenuView == null) return;

		setParkingEntryButtonEnabled(!loading);
		setParkingExitButtonEnabled(!loading && exitAllowed);
		setMainMenuCursor(loading);
	}

	/**
	 * Shows exit error.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 */
	private void showExitError(String message) {
		showMainError("Parking exit", message);
	}


	/**
	 * Cancels the reservation shown in the admin space-details dialog.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
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
			/**
			 * Runs the worker task away from the Swing screen thread.
			 * <p>
			 * This runs away from the Swing screen thread so database work or longer calculations do not freeze the
			 * interface while the user is waiting.
			 * </p>
			 *
			 * @return the result of the operation
			 * @throws Exception if the operation cannot be completed correctly
			 */
			@Override
			protected Boolean doInBackground() throws Exception {
				return cancelReservationByPlate(plate);
			}

			/**
			 * Finishes the worker task on the Swing screen thread.
			 * <p>
			 * This runs when the worker has finished, so it can read the final result, restore buttons or cursors,
			 * and show the user a message if something failed.
			 * </p>
			 */
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


	/**
	 * Sets the main menu view and initialises the space details view.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param mainMenuView main menu view that will be shown or updated
	 */
	public void setMainMenuView(MainMenuView mainMenuView) {
		this.mainMenuView = mainMenuView;
		createFreshDetailsView();
	}

	/**
	 * Sets the user service.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param userService user service used to apply the needed project logic
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Sets the admin service.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param adminService admin service used to apply the needed project logic
	 */
	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}

	/**
	 * Sets the admin controller to refresh when parking status changes.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param adminController admin controller that coordinates the related screen action
	 */
	public void setAdminController(AdminController adminController) {
		this.adminController = adminController;
	}

	/**
	 * Sets the slot booking controller to refresh when parking status changes.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param slotBookingController slot booking controller that coordinates the related screen action
	 */
	public void setSlotBookingController(AdminSlotBookingController slotBookingController) {
		this.slotBookingController = slotBookingController;
	}

	/**
	 * Sets the statistics controller to refresh visible chart data when parking changes.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param statisticsController statistics controller that coordinates the related screen action
	 */
	public void setStatisticsController(StatisticsController statisticsController) {
		this.statisticsController = statisticsController;
	}

	/**
	 * Handles clear session state.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	public void clearSessionState() {
		exitAllowed = false;
		parkingStatusLoadId++;
		if (mainMenuView != null) {
			setParkingEntryButtonEnabled(false);
			setParkingExitButtonEnabled(false);
		}
		clearSpaceDetailsSessionState();
	}

	/**
	 * Sets the action used when the user logs out from a parking dialog.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param logoutAction logout action used by this operation
	 */
	public void setLogoutAction(Runnable logoutAction) {
		this.logoutAction = logoutAction;
	}

	/**
	 * Handles run on event dispatch thread.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param task task used by this operation
	 */
	private void runOnEventDispatchThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}

	/**
	 * Checks whether a simulation message should be shown in the log. The method supports the simulated
	 * traffic flow while keeping the parking updates coordinated with the rest of the system.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 * @return true when the condition is met, false otherwise
	 */
	private boolean hasSimulationMessage(String message) {
		return message != null && !message.isBlank();
	}

	/**
	 * Writes a simulation message through the controller logger. The method supports the simulated traffic
	 * flow while keeping the parking updates coordinated with the rest of the system.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 */
	private void logSimulationMessage(String message) {
		LOGGER.info(message);
	}

	/**
	 * Checks whether parking status table visible.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @return true when the condition is met, false otherwise
	 */
	private boolean isParkingStatusTableVisible() {
		return mainMenuView != null && mainMenuView.isParkingSlotsTableVisible();
	}

	/**
	 * Checks whether admin controller exists.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @return true when the condition is met, false otherwise
	 */
	private boolean hasAdminController() {
		return adminController != null;
	}

	/**
	 * Asks the admin controller to refresh its visible screen.
	 * <p>
	 * This method asks the service for fresh data and sends it back to the visible table or dialog when the
	 * screen needs to change.
	 * </p>
	 */
	private void refreshAdminControllerIfVisible() {
		adminController.refreshIfVisible();
	}

	/**
	 * Checks whether slot booking controller exists.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @return true when the condition is met, false otherwise
	 */
	private boolean hasSlotBookingController() {
		return slotBookingController != null;
	}

	/**
	 * Asks the booking controller to refresh its visible screen.
	 * <p>
	 * This method asks the service for fresh data and sends it back to the visible table or dialog when the
	 * screen needs to change.
	 * </p>
	 */
	private void refreshSlotBookingControllerIfVisible() {
		slotBookingController.refreshIfVisible();
	}

	/**
	 * Checks whether statistics controller exists.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @return true when the condition is met, false otherwise
	 */
	private boolean hasStatisticsController() {
		return statisticsController != null;
	}

	/**
	 * Handles record and refresh visible chart.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	private void recordAndRefreshVisibleChart() {
		statisticsController.recordAndRefreshVisibleChart();
	}

	/**
	 * Shows parking slots table.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 */
	private void showParkingSlotsTable() {
		mainMenuView.showParkingSlotsTable();
	}

	/**
	 * Loads parking status from service.
	 * <p>
	 * This method asks the service for fresh data and sends it back to the visible table or dialog when the
	 * screen needs to change.
	 * </p>
	 *
	 * @return the loaded parking status from service
	 */
	private List<ParkingSpace> loadParkingStatusFromService() {
		return parkingService.getParkingStatus();
	}

	/**
	 * Checks whether logged in user exists.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @return true when the condition is met, false otherwise
	 */
	private boolean hasLoggedInUser() {
		return userService != null && getCurrentUserId() > 0;
	}

	/**
	 * Gets the id of the logged-in user from the user service.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current current user ID
	 */
	private int getCurrentUserId() {
		return userService.getLastLoggedInUserId();
	}

	/**
	 * Loads current user parked spaces.
	 * <p>
	 * This method asks the service for fresh data and sends it back to the visible table or dialog when the
	 * screen needs to change.
	 * </p>
	 *
	 * @return the loaded current user parked spaces
	 */
	private List<ParkingSpace> loadCurrentUserParkedSpaces() {
		return parkingService.getParkedSpacesByUser(getCurrentUserId());
	}

	/**
	 * Adds parking status row.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param row row position in the table
	 */
	private void addParkingStatusRow(ParkingStatusRow row) {
		mainMenuView.addParkingSpaceToTable(row.getSpace(), row.isUserParkedVehicle());
	}

	/**
	 * Handles remove parking spaces not in.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param loadedCodes loaded codes used by this operation
	 */
	private void removeParkingSpacesNotIn(Set<String> loadedCodes) {
		mainMenuView.removeParkingSpacesNotIn(loadedCodes);
	}

	/**
	 * Handles log parking spaces loaded.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	private void logParkingSpacesLoaded() {
		LOGGER.fine("Parking spaces loaded successfully.");
	}

	/**
	 * Shows an error from the main menu view.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param title title used by this operation
	 * @param message message shown to the user or written to the log
	 */
	private void showMainError(String title, String message) {
		mainMenuView.showError(title, message);
	}

	/**
	 * Shows an information message from the main menu view.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param title title used by this operation
	 * @param message message shown to the user or written to the log
	 */
	private void showMainInfo(String title, String message) {
		mainMenuView.showInfo(title, message);
	}

	/**
	 * Sets the main menu cursor to waiting.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 */
	private void setMainMenuWaitCursor() {
		mainMenuView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	/**
	 * Sets the main menu default cursor.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 */
	private void setMainMenuDefaultCursor() {
		mainMenuView.setCursor(Cursor.getDefaultCursor());
	}

	/**
	 * Sets the main menu cursor according to a loading state.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param loading true while the screen is waiting for an operation to finish
	 */
	private void setMainMenuCursor(boolean loading) {
		if (loading) {
			setMainMenuWaitCursor();
		} else {
			setMainMenuDefaultCursor();
		}
	}

	/**
	 * Finds parking space.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param code parking space code involved in the operation
	 * @return the matching parking space, or null when it is not found
	 */
	private ParkingSpace findParkingSpace(String code) {
		return parkingService.findByCode(code);
	}

	/**
	 * Creates the space details view if it has not been created yet.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	private void createDetailsViewIfNeeded() {
		if (parkingSpaceDetailsView == null) {
			createFreshDetailsView();
		}
	}

	/**
	 * Creates a new space details view and connects its cancel action.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	private void createFreshDetailsView() {
		parkingSpaceDetailsView = new ParkingSpaceDetailsView(mainMenuView);
		setDetailsCancelReservationListener();
		setDetailsLogoutListener();
	}

	/**
	 * Sets the details cancel reservation listener.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 */
	private void setDetailsCancelReservationListener() {
		parkingSpaceDetailsView.setCancelReservationListener(e -> cancelReservationFromDetails());
	}

	/**
	 * Sets the details logout listener.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 */
	private void setDetailsLogoutListener() {
		parkingSpaceDetailsView.setLogoutListener(e -> logoutFromDetailsIfConfirmed());
	}

	/**
	 * Logs out from the details dialog after confirmation.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	private void logoutFromDetailsIfConfirmed() {
		if (logoutAction != null && parkingSpaceDetailsView.confirmLogout()) {
			parkingSpaceDetailsView.dispose();
			logoutAction.run();
		}
	}

	/**
	 * Handles display space details.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param space space used by this operation
	 */
	private void displaySpaceDetails(ParkingSpace space) {
		parkingSpaceDetailsView.displaySpaceDetails(space);
	}

	/**
	 * Checks whether the details dialog is currently open.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @return true when the condition is met, false otherwise
	 */
	private boolean isSpaceDetailsVisible() {
		return parkingSpaceDetailsView != null && parkingSpaceDetailsView.isVisible();
	}

	/**
	 * Gets the parking space code currently shown in the details dialog.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current displayed space code
	 */
	private String getDisplayedSpaceCode() {
		return parkingSpaceDetailsView.getDisplayedSpaceCode();
	}

	/**
	 * Closes the current parking space details dialog.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	private void disposeSpaceDetailsView() {
		parkingSpaceDetailsView.dispose();
	}

	/**
	 * Updates the current parking space details dialog.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param space space used by this operation
	 */
	private void updateSpaceDetails(ParkingSpace space) {
		parkingSpaceDetailsView.updateSpaceDetails(space);
	}

	/**
	 * Prompts for license plate from main menu.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param title title used by this operation
	 * @return the answer chosen by the user
	 */
	private String promptLicensePlateFromMainMenu(String title) {
		return mainMenuView.promptLicensePlate(title);
	}

	/**
	 * Finds occupied space by plate.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return the matching occupied space by plate, or null when it is not found
	 */
	private ParkingSpace findOccupiedSpaceByPlate(String plate) {
		return parkingService.findOccupiedSpaceByPlate(plate);
	}

	/**
	 * Finds active reservation by plate.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return the matching active reservation by plate, or null when it is not found
	 */
	private Reservation findActiveReservationByPlate(String plate) {
		return parkingService.findActiveReservationByPlate(plate);
	}

	/**
	 * Handles user vehicle entry.
	 * <p>
	 * This method is called from a user action, gathers what the screen needs, and passes the real work to the
	 * service layer.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 * @param plate license plate involved in the operation
	 * @param type vehicle type involved in the operation
	 * @return the result of the operation
	 */
	private ParkingSpace handleUserVehicleEntry(int userId, String plate, VehicleType type) {
		return parkingService.handleUserVehicleEntry(userId, plate, type);
	}

	/**
	 * Prompts for vehicle type for entry.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return the answer chosen by the user
	 */
	private VehicleType promptVehicleTypeForEntry(String plate) {
		return mainMenuView.promptVehicleTypeForEntry(plate);
	}

	/**
	 * Shows assigned parking entry.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param message message shown to the user or written to the log
	 * @param space space used by this operation
	 */
	private void showAssignedParkingEntry(String message, ParkingSpace space) {
		mainMenuView.showAssignedParkingEntry(message, space);
	}

	/**
	 * Sets the parking entry button enabled.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param enabled enabled used by this operation
	 */
	private void setParkingEntryButtonEnabled(boolean enabled) {
		mainMenuView.setParkingEntryButtonEnabled(enabled);
	}

	/**
	 * Sets the parking exit button enabled.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param enabled enabled used by this operation
	 */
	private void setParkingExitButtonEnabled(boolean enabled) {
		mainMenuView.setParkingExitButtonEnabled(enabled);
	}

	/**
	 * Prompts for exit vehicle.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param parkedSpaces parked spaces used by this operation
	 * @return the answer chosen by the user
	 */
	private ParkingSpace promptExitVehicle(List<ParkingSpace> parkedSpaces) {
		return mainMenuView.promptExitVehicle(parkedSpaces);
	}

	/**
	 * Handles user vehicle exit.
	 * <p>
	 * This method is called from a user action, gathers what the screen needs, and passes the real work to the
	 * service layer.
	 * </p>
	 *
	 * @param userId identifier of the user involved in the operation
	 * @param plate license plate involved in the operation
	 * @return the result of the operation
	 */
	private ParkingSpace handleUserVehicleExit(int userId, String plate) {
		return parkingService.handleUserVehicleExit(userId, plate);
	}

	/**
	 * Gets the space code currently shown in a details dialog.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @param detailsView details view that will be shown or updated
	 * @return the current displayed space code
	 */
	private String getDisplayedSpaceCode(ParkingSpaceDetailsView detailsView) {
		return detailsView.getDisplayedSpaceCode();
	}

	/**
	 * Gets the reserved plate currently shown in a details dialog.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @param detailsView details view that will be shown or updated
	 * @return the current displayed reservation plate
	 */
	private String getDisplayedReservationPlate(ParkingSpaceDetailsView detailsView) {
		return detailsView.getDisplayedReservationPlate();
	}

	/**
	 * Shows an error in a details dialog.
	 * <p>
	 * This method prepares the information needed for a dialog and lets the view handle the actual Swing
	 * display.
	 * </p>
	 *
	 * @param detailsView details view that will be shown or updated
	 * @param message message shown to the user or written to the log
	 */
	private void showDetailsError(ParkingSpaceDetailsView detailsView, String message) {
		detailsView.showError(message);
	}

	/**
	 * Confirms cancel reservation.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param detailsView details view that will be shown or updated
	 * @param spaceCode parking space code involved in the operation
	 * @param plate license plate involved in the operation
	 * @return the answer chosen by the user
	 */
	private boolean confirmCancelReservation(ParkingSpaceDetailsView detailsView, String spaceCode, String plate) {
		return detailsView.confirmCancelReservation(spaceCode, plate);
	}

	/**
	 * Enables or disables loading state in a details dialog.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param detailsView details view that will be shown or updated
	 * @param loading true while the screen is waiting for an operation to finish
	 */
	private void setDetailsLoading(ParkingSpaceDetailsView detailsView, boolean loading) {
		detailsView.setLoading(loading);
	}

	/**
	 * Handles cancel reservation by plate.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param plate license plate involved in the operation
	 * @return true when the condition is met, false otherwise
	 */
	private boolean cancelReservationByPlate(String plate) {
		return adminService.cancelReservationByPlate(plate);
	}

	/**
	 * Hides and disposes a details dialog.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 *
	 * @param detailsView details view that will be shown or updated
	 */
	private void hideAndDisposeDetailsView(ParkingSpaceDetailsView detailsView) {
		detailsView.setVisible(false);
		detailsView.dispose();
	}

	/**
	 * Handles clear space details session state.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	private void clearSpaceDetailsSessionState() {
		if (parkingSpaceDetailsView != null) {
			parkingSpaceDetailsView.clearSessionViewState();
			parkingSpaceDetailsView.dispose();
			parkingSpaceDetailsView = null;
		}
	}

	/**
	 * Handles rebuild parking slots panel.
	 * <p>
	 * This method keeps the controller action separate from the view code and from the business rule itself.
	 * </p>
	 */
	private void rebuildParkingSlotsPanel() {
		mainMenuView.rebuildParkingSlotsPanel();
	}
}
