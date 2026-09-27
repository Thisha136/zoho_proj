/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.Module
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
 *  jakarta.servlet.http.HttpServlet
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 */
package com.bikebackend.servlet;

import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.model.Payment;
import com.bikebackend.service.InvoiceService;
import com.bikebackend.service.JobService;
import com.bikebackend.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Map;

public class InvoiceServlet
extends HttpServlet {
    private final InvoiceService invoiceService = new InvoiceService();
    private final PaymentService paymentService = new PaymentService();
    private final JobService jobService = new JobService();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule((Module)new JavaTimeModule());

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        block12: {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            try {
                String path = request.getServletPath();
                if (path.startsWith("/payments")) {
                    String p = request.getPathInfo();
                    if ("/summary".equals(p)) {
                        String from = request.getParameter("from");
                        String to = request.getParameter("to");
                        this.objectMapper.writeValue((Writer)response.getWriter(), this.paymentService.getSummary(null, from, to));
                    } else if (p == null || p.equals("/")) {
                        String status = request.getParameter("status");
                        String from = request.getParameter("from");
                        String to = request.getParameter("to");
                        String gidP = request.getParameter("gid");
                        Long gid = gidP != null ? Long.valueOf(Long.parseLong(gidP)) : null;
                        this.objectMapper.writeValue((Writer)response.getWriter(), this.paymentService.getAllPayments(status, gid, from, to));
                    } else {
                        long paymentId = this.parseLong(p.substring(1), "Payment ID");
                        this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.paymentService.getPayment(paymentId));
                    }
                    break block12;
                }
                String pathInfo = request.getPathInfo();
                if (pathInfo == null || "/".equals(pathInfo)) {
                    throw new BadRequestException("Invoice ID is required");
                }
                String[] parts = pathInfo.substring(1).split("/");
                long ivId = this.parseLong(parts[0], "Invoice ID");
                if (parts.length == 1) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.invoiceService.getInvoice(ivId));
                    break block12;
                }
                if (parts.length == 2 && "payments".equals(parts[1])) {
                    this.objectMapper.writeValue((Writer)response.getWriter(), this.paymentService.getPaymentsByInvoice(ivId));
                    break block12;
                }
                if (parts.length == 3 && "payments".equals(parts[1])) {
                    long paymentId = this.parseLong(parts[2], "Payment ID");
                    this.objectMapper.writeValue((Writer)response.getWriter(), (Object)this.paymentService.getPaymentForInvoice(ivId, paymentId));
                    break block12;
                }
                throw new BadRequestException("Invalid invoice URL");
            }
            catch (AppException e) {
                this.sendError(response, e.getStatus(), e.getMessage());
            }
            catch (Exception e) {
                this.handleUnexpected(response, e);
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            JsonNode json = this.objectMapper.readTree((Reader)request.getReader());
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || "/".equals(pathInfo)) {
                BigDecimal amount;
                long gid = this.getCallerGid(request);
                long jid = json.has("jid") && !json.get("jid").isNull() ? json.get("jid").asLong() : 0L;
                BigDecimal bigDecimal = amount = json.has("amount") && !json.get("amount").isNull() ? new BigDecimal(json.get("amount").asText()) : null;
                if (jid <= 0L) {
                    throw new BadRequestException("jid is required");
                }
                if (amount == null || amount.signum() <= 0) {
                    throw new BadRequestException("Valid amount is required");
                }
                this.jobService.getGarageJob(gid, jid);
                long invoiceId = this.invoiceService.createInvoice(jid, amount);
                response.setStatus(201);
                this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Invoice created successfully", "invoice_id", invoiceId));
                return;
            }
            String[] parts = pathInfo.substring(1).split("/");
            if (parts.length != 2 || !"payments".equals(parts[1])) {
                throw new BadRequestException("Invalid invoice payment URL");
            }
            long ivId = this.parseLong(parts[0], "Invoice ID");
            Payment payment = new Payment();
            if (json.has("amount") && !json.get("amount").isNull()) {
                payment.setAmount(new BigDecimal(json.get("amount").asText()));
            }
            if (json.has("payment_method") && !json.get("payment_method").isNull()) {
                payment.setPaymentMethod(json.get("payment_method").asText());
            }
            if (json.has("payment_reference") && !json.get("payment_reference").isNull()) {
                payment.setPaymentReference(json.get("payment_reference").asText());
            }
            long paymentId = this.paymentService.createPayment(ivId, payment);
            response.setStatus(201);
            this.objectMapper.writeValue((Writer)response.getWriter(), Map.of("message", "Payment recorded successfully", "payment_id", paymentId));
        }
        catch (AppException e) {
            this.sendError(response, e.getStatus(), e.getMessage());
        }
        catch (Exception e) {
            this.handleUnexpected(response, e);
        }
    }

    private long getCallerGid(HttpServletRequest request) {
        Object attr = request.getAttribute("gid");
        if (attr != null) {
            return (Long)attr;
        }
        String header = request.getHeader("X-Garage-Id");
        if (header != null) {
            return Long.parseLong(header);
        }
        throw new BadRequestException("Garage ID not available \u2014 authentication required");
    }

    private long parseLong(String value, String label) {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            throw new BadRequestException("Invalid " + label);
        }
    }

    private void sendError(HttpServletResponse r, int status, String msg) throws IOException {
        r.setStatus(status);
        r.setContentType("application/json");
        r.setCharacterEncoding("UTF-8");
        this.objectMapper.writeValue((Writer)r.getWriter(), Map.of("error", msg));
    }

    private void handleUnexpected(HttpServletResponse r, Exception e) throws IOException {
        e.printStackTrace();
        this.sendError(r, 500, "Internal server error");
    }
}
