package com.bikebackend.service;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.dao.ServiceRequestDAO;
import com.bikebackend.dao.BikeDAO;
import com.bikebackend.exception.*;
import com.bikebackend.model.ServiceRequest;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ServiceRequestService {

    private final ServiceRequestDAO srDAO = new ServiceRequestDAO();
    private final BikeDAO bikeDAO = new BikeDAO();

    // =========================
    // USER: CREATE
    // =========================

    public long createServiceRequest(long uid, ServiceRequest sr) throws SQLException {

        if (sr.getBikeId() == null) throw new BadRequestException("Bike ID is required");
        if (sr.getGid()    == null) throw new BadRequestException("Garage ID is required");
        if (sr.getTitle()  == null || sr.getTitle().isBlank())
            throw new BadRequestException("Title is required");
        var bike = bikeDAO.getBikeById(sr.getBikeId());
        if (bike == null) throw new NotFoundException("Bike not found");
        if (!Long.valueOf(uid).equals(bike.getUid())) throw new ForbiddenException("Bike does not belong to user");

        sr.setStatus("PENDING");

        return srDAO.createServiceRequest(uid, sr);
    }

    // =========================
    // USER: GET OWN REQUESTS
    // =========================

    public List<ServiceRequest> getUserRequests(long uid, String status, Long bikeId)
            throws SQLException {
        return srDAO.getByUser(uid, status, bikeId);
    }

    // =========================
    // USER/GARAGE/ADMIN: GET ONE
    // =========================

    public ServiceRequest getServiceRequest(long srId) throws SQLException {
        ServiceRequest sr = srDAO.getById(srId);
        if (sr == null) throw new NotFoundException("Service request not found");
        return sr;
    }

    // =========================
    // USER: VERIFY OWNERSHIP
    // =========================

    public ServiceRequest getUserServiceRequest(long uid, long srId) throws SQLException {
        ServiceRequest sr = getServiceRequest(srId);
        if (!srDAO.belongsToUser(srId, uid)) throw new ForbiddenException("Access denied");
        return sr;
    }

    // =========================
    // USER: UPDATE ALLOWED FIELDS
    // =========================

    public void updateServiceRequest(long uid, ServiceRequest incoming) throws SQLException {
        ServiceRequest existing = getUserServiceRequest(uid, incoming.getSrId());

        if (!"PENDING".equals(existing.getStatus()))
            throw new BadRequestException("Only PENDING requests can be updated");

        if (incoming.getTitle()                != null) existing.setTitle(incoming.getTitle());
        if (incoming.getIssue()                != null) existing.setIssue(incoming.getIssue());
        if (incoming.getImageUrl()             != null) existing.setImageUrl(incoming.getImageUrl());
        if (incoming.getVehicleHandlingOption()!= null) existing.setVehicleHandlingOption(incoming.getVehicleHandlingOption());
        if (incoming.getPreferredDeliveryDate()!= null) existing.setPreferredDeliveryDate(incoming.getPreferredDeliveryDate());
        srDAO.updateFields(existing);
    }

    // =========================
    // USER: CANCEL
    // =========================

    public void cancelRequest(long uid, long srId) throws SQLException {

        // Use a transaction + FOR UPDATE to prevent concurrent cancellation/acceptance
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ServiceRequest sr = srDAO.getByIdForUpdate(connection, srId);

                if (sr == null)               throw new NotFoundException("Service request not found");
                if (!srDAO.belongsToUser(srId, uid)) throw new ForbiddenException("Access denied");

                if (!"PENDING".equals(sr.getStatus())
                        && !"REVIEWED".equals(sr.getStatus())
                        && !"DATE_PROPOSED".equals(sr.getStatus())) {
                    throw new BadRequestException("Request cannot be cancelled in its current status");
                }

                srDAO.updateStatus(connection, srId, "CANCELLED");
                connection.commit();

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // =========================
    // USER: RESPOND TO PROPOSED DATE
    // =========================

    public void respondToProposedDate(long uid, long srId, String response) throws SQLException {

        if (!"ACCEPTED".equals(response) && !"REJECTED".equals(response))
            throw new BadRequestException("Response must be ACCEPTED or REJECTED");

        ServiceRequest sr = getUserServiceRequest(uid, srId);

        if (!"DATE_PROPOSED".equals(sr.getStatus()))
            throw new BadRequestException("No proposed date to respond to");

        srDAO.setCustomerDateResponse(srId, uid, response);
    }

    // =========================
    // GARAGE: GET REQUESTS
    // =========================

    public List<ServiceRequest> getGarageRequests(long gid, String status,
                                                   String from, String to) throws SQLException {
        return srDAO.getByGarage(gid, status, from, to);
    }

    // =========================
    // GARAGE: PHYSICAL REVIEW
    // =========================

    public void recordPhysicalReview(long gid, long srId, LocalDate date) throws SQLException {

        ServiceRequest sr = getServiceRequest(srId);
        if (!sr.getGid().equals(gid)) throw new ForbiddenException("Access denied");
        if (!"PENDING".equals(sr.getStatus()))
            throw new BadRequestException("Physical review requires PENDING status");

        srDAO.setPhysicalReviewDate(srId, gid, date);
    }

    // =========================
    // GARAGE: PROPOSE DELIVERY DATE
    // =========================

    public void proposeDeliveryDate(long gid, long srId, LocalDate date) throws SQLException {

        ServiceRequest sr = getServiceRequest(srId);
        if (!sr.getGid().equals(gid)) throw new ForbiddenException("Access denied");

        srDAO.setGarageProposedDate(srId, gid, date);
    }

    // =========================
    // GARAGE: ACCEPT / REJECT REQUEST
    // =========================

    public void acceptRequest(long gid, long srId) throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Lock row — prevents two concurrent accepts
                ServiceRequest sr = srDAO.getByIdForUpdate(connection, srId);

                if (sr == null)                throw new NotFoundException("Service request not found");
                if (!sr.getGid().equals(gid))  throw new ForbiddenException("Access denied");

                if (!"PENDING".equals(sr.getStatus())
                        && !"REVIEWED".equals(sr.getStatus())
                        && !"ACCEPTED".equals(sr.getCustomerDateResponse())) {
                    throw new BadRequestException("Request cannot be accepted in its current status");
                }

                srDAO.updateStatus(connection, srId, "ACCEPTED");
                connection.commit();

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void rejectRequest(long gid, long srId) throws SQLException {

        ServiceRequest sr = getServiceRequest(srId);
        if (!sr.getGid().equals(gid)) throw new ForbiddenException("Access denied");

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                srDAO.updateStatus(connection, srId, "REJECTED");
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // =========================
    // ADMIN
    // =========================

    public List<ServiceRequest> getAllRequests(String status, Long gid, Long bikeId,
                                               String from, String to, String sort)
            throws SQLException {
        return srDAO.getAll(status, gid, bikeId, from, to, sort);
    }

    public void adminUpdateStatus(long srId, String status) throws SQLException {

        ServiceRequest sr = getServiceRequest(srId);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                srDAO.updateStatus(connection, srId, status);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // =========================
    // ANALYTICS
    // =========================

    public int countRequests(Long gid, Long uid, String status) throws SQLException {
        return srDAO.countByStatus(gid, uid, status);
    }
}
