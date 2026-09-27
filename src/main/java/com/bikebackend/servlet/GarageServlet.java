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
import com.bikebackend.exception.ForbiddenException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.Garage;
import com.bikebackend.model.GarageVariant;
import com.bikebackend.model.ServiceRequest;
import com.bikebackend.service.FeedbackService;
import com.bikebackend.service.GarageService;
import com.bikebackend.service.GarageServiceService;
import com.bikebackend.service.GarageVariantService;
import com.bikebackend.service.JobService;
import com.bikebackend.service.ServiceRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class GarageServlet
extends HttpServlet {
    private final GarageService garageService = new GarageService();
    private final GarageServiceService garageServiceService = new GarageServiceService();
    private final GarageVariantService garageVariantService = new GarageVariantService();
    private final ServiceRequestService serviceRequestService = new ServiceRequestService();
    private final JobService jobService = new JobService();
    private final FeedbackService feedbackService = new FeedbackService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String[] parts = this.nestedParts(request.getPathInfo());
            if (parts.length >= 2 && "jobs".equals(parts[1])) {
                long gid = this.parseId(parts[0], "garage");
                if (parts.length == 2) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.jobService.getGarageJobs(gid, request.getParameter("status"), request.getParameter("from"), request.getParameter("to")));
                    return;
                } else {
                    if (parts.length != 3) throw new BadRequestException("Invalid garage jobs URL");
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.jobService.getGarageJob(gid, this.parseId(parts[2], "job")));
                }
                return;
            }
            if (parts.length >= 2 && "service-requests".equals(parts[1])) {
                long gid = Long.parseLong(parts[0]);
                if (parts.length == 2) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.serviceRequestService.getGarageRequests(gid, request.getParameter("status"), request.getParameter("from"), request.getParameter("to")));
                    return;
                } else if (parts.length == 3 && "count".equals(parts[2])) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("count", this.serviceRequestService.countRequests(gid, null, request.getParameter("status"))));
                    return;
                } else {
                    if (parts.length != 3) throw new BadRequestException("Invalid garage service-requests URL");
                    ServiceRequest sr = this.serviceRequestService.getServiceRequest(Long.parseLong(parts[2]));
                    if (sr.getGid() == null || sr.getGid() != gid) {
                        throw new ForbiddenException("Service request does not belong to this garage");
                    }
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)sr);
                }
                return;
            }
            if (parts.length >= 2 && "feedback".equals(parts[1])) {
                long gid = this.parseId(parts[0], "garage");
                if (parts.length == 2) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.feedbackService.getGarageFeedback(gid));
                    return;
                } else if (parts.length == 3 && "summary".equals(parts[2])) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.feedbackService.getGarageSummary(gid));
                    return;
                } else {
                    if (parts.length != 3) throw new BadRequestException("Invalid garage feedback URL");
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.feedbackService.getGarageFeedbackById(gid, this.parseId(parts[2], "feedback")));
                }
                return;
            }
            if (parts.length >= 2 && ("services".equals(parts[1]) || "variants".equals(parts[1]))) {
                long gid = Long.parseLong(parts[0]);
                if ("services".equals(parts[1])) {
                    if (parts.length == 2) {
                        this.objectMapper.writeValue((Writer)response.getWriter(), this.garageServiceService.getServices(gid));
                        return;
                    } else {
                        if (parts.length != 3) throw new BadRequestException("Invalid garage services URL");
                        this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.garageServiceService.getService(gid, Long.parseLong(parts[2])));
                    }
                    return;
                } else {
                    List<GarageVariant> variants = this.garageVariantService.getVariants(gid);
                    if (parts.length == 3) {
                        long variantId = Long.parseLong(parts[2]);
                        variants.removeIf(v -> !Long.valueOf(variantId).equals(v.getVariantId()));
                        if (variants.isEmpty()) {
                            throw new NotFoundException("Variant not found for this garage");
                        }
                        this.objectMapper.writeValue((Writer)response.getWriter(), (Object)variants.get(0));
                        return;
                    } else {
                        if (parts.length != 2) throw new BadRequestException("Invalid garage variants URL");
                        this.objectMapper.writeValue((Writer)response.getWriter(), variants);
                    }
                }
                return;
            }
            Long gid = this.extractId(request);
            if (gid != null) {
                this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.garageService.getGarage(gid));
                return;
            } else {
                String vParam = request.getParameter("variant_id");
                Long variantId = vParam != null ? Long.valueOf(Long.parseLong(vParam)) : null;
                this.objectMapper.writeValue((Writer)response.getWriter(), this.garageService.getGarages(variantId));
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String[] parts = this.nestedParts(request.getPathInfo());
            if (parts.length >= 4 && "service-requests".equals(parts[1])) {
                long gid = this.parseId(parts[0], "garage");
                long srId = this.parseId(parts[2], "service request");
                if ("accept".equals(parts[3])) {
                    this.serviceRequestService.acceptRequest(gid, srId);
                    this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service request accepted"));
                } else if ("reject".equals(parts[3])) {
                    this.serviceRequestService.rejectRequest(gid, srId);
                    this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service request rejected"));
                } else {
                    throw new BadRequestException("Invalid service-request action");
                }
                return;
            }
            if (parts.length >= 2 && ("services".equals(parts[1]) || "variants".equals(parts[1]))) {
                if (parts.length != 2) {
                    throw new BadRequestException("Invalid nested garage URL");
                }
                long gid = this.parseId(parts[0], "garage");
                JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
                if ("services".equals(parts[1])) {
                    if (!json.has("sid")) {
                        throw new BadRequestException("sid is required");
                    }
                    this.garageServiceService.addService(gid, json.get("sid").asLong());
                    response.setStatus(201);
                    this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service added to garage"));
                } else {
                    if (!json.has("variant_id")) {
                        throw new BadRequestException("variant_id is required");
                    }
                    this.garageVariantService.addVariant(gid, json.get("variant_id").asLong());
                    response.setStatus(201);
                    this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Variant added to garage"));
                }
                return;
            }
            Garage garage = this.readGarage(request);
            long gid = this.garageService.createGarage(garage);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Garage created successfully", "gid", gid));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String[] parts = this.nestedParts(request.getPathInfo());
            if (parts.length != 3 || !"services".equals(parts[1]) && !"variants".equals(parts[1])) {
                throw new BadRequestException("Invalid nested garage URL");
            }
            long gid = this.parseId(parts[0], "garage");
            long childId = this.parseId(parts[2], "services".equals(parts[1]) ? "service" : "variant");
            if ("services".equals(parts[1])) {
                this.garageServiceService.removeService(gid, childId);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Service removed from garage"));
            } else {
                this.garageVariantService.removeVariant(gid, childId);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Variant removed from garage"));
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
            long gid = this.requireId(request);
            Garage garage = this.readGarage(request);
            garage.setGid(gid);
            this.garageService.updateGarage(garage);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Garage updated successfully"));
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
                String[] parts = this.nestedParts(request.getPathInfo());
                if (parts.length >= 3 && "jobs".equals(parts[1])) {
                    long gid = this.parseId(parts[0], "garage");
                    long jid = this.parseId(parts[2], "job");
                    String action = parts.length >= 4 ? parts[3] : null;
                    JsonNode body = this.objectMapper.readTree((Reader)request.getReader());
                    switch (action == null ? "" : action) {
                        case "status": {
                            String status;
                            String string = status = body.has("status") ? body.get("status").asText(null) : null;
                            if (status == null) {
                                throw new BadRequestException("Status is required");
                            }
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
                            String value;
                            String string = value = body.has("expected_delivery_date") ? body.get("expected_delivery_date").asText(null) : null;
                            if (value == null) {
                                throw new BadRequestException("expected_delivery_date is required");
                            }
                            this.jobService.updateExpectedDeliveryDate(gid, jid, LocalDate.parse(value));
                            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Delivery date updated"));
                            break;
                        }
                        case "pickup-status": {
                            String value;
                            String string = value = body.has("pickup_status") ? body.get("pickup_status").asText(null) : null;
                            if (value == null) {
                                throw new BadRequestException("pickup_status is required");
                            }
                            this.jobService.updatePickupStatus(gid, jid, value);
                            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Pickup status updated"));
                            break;
                        }
                        case "delivery-status": {
                            String value;
                            String string = value = body.has("delivery_status") ? body.get("delivery_status").asText(null) : null;
                            if (value == null) {
                                throw new BadRequestException("delivery_status is required");
                            }
                            this.jobService.updateDeliveryStatus(gid, jid, value);
                            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Delivery status updated"));
                            break;
                        }
                        default: {
                            throw new BadRequestException("Invalid garage job action");
                        }
                    }
                    return;
                }
                if (parts.length >= 4 && "service-requests".equals(parts[1])) {
                    long gid = this.parseId(parts[0], "garage");
                    long srId = this.parseId(parts[2], "service request");
                    JsonNode body = this.objectMapper.readTree((Reader)request.getReader());
                    if (body.has("physical_review_date") || "physical-review".equals(parts[3])) {
                        String value;
                        String string = value = body.has("physical_review_date") ? body.get("physical_review_date").asText(null) : null;
                        if (value == null) {
                            throw new BadRequestException("physical_review_date is required");
                        }
                        this.serviceRequestService.recordPhysicalReview(gid, srId, LocalDate.parse(value));
                        this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Physical review recorded"));
                    } else if (body.has("garage_proposed_delivery_date") || "delivery-date".equals(parts[3])) {
                        String value;
                        String string = value = body.has("garage_proposed_delivery_date") ? body.get("garage_proposed_delivery_date").asText(null) : null;
                        if (value == null) {
                            throw new BadRequestException("garage_proposed_delivery_date is required");
                        }
                        this.serviceRequestService.proposeDeliveryDate(gid, srId, LocalDate.parse(value));
                        this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Delivery date proposed"));
                    } else {
                        throw new BadRequestException("Invalid garage service-request action");
                    }
                    return;
                }
                long gid = this.requireId(request);
                Garage incoming = this.readGarage(request);
                this.garageService.partialUpdateGarage(gid, incoming);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Garage partially updated"));
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

    private Garage readGarage(HttpServletRequest request) throws IOException {
        JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
        Garage g = new Garage();
        if (json.has("garage_name") && !json.get("garage_name").isNull()) {
            g.setGarageName(json.get("garage_name").asText());
        }
        if (json.has("address_line") && !json.get("address_line").isNull()) {
            g.setAddressLine(json.get("address_line").asText());
        }
        if (json.has("city") && !json.get("city").isNull()) {
            g.setCity(json.get("city").asText());
        }
        if (json.has("state") && !json.get("state").isNull()) {
            g.setState(json.get("state").asText());
        }
        if (json.has("pincode") && !json.get("pincode").isNull()) {
            g.setPincode(json.get("pincode").asText());
        }
        return g;
    }

    private String[] nestedParts(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) {
            return new String[0];
        }
        return pathInfo.substring(1).split("/");
    }

    private Long extractId(HttpServletRequest request) {
        String p = request.getPathInfo();
        if (p == null || p.equals("/")) {
            return null;
        }
        String[] parts = p.substring(1).split("/");
        if (parts.length == 1) {
            try {
                return Long.parseLong(parts[0]);
            }
            catch (NumberFormatException e) {
                throw new BadRequestException("Invalid garage ID");
            }
        }
        return null;
    }

    private long parseId(String value, String resource) {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            throw new BadRequestException("Invalid " + resource + " ID");
        }
    }

    private long requireId(HttpServletRequest request) {
        Long id = this.extractId(request);
        if (id == null) {
            throw new BadRequestException("Garage ID is required");
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
