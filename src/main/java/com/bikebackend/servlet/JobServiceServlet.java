/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.Module
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.http.HttpServlet
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 */
package com.bikebackend.servlet;

import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.model.Feedback;
import com.bikebackend.model.JobService_model;
import com.bikebackend.service.FeedbackService;
import com.bikebackend.service.JobService;
import com.bikebackend.service.JobServiceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

public class JobServiceServlet
extends HttpServlet {
    private final JobServiceService jsService = new JobServiceService();
    private final JobService jobService = new JobService();
    private final FeedbackService feedbackService = new FeedbackService();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule((Module)new JavaTimeModule());

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            if (request.getServletPath().startsWith("/job-services")) {
                String p = request.getPathInfo();
                if ("/count".equals(p)) {
                    String status = request.getParameter("status");
                    String sidP = request.getParameter("sid");
                    Long sid = sidP != null ? Long.valueOf(Long.parseLong(sidP)) : null;
                    this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("count", this.jsService.count(null, sid, status)));
                } else if (p == null || p.equals("/")) {
                    String status = request.getParameter("status");
                    String sidP = request.getParameter("sid");
                    Long sid = sidP != null ? Long.valueOf(Long.parseLong(sidP)) : null;
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.jsService.getAll(status, sid));
                } else {
                    long jsId = this.parseLong(p.substring(1), "Job service ID");
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.jsService.getJobService(0L, jsId));
                }
            } else if (request.getServletPath().startsWith("/jobs") && (request.getPathInfo() == null || "/".equals(request.getPathInfo()))) {
                String status = request.getParameter("status");
                String gidP = request.getParameter("gid");
                Long gid = gidP != null ? Long.valueOf(Long.parseLong(gidP)) : null;
                String from = request.getParameter("from");
                String to = request.getParameter("to");
                this.objectMapper.writeValue((Writer)response.getWriter(), this.jobService.getAllJobs(status, gid, from, to));
            } else if (request.getServletPath().startsWith("/jobs") && "/count".equals(request.getPathInfo())) {
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("count", this.jobService.countJobs(null, null, request.getParameter("status"))));
            } else if (request.getServletPath().startsWith("/jobs") && request.getPathInfo() != null && request.getPathInfo().matches("/\\d+")) {
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.jobService.getJob(this.parseLong(request.getPathInfo().substring(1), "Job ID")));
            } else {
                String[] parts = request.getPathInfo().substring(1).split("/");
                if (parts.length == 2 && "feedback".equals(parts[1])) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.feedbackService.getFeedbackByJob(this.getCallerUid(request), this.parseLong(parts[0], "Job ID")));
                    return;
                }
                long jid = this.extractJid(request);
                Long jsId = this.extractJsId(request);
                if (jsId != null) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.jsService.getJobService(jid, jsId));
                } else {
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.jsService.getJobServices(jid));
                }
            }
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String[] parts = request.getPathInfo().substring(1).split("/");
            if (parts.length == 2 && "feedback".equals(parts[1])) {
                JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
                Feedback feedback = new Feedback();
                if (json.has("rating") && !json.get("rating").isNull()) {
                    feedback.setRating(json.get("rating").asInt());
                }
                if (json.has("comments") && !json.get("comments").isNull()) {
                    feedback.setComments(json.get("comments").asText());
                }
                long feedbackId = this.feedbackService.submitFeedback(this.getCallerUid(request), this.parseLong(parts[0], "Job ID"), feedback);
                response.setStatus(201);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Feedback submitted", "feedback_id", feedbackId));
                return;
            }
            long jid = this.extractJid(request);
            Long jsId = this.extractJsId(request);
            String sub = this.extractSub(request);
            if (jsId != null && "approve".equals(sub)) {
                long uid = this.getCallerUid(request);
                this.jsService.approve(uid, jid, jsId);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service approved"));
            } else if (jsId != null && "reject".equals(sub)) {
                long uid = this.getCallerUid(request);
                this.jsService.reject(uid, jid, jsId);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service rejected"));
            } else {
                long gid = this.getCallerGid(request);
                JobService_model js = this.readJobService(request);
                long newJsId = this.jsService.addService(gid, jid, js);
                response.setStatus(201);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service added to job", "js_id", newJsId));
            }
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            try {
                long gid = this.getCallerGid(request);
                long jid = this.extractJid(request);
                long jsId = this.requireJsId(request);
                String sub = this.extractSub(request);
                switch (sub == null ? "" : sub) {
                    case "status": {
                        String status;
                        JsonNode body = this.objectMapper.readTree((Reader)request.getReader());
                        String string = status = body.has("status") ? body.get("status").asText(null) : null;
                        if (status == null) {
                            throw new BadRequestException("Status is required");
                        }
                        this.jsService.updateStatus(gid, jid, jsId, status);
                        this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Status updated"));
                        break;
                    }
                    case "start": {
                        this.jsService.recordStart(gid, jid, jsId);
                        this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Start time recorded"));
                        break;
                    }
                    case "complete": {
                        this.jsService.recordComplete(gid, jid, jsId);
                        this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Completion time recorded"));
                        break;
                    }
                    default: {
                        throw new BadRequestException("Invalid patch sub-action");
                    }
                }
            }
            catch (AppException e) {
                this.sendError(response, e.getStatus(), e.getMessage());
            }
            catch (Exception e) {
                this.handleUnexpected(response, e);
            }
            return;
        }
        super.service(request, response);
    }

    private long extractJid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        return this.parseLong(parts[0], "Job ID");
    }

    private Long extractJsId(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        if (parts.length >= 3) {
            try {
                return Long.parseLong(parts[2]);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private long requireJsId(HttpServletRequest request) {
        Long id = this.extractJsId(request);
        if (id == null) {
            throw new BadRequestException("Job service ID is required");
        }
        return id;
    }

    private String extractSub(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        return parts.length >= 4 ? parts[3] : null;
    }

    private JobService_model readJobService(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        JobService_model js = new JobService_model();
        if (json.has("sid") && !json.get("sid").isNull()) {
            js.setSid(json.get("sid").asLong());
        }
        if (json.has("selection_type") && !json.get("selection_type").isNull()) {
            js.setSelectionType("REQUIRED".equals(json.get("selection_type").asText()) ? "COMPULSORY" : json.get("selection_type").asText());
        }
        if (json.has("service_part_type") && !json.get("service_part_type").isNull()) {
            js.setServicePartType("OEM".equals(json.get("service_part_type").asText()) ? "SERVICE" : json.get("service_part_type").asText());
        }
        return js;
    }

    private long parseLong(String value, String label) {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            throw new BadRequestException("Invalid " + label);
        }
    }

    private long getCallerUid(HttpServletRequest request) {
        Object attr = request.getAttribute("uid");
        if (attr != null) {
            return (Long)attr;
        }
        String h = request.getHeader("X-User-Id");
        if (h != null) {
            return Long.parseLong(h);
        }
        throw new BadRequestException("Authentication required");
    }

    private long getCallerGid(HttpServletRequest request) {
        Object attr = request.getAttribute("gid");
        if (attr != null) {
            return (Long)attr;
        }
        String h = request.getHeader("X-Garage-Id");
        if (h != null) {
            return Long.parseLong(h);
        }
        throw new BadRequestException("Authentication required");
    }

    private void sendError(HttpServletResponse r, int status, String msg) throws IOException {
        r.setStatus(status);
        r.setContentType("application/json");
        r.setCharacterEncoding("UTF-8");
        this.objectMapper.writeValue((Writer)r.getWriter(), Map.of("error", msg));
    }

    private void handleUnexpected(HttpServletResponse r, Exception e) throws IOException {
        e.printStackTrace();
        this.sendError(r, 500, "Internal server error");
    }
}
