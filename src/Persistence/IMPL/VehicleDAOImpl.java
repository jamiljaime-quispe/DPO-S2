package Persistence.IMPL;

import java.util.ArrayList;
import java.util.List;

import Business.Entities.Reservation;
import Business.Entities.Vehicle;
import Business.Entities.*;
import Persistence.DatabaseManager;
import Persistence.VehicleDAO;

public class VehicleDAOImpl implements VehicleDAO {
    private final DatabaseManager db;

    public VehicleDAOImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void save(Vehicle vehicle) {
        System.out.println("Saving Vehicle: " + vehicle);
    }

    @Override
    public Vehicle findByPlate(String plate) {
        return new Vehicle(
                plate,
                VehicleType.CAR,
                "user1",
                false);
                //new Reservation[0]);
    }

    @Override
    public List<Vehicle> findByUser(int userId) {
        List<Vehicle> list = new ArrayList<>();

        list.add(new Vehicle("ABC123", VehicleType.CAR, "user" + userId, false));//, new Reservation[0]));

        return list;
    }

    @Override
    public void delete(String plate) {
        System.out.println("Deleting Vehicle with plate: " + plate);
    }
}