package Persistence;

import java.util.List;

import Business.Entities.ParkingSpace;
import Business.Entities.VehicleType;

public interface ParkingSpaceDAO {
	public void save(ParkingSpace space);

	public void update(ParkingSpace space);

	public void updateDetails(ParkingSpace space);

	public void delete(String code);

	public ParkingSpace findByCode(String code);

	public List<ParkingSpace> findAll();

	public List<ParkingSpace> findAvailableByType(VehicleType type);
}
