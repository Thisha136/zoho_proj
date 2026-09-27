package com.bikebackend.service;

import com.bikebackend.dao.InvoiceDAO;
import com.bikebackend.dao.PaymentDAO;
import com.bikebackend.exception.*;
import com.bikebackend.model.Invoice;
import com.bikebackend.model.Payment;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class PaymentService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();

    public long createPayment(long ivId, Payment payment) throws SQLException {

        Invoice invoice = invoiceDAO.getById(ivId);
        if (invoice == null) throw new NotFoundException("Invoice not found");
        if ("PAID".equals(invoice.getStatus()))
            throw new ConflictException("Invoice is already paid");

        if (payment.getAmount() == null)         throw new BadRequestException("Amount is required");
        if (payment.getPaymentMethod() == null)  throw new BadRequestException("Payment method is required");

        payment.setIvId(ivId);
        payment.setStatus("PAID");

        long paymentId = paymentDAO.createPayment(payment);

        // Mark invoice as paid
        invoiceDAO.updateStatus(ivId, "PAID");

        return paymentId;
    }

    public List<Payment> getPaymentsByInvoice(long ivId) throws SQLException {
        return paymentDAO.getByInvoice(ivId);
    }

    public Payment getPayment(long paymentId) throws SQLException {
        Payment payment = paymentDAO.getById(paymentId);
        if (payment == null) throw new NotFoundException("Payment not found");
        return payment;
    }

    public Payment getPaymentForInvoice(long ivId, long paymentId) throws SQLException {
        Payment payment = getPayment(paymentId);
        if (!Long.valueOf(ivId).equals(payment.getIvId()))
            throw new NotFoundException("Payment not found for invoice");
        return payment;
    }

    public List<Payment> getAllPayments(String status, Long gid, String from, String to)
            throws SQLException {
        return paymentDAO.getAll(status, gid, from, to);
    }

    public Map<String, Object> getSummary(Long gid, String from, String to) throws SQLException {
        return paymentDAO.getSummary(gid, from, to);
    }
}
