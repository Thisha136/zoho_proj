package com.bikebackend.service;

import com.bikebackend.dao.ServiceDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class ServiceService {

    private final ServiceDAO serviceDAO = new ServiceDAO();

    public long createService(Service service) throws SQLException {
        validateServiceType(service.getServiceType());
        if (service.getServiceName() == null || service.getServiceName().isBlank())
            throw new BadRequestException("Service name is required");
        return serviceDAO.createService(service);
    }

    public List<Service> getServices(String serviceType, String sortOrder) throws SQLException {
        return serviceDAO.getAllServices(serviceType, sortOrder);
    }

    public Service getService(long sid) throws SQLException {
        Service service = serviceDAO.getServiceById(sid);
        if (service == null) throw new NotFoundException("Service not found");
        return service;
    }

    public List<Service> getServicesByGarage(long gid) throws SQLException {
        return serviceDAO.getServicesByGarage(gid);
    }

    public Service getServiceByGarage(long gid, long sid) throws SQLException {
        Service service = serviceDAO.getServiceByGarage(gid, sid);
        if (service == null) throw new NotFoundException("Service not found for this garage");
        return service;
    }

    public void updateService(Service service) throws SQLException {
        validateServiceType(service.getServiceType());
        if (service.getServiceName() == null || service.getServiceName().isBlank())
            throw new BadRequestException("Service name is required");
        if (!serviceDAO.updateService(service))
            throw new NotFoundException("Service not found");
    }

    public void partialUpdateService(long sid, Service incoming) throws SQLException {
        Service existing = serviceDAO.getServiceById(sid);
        if (existing == null) throw new NotFoundException("Service not found");
        if (incoming.getServiceName() != null) existing.setServiceName(incoming.getServiceName());
        if (incoming.getServiceType() != null) {
            validateServiceType(incoming.getServiceType());
            existing.setServiceType(incoming.getServiceType());
        }
        serviceDAO.updateService(existing);
    }

    private void validateServiceType(String serviceType) {
        if (serviceType == null || serviceType.isBlank())
            throw new BadRequestException("service_type is required");
        if (!Set.of("SERVICE", "PART", "MAINTENANCE").contains(serviceType.toUpperCase()))
            throw new BadRequestException("service_type must be SERVICE, PART, or MAINTENANCE");
    }

    public void deleteService(long sid) throws SQLException {
        if (!serviceDAO.deleteService(sid))
            throw new NotFoundException("Service not found");
    }
}
