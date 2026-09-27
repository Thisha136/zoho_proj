package com.bikebackend.model;

import java.time.LocalDateTime;

public class MechanicAllocation {
    private Long jsId;
    private Long mechanicId;
    private LocalDateTime start;
    private LocalDateTime end;
    public MechanicAllocation() {}
    public MechanicAllocation(Long jsId, Long mechanicId, LocalDateTime start, LocalDateTime end) { this.jsId = jsId; this.mechanicId = mechanicId; this.start = start; this.end = end; }
    public Long getJsId() { return jsId; }
    public void setJsId(Long value) { jsId = value; }
    public Long getMechanicId() { return mechanicId; }
    public void setMechanicId(Long value) { mechanicId = value; }
    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime value) { start = value; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime value) { end = value; }
}
