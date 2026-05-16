package Persistence;

import java.util.List;

import Business.Entities.Reservation;

public interface ReservationDAO {
	public void save(Reservation reservation);

	public void delete(int id);

	public Reservation findById(int id);

	public List<Reservation> findByUser(int userId);

	public Reservation findByPlate(String plate);

	public List<Reservation> findAll();

	public void update(Reservation reservation);
}
