package com.bikebackend.service;

import com.bikebackend.dao.VariantDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.Variant;

import java.sql.SQLException;
import java.util.List;

public class VariantService {

    private final VariantDAO variantDAO = new VariantDAO();

    public long createVariant(Variant variant) throws SQLException {
        if (variant.getModelId() == null)
            throw new BadRequestException("Model ID is required");
        if (variant.getVariantName() == null || variant.getVariantName().isBlank())
            throw new BadRequestException("Variant name is required");
        return variantDAO.createVariant(variant);
    }

    public List<Variant> getVariants(Long modelId) throws SQLException {
        return variantDAO.getAllVariants(modelId);
    }

    public Variant getVariant(long variantId) throws SQLException {
        Variant variant = variantDAO.getVariantById(variantId);
        if (variant == null) throw new NotFoundException("Variant not found");
        return variant;
    }

    public void updateVariant(Variant variant) throws SQLException {
        if (variant.getVariantName() == null || variant.getVariantName().isBlank())
            throw new BadRequestException("Variant name is required");
        if (!variantDAO.updateVariant(variant))
            throw new NotFoundException("Variant not found");
    }

    public void partialUpdateVariant(long variantId, Variant incoming) throws SQLException {
        Variant existing = variantDAO.getVariantById(variantId);
        if (existing == null) throw new NotFoundException("Variant not found");
        if (incoming.getVariantName() != null) existing.setVariantName(incoming.getVariantName());
        if (incoming.getModelId()     != null) existing.setModelId(incoming.getModelId());
        variantDAO.updateVariant(existing);
    }

    public void deleteVariant(long variantId) throws SQLException {
        if (!variantDAO.deleteVariant(variantId))
            throw new NotFoundException("Variant not found");
    }
}
