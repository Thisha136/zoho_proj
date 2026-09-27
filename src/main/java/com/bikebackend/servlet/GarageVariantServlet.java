package com.bikebackend.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.service.GarageVariantService;

import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;

/**
 * GET    /garages/{gid}/variants
 * POST   /garages/{gid}/variants
 * DELETE /garages/{gid}/variants/{variant_id}
 */
public class GarageVariantServlet extends HttpServlet {

    private final GarageVariantService service = new GarageVariantService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long gid = extractGid(request);
            objectMapper.writeValue(response.getWriter(), service.getVariants(gid));
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
            if (!json.has("variant_id"))
                throw new BadRequestException("variant_id is required");
            long variantId = json.get("variant_id").asLong();
            service.addVariant(gid, variantId);
            response.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Variant added to garage"));
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
            long gid       = extractGid(request);
            long variantId = extractVariantId(request);
            service.removeVariant(gid, variantId);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Variant removed from garage"));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    // path: /{gid}/variants/{variant_id}
    private long extractGid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        try { return Long.parseLong(parts[0]); }
        catch (NumberFormatException e) { throw new BadRequestException("Invalid garage ID"); }
    }

    private long extractVariantId(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        if (parts.length < 3) throw new BadRequestException("Variant ID is required");
        try { return Long.parseLong(parts[2]); }
        catch (NumberFormatException e) { throw new BadRequestException("Invalid variant ID"); }
    }

    private void sendError(HttpServletResponse r, int status, String msg) throws IOException {
        r.setStatus(status); r.setContentType("application/json"); r.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(r.getWriter(), Map.of("error", msg));
    }

    private void handleUnexpected(HttpServletResponse r, Exception e) throws IOException {
        e.printStackTrace(); sendError(r, 500, "Internal server error");
    }
}
