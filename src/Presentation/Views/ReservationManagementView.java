package Presentation.Views;

import Business.Entities.Reservation;
import Presentation.Controllers.ReservationController;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ReservationManagementView {
    private JFrame frame;
    private JTable reservationTable;
    private JButton cancelReservationButton;
    private JButton backButton;
    private ReservationController controller;

    /**
     *
     */
    public ReservationManagementView(JFrame frame, JTable reservationTable,
                                     JButton cancelReservationButton, JButton backButton, ReservationController controller) {
        this.frame = frame;
        this.reservationTable = reservationTable;
        this.cancelReservationButton = cancelReservationButton;
        this.backButton = backButton;
        this.controller = controller;
    }

    public void updateReservations(List<Reservation> reservations) {
        if (reservationTable == null)
            return;
        DefaultTableModel model = (DefaultTableModel) reservationTable.getModel();
        model.setRowCount(0);
        if (reservations == null)
            return;
        for (Reservation r : reservations) {
            String plate = r.getVehicle() != null ? r.getVehicle().getLicensePlate() : "";
            Object spaceType = r.getParkingSpace() != null ? r.getParkingSpace().getVehicleType() : "";
            Object when = r.getReservationDate();
            model.addRow(new Object[] { plate, spaceType, when });
        }
    }

    public String getSelectedReservationPlate() {
        if (reservationTable == null)
            return null;
        int row = reservationTable.getSelectedRow();
        if (row < 0)
            return null;
        Object plate = reservationTable.getModel().getValueAt(row, 0);
        return plate != null ? plate.toString() : null;
    }

    public void setController(ReservationController controller) {
        this.controller = controller;
    }

    public JFrame getFrame() {
        return frame;
    }
}
