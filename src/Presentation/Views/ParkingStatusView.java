package Presentation.Views;

import Business.Entities.ParkingSpace;
import Presentation.Controllers.ParkingController;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ParkingStatusView {
	private JDialog frame;
	private JTable parkingTable;
	private JButton refreshButton;
	private JButton backButton;
	private ParkingController controller;

	public ParkingStatusView(Frame parent) {
		frame = new JDialog(parent, "Parking status", false);
		initComponents();
	}

	private void initComponents() {
		String[] columns = { "Code", "Floor", "Status", "Reservation", "License plate" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		parkingTable = new JTable(model);
		refreshButton = new JButton("Refresh");
		backButton = new JButton("Close");

		javax.swing.JPanel panel = new javax.swing.JPanel(new BorderLayout(8, 8));
		panel.add(new JScrollPane(parkingTable), BorderLayout.CENTER);
		javax.swing.JPanel south = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT));
		south.add(refreshButton);
		south.add(backButton);
		panel.add(south, BorderLayout.SOUTH);

		frame.setContentPane(panel);
		frame.setSize(620, 400);
		frame.setLocationRelativeTo(frame.getParent());
	}

	public void updateTable(List<ParkingSpace> spaces) {
		if (parkingTable == null)
			return;

		String[] columns = { "Code", "Floor", "Status", "Reservation", "License plate" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		if (spaces != null) {
			for (ParkingSpace space : spaces) {
				String licensePlate = "";
				if (space.getParkedVehicle() != null) {
					licensePlate = space.getParkedVehicle().getLicensePlate();
				} else if (space.getReservation() != null && space.getReservation().getVehicle() != null) {
					licensePlate = space.getReservation().getVehicle().getLicensePlate();
				}
				model.addRow(new Object[] {
						space.getId(),
						space.getFloor(),
						space.isOccupied() ? "Occupied" : "Vacant",
						space.isReserved() ? "Reserved" : "Available",
						licensePlate
				});
			}
		}

		parkingTable.setModel(model);
	}

	public String getSelectedSpaceCode() {
		if (parkingTable == null)
			return null;
		int row = parkingTable.getSelectedRow();
		if (row < 0)
			return null;
		Object value = parkingTable.getValueAt(row, 0);
		return value != null ? value.toString() : null;
	}

	public void setController(ParkingController controller) {
		this.controller = controller;
		if (refreshButton != null) {
			refreshButton.addActionListener(e -> {
				if (this.controller != null)
					this.controller.loadParkingStatus();
			});
		}
		if (backButton != null) {
			backButton.addActionListener(e -> {
				if (frame != null)
					frame.setVisible(false);
			});
		}
	}

	public JDialog getFrame() {
		return frame;
	}
}
