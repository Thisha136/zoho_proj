package com.bikebackend.service;

import com.bikebackend.config.RedisCache;
import com.bikebackend.dao.BikeDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.ForbiddenException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.Bike;
import java.sql.SQLException;
import java.util.List;

public class BikeService {
    private final BikeDAO bikeDAO = new BikeDAO();

    public long createBike(Bike bike) throws SQLException {
        if (bike.getVariantId() == null) throw new BadRequestException("Variant ID is required");
        long id = bikeDAO.createBike(bike);
        invalidateBikes();
        return id;
    }

    public List<Bike> getBikes(long uid) throws SQLException {
        long version = RedisCache.version(RedisCache.bikesVersionKey());
        String key = RedisCache.bikesByUserKey(version, uid);
        List<Bike> bikes = RedisCache.get(key, RedisCache.BIKES);
        if (bikes == null) {
            bikes = bikeDAO.getBikesByUser(uid);
            RedisCache.put(key, bikes);
        }
        return bikes;
    }

    public Bike getBike(long uid, long bikeId) throws SQLException {
        Bike bike = getCachedBike(bikeId);
        if (bike == null) throw new NotFoundException("Bike not found");
        if (!bike.getUid().equals(uid)) throw new ForbiddenException("Access denied");
        return bike;
    }

    public List<Bike> getAllBikes(Long uid, Long variantId) throws SQLException {
        long version = RedisCache.version(RedisCache.bikesVersionKey());
        String key = RedisCache.allBikesKey(version, uid, variantId);
        List<Bike> bikes = RedisCache.get(key, RedisCache.BIKES);
        if (bikes == null) {
            bikes = bikeDAO.getAllBikes(uid, variantId);
            RedisCache.put(key, bikes);
        }
        return bikes;
    }

    public Bike getAnyBike(long bikeId) throws SQLException {
        Bike bike = getCachedBike(bikeId);
        if (bike == null) throw new NotFoundException("Bike not found");
        return bike;
    }

    public void updateBike(long uid, Bike bike) throws SQLException {
        Bike existing = getCachedBike(bike.getBikeId());
        if (existing == null) throw new NotFoundException("Bike not found");
        if (!existing.getUid().equals(uid)) throw new ForbiddenException("Access denied");
        if (bike.getVariantId() == null) throw new BadRequestException("Variant ID is required");
        bike.setUid(uid);
        if (!bikeDAO.updateBike(bike)) throw new NotFoundException("Bike not found");
        invalidateBike(bike.getBikeId());
    }

    public void partialUpdateBike(long uid, long bikeId, Bike incoming) throws SQLException {
        Bike existing = getCachedBike(bikeId);
        if (existing == null) throw new NotFoundException("Bike not found");
        if (!existing.getUid().equals(uid)) throw new ForbiddenException("Access denied");
        if (incoming.getVariantId() != null) existing.setVariantId(incoming.getVariantId());
        if (incoming.getManufacturingYear() != null) existing.setManufacturingYear(incoming.getManufacturingYear());
        if (incoming.getPurchaseDate() != null) existing.setPurchaseDate(incoming.getPurchaseDate());
        if (incoming.getRegistrationNo() != null) existing.setRegistrationNo(incoming.getRegistrationNo());
        if (incoming.getFuelType() != null) existing.setFuelType(incoming.getFuelType());
        if (incoming.getOdometerReading() != null) existing.setOdometerReading(incoming.getOdometerReading());
        if (incoming.getColor() != null) existing.setColor(incoming.getColor());
        if (incoming.getImageUrl() != null) existing.setImageUrl(incoming.getImageUrl());
        if (!bikeDAO.updateBike(existing)) throw new NotFoundException("Bike not found");
        invalidateBike(bikeId);
    }

    public void deleteBike(long uid, long bikeId) throws SQLException {
        Bike existing = getCachedBike(bikeId);
        if (existing == null) throw new NotFoundException("Bike not found");
        if (!existing.getUid().equals(uid)) throw new ForbiddenException("Access denied");
        if (!bikeDAO.deleteBike(bikeId, uid)) throw new NotFoundException("Bike not found");
        invalidateBike(bikeId);
    }

    private Bike getCachedBike(long bikeId) throws SQLException {
        Bike bike = RedisCache.get(RedisCache.bikeKey(bikeId), Bike.class);
        if (bike == null) {
            bike = bikeDAO.getBikeById(bikeId);
            if (bike != null) RedisCache.put(RedisCache.bikeKey(bikeId), bike);
        }
        return bike;
    }

    private void invalidateBike(long bikeId) { RedisCache.evict(RedisCache.bikeKey(bikeId)); invalidateBikes(); }
    private void invalidateBikes() { RedisCache.bumpVersion(RedisCache.bikesVersionKey()); }
}
