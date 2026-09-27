/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.http.HttpServlet
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 */
package com.bikebackend.servlet;

import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.model.Service;
import com.bikebackend.service.ServiceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Map;

public class ServiceServlet
extends HttpServlet {
    private final ServiceService serviceService = new ServiceService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            Long sid = this.extractId(request);
            if (sid != null) {
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.serviceService.getService(sid));
            } else {
                String serviceType = request.getParameter("service_type");
                String order = request.getParameter("order");
                this.objectMapper.writeValue((Writer)response.getWriter(), this.serviceService.getServices(serviceType, order));
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
            Service service = this.readService(request);
            long sid = this.serviceService.createService(service);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service created successfully", "sid", sid));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long sid = this.requireId(request);
            Service service = this.readService(request);
            service.setSid(sid);
            this.serviceService.updateService(service);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service updated successfully"));
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
                long sid = this.requireId(request);
                Service incoming = this.readService(request);
                this.serviceService.partialUpdateService(sid, incoming);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service partially updated"));
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

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long sid = this.requireId(request);
            this.serviceService.deleteService(sid);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service deleted successfully"));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    private Service readService(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        Service s = new Service();
        if (json.has("service_name") && !json.get("service_name").isNull()) {
            s.setServiceName(json.get("service_name").asText());
        }
        if (json.has("specialization") && !json.get("specialization").isNull()) {
            s.setSpecialization(json.get("specialization").asText());
        }
        if (json.has("duration") && !json.get("duration").isNull()) {
            s.setDuration(json.get("duration").asInt());
        }
        if (json.has("price") && !json.get("price").isNull()) {
            s.setPrice(new BigDecimal(json.get("price").asText()));
        }
        if (json.has("service_type") && !json.get("service_type").isNull()) {
            s.setServiceType(json.get("service_type").asText());
        }
        return s;
    }

    private Long extractId(HttpServletRequest request) {
        String p = request.getPathInfo();
        if (p == null || p.equals("/")) {
            return null;
        }
        try {
            return Long.parseLong(p.substring(1));
        }
        catch (NumberFormatException e) {
            throw new BadRequestException("Invalid service ID");
        }
    }

    private long requireId(HttpServletRequest request) {
        Long id = this.extractId(request);
        if (id == null) {
            throw new BadRequestException("Service ID is required");
        }
        return id;
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
