package com.bikebackend.model;

public class Garage {

    private Long gid;
    private String garageName;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;

    public Garage() {}

    public Long getGid() { return gid; }
    public void setGid(Long gid) { this.gid = gid; }

    public String getGarageName() { return garageName; }
    public void setGarageName(String garageName) { this.garageName = garageName; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
}
