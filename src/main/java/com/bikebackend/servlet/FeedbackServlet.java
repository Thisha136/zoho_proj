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
import com.bikebackend.service.FeedbackService;
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

public class FeedbackServlet
extends HttpServlet {
    private final FeedbackService feedbackService = new FeedbackService();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule((Module)new JavaTimeModule());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long jid = this.extractJid(request);
            long uid = this.getCallerUid(request);
            JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
            Feedback fb = new Feedback();
            if (json.has("rating") && !json.get("rating").isNull()) {
                fb.setRating(json.get("rating").asInt());
            }
            if (json.has("comments") && !json.get("comments").isNull()) {
                fb.setComments(json.get("comments").asText());
            }
            long feedbackId = this.feedbackService.submitFeedback(uid, jid, fb);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Feedback submitted", "feedback_id", feedbackId));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String sp = request.getServletPath();
            if (sp.startsWith("/job-feedback")) {
                long uid = this.getCallerUid(request);
                long jid = this.extractJid(request);
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.feedbackService.getFeedbackByJob(uid, jid));
                return;
            } else if (sp.startsWith("/garage-feedback")) {
                String p = request.getPathInfo();
                if (p == null || "/".equals(p)) {
                    throw new BadRequestException("Garage ID is required");
                }
                String[] parts = p.substring(1).split("/");
                long gid = this.parseLong(parts[0], "Garage ID");
                if (parts.length == 1) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.feedbackService.getGarageFeedback(gid));
                    return;
                } else {
                    if (parts.length != 2) throw new BadRequestException("Invalid garage feedback URL");
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.feedbackService.getGarageFeedbackById(gid, this.parseLong(parts[1], "Feedback ID")));
                }
                return;
            } else {
                String p = request.getPathInfo();
                if ("/summary".equals(p)) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.feedbackService.getOverallSummary());
                    return;
                } else if (p == null || p.equals("/")) {
                    String gidP = request.getParameter("gid");
                    String ratingP = request.getParameter("rating");
                    Long gid = gidP != null ? Long.valueOf(Long.parseLong(gidP)) : null;
                    Integer rating = ratingP != null ? Integer.valueOf(Integer.parseInt(ratingP)) : null;
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.feedbackService.getAllFeedback(gid, rating));
                    return;
                } else {
                    long feedbackId = this.parseLong(p.substring(1), "Feedback ID");
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.feedbackService.getAnyFeedback(feedbackId));
                }
            }
            return;
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
            return;
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
                String p = request.getPathInfo();
                if (p == null || p.equals("/")) {
                    throw new BadRequestException("Feedback ID is required");
                }
                long feedbackId = this.parseLong(p.substring(1), "Feedback ID");
                long uid = this.getCallerUid(request);
                JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
                Integer rating = json.has("rating") && !json.get("rating").isNull() ? Integer.valueOf(json.get("rating").asInt()) : null;
                String comments = json.has("comments") && !json.get("comments").isNull() ? json.get("comments").asText() : null;
                this.feedbackService.updateFeedback(uid, feedbackId, rating, comments);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Feedback updated"));
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
