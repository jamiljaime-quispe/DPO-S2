package Persistence;

import java.util.List;

import Business.Entities.Vehicle;

public interface VehicleDAO {
	public abstract void save(Vehicle vehicle);

	public abstract Vehicle findByPlate(String plate);

	public abstract List<Vehicle> findByUser(int userId);

	public abstract void delete(String plate);
}
