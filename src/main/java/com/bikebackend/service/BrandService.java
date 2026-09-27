package com.bikebackend.service;

import com.bikebackend.dao.BrandDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.Brand;

import java.sql.SQLException;
import java.util.List;

public class BrandService {

    private final BrandDAO brandDAO = new BrandDAO();

    public long createBrand(Brand brand) throws SQLException {
        if (brand.getBrandName() == null || brand.getBrandName().isBlank())
            throw new BadRequestException("Brand name is required");
        return brandDAO.createBrand(brand);
    }

    public List<Brand> getBrands(String search) throws SQLException {
        return brandDAO.getAllBrands(search);
    }

    public Brand getBrand(long bid) throws SQLException {
        Brand brand = brandDAO.getBrandById(bid);
        if (brand == null) throw new NotFoundException("Brand not found");
        return brand;
    }

    public void updateBrand(Brand brand) throws SQLException {
        if (brand.getBrandName() == null || brand.getBrandName().isBlank())
            throw new BadRequestException("Brand name is required");
        if (!brandDAO.updateBrand(brand))
            throw new NotFoundException("Brand not found");
    }

    public void partialUpdateBrand(long bid, Brand incoming) throws SQLException {
        Brand existing = brandDAO.getBrandById(bid);
        if (existing == null) throw new NotFoundException("Brand not found");
        if (incoming.getBrandName() != null) existing.setBrandName(incoming.getBrandName());
        brandDAO.updateBrand(existing);
    }

    public void deleteBrand(long bid) throws SQLException {
        if (!brandDAO.deleteBrand(bid))
            throw new NotFoundException("Brand not found");
    }
}
