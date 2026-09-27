package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Invoice;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    private static final String SELECT_ALL =
            "SELECT iv_id, jid, status, invoice_date, amount FROM invoices";

    public long createInvoice(Invoice invoice) throws SQLException {

        String sql = "INSERT INTO invoices (jid, status, amount) VALUES (?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            st.setLong(1, invoice.getJid());
            st.setString(2, invoice.getStatus());
            st.setBigDecimal(3, invoice.getAmount());
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Invoice ID was not generated");
    }

    public Invoice getById(long ivId) throws SQLException {

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(SELECT_ALL + " WHERE iv_id = ?")
        ) {
            st.setLong(1, ivId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapInvoice(rs);
            }
        }

        return null;
    }

    public Invoice getByJob(long jid) throws SQLException {

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(SELECT_ALL + " WHERE jid = ?")
        ) {
            st.setLong(1, jid);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapInvoice(rs);
            }
        }

        return null;
    }

    public boolean updateStatus(long ivId, String status) throws SQLException {

        String sql = "UPDATE invoices SET status = ? WHERE iv_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setString(1, status);
            st.setLong(2, ivId);
            return st.executeUpdate() > 0;
        }
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setIvId(rs.getLong("iv_id"));
        inv.setJid(rs.getLong("jid"));
        inv.setStatus(rs.getString("status"));
        Timestamp dt = rs.getTimestamp("invoice_date");
        if (dt != null) inv.setInvoiceDate(dt.toLocalDateTime());
        inv.setAmount(rs.getBigDecimal("amount"));
        return inv;
    }
}
