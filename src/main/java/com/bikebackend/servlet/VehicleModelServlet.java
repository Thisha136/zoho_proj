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
import com.bikebackend.model.VehicleModel;
import com.bikebackend.service.VehicleModelService;
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

public class VehicleModelServlet
extends HttpServlet {
    private final VehicleModelService modelService = new VehicleModelService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            Long modelId = this.extractId(request);
            if (modelId != null) {
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.modelService.getModel(modelId));
            } else {
                String bidParam = request.getParameter("bid");
                Long bid = bidParam != null ? Long.valueOf(Long.parseLong(bidParam)) : null;
                this.objectMapper.writeValue((Writer)response.getWriter(), this.modelService.getModels(bid));
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
            VehicleModel model = this.readModel(request);
            long modelId = this.modelService.createModel(model);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Model created successfully", "model_id", modelId));
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
            long modelId = this.requireId(request);
            VehicleModel model = this.readModel(request);
            model.setModelId(modelId);
            this.modelService.updateModel(model);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Model updated successfully"));
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
                long modelId = this.requireId(request);
                VehicleModel incoming = this.readModel(request);
                this.modelService.partialUpdateModel(modelId, incoming);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Model partially updated"));
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
            long modelId = this.requireId(request);
            this.modelService.deleteModel(modelId);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Model deleted successfully"));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    private VehicleModel readModel(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        VehicleModel m = new VehicleModel();
        if (json.has("bid") && !json.get("bid").isNull()) {
            m.setBid(json.get("bid").asLong());
        }
        if (json.has("model_name") && !json.get("model_name").isNull()) {
            m.setModelName(json.get("model_name").asText());
        }
        return m;
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
            throw new BadRequestException("Invalid model ID");
        }
    }

    private long requireId(HttpServletRequest request) {
        Long id = this.extractId(request);
        if (id == null) {
            throw new BadRequestException("Model ID is required");
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
