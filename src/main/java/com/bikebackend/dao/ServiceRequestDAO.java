package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.ServiceRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRequestDAO {
    private static final String SELECT_ALL_COLS = """
            SELECT sr_id, bike_id, title, issue, image_url, gid, status, request_date,
                   physical_review_date, vehicle_handling_option, preferred_delivery_date,
                   garage_proposed_delivery_date, customer_date_response
            FROM service_requests
            """;

    public long createServiceRequest(long uid, ServiceRequest sr) throws SQLException {
        String sql = """
                INSERT INTO service_requests
                (bike_id, title, issue, image_url, gid, status, preferred_delivery_date, vehicle_handling_option)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setLong(1, sr.getBikeId());
            st.setString(2, sr.getTitle());
            st.setString(3, sr.getIssue());
            st.setString(4, sr.getImageUrl());
            st.setLong(5, sr.getGid());
            st.setString(6, sr.getStatus());
            st.setObject(7, sr.getPreferredDeliveryDate());
            st.setString(8, sr.getVehicleHandlingOption());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Service request ID was not generated");
    }

    public ServiceRequest getById(long srId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(SELECT_ALL_COLS + " WHERE sr_id = ?")) {
            st.setLong(1, srId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapSR(rs);
            }
        }
        return null;
    }

    public ServiceRequest getByIdForUpdate(Connection connection, long srId) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(SELECT_ALL_COLS + " WHERE sr_id = ? FOR UPDATE")) {
            st.setLong(1, srId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapSR(rs);
            }
        }
        return null;
    }

    public boolean belongsToUser(long srId, long uid) throws SQLException {
        String sql = """
                SELECT 1 FROM service_requests sr
                JOIN bikes b ON sr.bike_id = b.bike_id
                WHERE sr.sr_id = ? AND b.uid = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, srId);
            st.setLong(2, uid);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    public List<ServiceRequest> getByUser(long uid, String status, Long bikeId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ALL_COLS + " JOIN bikes b ON service_requests.bike_id = b.bike_id WHERE b.uid = ?");
        List<Object> params = new ArrayList<>();
        params.add(uid);
        if (status != null && !status.isBlank()) { sql.append(" AND status = ?"); params.add(status); }
        if (bikeId != null) { sql.append(" AND bike_id = ?"); params.add(bikeId); }
        sql.append(" ORDER BY request_date DESC");
        return query(sql.toString(), params);
    }

    public List<ServiceRequest> getByGarage(long gid, String status, String from, String to) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ALL_COLS + " WHERE gid = ?");
        List<Object> params = new ArrayList<>();
        params.add(gid);
        if (status != null && !status.isBlank()) { sql.append(" AND status = ?"); params.add(status); }
        if (from != null && !from.isBlank()) { sql.append(" AND request_date >= ?"); params.add(from); }
        if (to != null && !to.isBlank()) { sql.append(" AND request_date <= ?"); params.add(to); }
        sql.append(" ORDER BY request_date DESC");
        return query(sql.toString(), params);
    }

    public List<ServiceRequest> getAll(String status, Long gid, Long bikeId, String from, String to, String sort) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ALL_COLS + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append(" AND status = ?"); params.add(status); }
        if (gid != null) { sql.append(" AND gid = ?"); params.add(gid); }
        if (bikeId != null) { sql.append(" AND bike_id = ?"); params.add(bikeId); }
        if (from != null && !from.isBlank()) { sql.append(" AND request_date >= ?"); params.add(from); }
        if (to != null && !to.isBlank()) { sql.append(" AND request_date <= ?"); params.add(to); }
        sql.append(" ORDER BY request_date DESC");
        return query(sql.toString(), params);
    }

    public boolean updateStatus(Connection connection, long srId, String status) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("UPDATE service_requests SET status = ?, version = version + 1 WHERE sr_id = ?")) {
            st.setString(1, status); st.setLong(2, srId); return st.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(Connection connection, long srId, String status, int expectedVersion) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("UPDATE service_requests SET status = ?, version = version + 1 WHERE sr_id = ? AND version = ?")) {
            st.setString(1, status); st.setLong(2, srId); st.setInt(3, expectedVersion); return st.executeUpdate() > 0;
        }
    }

    public int cancelActivePendingForGarageVariant(Connection connection, long gid, long variantId) throws SQLException {
        String sql = """
                UPDATE service_requests sr JOIN bikes b ON sr.bike_id = b.bike_id
                SET sr.status = 'CANCELLED', sr.version = sr.version + 1
                WHERE sr.gid = ? AND b.variant_id = ? AND sr.status = 'PENDING'
                """;
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, gid); st.setLong(2, variantId); return st.executeUpdate();
        }
    }

    public boolean updateFields(ServiceRequest sr) throws SQLException {
        String sql = """
                UPDATE service_requests
                SET title = ?, issue = ?, image_url = ?, vehicle_handling_option = ?, preferred_delivery_date = ?
                WHERE sr_id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, sr.getTitle()); st.setString(2, sr.getIssue()); st.setString(3, sr.getImageUrl());
            st.setString(4, sr.getVehicleHandlingOption()); st.setObject(5, sr.getPreferredDeliveryDate());
            st.setLong(6, sr.getSrId()); return st.executeUpdate() > 0;
        }
    }

    public boolean setPhysicalReviewDate(long srId, long gid, java.time.LocalDate date) throws SQLException {
        String sql = "UPDATE service_requests SET physical_review_date = ?, status = 'REVIEWED', version = version + 1 WHERE sr_id = ? AND gid = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement st = connection.prepareStatement(sql)) {
            st.setObject(1, date); st.setLong(2, srId); st.setLong(3, gid); return st.executeUpdate() > 0;
        }
    }

    public boolean setGarageProposedDate(long srId, long gid, java.time.LocalDate date) throws SQLException {
        String sql = "UPDATE service_requests SET garage_proposed_delivery_date = ?, status = 'DATE_PROPOSED', version = version + 1 WHERE sr_id = ? AND gid = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement st = connection.prepareStatement(sql)) {
            st.setObject(1, date); st.setLong(2, srId); st.setLong(3, gid); return st.executeUpdate() > 0;
        }
    }

    public boolean setCustomerDateResponse(long srId, long uid, String response) throws SQLException {
        String newStatus = "ACCEPTED".equals(response) ? "ACCEPTED" : "DATE_REJECTED";
        String sql = """
                UPDATE service_requests sr JOIN bikes b ON sr.bike_id = b.bike_id
                SET sr.customer_date_response = ?, sr.status = ?, sr.version = sr.version + 1
                WHERE sr.sr_id = ? AND b.uid = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, response); st.setString(2, newStatus); st.setLong(3, srId); st.setLong(4, uid); return st.executeUpdate() > 0;
        }
    }

    public int countByStatus(Long gid, Long uid, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM service_requests sr");
        List<Object> params = new ArrayList<>();
        if (uid != null) sql.append(" JOIN bikes b ON sr.bike_id = b.bike_id");
        sql.append(" WHERE 1=1");
        if (gid != null) { sql.append(" AND sr.gid = ?"); params.add(gid); }
        if (uid != null) { sql.append(" AND b.uid = ?"); params.add(uid); }
        if (status != null && !status.isBlank()) { sql.append(" AND sr.status = ?"); params.add(status); }
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement st = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        }
        return 0;
    }

    private List<ServiceRequest> query(String sql, List<?> params) throws SQLException {
        List<ServiceRequest> list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement st = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) list.add(mapSR(rs)); }
        }
        return list;
    }

    private ServiceRequest mapSR(ResultSet rs) throws SQLException {
        ServiceRequest sr = new ServiceRequest();
        sr.setSrId(rs.getLong("sr_id")); sr.setBikeId(rs.getLong("bike_id"));
        sr.setTitle(rs.getString("title")); sr.setIssue(rs.getString("issue"));
        sr.setImageUrl(rs.getString("image_url")); sr.setGid(rs.getLong("gid"));
        sr.setStatus(rs.getString("status"));
        Timestamp rd = rs.getTimestamp("request_date"); if (rd != null) sr.setRequestDate(rd.toLocalDateTime());
        Date prd = rs.getDate("physical_review_date"); if (prd != null) sr.setPhysicalReviewDate(prd.toLocalDate());
        sr.setVehicleHandlingOption(rs.getString("vehicle_handling_option"));
        Date pdd = rs.getDate("preferred_delivery_date"); if (pdd != null) sr.setPreferredDeliveryDate(pdd.toLocalDate());
        Date gpd = rs.getDate("garage_proposed_delivery_date"); if (gpd != null) sr.setGarageProposedDeliveryDate(gpd.toLocalDate());
        sr.setCustomerDateResponse(rs.getString("customer_date_response"));
        return sr;
    }
}
