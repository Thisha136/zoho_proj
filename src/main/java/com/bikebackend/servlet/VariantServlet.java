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
import com.bikebackend.model.Variant;
import com.bikebackend.service.VariantService;
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

public class VariantServlet
extends HttpServlet {
    private final VariantService variantService = new VariantService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            Long variantId = this.extractId(request);
            if (variantId != null) {
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.variantService.getVariant(variantId));
            } else {
                String midParam = request.getParameter("model_id");
                Long modelId = midParam != null ? Long.valueOf(Long.parseLong(midParam)) : null;
                this.objectMapper.writeValue((Writer)response.getWriter(), this.variantService.getVariants(modelId));
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
            Variant variant = this.readVariant(request);
            long variantId = this.variantService.createVariant(variant);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Variant created successfully", "variant_id", variantId));
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
            long variantId = this.requireId(request);
            Variant variant = this.readVariant(request);
            variant.setVariantId(variantId);
            this.variantService.updateVariant(variant);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Variant updated successfully"));
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
                long variantId = this.requireId(request);
                Variant incoming = this.readVariant(request);
                this.variantService.partialUpdateVariant(variantId, incoming);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Variant partially updated"));
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
            long variantId = this.requireId(request);
            this.variantService.deleteVariant(variantId);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Variant deleted successfully"));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    private Variant readVariant(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        Variant v = new Variant();
        if (json.has("model_id") && !json.get("model_id").isNull()) {
            v.setModelId(json.get("model_id").asLong());
        }
        if (json.has("variant_name") && !json.get("variant_name").isNull()) {
            v.setVariantName(json.get("variant_name").asText());
        }
        return v;
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
            throw new BadRequestException("Invalid variant ID");
        }
    }

    private long requireId(HttpServletRequest request) {
        Long id = this.extractId(request);
        if (id == null) {
            throw new BadRequestException("Variant ID is required");
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
