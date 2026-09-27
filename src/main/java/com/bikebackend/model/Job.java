package com.bikebackend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Job {

    private Long jid;
    private Long srId;
    private String status;
    private LocalDate jobDate;
    private LocalDateTime serviceStart;
    private LocalDateTime serviceEnd;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String pickupStatus;
    private String deliveryStatus;

    public Job() {}

    public Long getJid() { return jid; }
    public void setJid(Long jid) { this.jid = jid; }

    public Long getSrId() { return srId; }
    public void setSrId(Long srId) { this.srId = srId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getJobDate() { return jobDate; }
    public void setJobDate(LocalDate jobDate) { this.jobDate = jobDate; }

    public LocalDateTime getServiceStart() { return serviceStart; }
    public void setServiceStart(LocalDateTime serviceStart) { this.serviceStart = serviceStart; }

    public LocalDateTime getServiceEnd() { return serviceEnd; }
    public void setServiceEnd(LocalDateTime serviceEnd) { this.serviceEnd = serviceEnd; }

    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }

    public LocalDate getActualDeliveryDate() { return actualDeliveryDate; }
    public void setActualDeliveryDate(LocalDate actualDeliveryDate) { this.actualDeliveryDate = actualDeliveryDate; }

    public String getPickupStatus() { return pickupStatus; }
    public void setPickupStatus(String pickupStatus) { this.pickupStatus = pickupStatus; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
}
