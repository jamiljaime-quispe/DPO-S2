package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.VehicleType;
import Business.Services.ReservationService;
import Presentation.Views.ReservationManagementView;
import Presentation.Views.ReservationView;

import java.awt.Component;
import java.util.List;

import javax.swing.JOptionPane;

public class ReservationController {
    private ReservationView reservationView;
    private ReservationManagementView reservationManagementView;
    private ReservationService reservationService;
    private int currentUserId;

    /**
     *
     */
    public ReservationController(ReservationView reservationView,
                                 ReservationManagementView reservationManagementView, ReservationService reservationService) {
        this.reservationView = reservationView;
        this.reservationManagementView = reservationManagementView;
        this.reservationService = reservationService;
    }

    public void searchAvailableSpaces(VehicleType type) {
        List<ParkingSpace> spaces = reservationService.getAvailableSpaces(type);
        reservationView.updateAvailableSpaces(spaces);
    }

    public void reserveSpace(String plate, VehicleType type, String spaceCode) {
        Component parent = reservationView.getFrame() != null ? reservationView.getFrame() : null;
        if (currentUserId == 0) {
            JOptionPane.showMessageDialog(parent, "No user session for reservations.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (plate == null || plate.isBlank()) {
            JOptionPane.showMessageDialog(parent, "Enter a license plate.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (spaceCode == null || spaceCode.isBlank()) {
            JOptionPane.showMessageDialog(parent, "Select an available parking space from the list.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        Reservation created = reservationService.createReservation(currentUserId, plate, type, spaceCode);
        if (created == null) {
            JOptionPane.showMessageDialog(parent,
                    "Could not create the reservation. The space may no longer be free, the vehicle type must match the space, "
                            + "this plate may already have an active reservation, or the plate may already be registered to another user.",
                    "Reservation failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(parent,
                "Reservation confirmed for space " + created.getParkingSpace().getId() + ".",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        searchAvailableSpaces(type);
        if (currentUserId != 0)
            loadUserReservations(currentUserId);
    }

    public void cancelReservation(String plate) {
        Component parent = reservationView.getFrame();
        if (currentUserId == 0) {
            JOptionPane.showMessageDialog(parent, "No user session.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (plate == null || plate.isBlank()) {
            JOptionPane.showMessageDialog(parent, "Select a reservation to cancel.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String typed = JOptionPane.showInputDialog(parent,
                "Confirm cancellation by typing the vehicle license plate:", plate);
        if (typed == null)
            return;
        if (!typed.trim().equalsIgnoreCase(plate.trim())) {
            JOptionPane.showMessageDialog(parent, "The plate does not match. Cancellation aborted.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean cancelled = reservationService.cancelReservationByPlate(currentUserId, typed.trim());
        JOptionPane.showMessageDialog(parent,
                cancelled ? "Reservation cancelled."
                        : "Could not cancel (no active reservation for this plate on your account).",
                "Done", cancelled ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (currentUserId != 0)
            loadUserReservations(currentUserId);
    }

    public void loadUserReservations(int userId) {
        this.currentUserId = userId;
        reservationManagementView.updateReservations(reservationService.getReservationsByUser(userId));
        searchAvailableSpaces(reservationView.getVehicleType());
    }
}
