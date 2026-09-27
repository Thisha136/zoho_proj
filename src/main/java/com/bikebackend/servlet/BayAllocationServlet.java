package com.bikebackend.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.model.BayAllocation;
import com.bikebackend.service.BayAllocationService;

import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Bay allocations and availability:
 *
 * POST   /garages/{gid}/bay-allocations          — allocate a bay to a job
 * GET    /garages/{gid}/bay-allocations           — list all allocations
 * GET    /garages/{gid}/bay-allocations/{id}      — get one allocation
 * PATCH  /garages/{gid}/bay-allocations/{id}      — update status
 *
 * GET    /garages/{gid}/bay-availability?date=YYYY-MM-DD  — check availability
 */
public class BayAllocationServlet extends HttpServlet {

    private final BayAllocationService allocationService = new BayAllocationService();
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long gid = extractGid(request);
            BayAllocation allocation = readAllocation(request);
            long allocationId = allocationService.allocate(gid, allocation);
            response.setStatus(HttpServletResponse.SC_CREATED);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Bay allocated successfully",
                            "allocation_id", allocationId));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long gid = extractGid(request);

            // Bay availability check: /garages/{gid}/bay-availability?date=
            if (request.getServletPath().contains("bay-availability")) {
                String date = request.getParameter("date");
                objectMapper.writeValue(response.getWriter(),
                        allocationService.getAvailability(gid, date));
                return;
            }

            // Allocations
            Long allocationId = extractAllocationId(request);
            if (allocationId != null) {
                objectMapper.writeValue(response.getWriter(),
                        allocationService.getAllocation(gid, allocationId));
            } else {
                objectMapper.writeValue(response.getWriter(),
                        allocationService.getAllocations(gid));
            }

        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    @Override
    protected void doPatch(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long gid          = extractGid(request);
            long allocationId = requireAllocationId(request);
            JsonNode body     = objectMapper.readTree(request.getReader());
            String status     = body.has("status") ? body.get("status").asText(null) : null;
            if (status == null) throw new BadRequestException("Status is required");
            if (body.has("expected_version") && !body.get("expected_version").isNull())
                allocationService.updateStatus(gid, allocationId, status, body.get("expected_version").asInt());
            else
                allocationService.updateStatus(gid, allocationId, status);
            objectMapper.writeValue(response.getWriter(),
                    Map.of("message", "Allocation status updated"));
        } catch (AppException e) {
            sendError(response, e.getStatus(), e.getMessage());
        } catch (Exception e) { handleUnexpected(response, e); }
    }

    // =====================================================
    // PATH HELPERS
    // /garages/{gid}/bay-allocations/{allocation_id}
    // pathInfo from servlet: /{gid}/bay-allocations/{id}
    // =====================================================

    private long extractGid(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        try { return Long.parseLong(parts[0]); }
        catch (NumberFormatException e) { throw new BadRequestException("Invalid garage ID"); }
    }

    private Long extractAllocationId(HttpServletRequest request) {
        String[] parts = request.getPathInfo().substring(1).split("/");
        // parts[0]=gid, parts[1]="bay-allocations", parts[2]=allocation_id
        if (parts.length >= 3) {
            try { return Long.parseLong(parts[2]); }
            catch (NumberFormatException e) { throw new BadRequestException("Invalid allocation ID"); }
        }
        return null;
    }

    private long requireAllocationId(HttpServletRequest request) {
        Long id = extractAllocationId(request);
        if (id == null) throw new BadRequestException("Allocation ID is required");
        return id;
    }

    private BayAllocation readAllocation(HttpServletRequest request) throws IOException {
        JsonNode json = objectMapper.readTree(request.getReader());
        BayAllocation ba = new BayAllocation();
        if (json.has("bay_id")     && !json.get("bay_id").isNull())     ba.setBayId(json.get("bay_id").asLong());
        if (json.has("jid")        && !json.get("jid").isNull())        ba.setJid(json.get("jid").asLong());
        if (json.has("start_date") && !json.get("start_date").isNull()) ba.setStartDate(LocalDate.parse(json.get("start_date").asText()));
        if (json.has("end_date")   && !json.get("end_date").isNull())   ba.setEndDate(LocalDate.parse(json.get("end_date").asText()));
        if (json.has("status")     && !json.get("status").isNull())     ba.setStatus(json.get("status").asText());
        return ba;
    }

    private void sendError(HttpServletResponse r, int status, String msg) throws IOException {
        r.setStatus(status); r.setContentType("application/json"); r.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(r.getWriter(), Map.of("error", msg));
    }

    private void handleUnexpected(HttpServletResponse r, Exception e) throws IOException {
        e.printStackTrace(); sendError(r, 500, "Internal server error");
    }
}
