package com.bikebackend.service;

import com.bikebackend.dao.JobDAO;
import com.bikebackend.dao.ServiceRequestDAO;
import com.bikebackend.exception.*;
import com.bikebackend.model.Job;
import com.bikebackend.model.ServiceRequest;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class JobService {

    private final JobDAO jobDAO = new JobDAO();
    private final ServiceRequestDAO srDAO = new ServiceRequestDAO();

    // =========================
    // GARAGE: CREATE JOB from accepted service request
    // =========================

    public long createJob(long gid, long srId) throws SQLException {

        ServiceRequest sr = srDAO.getById(srId);

        if (sr == null)               throw new NotFoundException("Service request not found");
        if (!sr.getGid().equals(gid)) throw new ForbiddenException("Access denied");
        if (!"ACCEPTED".equals(sr.getStatus()))
            throw new BadRequestException("Job can only be created from an ACCEPTED service request");

        Job job = new Job();
        job.setSrId(srId);
        job.setStatus("CREATED");
        job.setJobDate(LocalDate.now());

        return jobDAO.createJob(job);
    }

    // =========================
    // USER: GET OWN JOBS
    // =========================

    public List<Job> getUserJobs(long uid) throws SQLException {
        return jobDAO.getByUser(uid);
    }

    public Job getUserJob(long uid, long jid) throws SQLException {
        Job job = jobDAO.getByIdForUser(jid, uid);
        if (job == null) throw new NotFoundException("Job not found");
        return job;
    }

    // =========================
    // GARAGE
    // =========================

    public List<Job> getGarageJobs(long gid, String status, String from, String to)
            throws SQLException {
        return jobDAO.getByGarage(gid, status, from, to);
    }

    public Job getGarageJob(long gid, long jid) throws SQLException {
        Job job = jobDAO.getById(jid);
        if (job == null) throw new NotFoundException("Job not found");
        // Verify the job belongs to a service request from this garage
        ServiceRequest sr = srDAO.getById(job.getSrId());
        if (sr == null || !sr.getGid().equals(gid)) throw new ForbiddenException("Access denied");
        return job;
    }

    public void updateJobStatus(long gid, long jid, String status) throws SQLException {
        getGarageJob(gid, jid); // verify ownership
        if (!jobDAO.updateStatus(jid, status))
            throw new NotFoundException("Job not found");
    }

    public void updateJobStatus(long gid, long jid, String status, int expectedVersion) throws SQLException {
        getGarageJob(gid, jid);
        if (!jobDAO.updateStatus(jid, status, expectedVersion))
            throw new ConflictException("Job was modified by another request; refresh and retry");
    }

    public void recordServiceStart(long gid, long jid) throws SQLException {
        getGarageJob(gid, jid);
        jobDAO.updateField(jid, "service_start",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
    }

    public void recordServiceEnd(long gid, long jid) throws SQLException {
        getGarageJob(gid, jid);
        jobDAO.updateField(jid, "service_end",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
    }

    public void updateExpectedDeliveryDate(long gid, long jid, LocalDate date) throws SQLException {
        getGarageJob(gid, jid);
        jobDAO.updateField(jid, "expected_delivery_date", date);
    }

    public void recordActualDelivery(long gid, long jid, LocalDate date) throws SQLException {
        getGarageJob(gid, jid);
        jobDAO.updateField(jid, "actual_delivery_date", date);
    }

    public void updatePickupStatus(long gid, long jid, String pickupStatus) throws SQLException {
        getGarageJob(gid, jid);
        jobDAO.updateField(jid, "pickup_status", pickupStatus);
    }

    public void updateDeliveryStatus(long gid, long jid, String deliveryStatus) throws SQLException {
        getGarageJob(gid, jid);
        jobDAO.updateField(jid, "delivery_status", deliveryStatus);
    }

    // =========================
    // ADMIN
    // =========================

    public List<Job> getAllJobs(String status, Long gid, String from, String to)
            throws SQLException {
        return jobDAO.getAll(status, gid, from, to);
    }

    public Job getJob(long jid) throws SQLException {
        Job job = jobDAO.getById(jid);
        if (job == null) throw new NotFoundException("Job not found");
        return job;
    }

    // =========================
    // ANALYTICS
    // =========================

    public int countJobs(Long gid, Long uid, String status) throws SQLException {
        return jobDAO.countByStatus(gid, uid, status);
    }
}
