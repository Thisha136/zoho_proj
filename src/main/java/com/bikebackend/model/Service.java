package com.bikebackend.model;

import java.math.BigDecimal;

public class Service {
    private Long sid;
    private String serviceName;
    private Integer duration;
    private BigDecimal price;
    private String serviceType;
    private String specialization;

    public Service() {}
    public Long getSid() { return sid; }
    public void setSid(Long value) { sid = value; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String value) { serviceName = value; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer value) { duration = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price = value; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String value) { serviceType = value; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String value) { specialization = value; }
}
