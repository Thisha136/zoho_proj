package com.bikebackend.model;

import java.time.LocalDateTime;

public class JobService_model {
    private Long jsId, jid, sid;
    private String status, selectionType, servicePartType, specialization;
    private Integer duration;
    private LocalDateTime actualStartTime, actualEndTime;
    public JobService_model() {}
    public Long getJsId() { return jsId; }
    public void setJsId(Long value) { jsId = value; }
    public Long getJid() { return jid; }
    public void setJid(Long value) { jid = value; }
    public Long getSid() { return sid; }
    public void setSid(Long value) { sid = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getSelectionType() { return selectionType; }
    public void setSelectionType(String value) { selectionType = value; }
    public String getServicePartType() { return servicePartType; }
    public void setServicePartType(String value) { servicePartType = value; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String value) { specialization = value; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer value) { duration = value; }
    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(LocalDateTime value) { actualStartTime = value; }
    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime value) { actualEndTime = value; }
}
