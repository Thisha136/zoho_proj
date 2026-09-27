package com.bikebackend.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.service.GarageServiceService;

import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * GET    /garages/{gid}/services
 * GET    /garages/{gid}/services/{sid}
 * POST   /garages/{gid}/services
 * DELETE /garages/{gid}/services/{sid}
 */
public class GarageServiceServlet extends HttpServlet {

    private final GarageServiceService service = new GarageServiceService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long gid  = extractGid(request);
            Long sid  = extractSid(request);
            if (sid != null) {
                objectMapper.writeValue(response.getWriter(), service.getService(gid, sid));
            } else {
                objectMapper.writeValue(response.getWriter(), service.getServices(gid));
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
            long gid = extractGid(request);
            JsonNode json = objectMapper.readTree(request.getReader());
            if (!json.has("sid")) throw new BadRequestException("sid is required");
            long sid = json.get("sid").asLong();
            Integer duration = json.has("duration") && !json.get("duration").isNull()
                    ? json.get("duration").asInt() : null;
            BigDecimal price = json.has("price") && !json.get("price").isNull()
                    ? new BigDecimal(json.get("price").asText()) : null;
            service.addService(gid, sid, duration, price);
            response.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Service added to garage"));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    @Override
    protected void doDelete(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long gid = extractGid(request);
            long sid = requireSid(request);
            service.removeService(gid, sid);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Service removed from garage"));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    // path: /{gid}/services/{sid}
    private long extractGid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        try { return Long.parseLong(parts[0]); }
        catch (NumberFormatException e) { throw new BadRequestException("Invalid garage ID"); }
    }

    private Long extractSid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        if (parts.length >= 3) {
            try { return Long.parseLong(parts[2]); }
            catch (NumberFormatException e) { throw new BadRequestException("Invalid service ID"); }
        }
        return null;
    }

    private long requireSid(HttpServletRequest request) {
        Long sid = extractSid(request);
        if (sid == null) throw new BadRequestException("Service ID is required");
        return sid;
    }

    private void sendError(HttpServletResponse r, int status, String msg) throws IOException {
        r.setStatus(status); r.setContentType("application/json"); r.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(r.getWriter(), Map.of("error", msg));
    }

    private void handleUnexpected(HttpServletResponse r, Exception e) throws IOException {
        e.printStackTrace(); sendError(r, 500, "Internal server error");
    }
}
