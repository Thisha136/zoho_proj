package com.bikebackend.service;

import com.bikebackend.dao.GarageVariantDAO;
import com.bikebackend.dao.GarageDAO;
import com.bikebackend.dao.ServiceRequestDAO;
import com.bikebackend.dao.VariantDAO;
import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.ConflictException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.GarageVariant;

import java.sql.SQLException;
import java.sql.Connection;
import java.util.List;

public class GarageVariantService {

    private final GarageVariantDAO garageVariantDAO = new GarageVariantDAO();
    private final GarageDAO garageDAO = new GarageDAO();
    private final VariantDAO variantDAO = new VariantDAO();
    private final ServiceRequestDAO serviceRequestDAO = new ServiceRequestDAO();

    public void addVariant(long gid, long variantId) throws SQLException {
        if (garageDAO.getGarageById(gid) == null) throw new NotFoundException("Garage not found");
        if (variantDAO.getVariantById(variantId) == null) throw new NotFoundException("Variant not found");
        if (garageVariantDAO.exists(gid, variantId))
            throw new ConflictException("Variant already assigned to this garage");
        garageVariantDAO.addVariant(gid, variantId);
    }

    public List<GarageVariant> getVariants(long gid) throws SQLException {
        return garageVariantDAO.getVariantsByGarage(gid);
    }

    public void removeVariant(long gid, long variantId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!garageVariantDAO.removeVariant(connection, gid, variantId))
                    throw new NotFoundException("Variant not found for this garage");

                serviceRequestDAO.cancelActivePendingForGarageVariant(connection, gid, variantId);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
                throw e;
            }
        }
    }
}
