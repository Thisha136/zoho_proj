package com.bikebackend.service;

import com.bikebackend.dao.InvoiceDAO;
import com.bikebackend.dao.JobDAO;
import com.bikebackend.dao.ServiceRequestDAO;
import com.bikebackend.exception.*;
import com.bikebackend.model.Invoice;
import com.bikebackend.model.Job;
import com.bikebackend.model.ServiceRequest;

import java.math.BigDecimal;
import java.sql.SQLException;

public class InvoiceService {

    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final JobDAO jobDAO = new JobDAO();
    private final ServiceRequestDAO srDAO = new ServiceRequestDAO();

    public long createInvoice(long jid, BigDecimal amount) throws SQLException {

        Job job = jobDAO.getById(jid);
        if (job == null) throw new NotFoundException("Job not found");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("Valid amount is required");

        Invoice invoice = new Invoice();
        invoice.setJid(jid);
        invoice.setStatus("UNPAID");
        invoice.setAmount(amount);

        return invoiceDAO.createInvoice(invoice);
    }

    public Invoice getInvoice(long ivId) throws SQLException {
        Invoice invoice = invoiceDAO.getById(ivId);
        if (invoice == null) throw new NotFoundException("Invoice not found");
        return invoice;
    }

    public Invoice getInvoiceByJob(long jid) throws SQLException {
        Invoice invoice = invoiceDAO.getByJob(jid);
        if (invoice == null) throw new NotFoundException("Invoice not found for this job");
        return invoice;
    }

    // Verify a user owns the invoice (via job → service_request)
    public Invoice getUserInvoice(long uid, long ivId) throws SQLException {
        Invoice invoice = getInvoice(ivId);
        Job job = jobDAO.getById(invoice.getJid());
        ServiceRequest sr = srDAO.getById(job.getSrId());
        if (sr == null || !srDAO.belongsToUser(sr.getSrId(), uid))
            throw new ForbiddenException("Access denied");
        return invoice;
    }
}
