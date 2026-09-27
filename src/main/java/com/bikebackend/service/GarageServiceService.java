package com.bikebackend.service;

import com.bikebackend.dao.GarageServiceDAO;
import com.bikebackend.dao.GarageDAO;
import com.bikebackend.dao.ServiceDAO;
import com.bikebackend.exception.ConflictException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.model.Service;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;

public class GarageServiceService {

    private final GarageServiceDAO garageServiceDAO = new GarageServiceDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final GarageDAO garageDAO = new GarageDAO();

    public void addService(long gid, long sid) throws SQLException {
        throw new BadRequestException("duration and price are required when assigning a service to a garage");
    }

    public void addService(long gid, long sid, Integer duration, BigDecimal price) throws SQLException {
        if (duration == null || duration <= 0)
            throw new BadRequestException("duration must be greater than zero");
        if (price == null || price.signum() < 0)
            throw new BadRequestException("price must be zero or greater");
        if (garageDAO.getGarageById(gid) == null) throw new NotFoundException("Garage not found");
        if (serviceDAO.getServiceById(sid) == null) throw new NotFoundException("Service not found");
        if (garageServiceDAO.exists(gid, sid))
            throw new ConflictException("Service already assigned to this garage");
        garageServiceDAO.addService(gid, sid, duration, price);
    }

    public List<Service> getServices(long gid) throws SQLException {
        return serviceDAO.getServicesByGarage(gid);
    }

    public Service getService(long gid, long sid) throws SQLException {
        Service service = serviceDAO.getServiceByGarage(gid, sid);
        if (service == null) throw new NotFoundException("Service not found for this garage");
        return service;
    }

    public void removeService(long gid, long sid) throws SQLException {
        if (!garageServiceDAO.removeService(gid, sid))
            throw new NotFoundException("Service not found for this garage");
    }
}
