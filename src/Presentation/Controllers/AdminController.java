package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Business.Services.ParkingService;
import Presentation.Views.AdminParkingManagementView;

import javax.swing.SwingWorker;
import java.util.List;

public class AdminController {
    private static final int BACKGROUND_TEST_DELAY_MS = 300;
    private static final int ROW_LOAD_DELAY_MS = 100;

    private AdminParkingManagementView adminView;
    private ParkingService parkingService;

    public AdminController(AdminParkingManagementView adminView, ParkingService parkingService) {
        this.adminView = adminView;
        this.parkingService = parkingService;
        adminView.setController(this);
    }

    public void showView() {
        loadSpaces();
        adminView.setVisible(true);
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
                    boolean success = parkingService.deleteParkingSpace(code);
                    if (!success) {
                        errorMessage = "Cannot delete space \"" + code + "\": it is currently occupied.";
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
        adminView.setLoading(true);
        adminView.clearSpacesTable();

        new SwingWorker<Void, ParkingSpace>() {
            @Override
            protected Void doInBackground() {
                List<ParkingSpace> spaces = parkingService.getAllSpaces();

                for (ParkingSpace space : spaces) {
                    delayRowLoad();
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    publish(space);
                }

                return null;
            }

            @Override
            protected void process(List<ParkingSpace> chunks) {
                for (ParkingSpace space : chunks) {
                    adminView.addSpaceToTable(space);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    adminView.showError("Failed to load spaces: " + e.getMessage());
                } finally {
                    adminView.setLoading(false);
                }
            }
        }.execute();
    }

    private void delayRowLoad() {
        try {
            Thread.sleep(ROW_LOAD_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateDatabaseDelay() {
        try {
            Thread.sleep(BACKGROUND_TEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
