package com.bikebackend.service;

import com.bikebackend.dao.JobServiceDAO;
import com.bikebackend.dao.JobDAO;
import com.bikebackend.dao.ServiceRequestDAO;
import com.bikebackend.exception.*;
import com.bikebackend.model.Job;
import com.bikebackend.model.JobService_model;
import com.bikebackend.model.ServiceRequest;

import java.sql.SQLException;
import java.util.List;

public class JobServiceService {

    private final JobServiceDAO jsDAO  = new JobServiceDAO();
    private final JobDAO        jobDAO = new JobDAO();
    private final ServiceRequestDAO srDAO = new ServiceRequestDAO();

    private void verifyGarageOwnsJob(long gid, long jid) throws SQLException {
        Job job = jobDAO.getById(jid);
        if (job == null) throw new NotFoundException("Job not found");
        ServiceRequest sr = srDAO.getById(job.getSrId());
        if (sr == null || !sr.getGid().equals(gid)) throw new ForbiddenException("Access denied");
    }

    private void verifyUserOwnsJob(long uid, long jid) throws SQLException {
        Job job = jobDAO.getByIdForUser(jid, uid);
        if (job == null) throw new NotFoundException("Job not found");
    }

    public long addService(long gid, long jid, JobService_model js) throws SQLException {
        verifyGarageOwnsJob(gid, jid);
        if (js.getSid()   == null) throw new BadRequestException("Service ID is required");
        js.setJid(jid);
        js.setStatus("PENDING");
        return jsDAO.addService(js);
    }

    public List<JobService_model> getJobServices(long jid) throws SQLException {
        return jsDAO.getByJob(jid);
    }

    public JobService_model getJobService(long jid, long jsId) throws SQLException {
        JobService_model js = jsDAO.getById(jsId);
        if (js == null || !js.getJid().equals(jid)) throw new NotFoundException("Job service not found");
        return js;
    }

    public void updateStatus(long gid, long jid, long jsId, String status) throws SQLException {
        verifyGarageOwnsJob(gid, jid);
        if (!jsDAO.updateStatus(jsId, status)) throw new NotFoundException("Job service not found");
    }

    public void recordStart(long gid, long jid, long jsId) throws SQLException {
        verifyGarageOwnsJob(gid, jid);
        jsDAO.updateField(jsId, "actual_start_time",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
    }

    public void recordComplete(long gid, long jid, long jsId) throws SQLException {
        verifyGarageOwnsJob(gid, jid);
        jsDAO.updateField(jsId, "actual_end_time",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
    }

    // User: approve/reject optional service
    public void approve(long uid, long jid, long jsId) throws SQLException {
        verifyUserOwnsJob(uid, jid);
        JobService_model js = getJobService(jid, jsId);
        if (!"OPTIONAL".equals(js.getSelectionType()))
            throw new BadRequestException("Only optional services can be approved/rejected");
        jsDAO.updateStatus(jsId, "APPROVED");
    }

    public void reject(long uid, long jid, long jsId) throws SQLException {
        verifyUserOwnsJob(uid, jid);
        JobService_model js = getJobService(jid, jsId);
        if (!"OPTIONAL".equals(js.getSelectionType()))
            throw new BadRequestException("Only optional services can be approved/rejected");
        jsDAO.updateStatus(jsId, "REJECTED");
    }

    // Admin
    public List<JobService_model> getAll(String status, Long sid) throws SQLException {
        return jsDAO.getAll(status, sid);
    }

    public int count(Long gid, Long sid, String status) throws SQLException {
        return jsDAO.count(gid, sid, status);
    }
}
