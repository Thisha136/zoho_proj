package com.bikebackend.model;

// Named VehicleModel to avoid conflict with java.lang.reflect.Model
public class VehicleModel {

    private Long modelId;
    private Long bid;
    private String modelName;

    public VehicleModel() {}

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }

    public Long getBid() { return bid; }
    public void setBid(Long bid) { this.bid = bid; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}
