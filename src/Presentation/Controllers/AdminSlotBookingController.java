package Presentation.Controllers;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;
import Business.Services.AdminService;
import Business.Services.ParkingService;
import Presentation.Views.AdminSlotBookingManagementView;

import javax.swing.SwingWorker;
import java.util.List;

public class AdminSlotBookingController {
    private static final int BACKGROUND_TEST_DELAY_MS = 300;
    private static final int ROW_LOAD_DELAY_MS = 100;

    private AdminSlotBookingManagementView bookingView;
    private ParkingService parkingService;
    private AdminService adminService;

    public AdminSlotBookingController(AdminSlotBookingManagementView bookingView, ParkingService parkingService,
                                      AdminService adminService) {
        this.bookingView = bookingView;
        this.parkingService = parkingService;
        this.adminService = adminService;
        bookingView.setController(this);
    }

    public void showView(int mode) {
        bookingView.setMode(mode);
        loadBookings();
        bookingView.setVisible(true);
    }

    public void loadBookings() {
        bookingView.setLoading(true);
        bookingView.clearBookingsTable();

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
                    bookingView.addBookingToTable(space);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    bookingView.showError("Failed to load slot bookings: " + e.getMessage());
                } finally {
                    bookingView.setLoading(false);
                }
            }
        }.execute();
    }

    public void createBooking(String plate, VehicleType type, String spaceCode) {
        runBookingOperation("Booking creation UI is ready. Reservation persistence wiring is the next step.");
    }

    public void editBooking(String originalSpaceCode, String plate, VehicleType type, String spaceCode) {
        runBookingOperation("Booking reassignment UI is ready. Reservation persistence wiring is the next step.");
    }

    public void deleteBooking(String spaceCode, String plate) {
        if (plate == null || plate.isBlank()) {
            bookingView.showError("Cannot cancel a booking without a license plate.");
            return;
        }

        bookingView.setLoading(true);
        new SwingWorker<Boolean, Void>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    simulateDatabaseDelay();
                    return adminService.cancelReservationByPlate(plate);
                } catch (Exception e) {
                    errorMessage = "Failed to cancel booking: " + e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean cancelled = get();
                    if (cancelled) {
                        bookingView.showInfo("Booking for space \"" + spaceCode
                                + "\" has been cancelled. The space may still show occupied if a vehicle is parked there.");
                        loadBookings();
                    } else {
                        bookingView.setLoading(false);
                        bookingView.showError(errorMessage != null
                                ? errorMessage
                                : "No active booking found for license plate \"" + plate + "\".");
                    }
                } catch (Exception e) {
                    bookingView.setLoading(false);
                    bookingView.showError("Failed to cancel booking: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void runBookingOperation(String successMessage) {
        bookingView.setLoading(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                simulateDatabaseDelay();
                return successMessage;
            }

            @Override
            protected void done() {
                try {
                    bookingView.showInfo(get());
                } catch (Exception e) {
                    bookingView.showError("Failed to update slot bookings: " + e.getMessage());
                } finally {
                    bookingView.setLoading(false);
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
