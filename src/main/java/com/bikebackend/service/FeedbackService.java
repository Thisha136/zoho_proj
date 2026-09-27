package com.bikebackend.service;

import com.bikebackend.dao.FeedbackDAO;
import com.bikebackend.dao.JobDAO;
import com.bikebackend.dao.ServiceRequestDAO;
import com.bikebackend.exception.*;
import com.bikebackend.model.Feedback;
import com.bikebackend.model.Job;
import com.bikebackend.model.ServiceRequest;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class FeedbackService {

    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final JobDAO jobDAO = new JobDAO();
    private final ServiceRequestDAO srDAO = new ServiceRequestDAO();

    public long submitFeedback(long uid, long jid, Feedback incoming) throws SQLException {

        Job job = jobDAO.getByIdForUser(jid, uid);
        if (job == null) throw new NotFoundException("Job not found");
        if (!"COMPLETED".equals(job.getStatus()))
            throw new BadRequestException("Feedback can only be submitted for completed jobs");

        if (incoming.getRating() == null || incoming.getRating() < 1 || incoming.getRating() > 5)
            throw new BadRequestException("Rating must be between 1 and 5");

        Feedback fb = new Feedback();
        fb.setJid(jid);
        fb.setRating(incoming.getRating());
        fb.setComments(incoming.getComments());

        return feedbackDAO.createFeedback(fb);
    }

    public Feedback getFeedbackByJob(long uid, long jid) throws SQLException {
        Job job = jobDAO.getByIdForUser(jid, uid);
        if (job == null) throw new NotFoundException("Job not found");
        Feedback fb = feedbackDAO.getByJob(jid);
        if (fb == null) throw new NotFoundException("Feedback not found");
        return fb;
    }

    public void updateFeedback(long uid, long feedbackId, Integer rating, String comments)
            throws SQLException {
        Feedback existing = feedbackDAO.getById(feedbackId);
        if (existing == null)                 throw new NotFoundException("Feedback not found");
        Job ownerJob = jobDAO.getByIdForUser(existing.getJid(), uid);
        if (ownerJob == null) throw new ForbiddenException("Access denied");
        if (rating != null && (rating < 1 || rating > 5))
            throw new BadRequestException("Rating must be between 1 and 5");
        int newRating   = rating   != null ? rating   : existing.getRating();
        String newComments = comments != null ? comments : existing.getComments();
        feedbackDAO.updateFeedback(feedbackId, newRating, newComments);
    }

    public List<Feedback> getGarageFeedback(long gid) throws SQLException {
        return feedbackDAO.getByGarage(gid);
    }

    public Feedback getGarageFeedbackById(long gid, long feedbackId) throws SQLException {
        Feedback fb = feedbackDAO.getById(feedbackId);
        if (fb == null) throw new NotFoundException("Feedback not found");
        Job job = jobDAO.getById(fb.getJid());
        if (job == null) throw new NotFoundException("Feedback not found");
        ServiceRequest sr = srDAO.getById(job.getSrId());
        if (sr == null || !Long.valueOf(gid).equals(sr.getGid()))
            throw new NotFoundException("Feedback not found");
        return fb;
    }

    public List<Feedback> getAllFeedback(Long gid, Integer rating) throws SQLException {
        return feedbackDAO.getAll(gid, rating);
    }

    public Feedback getAnyFeedback(long feedbackId) throws SQLException {
        Feedback fb = feedbackDAO.getById(feedbackId);
        if (fb == null) throw new NotFoundException("Feedback not found");
        return fb;
    }

    public Map<String, Object> getGarageSummary(long gid) throws SQLException {
        return feedbackDAO.getSummary(gid);
    }

    public Map<String, Object> getOverallSummary() throws SQLException {
        return feedbackDAO.getSummary(0); // 0 = no gid filter — will need a separate query
    }
}
