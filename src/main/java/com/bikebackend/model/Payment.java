package com.bikebackend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {

    private Long paymentId;
    private Long ivId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentReference;
    private String status;
    private LocalDateTime paymentDate;

    public Payment() {}

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getIvId() { return ivId; }
    public void setIvId(Long ivId) { this.ivId = ivId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
