package com.bikebackend.service;

import com.bikebackend.dao.GarageBayDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.ForbiddenException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.GarageBay;

import java.sql.SQLException;
import java.util.List;

public class GarageBayService {

    private final GarageBayDAO bayDAO = new GarageBayDAO();

    public long createBay(long gid, GarageBay bay) throws SQLException {
        if (bay.getBayName() == null || bay.getBayName().isBlank())
            throw new BadRequestException("Bay name is required");
        bay.setGid(gid);
        return bayDAO.createBay(bay);
    }

    public List<GarageBay> getBays(long gid) throws SQLException {
        return bayDAO.getByGarage(gid);
    }

    public GarageBay getBay(long gid, long bayId) throws SQLException {
        GarageBay bay = bayDAO.getById(bayId);
        if (bay == null)               throw new NotFoundException("Bay not found");
        if (!bay.getGid().equals(gid)) throw new ForbiddenException("Access denied");
        return bay;
    }

    public void updateBay(long gid, long bayId, GarageBay incoming) throws SQLException {
        GarageBay existing = bayDAO.getById(bayId);
        if (existing == null)               throw new NotFoundException("Bay not found");
        if (!existing.getGid().equals(gid)) throw new ForbiddenException("Access denied");
        if (incoming.getBayName() != null) existing.setBayName(incoming.getBayName());
        bayDAO.updateBay(existing);
    }

    public void deleteBay(long gid, long bayId) throws SQLException {
        GarageBay bay = bayDAO.getById(bayId);
        if (bay == null)               throw new NotFoundException("Bay not found");
        if (!bay.getGid().equals(gid)) throw new ForbiddenException("Access denied");
        bayDAO.deleteBay(bayId, gid);
    }
}
