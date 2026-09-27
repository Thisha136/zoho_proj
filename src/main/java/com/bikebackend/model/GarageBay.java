package com.bikebackend.model;

public class GarageBay {

    private Long bayId;
    private Long gid;
    private String bayName;

    public GarageBay() {}

    public Long getBayId() { return bayId; }
    public void setBayId(Long bayId) { this.bayId = bayId; }

    public Long getGid() { return gid; }
    public void setGid(Long gid) { this.gid = gid; }

    public String getBayName() { return bayName; }
    public void setBayName(String bayName) { this.bayName = bayName; }

}
