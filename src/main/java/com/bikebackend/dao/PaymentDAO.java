package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaymentDAO {

    private static final String SELECT_ALL = """
            SELECT payment_id, iv_id, amount, payment_method, payment_reference, status, payment_date
            FROM payments
            """;

    public long createPayment(Payment payment) throws SQLException {

        String sql = """
                INSERT INTO payments (iv_id, amount, payment_method, payment_reference, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            st.setLong(1, payment.getIvId());
            st.setBigDecimal(2, payment.getAmount());
            st.setString(3, payment.getPaymentMethod());
            st.setString(4, payment.getPaymentReference());
            st.setString(5, payment.getStatus());
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Payment ID was not generated");
    }

    public List<Payment> getByInvoice(long ivId) throws SQLException {

        List<Payment> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(SELECT_ALL + " WHERE iv_id = ?")
        ) {
            st.setLong(1, ivId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapPayment(rs));
            }
        }

        return list;
    }

    public Payment getById(long paymentId) throws SQLException {

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(SELECT_ALL + " WHERE payment_id = ?")
        ) {
            st.setLong(1, paymentId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapPayment(rs);
            }
        }

        return null;
    }

    public List<Payment> getAll(String status, Long gid, String from, String to) throws SQLException {

        StringBuilder sql = new StringBuilder(SELECT_ALL);

        if (gid != null) {
            sql.append("""
                    JOIN invoices i ON payments.iv_id = i.iv_id
                    JOIN jobs j ON i.jid = j.jid
                    JOIN service_requests sr ON j.sr_id = sr.sr_id
                    WHERE sr.gid = ?
                    """);
        } else {
            sql.append(" WHERE 1=1");
        }

        List<Object> params = new ArrayList<>();
        if (gid != null) params.add(gid);

        if (status != null && !status.isBlank()) { sql.append(" AND payments.status = ?"); params.add(status); }
        if (from   != null && !from.isBlank())   { sql.append(" AND payment_date >= ?");   params.add(from); }
        if (to     != null && !to.isBlank())     { sql.append(" AND payment_date <= ?");   params.add(to); }

        sql.append(" ORDER BY payment_date DESC");

        List<Payment> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapPayment(rs));
            }
        }

        return list;
    }

    public Map<String, Object> getSummary(Long gid, String from, String to) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS total_count,
                       COALESCE(SUM(amount), 0) AS total_amount
                FROM payments
                """);

        if (gid != null) {
            sql.append("""
                    JOIN invoices i ON payments.iv_id = i.iv_id
                    JOIN jobs j ON i.jid = j.jid
                    JOIN service_requests sr ON j.sr_id = sr.sr_id
                    WHERE sr.gid = ? AND payments.status = 'PAID'
                    """);
        } else {
            sql.append(" WHERE payments.status = 'PAID'");
        }

        List<Object> params = new ArrayList<>();
        if (gid != null) params.add(gid);

        if (from != null && !from.isBlank()) { sql.append(" AND payment_date >= ?"); params.add(from); }
        if (to   != null && !to.isBlank())   { sql.append(" AND payment_date <= ?"); params.add(to); }

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "total_count",  rs.getInt("total_count"),
                            "total_amount", rs.getBigDecimal("total_amount")
                    );
                }
            }
        }

        return Map.of("total_count", 0, "total_amount", 0);
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getLong("payment_id"));
        p.setIvId(rs.getLong("iv_id"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setPaymentReference(rs.getString("payment_reference"));
        p.setStatus(rs.getString("status"));
        Timestamp dt = rs.getTimestamp("payment_date");
        if (dt != null) p.setPaymentDate(dt.toLocalDateTime());
        return p;
    }
}
