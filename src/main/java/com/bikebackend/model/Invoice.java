package com.bikebackend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Invoice {

    private Long ivId;
    private Long jid;
    private String status;
    private LocalDateTime invoiceDate;
    private BigDecimal amount;

    public Invoice() {}

    public Long getIvId() { return ivId; }
    public void setIvId(Long ivId) { this.ivId = ivId; }

    public Long getJid() { return jid; }
    public void setJid(Long jid) { this.jid = jid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDateTime invoiceDate) { this.invoiceDate = invoiceDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
