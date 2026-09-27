package com.bikebackend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ScheduleResult {
    private Long jobId;
    private String status;
    private LocalDate deliveryDate;
    private LocalDateTime jobStart;
    private LocalDateTime jobEnd;
    private Long bayId;
    private List<MechanicAllocation> mechanicAllocations;

    public ScheduleResult() {}
    public ScheduleResult(Long jobId, LocalDateTime start, LocalDateTime end, Long bayId, List<MechanicAllocation> allocations) {
        this.jobId = jobId;
        this.status = "ALLOCATED";
        this.jobStart = start;
        this.jobEnd = end;
        this.deliveryDate = end.toLocalDate();
        this.bayId = bayId;
        this.mechanicAllocations = allocations;
    }
    public Long getJobId() { return jobId; }
    public void setJobId(Long value) { jobId = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate value) { deliveryDate = value; }
    public LocalDateTime getJobStart() { return jobStart; }
    public void setJobStart(LocalDateTime value) { jobStart = value; }
    public LocalDateTime getJobEnd() { return jobEnd; }
    public void setJobEnd(LocalDateTime value) { jobEnd = value; }
    public Long getBayId() { return bayId; }
    public void setBayId(Long value) { bayId = value; }
    public List<MechanicAllocation> getMechanicAllocations() { return mechanicAllocations; }
    public void setMechanicAllocations(List<MechanicAllocation> value) { mechanicAllocations = value; }
}
