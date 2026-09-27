package com.bikebackend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Bike {

    private Long bikeId;
    private Long uid;
    private Long variantId;
    private Integer manufacturingYear;
    private LocalDate purchaseDate;
    private String registrationNo;
    private String fuelType;
    private Integer odometerReading;
    private String color;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Bike() {}

    public Long getBikeId() { return bikeId; }
    public void setBikeId(Long bikeId) { this.bikeId = bikeId; }

    public Long getUid() { return uid; }
    public void setUid(Long uid) { this.uid = uid; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public Integer getManufacturingYear() { return manufacturingYear; }
    public void setManufacturingYear(Integer manufacturingYear) { this.manufacturingYear = manufacturingYear; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public Integer getOdometerReading() { return odometerReading; }
    public void setOdometerReading(Integer odometerReading) { this.odometerReading = odometerReading; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
