package com.bikebackend.service;

import com.bikebackend.dao.AddressDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.ForbiddenException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.Address;

import java.sql.SQLException;
import java.util.List;

public class AddressService {

    private final AddressDAO addressDAO = new AddressDAO();

    // =========================
    // CREATE
    // =========================

    public long createAddress(Address address) throws SQLException {

        if (address.getAddressLine() == null || address.getAddressLine().isBlank())
            throw new BadRequestException("Address line is required");
        if (address.getCity() == null || address.getCity().isBlank())
            throw new BadRequestException("City is required");
        if (address.getState() == null || address.getState().isBlank())
            throw new BadRequestException("State is required");
        if (address.getPincode() == null || address.getPincode().isBlank())
            throw new BadRequestException("Pincode is required");

        return addressDAO.createAddress(address);
    }

    // =========================
    // GET ALL FOR USER
    // =========================

    public List<Address> getAddresses(long uid) throws SQLException {
        return addressDAO.getAddressesByUser(uid);
    }

    // =========================
    // GET ONE — verify ownership
    // =========================

    public Address getAddress(long uid, long addressId) throws SQLException {

        Address address = addressDAO.getAddressById(addressId);

        if (address == null)
            throw new NotFoundException("Address not found");

        if (!address.getUid().equals(uid))
            throw new ForbiddenException("Access denied");

        return address;
    }

    // =========================
    // FULL UPDATE
    // =========================

    public void updateAddress(long uid, Address address) throws SQLException {

        // Verify ownership first
        Address existing = addressDAO.getAddressById(address.getAddressId());

        if (existing == null)
            throw new NotFoundException("Address not found");

        if (!existing.getUid().equals(uid))
            throw new ForbiddenException("Access denied");

        if (address.getAddressLine() == null || address.getAddressLine().isBlank())
            throw new BadRequestException("Address line is required");
        if (address.getCity() == null || address.getCity().isBlank())
            throw new BadRequestException("City is required");
        if (address.getState() == null || address.getState().isBlank())
            throw new BadRequestException("State is required");
        if (address.getPincode() == null || address.getPincode().isBlank())
            throw new BadRequestException("Pincode is required");

        address.setUid(uid);
        addressDAO.updateAddress(address);
    }

    // =========================
    // PARTIAL UPDATE (PATCH)
    // =========================

    public void partialUpdateAddress(long uid, long addressId, Address incoming)
            throws SQLException {

        Address existing = addressDAO.getAddressById(addressId);

        if (existing == null)
            throw new NotFoundException("Address not found");

        if (!existing.getUid().equals(uid))
            throw new ForbiddenException("Access denied");

        if (incoming.getAddressType() != null)  existing.setAddressType(incoming.getAddressType());
        if (incoming.getAddressLine() != null)  existing.setAddressLine(incoming.getAddressLine());
        if (incoming.getCity()        != null)  existing.setCity(incoming.getCity());
        if (incoming.getState()       != null)  existing.setState(incoming.getState());
        if (incoming.getPincode()     != null)  existing.setPincode(incoming.getPincode());
        if (incoming.getIsDefault()   != null)  existing.setIsDefault(incoming.getIsDefault());

        addressDAO.updateAddress(existing);
    }

    // =========================
    // SET DEFAULT
    // =========================

    public void setDefault(long uid, long addressId) throws SQLException {

        Address address = addressDAO.getAddressById(addressId);

        if (address == null)
            throw new NotFoundException("Address not found");

        if (!address.getUid().equals(uid))
            throw new ForbiddenException("Access denied");

        addressDAO.setDefault(uid, addressId);
    }

    // =========================
    // DELETE
    // =========================

    public void deleteAddress(long uid, long addressId) throws SQLException {

        Address address = addressDAO.getAddressById(addressId);

        if (address == null)
            throw new NotFoundException("Address not found");

        if (!address.getUid().equals(uid))
            throw new ForbiddenException("Access denied");

        addressDAO.deleteAddress(addressId, uid);
    }
}
