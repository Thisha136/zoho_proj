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
import com.bikebackend.model.GarageBay;
import com.bikebackend.service.BayAllocationService;
import com.bikebackend.service.GarageBayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

public class GarageBayServlet
extends HttpServlet {
    private final GarageBayService bayService = new GarageBayService();
    private final BayAllocationService allocationService = new BayAllocationService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long gid = this.extractGid(request);
            GarageBay bay = this.readBay(request);
            long bayId = this.bayService.createBay(gid, bay);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Bay created successfully", "bay_id", bayId));
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
            Long bayId = this.extractBayId(request);
            if (bayId != null) {
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.bayService.getBay(gid, bayId));
            } else {
                this.objectMapper.writeValue((Writer)response.getWriter(), this.bayService.getBays(gid));
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
                long gid = this.extractGid(request);
                long bayId = this.requireBayId(request);
                GarageBay incoming = this.readBay(request);
                this.bayService.updateBay(gid, bayId, incoming);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Bay updated successfully"));
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
            long gid = this.extractGid(request);
            long bayId = this.requireBayId(request);
            this.bayService.deleteBay(gid, bayId);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Bay deleted successfully"));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    private long extractGid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        try {
            return Long.parseLong(parts[0]);
        }
        catch (NumberFormatException e) {
            throw new BadRequestException("Invalid garage ID");
        }
    }

    private Long extractBayId(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        if (parts.length >= 3) {
            try {
                return Long.parseLong(parts[2]);
            }
            catch (NumberFormatException e) {
                throw new BadRequestException("Invalid bay ID");
            }
        }
        return null;
    }

    private long requireBayId(HttpServletRequest request) {
        Long id = this.extractBayId(request);
        if (id == null) {
            throw new BadRequestException("Bay ID is required");
        }
        return id;
    }

    private GarageBay readBay(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        GarageBay b = new GarageBay();
        if (json.has("bay_name") && !json.get("bay_name").isNull()) {
            b.setBayName(json.get("bay_name").asText());
        }
        return b;
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
