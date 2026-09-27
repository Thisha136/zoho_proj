package com.bikebackend.model;

public class Brand {

    private Long bid;
    private String brandName;

    public Brand() {}

    public Long getBid() { return bid; }
    public void setBid(Long bid) { this.bid = bid; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
}
