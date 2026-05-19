package Presentation.Views;

import Business.Entities.Reservation;
import Presentation.Controllers.ReservationController;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ReservationManagementView {
    private JPanel contentPanel;
    private JTable reservationTable;
    private JButton cancelReservationButton;
    private JButton backButton;
    private ReservationController controller;

    public void initComponents() {
        String[] resCols = { "License plate", "Space type", "Reserved on" };
        DefaultTableModel resTableModel = new DefaultTableModel(resCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reservationTable = new JTable(resTableModel);
        cancelReservationButton = new JButton("Cancel selected");
        backButton = new JButton("Close");

        contentPanel = new JPanel(new BorderLayout(8, 8));
        contentPanel.add(new JScrollPane(reservationTable), BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(cancelReservationButton);
        south.add(backButton);
        contentPanel.add(south, BorderLayout.SOUTH);
    }

    public JPanel getContentPanel() {
        return contentPanel;
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

    public JButton getCancelReservationButton() {
        return cancelReservationButton;
    }

    public JButton getBackButton() {
        return backButton;
    }
}
