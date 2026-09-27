package com.bikebackend.service;

import com.bikebackend.dao.BayAllocationDAO;
import com.bikebackend.dao.GarageBayDAO;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.ConflictException;
import com.bikebackend.exception.ForbiddenException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.BayAllocation;
import com.bikebackend.model.GarageBay;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class BayAllocationService {

    private final BayAllocationDAO allocationDAO = new BayAllocationDAO();
    private final GarageBayDAO     bayDAO        = new GarageBayDAO();

    // =========================
    // ALLOCATE A BAY TO A JOB
    // =========================
    //
    // Concurrency note:
    // hasConflict() + createAllocation() are two separate queries.
    // For race-condition safety the DB should have a UNIQUE constraint
    // or the calling code wraps this in a transaction with SELECT FOR UPDATE.
    // That will be added in the concurrency module.
    //
    public long allocate(long gid, BayAllocation allocation) throws SQLException {

        if (allocation.getBayId()    == null) throw new BadRequestException("Bay ID is required");
        if (allocation.getJid()      == null) throw new BadRequestException("Job ID is required");
        if (allocation.getStartDate()== null) throw new BadRequestException("Start date is required");
        if (allocation.getEndDate()  == null) throw new BadRequestException("End date is required");

        if (!allocation.getStartDate().isBefore(allocation.getEndDate())
                && !allocation.getStartDate().isEqual(allocation.getEndDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        // Verify the bay belongs to this garage
        GarageBay bay = bayDAO.getById(allocation.getBayId());
        if (bay == null)               throw new NotFoundException("Bay not found");
        if (!bay.getGid().equals(gid)) throw new ForbiddenException("Bay does not belong to this garage");

        // Check for overlapping allocations
        if (allocationDAO.hasConflict(allocation.getBayId(),
                allocation.getStartDate(), allocation.getEndDate())) {
            throw new ConflictException("Bay is already allocated for the requested date range");
        }

        return allocationDAO.createAllocation(allocation);
    }

    public List<BayAllocation> getAllocations(long gid) throws SQLException {
        return allocationDAO.getByGarage(gid);
    }

    public BayAllocation getAllocation(long gid, long allocationId) throws SQLException {
        BayAllocation allocation = allocationDAO.getById(allocationId);
        if (allocation == null) throw new NotFoundException("Allocation not found");

        // Verify it belongs to this garage via the bay
        GarageBay bay = bayDAO.getById(allocation.getBayId());
        if (bay == null || !bay.getGid().equals(gid)) throw new ForbiddenException("Access denied");

        return allocation;
    }

    public void updateStatus(long gid, long allocationId, String status) throws SQLException {
        getAllocation(gid, allocationId); // verifies ownership
        allocationDAO.updateStatus(allocationId, status);
    }

    public void updateStatus(long gid, long allocationId, String status, int expectedVersion) throws SQLException {
        getAllocation(gid, allocationId);
        if (!allocationDAO.updateStatus(allocationId, status, expectedVersion))
            throw new ConflictException("Bay allocation was modified by another request; refresh and retry");
    }

    // =========================
    // BAY AVAILABILITY CHECK
    // GET /garages/{gid}/bay-availability?date={date}
    // =========================

    public List<Map<String, Object>> getAvailability(long gid, String dateStr) throws SQLException {
        if (dateStr == null || dateStr.isBlank())
            throw new BadRequestException("date parameter is required");
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new BadRequestException("Invalid date format — use YYYY-MM-DD");
        }
        return allocationDAO.getAvailability(gid, date);
    }
}
