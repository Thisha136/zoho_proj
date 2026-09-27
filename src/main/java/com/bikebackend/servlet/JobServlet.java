package com.bikebackend.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.service.JobService;
import com.bikebackend.service.FeedbackService;
import com.bikebackend.model.Feedback;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

/**
 * USER:
 * GET  /users/{uid}/jobs
 * GET  /users/{uid}/jobs/{jid}
 * GET  /users/{uid}/jobs/count
 *
 * ADMIN:
 * GET  /jobs
 * GET  /jobs/{jid}
 *
 * GARAGE endpoints → GarageJobServlet
 */
public class JobServlet extends HttpServlet {

    private final JobService jobService = new JobService();
    private final FeedbackService feedbackService = new FeedbackService();
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String servletPath = request.getServletPath();

            if (servletPath.startsWith("/jobs")) {
                // Admin
                String p = request.getPathInfo();
                String[] jobParts = p == null || "/".equals(p) ? new String[0] : p.substring(1).split("/");
                if (jobParts.length == 2 && "feedback".equals(jobParts[1])) {
                    objectMapper.writeValue(response.getWriter(), feedbackService.getFeedbackByJob(
                            callerUid(request), parseLong(jobParts[0], "Job ID")));
                    return;
                }
                if (p == null || p.equals("/")) {
                    String status = request.getParameter("status");
                    String gidP   = request.getParameter("gid");
                    String from   = request.getParameter("from");
                    String to     = request.getParameter("to");
                    Long gid = gidP != null ? Long.parseLong(gidP) : null;
                    objectMapper.writeValue(response.getWriter(),
                            jobService.getAllJobs(status, gid, from, to));
                } else if ("/count".equals(p)) {
                    String status = request.getParameter("status");
                    objectMapper.writeValue(response.getWriter(),
                            Map.of("count", jobService.countJobs(null, null, status)));
                } else {
                    long jid = parseLong(p.substring(1), "Job ID");
                    objectMapper.writeValue(response.getWriter(), jobService.getJob(jid));
                }
            } else {
                // User: /users/{uid}/jobs[/{jid}]
                long uid    = extractUid(request);
                String extra = extractExtra(request);
                if ("count".equals(extra)) {
                    String status = request.getParameter("status");
                    objectMapper.writeValue(response.getWriter(),
                            Map.of("count", jobService.countJobs(null, uid, status)));
                } else if (extra != null) {
                    long jid = parseLong(extra, "Job ID");
                    objectMapper.writeValue(response.getWriter(), jobService.getUserJob(uid, jid));
                } else {
                    objectMapper.writeValue(response.getWriter(), jobService.getUserJobs(uid));
                }
            }
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String path = request.getPathInfo();
            String[] parts = path == null || "/".equals(path) ? new String[0] : path.substring(1).split("/");
            if (parts.length != 2 || !"feedback".equals(parts[1]))
                throw new BadRequestException("Invalid job feedback URL");
            JsonNode json = objectMapper.readTree(request.getReader());
            Feedback feedback = new Feedback();
            if (json.has("rating") && !json.get("rating").isNull()) feedback.setRating(json.get("rating").asInt());
            if (json.has("comments") && !json.get("comments").isNull()) feedback.setComments(json.get("comments").asText());
            long feedbackId = feedbackService.submitFeedback(callerUid(request), parseLong(parts[0], "Job ID"), feedback);
            response.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(response.getWriter(), Map.of("message", "Feedback submitted", "feedback_id", feedbackId));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    // path: /{uid}/jobs/{jid_or_count}
    private long extractUid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        return parseLong(parts[0], "User ID");
    }

    private String extractExtra(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        return parts.length >= 3 ? parts[2] : null;
    }

    private long parseLong(String value, String label) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { throw new BadRequestException("Invalid " + label); }
    }

    private long callerUid(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) throw new BadRequestException("Authentication required");
        return parseLong(header, "User ID");
    }

    private void sendError(HttpServletResponse r, int status, String msg) throws IOException {
        r.setStatus(status); r.setContentType("application/json"); r.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(r.getWriter(), Map.of("error", msg));
    }

    private void handleUnexpected(HttpServletResponse r, Exception e) throws IOException {
        e.printStackTrace(); sendError(r, 500, "Internal server error");
    }
}
