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
import com.bikebackend.model.Brand;
import com.bikebackend.service.BrandService;
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

public class BrandServlet
extends HttpServlet {
    private final BrandService brandService = new BrandService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            Long bid = this.extractId(request);
            if (bid != null) {
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.brandService.getBrand(bid));
            } else {
                this.objectMapper.writeValue((Writer)response.getWriter(), this.brandService.getBrands(request.getParameter("search")));
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
            Brand brand = this.readBrand(request);
            long bid = this.brandService.createBrand(brand);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Brand created successfully", "bid", bid));
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
            long bid = this.requireId(request);
            Brand brand = this.readBrand(request);
            brand.setBid(bid);
            this.brandService.updateBrand(brand);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Brand updated successfully"));
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
                long bid = this.requireId(request);
                Brand incoming = this.readBrand(request);
                this.brandService.partialUpdateBrand(bid, incoming);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Brand partially updated"));
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
            long bid = this.requireId(request);
            this.brandService.deleteBrand(bid);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Brand deleted successfully"));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    private Brand readBrand(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        Brand b = new Brand();
        if (json.has("brand_name") && !json.get("brand_name").isNull()) {
            b.setBrandName(json.get("brand_name").asText());
        }
        return b;
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
            throw new BadRequestException("Invalid brand ID");
        }
    }

    private long requireId(HttpServletRequest request) {
        Long id = this.extractId(request);
        if (id == null) {
            throw new BadRequestException("Brand ID is required");
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
