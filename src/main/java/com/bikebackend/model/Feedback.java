package com.bikebackend.model;

import java.time.LocalDateTime;

public class Feedback {

    private Long feedbackId;
    private Long jid;
    private Integer rating;
    private String comments;
    private LocalDateTime createdAt;

    public Feedback() {}

    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public Long getJid() { return jid; }
    public void setJid(Long jid) { this.jid = jid; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
