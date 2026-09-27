package com.bikebackend.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.model.Bike;
import com.bikebackend.storage.BikeImageStorage;
import com.bikebackend.service.BikeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.time.LocalDate;
import java.util.Map;

/**
 * POST   /users/{uid}/bikes
 * GET    /users/{uid}/bikes
 * GET    /users/{uid}/bikes/{bike_id}
 * PUT    /users/{uid}/bikes/{bike_id}
 * PATCH  /users/{uid}/bikes/{bike_id}
 * DELETE /users/{uid}/bikes/{bike_id}
 *
 * Admin:
 * GET    /bikes
 * GET    /bikes/{bike_id}
 * GET    /bikes?uid={uid}&variant_id={variant_id}
 */
public class BikeServlet extends HttpServlet {

    private final BikeService bikeService = new BikeService();
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String path = request.getServletPath(); // /bikes or /users

            if (path.startsWith("/bikes")) {
                // Admin: GET /bikes or /bikes/{bike_id}
                Long bikeId = extractBikeIdAdmin(request);
                if (bikeId != null) {
                    objectMapper.writeValue(response.getWriter(),
                            bikeService.getAnyBike(bikeId));
                } else {
                    String uidParam = request.getParameter("uid");
                    String vidParam = request.getParameter("variant_id");
                    Long uid       = uidParam != null ? Long.parseLong(uidParam) : null;
                    Long variantId = vidParam != null ? Long.parseLong(vidParam) : null;
                    objectMapper.writeValue(response.getWriter(),
                            bikeService.getAllBikes(uid, variantId));
                }
            } else {
                // User: GET /users/{uid}/bikes or /users/{uid}/bikes/{bike_id}
                long uid    = extractUid(request);
                Long bikeId = extractBikeId(request);
                if (bikeId != null) {
                    objectMapper.writeValue(response.getWriter(),
                            bikeService.getBike(uid, bikeId));
                } else {
                    objectMapper.writeValue(response.getWriter(),
                            bikeService.getBikes(uid));
                }
            }

        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long uid  = extractUid(request);
            Bike bike = isMultipart(request)
                    ? readMultipartBike(request)
                    : readBike(request);
            bike.setUid(uid);
            long bikeId = bikeService.createBike(bike);
            response.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Bike created successfully", "bike_id", bikeId));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long uid    = extractUid(request);
            long bikeId = requireBikeId(request);
            Bike bike   = readBike(request);
            bike.setBikeId(bikeId);
            bikeService.updateBike(uid, bike);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Bike updated successfully"));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    @Override
    protected void service(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            try {
                long uid    = extractUid(request);
                long bikeId = requireBikeId(request);
                Bike incoming = readBike(request);
                bikeService.partialUpdateBike(uid, bikeId, incoming);
                objectMapper.writeValue(response.getWriter(),
                        Map.of("message", "Bike partially updated"));
            } catch (AppException e) {
                sendError(response, e.getStatus(), e.getMessage());
            } catch (Exception e) { handleUnexpected(response, e); }
            return;
        }
        super.service(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long uid    = extractUid(request);
            long bikeId = requireBikeId(request);
            bikeService.deleteBike(uid, bikeId);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Bike deleted successfully"));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    // =====================================================
    // PATH HELPERS
    // /users/{uid}/bikes/{bike_id}
    // =====================================================

    private long extractUid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        try { return Long.parseLong(parts[0]); }
        catch (NumberFormatException e) { throw new BadRequestException("Invalid user ID"); }
    }

    private Long extractBikeId(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        // parts[0]=uid, parts[1]="bikes", parts[2]=bike_id
        if (parts.length >= 3) {
            try { return Long.parseLong(parts[2]); }
            catch (NumberFormatException e) { throw new BadRequestException("Invalid bike ID"); }
        }
        return null;
    }

    private long requireBikeId(HttpServletRequest request) {
        Long id = extractBikeId(request);
        if (id == null) throw new BadRequestException("Bike ID is required");
        return id;
    }

    // Admin: /bikes/{bike_id}
    private Long extractBikeIdAdmin(HttpServletRequest request) {
        String p = request.getPathInfo();
        if (p == null || p.equals("/")) return null;
        try { return Long.parseLong(p.substring(1)); }
        catch (NumberFormatException e) { throw new BadRequestException("Invalid bike ID"); }
    }

    private Bike readBike(HttpServletRequest request) throws IOException {
        JsonNode json = objectMapper.readTree(request.getReader());
        Bike b = new Bike();
        if (json.has("variant_id")        && !json.get("variant_id").isNull())        b.setVariantId(json.get("variant_id").asLong());
        if (json.has("manufacturing_year") && !json.get("manufacturing_year").isNull()) b.setManufacturingYear(json.get("manufacturing_year").asInt());
        if (json.has("purchase_date")     && !json.get("purchase_date").isNull())     b.setPurchaseDate(LocalDate.parse(json.get("purchase_date").asText()));
        if (json.has("registration_no")   && !json.get("registration_no").isNull())   b.setRegistrationNo(json.get("registration_no").asText());
        if (json.has("fuel_type")         && !json.get("fuel_type").isNull())         b.setFuelType(json.get("fuel_type").asText());
        if (json.has("odometer_reading")  && !json.get("odometer_reading").isNull())  b.setOdometerReading(json.get("odometer_reading").asInt());
        if (json.has("color")             && !json.get("color").isNull())             b.setColor(json.get("color").asText());
        if (json.has("image_url")         && !json.get("image_url").isNull())         b.setImageUrl(json.get("image_url").asText());
        return b;
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }

    private Bike readMultipartBike(HttpServletRequest request) throws IOException, ServletException {
        Bike b = new Bike();
        b.setVariantId(formLong(request, "variant_id"));
        b.setManufacturingYear(formInteger(request, "manufacturing_year"));
        String purchaseDate = formText(request, "purchase_date");
        if (purchaseDate != null && !purchaseDate.isBlank()) b.setPurchaseDate(LocalDate.parse(purchaseDate));
        b.setRegistrationNo(formText(request, "registration_no"));
        b.setFuelType(formText(request, "fuel_type"));
        b.setOdometerReading(formInteger(request, "odometer_reading"));
        b.setColor(formText(request, "color"));

        Part image = request.getPart("image");
        if (image != null && image.getSize() > 0) {
            b.setImageUrl(BikeImageStorage.store(request, image));
        }
        return b;
    }

    private String formText(HttpServletRequest request, String name) throws IOException, ServletException {
        Part part = request.getPart(name);
        return part == null ? null : new String(part.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private Integer formInteger(HttpServletRequest request, String name) throws IOException, ServletException {
        String value = formText(request, name);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private Long formLong(HttpServletRequest request, String name) throws IOException, ServletException {
        String value = formText(request, name);
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private String extensionFor(String contentType, String submittedName) {
        if (submittedName != null && submittedName.contains("."))
            return submittedName.substring(submittedName.lastIndexOf('.')).toLowerCase();
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private void sendError(HttpServletResponse r, int status, String msg) throws IOException {
        r.setStatus(status); r.setContentType("application/json"); r.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(r.getWriter(), Map.of("error", msg));
    }

    private void handleUnexpected(HttpServletResponse r, Exception e) throws IOException {
        e.printStackTrace(); sendError(r, 500, "Internal server error");
    }
}
