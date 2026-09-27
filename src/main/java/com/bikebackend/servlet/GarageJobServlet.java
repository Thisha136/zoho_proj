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
import com.bikebackend.service.JobService;
import com.bikebackend.service.JobSchedulingService;
import com.bikebackend.model.ScheduleResult;
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
import java.time.LocalDate;
import java.util.Map;

public class GarageJobServlet
extends HttpServlet {
    private final JobService jobService = new JobService();
    private final JobSchedulingService schedulingService = new JobSchedulingService();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule((Module)new JavaTimeModule());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String[] parts = request.getPathInfo().substring(1).split("/");
            if (parts.length >= 4 && "schedule".equals(parts[3])) {
                long gid = parseLong(parts[0], "Garage ID");
                long jid = parseLong(parts[2], "Job ID");
                JsonNode body = objectMapper.readTree(request.getReader());
                String start = body.has("requested_start") ? body.get("requested_start").asText(null) : null;
                String delivery = body.has("delivery_date") ? body.get("delivery_date").asText(null) : null;
                if (start == null || delivery == null) throw new BadRequestException("requested_start and delivery_date are required");
                ScheduleResult result = schedulingService.allocate(gid, jid, LocalDate.parse(start), LocalDate.parse(delivery));
                response.setStatus(200);
                objectMapper.writeValue(response.getWriter(), result);
                return;
            }
            long srId = this.parseLong(parts[0], "Service request ID");
            long gid = this.getCallerGid(request);
            long jid = this.jobService.createJob(gid, srId);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Job created successfully", "jid", jid));
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
            long gid = this.extractGid(request);
            String extra = this.extractExtra(request);
            if ("count".equals(extra)) {
                String status = request.getParameter("status");
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("count", this.jobService.countJobs(gid, null, status)));
            } else if (extra != null) {
                long jid = this.parseLong(extra, "Job ID");
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.jobService.getGarageJob(gid, jid));
            } else {
                String status = request.getParameter("status");
                String from = request.getParameter("from");
                String to = request.getParameter("to");
                this.objectMapper.writeValue((Writer)response.getWriter(), this.jobService.getGarageJobs(gid, status, from, to));
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

    private void handlePatch(HttpServletRequest request, HttpServletResponse response) throws IOException, Exception {
        long gid = this.extractGid(request);
        String extra = this.extractExtra(request);
        if (extra == null) {
            throw new BadRequestException("Job ID is required");
        }
        long jid = this.parseLong(extra, "Job ID");
        String sub = this.extractSub(request);
        JsonNode body = this.objectMapper.readTree((Reader)request.getReader());
        switch (sub == null ? "" : sub) {
            case "status": {
                String status;
                String string = status = body.has("status") ? body.get("status").asText(null) : null;
                if (status == null) {
                    throw new BadRequestException("Status is required");
                }
                if (body.has("expected_version") && !body.get("expected_version").isNull())
                    this.jobService.updateJobStatus(gid, jid, status, body.get("expected_version").asInt());
                else
                    this.jobService.updateJobStatus(gid, jid, status);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Status updated"));
                break;
            }
            case "service-start": {
                this.jobService.recordServiceStart(gid, jid);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service start recorded"));
                break;
            }
            case "service-end": {
                this.jobService.recordServiceEnd(gid, jid);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service end recorded"));
                break;
            }
            case "delivery-date": {
                String dateStr;
                String string = dateStr = body.has("expected_delivery_date") ? body.get("expected_delivery_date").asText(null) : null;
                if (dateStr == null) {
                    throw new BadRequestException("expected_delivery_date is required");
                }
                this.jobService.updateExpectedDeliveryDate(gid, jid, LocalDate.parse(dateStr));
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Delivery date updated"));
                break;
            }
            case "pickup-status": {
                String ps;
                String string = ps = body.has("pickup_status") ? body.get("pickup_status").asText(null) : null;
                if (ps == null) {
                    throw new BadRequestException("pickup_status is required");
                }
                this.jobService.updatePickupStatus(gid, jid, ps);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Pickup status updated"));
                break;
            }
            case "delivery-status": {
                String ds;
                String string = ds = body.has("delivery_status") ? body.get("delivery_status").asText(null) : null;
                if (ds == null) {
                    throw new BadRequestException("delivery_status is required");
                }
                this.jobService.updateDeliveryStatus(gid, jid, ds);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Delivery status updated"));
                break;
            }
            default: {
                throw new BadRequestException("Invalid patch endpoint");
            }
        }
    }

    private long extractGid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        return this.parseLong(parts[0], "Garage ID");
    }

    private String extractExtra(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        return parts.length >= 3 ? parts[2] : null;
    }

    private String extractSub(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        return parts.length >= 4 ? parts[3] : null;
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
