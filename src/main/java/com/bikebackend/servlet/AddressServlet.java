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
import com.bikebackend.model.Address;
import com.bikebackend.service.AddressService;
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
import java.util.Map;

public class AddressServlet
extends HttpServlet {
    private final AddressService addressService = new AddressService();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule((Module)new JavaTimeModule());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long uid = this.extractUid(request);
            Address address = this.readAddress(request);
            address.setUid(uid);
            long addressId = this.addressService.createAddress(address);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Address created successfully", "address_id", addressId));
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
            long uid = this.extractUid(request);
            Long addressId = this.extractAddressId(request);
            if (addressId != null) {
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.addressService.getAddress(uid, addressId));
            } else {
                this.objectMapper.writeValue((Writer)response.getWriter(), this.addressService.getAddresses(uid));
            }
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
            long uid = this.extractUid(request);
            long addressId = this.requireAddressId(request);
            Address address = this.readAddress(request);
            address.setAddressId(addressId);
            this.addressService.updateAddress(uid, address);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Address updated successfully"));
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
        long uid = this.extractUid(request);
        long addressId = this.requireAddressId(request);
        String extra = this.extractExtra(request);
        if ("default".equals(extra)) {
            this.addressService.setDefault(uid, addressId);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Default address updated"));
        } else {
            Address incoming = this.readAddress(request);
            this.addressService.partialUpdateAddress(uid, addressId, incoming);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Address partially updated"));
        }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long uid = this.extractUid(request);
            long addressId = this.requireAddressId(request);
            this.addressService.deleteAddress(uid, addressId);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Address deleted successfully"));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    private long extractUid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        try {
            return Long.parseLong(parts[0]);
        }
        catch (NumberFormatException e) {
            throw new BadRequestException("Invalid user ID");
        }
    }

    private Long extractAddressId(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        if (parts.length >= 3) {
            try {
                return Long.parseLong(parts[2]);
            }
            catch (NumberFormatException e) {
                throw new BadRequestException("Invalid address ID");
            }
        }
        return null;
    }

    private long requireAddressId(HttpServletRequest request) {
        Long id = this.extractAddressId(request);
        if (id == null) {
            throw new BadRequestException("Address ID is required");
        }
        return id;
    }

    private String extractExtra(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        return null;
    }

    private Address readAddress(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        Address a = new Address();
        if (json.has("address_type") && !json.get("address_type").isNull()) {
            a.setAddressType(json.get("address_type").asText());
        }
        if (json.has("address_line") && !json.get("address_line").isNull()) {
            a.setAddressLine(json.get("address_line").asText());
        }
        if (json.has("city") && !json.get("city").isNull()) {
            a.setCity(json.get("city").asText());
        }
        if (json.has("state") && !json.get("state").isNull()) {
            a.setState(json.get("state").asText());
        }
        if (json.has("pincode") && !json.get("pincode").isNull()) {
            a.setPincode(json.get("pincode").asText());
        }
        if (json.has("is_default") && !json.get("is_default").isNull()) {
            a.setIsDefault(json.get("is_default").asBoolean());
        }
        return a;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("error", message));
    }

    private void handleUnexpected(HttpServletResponse response, Exception e) throws IOException {
        e.printStackTrace();
        this.sendError(response, 500, "Internal server error");
    }
}
