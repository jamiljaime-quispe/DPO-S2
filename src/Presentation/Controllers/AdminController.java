package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Presentation.Views.AdminParkingManagementView;

import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminController {
    private static final int BACKGROUND_TEST_DELAY_MS = 300;
    private static final int ROW_LOAD_DELAY_MS = 100;

    private AdminParkingManagementView adminView;
    private ParkingService parkingService;
    private AdminService adminService;
    private volatile int spacesLoadId;

    public AdminController(AdminParkingManagementView adminView, ParkingService parkingService) {
        this.adminView = adminView;
        this.parkingService = parkingService;
        adminView.setController(this);
    }

    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    public void showView() {
        adminView.clearSpacesTable();
        loadSpaces();
        adminView.setVisible(true);
    }

    public void refreshIfVisible() {
        if (adminView == null || !adminView.isVisible()) return;

        if (SwingUtilities.isEventDispatchThread()) {
            loadSpaces();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    loadSpaces();
                }
            });
        }
    }

    public void createSpace(String code, int floor, VehicleType type) {
        adminView.setLoading(true);
        new SwingWorker<Boolean, Void>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    simulateDatabaseDelay();
                    ParkingSpace space = new ParkingSpace(code, floor, type, false, false, null, null);
                    parkingService.createParkingSpace(space);
                    return true;
                } catch (Exception e) {
                    errorMessage = "Failed to create space: " + e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadSpaces();
                    } else {
                        adminView.setLoading(false);
                        adminView.showError(errorMessage);
                    }
                } catch (Exception e) {
                    adminView.setLoading(false);
                    adminView.showError("Failed to create space: " + e.getMessage());
                }
            }
        }.execute();
    }

    public void editSpace(String code, int floor, VehicleType type) {
        adminView.setLoading(true);
        new SwingWorker<Boolean, Void>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    simulateDatabaseDelay();
                    ParkingSpace space = parkingService.findByCode(code);
                    if (space == null) {
                        errorMessage = "Space not found: " + code;
                        return false;
                    }
                    space.setFloor(floor);
                    space.setVehicleType(type);
                    parkingService.updateParkingSpaceDetails(space);
                    return true;
                } catch (Exception e) {
                    errorMessage = "Failed to edit space: " + e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadSpaces();
                    } else {
                        adminView.setLoading(false);
                        adminView.showError(errorMessage);
                    }
                } catch (Exception e) {
                    adminView.setLoading(false);
                    adminView.showError("Failed to edit space: " + e.getMessage());
                }
            }
        }.execute();
    }

    public void deleteSpace(String code) {
        adminView.setLoading(true);
        new SwingWorker<Boolean, Void>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    simulateDatabaseDelay();
                    ParkingSpace space = parkingService.findByCode(code);
                    if (space == null) {
                        errorMessage = "Space not found: " + code;
                        return false;
                    }
                    if (space.isOccupied()) {
                        errorMessage = "Cannot delete space \"" + code + "\": it is currently occupied.";
                        return false;
                    }
                    if (adminService != null) {
                        adminService.reassignOrDeleteReservation(code);
                    }
                    boolean success = parkingService.deleteParkingSpace(code);
                    if (!success) {
                        errorMessage = "Could not delete space \"" + code + "\".";
                    }
                    return success;
                } catch (Exception e) {
                    errorMessage = "Failed to delete space: " + e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadSpaces();
                    } else {
                        adminView.setLoading(false);
                        adminView.showError(errorMessage);
                    }
                } catch (Exception e) {
                    adminView.setLoading(false);
                    adminView.showError("Failed to delete space: " + e.getMessage());
                }
            }
        }.execute();
    }

    public void loadSpaces() {
        spacesLoadId++;
        int loadId = spacesLoadId;

        adminView.setLoading(true);

        new SwingWorker<Set<String>, ParkingSpace>() {
            @Override
            protected Set<String> doInBackground() {
                List<ParkingSpace> spaces = parkingService.getAllSpaces();
                Set<String> loadedCodes = new HashSet<>();

                for (ParkingSpace space : spaces) {
                    loadedCodes.add(space.getId());
                    delayRowLoad();
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    if (loadId == spacesLoadId) {
                        publish(space);
                    }
                }

                return loadedCodes;
            }

            @Override
            protected void process(List<ParkingSpace> chunks) {
                if (loadId != spacesLoadId) return;

                for (ParkingSpace space : chunks) {
                    adminView.addSpaceToTable(space);
                }
            }

            @Override
            protected void done() {
                if (loadId != spacesLoadId) return;

                try {
                    Set<String> loadedCodes = get();
                    adminView.removeSpacesNotIn(loadedCodes);
                    adminView.closeActiveDeleteDialogIfTargetUnavailable();
                    adminView.closeActiveEditDialogIfTargetUnavailable();
                } catch (Exception e) {
                    adminView.showError("Failed to load spaces: " + e.getMessage());
                } finally {
                    adminView.setLoading(false);
                }
            }
        }.execute();
    }

    private void delayRowLoad() {
        // Row-by-row display delay disabled because automatic refreshes should be fast.
        // try {
        //     Thread.sleep(ROW_LOAD_DELAY_MS);
        // } catch (InterruptedException e) {
        //     Thread.currentThread().interrupt();
        // }
    }

    private void simulateDatabaseDelay() {
        try {
            Thread.sleep(BACKGROUND_TEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
