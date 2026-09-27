package com.bikebackend.model;

import java.time.LocalDate;

public class BayAllocation {

    private Long allocationId;
    private Long bayId;
    private Long jid;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public BayAllocation() {}

    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }

    public Long getBayId() { return bayId; }
    public void setBayId(Long bayId) { this.bayId = bayId; }

    public Long getJid() { return jid; }
    public void setJid(Long jid) { this.jid = jid; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
