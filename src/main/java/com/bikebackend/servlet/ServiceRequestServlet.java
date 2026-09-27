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
import com.bikebackend.model.ServiceRequest;
import com.bikebackend.service.JobService;
import com.bikebackend.service.ServiceRequestService;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

public class ServiceRequestServlet
extends HttpServlet {
    private final ServiceRequestService srService = new ServiceRequestService();
    private final JobService jobService = new JobService();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule((Module)new JavaTimeModule());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.endsWith("/job") && pathInfo.split("/").length == 3) {
                long srId = this.extractSrId(pathInfo);
                long gid = this.getCallerGid(request);
                long jid = this.jobService.createJob(gid, srId);
                response.setStatus(201);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Job created successfully", "jid", jid));
                return;
            }
            if (pathInfo != null && pathInfo.matches("/\\d+/cancel")) {
                long srId = this.extractSrId(pathInfo);
                long uid = this.getCallerUid(request);
                this.srService.cancelRequest(uid, srId);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service request cancelled"));
                return;
            }
            ServiceRequest sr = this.readServiceRequest(request);
            long srId = this.srService.createServiceRequest(this.getCallerUid(request), sr);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service request created", "sr_id", srId));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String pathInfo = request.getPathInfo();
            if ("/count".equals(pathInfo)) {
                String status = request.getParameter("status");
                int count = this.srService.countRequests(null, null, status);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("count", count));
                return;
            }
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                long srId = this.extractSrId(pathInfo);
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.srService.getServiceRequest(srId));
                return;
            }
            String status = request.getParameter("status");
            String gidP = request.getParameter("gid");
            String bikeIdP = request.getParameter("bike_id");
            String from = request.getParameter("from");
            String to = request.getParameter("to");
            String sort = request.getParameter("sort");
            Long gid = gidP != null ? Long.valueOf(Long.parseLong(gidP)) : null;
            Long bikeId = bikeIdP != null ? Long.valueOf(Long.parseLong(bikeIdP)) : null;
            this.objectMapper.writeValue((Writer)response.getWriter(), this.srService.getAllRequests(status, gid, bikeId, from, to, sort));
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
                this.handlePatch(request, response);
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

    private void handlePatch(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            throw new BadRequestException("Service request ID is required");
        }
        String[] parts = pathInfo.substring(1).split("/");
        long srId = this.parseLong(parts[0], "Service request ID");
        if (parts.length == 2 && "date-response".equals(parts[1])) {
            JsonNode body = this.objectMapper.readTree((Reader)request.getReader());
            String dateResponse = body.has("response") ? body.get("response").asText(null) : null;
            long uid = this.getCallerUid(request);
            this.srService.respondToProposedDate(uid, srId, dateResponse);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Date response recorded"));
        } else if (parts.length == 2 && "status".equals(parts[1])) {
            String status;
            JsonNode body = this.objectMapper.readTree((Reader)request.getReader());
            String string = status = body.has("status") ? body.get("status").asText(null) : null;
            if (status == null) {
                throw new BadRequestException("Status is required");
            }
            this.srService.adminUpdateStatus(srId, status);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Status updated"));
        } else if (parts.length == 1) {
            long uid = this.getCallerUid(request);
            ServiceRequest incoming = this.readServiceRequest(request);
            incoming.setSrId(srId);
            this.srService.updateServiceRequest(uid, incoming);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service request updated"));
        } else {
            throw new BadRequestException("Invalid endpoint");
        }
    }

    private ServiceRequest readServiceRequest(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        ServiceRequest sr = new ServiceRequest();
        if (json.has("bike_id") && !json.get("bike_id").isNull()) {
            sr.setBikeId(json.get("bike_id").asLong());
        }
        if (json.has("title") && !json.get("title").isNull()) {
            sr.setTitle(json.get("title").asText());
        }
        if (json.has("issue") && !json.get("issue").isNull()) {
            sr.setIssue(json.get("issue").asText());
        }
        if (json.has("image_url") && !json.get("image_url").isNull()) {
            sr.setImageUrl(json.get("image_url").asText());
        }
        if (json.has("vehicle_handling_option") && !json.get("vehicle_handling_option").isNull()) {
            sr.setVehicleHandlingOption(json.get("vehicle_handling_option").asText());
        }
        if (json.has("preferred_delivery_date") && !json.get("preferred_delivery_date").isNull()) {
            sr.setPreferredDeliveryDate(LocalDate.parse(json.get("preferred_delivery_date").asText()));
        }
        return sr;
    }

    private long extractSrId(String pathInfo) {
        String[] parts = pathInfo.substring(1).split("/");
        return this.parseLong(parts[0], "Service request ID");
    }

    private long parseLong(String value, String label) {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            throw new BadRequestException("Invalid " + label);
        }
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
        throw new BadRequestException("Garage ID not available \u2014 authentication required");
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
        throw new BadRequestException("User ID not available \u2014 authentication required");
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
