package com.bikebackend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ServiceRequest {

    private Long srId;
    private Long bikeId;
    private String title;
    private String issue;
    private String imageUrl;
    private Long gid;
    private String status;
    private LocalDateTime requestDate;
    private LocalDate physicalReviewDate;
    private String vehicleHandlingOption;
    private LocalDate preferredDeliveryDate;
    private LocalDate garageProposedDeliveryDate;
    private String customerDateResponse;

    public ServiceRequest() {}

    public Long getSrId() { return srId; }
    public void setSrId(Long srId) { this.srId = srId; }

    public Long getBikeId() { return bikeId; }
    public void setBikeId(Long bikeId) { this.bikeId = bikeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Long getGid() { return gid; }
    public void setGid(Long gid) { this.gid = gid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public LocalDate getPhysicalReviewDate() { return physicalReviewDate; }
    public void setPhysicalReviewDate(LocalDate physicalReviewDate) { this.physicalReviewDate = physicalReviewDate; }

    public String getVehicleHandlingOption() { return vehicleHandlingOption; }
    public void setVehicleHandlingOption(String vehicleHandlingOption) { this.vehicleHandlingOption = vehicleHandlingOption; }

    public LocalDate getPreferredDeliveryDate() { return preferredDeliveryDate; }
    public void setPreferredDeliveryDate(LocalDate preferredDeliveryDate) { this.preferredDeliveryDate = preferredDeliveryDate; }

    public LocalDate getGarageProposedDeliveryDate() { return garageProposedDeliveryDate; }
    public void setGarageProposedDeliveryDate(LocalDate garageProposedDeliveryDate) { this.garageProposedDeliveryDate = garageProposedDeliveryDate; }

    public String getCustomerDateResponse() { return customerDateResponse; }
    public void setCustomerDateResponse(String customerDateResponse) { this.customerDateResponse = customerDateResponse; }
}
