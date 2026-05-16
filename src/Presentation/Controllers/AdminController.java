package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Business.Services.ParkingService;
import Presentation.Views.AdminParkingManagementView;

import javax.swing.SwingWorker;
import java.util.List;

public class AdminController {
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
        try {
            ParkingSpace space = new ParkingSpace(code, floor, type, false, false, null, null);
            parkingService.createParkingSpace(space);
            loadSpaces();
        } catch (Exception e) {
            adminView.showError("Failed to create space: " + e.getMessage());
        }
    }

    public void editSpace(String code, int floor, VehicleType type) {
        try {
            ParkingSpace space = parkingService.findByCode(code);
            if (space == null) {
                adminView.showError("Space not found: " + code);
                return;
            }
            space.setFloor(floor);
            space.setVehicleType(type);
            parkingService.updateParkingSpaceDetails(space);
            loadSpaces();
        } catch (Exception e) {
            adminView.showError("Failed to edit space: " + e.getMessage());
        }
    }

    public void deleteSpace(String code) {
        try {
            boolean success = parkingService.deleteParkingSpace(code);
            if (!success) {
                adminView.showError("Cannot delete space \"" + code + "\": it is currently occupied.");
            }
            loadSpaces();
        } catch (Exception e) {
            adminView.showError("Failed to delete space: " + e.getMessage());
        }
    }

    public void loadSpaces() {
        new SwingWorker<List<ParkingSpace>, Void>() {
            @Override
            protected List<ParkingSpace> doInBackground() {
                return parkingService.getAllSpaces();
            }

            @Override
            protected void done() {
                try {
                    adminView.updateSpaces(get());
                } catch (Exception e) {
                    adminView.showError("Failed to load spaces: " + e.getMessage());
                }
            }
        }.execute();
    }
}
