package com.bikebackend.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.service.ServiceRequestService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

/**
 * GARAGE endpoints for service requests:
 *
 * GET    /garages/{gid}/service-requests
 * GET    /garages/{gid}/service-requests/{sr_id}
 * GET    /garages/{gid}/service-requests/count
 * PATCH  /garages/{gid}/service-requests/{sr_id}/physical-review
 * PATCH  /garages/{gid}/service-requests/{sr_id}/delivery-date
 * POST   /garages/{gid}/service-requests/{sr_id}/accept
 * POST   /garages/{gid}/service-requests/{sr_id}/reject
 */
public class GarageServiceRequestServlet extends HttpServlet {

    private final ServiceRequestService srService = new ServiceRequestService();
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long gid     = extractGid(request);
            String extra = extractExtra(request); // sr_id or "count" or null
            Long srId    = parseSrId(extra);

            if ("count".equals(extra)) {
                String status = request.getParameter("status");
                int count = srService.countRequests(gid, null, status);
                objectMapper.writeValue(response.getWriter(), Map.of("count", count));
            } else if (srId != null) {
                objectMapper.writeValue(response.getWriter(),
                        srService.getServiceRequest(srId));
            } else {
                String status = request.getParameter("status");
                String from   = request.getParameter("from");
                String to     = request.getParameter("to");
                objectMapper.writeValue(response.getWriter(),
                        srService.getGarageRequests(gid, status, from, to));
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
            long gid   = extractGid(request);
            long srId  = requireSrId(request);
            String action = extractAction(request);

            if ("accept".equals(action)) {
                srService.acceptRequest(gid, srId);
                objectMapper.writeValue(response.getWriter(),
                        Map.of("message", "Service request accepted"));
            } else if ("reject".equals(action)) {
                srService.rejectRequest(gid, srId);
                objectMapper.writeValue(response.getWriter(),
                        Map.of("message", "Service request rejected"));
            } else {
                throw new BadRequestException("Invalid action");
            }
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
                handlePatch(request, response);
            } catch (AppException e) {
                sendError(response, e.getStatus(), e.getMessage());
            } catch (Exception e) { handleUnexpected(response, e); }
            return;
        }
        super.service(request, response);
    }

    private void handlePatch(HttpServletRequest request,
                              HttpServletResponse response)
            throws IOException, SQLException {

        long gid    = extractGid(request);
        long srId   = requireSrId(request);
        String sub  = extractAction(request);

        JsonNode body = objectMapper.readTree(request.getReader());

        if ("physical-review".equals(sub)) {
            String dateStr = body.has("physical_review_date")
                    ? body.get("physical_review_date").asText(null) : null;
            if (dateStr == null) throw new BadRequestException("physical_review_date is required");
            srService.recordPhysicalReview(gid, srId, LocalDate.parse(dateStr));
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Physical review recorded"));

        } else if ("delivery-date".equals(sub)) {
            String dateStr = body.has("garage_proposed_delivery_date")
                    ? body.get("garage_proposed_delivery_date").asText(null) : null;
            if (dateStr == null) throw new BadRequestException("garage_proposed_delivery_date is required");
            srService.proposeDeliveryDate(gid, srId, LocalDate.parse(dateStr));
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Delivery date proposed"));
        } else {
            throw new BadRequestException("Invalid action");
        }
    }

    // path: /{gid}/service-requests/{sr_id}/{action}
    private long extractGid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        try { return Long.parseLong(parts[0]); }
        catch (NumberFormatException e) { throw new BadRequestException("Invalid garage ID"); }
    }

    private String extractExtra(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        // parts[0]=gid, parts[1]="service-requests", parts[2]=sr_id or "count"
        return parts.length >= 3 ? parts[2] : null;
    }

    private Long parseSrId(String value) {
        if (value == null) return null;
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { return null; }
    }

    private long requireSrId(HttpServletRequest request) {
        Long srId = parseSrId(extractExtra(request));
        if (srId == null) throw new BadRequestException("Service request ID is required");
        return srId;
    }

    private String extractAction(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        return parts.length >= 4 ? parts[3] : null;
    }

    private void sendError(HttpServletResponse r, int status, String msg) throws IOException {
        r.setStatus(status); r.setContentType("application/json"); r.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(r.getWriter(), Map.of("error", msg));
    }

    private void handleUnexpected(HttpServletResponse r, Exception e) throws IOException {
        e.printStackTrace(); sendError(r, 500, "Internal server error");
    }
}
