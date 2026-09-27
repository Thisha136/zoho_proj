package com.bikebackend.service;

import com.bikebackend.dao.GarageDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.Garage;

import java.sql.SQLException;
import java.util.List;

public class GarageService {

    private final GarageDAO garageDAO = new GarageDAO();

    public long createGarage(Garage garage) throws SQLException {
        if (garage.getGarageName() == null || garage.getGarageName().isBlank())
            throw new BadRequestException("Garage name is required");
        if (garage.getCity() == null || garage.getCity().isBlank())
            throw new BadRequestException("City is required");
        return garageDAO.createGarage(garage);
    }

    public List<Garage> getGarages(Long variantId) throws SQLException {
        return garageDAO.getAllGarages(variantId);
    }

    public Garage getGarage(long gid) throws SQLException {
        Garage garage = garageDAO.getGarageById(gid);
        if (garage == null) throw new NotFoundException("Garage not found");
        return garage;
    }

    public void updateGarage(Garage garage) throws SQLException {
        if (garage.getGarageName() == null || garage.getGarageName().isBlank())
            throw new BadRequestException("Garage name is required");
        if (!garageDAO.updateGarage(garage))
            throw new NotFoundException("Garage not found");
    }

    public void partialUpdateGarage(long gid, Garage incoming) throws SQLException {
        Garage existing = garageDAO.getGarageById(gid);
        if (existing == null) throw new NotFoundException("Garage not found");
        if (incoming.getGarageName()  != null) existing.setGarageName(incoming.getGarageName());
        if (incoming.getAddressLine() != null) existing.setAddressLine(incoming.getAddressLine());
        if (incoming.getCity()        != null) existing.setCity(incoming.getCity());
        if (incoming.getState()       != null) existing.setState(incoming.getState());
        if (incoming.getPincode()     != null) existing.setPincode(incoming.getPincode());
        garageDAO.updateGarage(existing);
    }
}
