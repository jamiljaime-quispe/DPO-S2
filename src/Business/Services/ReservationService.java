package Business.Services;

import Business.Entities.Client;
import Business.Entities.ParkingSpace;
import Business.Entities.Reservation;
import Business.Entities.User;
import Business.Entities.Vehicle;
import Business.Entities.VehicleType;
import Persistence.ParkingSpaceDAO;
import Persistence.ReservationDAO;
import Persistence.UserDAO;
import Persistence.VehicleDAO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the lifecycle of parking space reservations.
 */
public class ReservationService {
    private ReservationDAO reservationDAO;
    private ParkingSpaceDAO parkingSpaceDAO;
    private VehicleDAO vehicleDAO;
    private UserDAO userDAO;

    /**
     * @param reservationDAO  DAO for reservations
     * @param parkingSpaceDAO DAO for parking spaces
     * @param vehicleDAO      DAO for vehicles
     * @param userDAO         DAO to resolve the reserving user (for new vehicle rows and ownership checks)
     */
    public ReservationService(ReservationDAO reservationDAO, ParkingSpaceDAO parkingSpaceDAO,
                              VehicleDAO vehicleDAO, UserDAO userDAO) {
        this.reservationDAO = reservationDAO;
        this.parkingSpaceDAO = parkingSpaceDAO;
        this.vehicleDAO = vehicleDAO;
        this.userDAO = userDAO;
    }

    /**
     * Creates a reservation from the plate and vehicle type entered by the user (spec §2.8):
     * validates the space matches the type and is still free, ensures no other active reservation
     * uses the same plate, registers the vehicle for this user when the plate is new, then persists.
     * @param userId    ID of the user making the reservation
     * @param plate     license plate entered by the user
     * @param type      vehicle type entered by the user (must match the space type)
     * @param spaceCode code of the space to reserve
     * @return created Reservation, or null if validation fails or the plate is already used by another user
     */
    public Reservation createReservation(int userId, String plate, VehicleType type, String spaceCode) {
        if (plate == null || type == null || spaceCode == null)
            return null;
        String normalizedPlate = plate.trim();
        if (normalizedPlate.isEmpty())
            return null;
        String normalizedCode = spaceCode.trim();

        User user = userDAO.findById(userId);
        if (user == null)
            return null;
        String username = user.getUsername();

        ParkingSpace space = parkingSpaceDAO.findByCode(normalizedCode);
        if (space == null || !space.isAvailable() || space.getVehicleType() != type)
            return null;

        Reservation plateTaken = reservationDAO.findByPlate(normalizedPlate);
        if (plateTaken != null && plateTaken.isActive())
            return null;

        Vehicle existing = vehicleDAO.findByPlate(normalizedPlate);
        Vehicle vehicle;
        if (existing != null) {
            if (existing.getOwner() == null || !existing.getOwner().equalsIgnoreCase(username))
                return null;
            if (existing.getType() != type)
                return null;
            vehicle = existing;
        } else {
            vehicle = new Vehicle(normalizedPlate, type, username, false);
            vehicleDAO.save(vehicle);
        }

        Client client = user instanceof Client ? (Client) user
                : new Client(user.getId(), username, user.getEmail(), user.getPassword(), user.getUserType(),
                new ArrayList<>());
        Reservation reservation = new Reservation(0, client, vehicle, space, LocalDateTime.now());
        space.reserve(reservation);
        parkingSpaceDAO.update(space);
        reservationDAO.save(reservation);
        return reservation;
    }

    /**
     * Cancels a reservation by ID, marking it inactive and freeing the space.
     * @param reservationId ID of the reservation to cancel
     */
    public void cancelReservation(int reservationId) {
        Reservation reservation = reservationDAO.findById(reservationId);
        if (reservation == null || !reservation.isActive()) return;

        reservation.cancel();
        reservationDAO.update(reservation);

        ParkingSpace space = reservation.getParkingSpace();
        if (space != null) {
            space.cancelReservation();
            parkingSpaceDAO.update(space);
        }
    }

    /**
     * Cancels the active reservation for a license plate only if it belongs to the given user
     * (vehicle row is tied to that user's account).
     * @param userId id of the logged-in user
     * @param plate  license plate to match
     * @return true if an active reservation was cancelled
     */
    public boolean cancelReservationByPlate(int userId, String plate) {
        if (plate == null || plate.isBlank())
            return false;
        Reservation reservation = reservationDAO.findByPlate(plate.trim());
        if (reservation == null || !reservation.isActive())
            return false;
        if (!reservationBelongsToUser(reservation, userId))
            return false;
        cancelReservation(reservation.getId());
        return true;
    }

    private boolean reservationBelongsToUser(Reservation reservation, int userId) {
        User sessionUser = userDAO.findById(userId);
        if (sessionUser == null)
            return false;
        Vehicle v = reservation.getVehicle();
        if (v == null || v.getOwner() == null)
            return false;
        return sessionUser.getUsername().equalsIgnoreCase(v.getOwner());
    }

    /**
     * Returns all active reservations belonging to a user.
     * @param userId user ID
     * @return list of active reservations
     */
    public List<Reservation> getReservationsByUser(int userId) {
        List<Reservation> all = reservationDAO.findByUser(userId);
        List<Reservation> active = new ArrayList<>();
        for (Reservation r : all) {
            if (r.isActive()) active.add(r);
        }
        return active;
    }

    /**
     * Returns all spaces available for reservation for the given vehicle type.
     * @param type vehicle type
     * @return list of available spaces
     */
    public List<ParkingSpace> getAvailableSpaces(VehicleType type) {
        return parkingSpaceDAO.findAvailableByType(type);
    }

    /**
     * Returns admin-cancelled reservations not yet shown to the user and marks them as notified.
     * Used by AuthController at login to display cancellation alerts.
     * @param userId user ID
     * @return list of unnotified admin-cancelled reservations
     */
    public List<Reservation> consumeCancelledByAdminNotifications(int userId) {
        List<Reservation> all = reservationDAO.findByUser(userId);
        List<Reservation> pending = new ArrayList<>();
        for (Reservation r : all) {
            if (r.isCancelledByAdmin() && !r.isNotified()) {
                pending.add(r);
                r.setNotified(true);
                reservationDAO.update(r);
            }
        }
        return pending;
    }

    /**
     * Checks whether a specific space is available for reservation.
     * @param spaceCode space code
     * @return true if the space exists and is available
     */
    public boolean isSpaceAvailable(String spaceCode) {
        ParkingSpace space = parkingSpaceDAO.findByCode(spaceCode);
        return space != null && space.isAvailable();
    }
}
