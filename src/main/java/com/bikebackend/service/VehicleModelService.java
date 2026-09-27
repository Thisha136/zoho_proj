package com.bikebackend.service;

import com.bikebackend.dao.VehicleModelDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.VehicleModel;

import java.sql.SQLException;
import java.util.List;

public class VehicleModelService {

    private final VehicleModelDAO modelDAO = new VehicleModelDAO();

    public long createModel(VehicleModel model) throws SQLException {
        if (model.getBid() == null)
            throw new BadRequestException("Brand ID (bid) is required");
        if (model.getModelName() == null || model.getModelName().isBlank())
            throw new BadRequestException("Model name is required");
        return modelDAO.createModel(model);
    }

    public List<VehicleModel> getModels(Long bid) throws SQLException {
        return modelDAO.getAllModels(bid);
    }

    public VehicleModel getModel(long modelId) throws SQLException {
        VehicleModel model = modelDAO.getModelById(modelId);
        if (model == null) throw new NotFoundException("Model not found");
        return model;
    }

    public void updateModel(VehicleModel model) throws SQLException {
        if (model.getModelName() == null || model.getModelName().isBlank())
            throw new BadRequestException("Model name is required");
        if (!modelDAO.updateModel(model))
            throw new NotFoundException("Model not found");
    }

    public void partialUpdateModel(long modelId, VehicleModel incoming) throws SQLException {
        VehicleModel existing = modelDAO.getModelById(modelId);
        if (existing == null) throw new NotFoundException("Model not found");
        if (incoming.getModelName() != null) existing.setModelName(incoming.getModelName());
        if (incoming.getBid()       != null) existing.setBid(incoming.getBid());
        modelDAO.updateModel(existing);
    }

    public void deleteModel(long modelId) throws SQLException {
        if (!modelDAO.deleteModel(modelId))
            throw new NotFoundException("Model not found");
    }
}
