package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.JobService_model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobServiceDAO {

    private static final String SELECT_ALL = """
            SELECT js_id, jid, sid, status, selection_type,
                   service_part_type, actual_start_time, actual_end_time
            FROM job_services
            """;

    public long addService(JobService_model js) throws SQLException {
        String sql = """
                INSERT INTO job_services (jid, sid, status, selection_type, service_part_type)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setLong(1, js.getJid());
            st.setLong(2, js.getSid());
            st.setString(3, js.getStatus());
            st.setString(4, js.getSelectionType());
            st.setString(5, js.getServicePartType());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Job service ID was not generated");
    }

    public List<JobService_model> getByJob(long jid) throws SQLException {
        return query(SELECT_ALL + " WHERE jid = ?", List.of(jid));
    }

    public JobService_model getById(long jsId) throws SQLException {
        List<JobService_model> list = query(SELECT_ALL + " WHERE js_id = ?", List.of(jsId));
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean updateStatus(long jsId, String status) throws SQLException {
        String sql = "UPDATE job_services SET status = ?, version = version + 1 WHERE js_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, status);
            st.setLong(2, jsId);
            return st.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(long jsId, String status, int expectedVersion) throws SQLException {
        String sql = "UPDATE job_services SET status = ?, version = version + 1 WHERE js_id = ? AND version = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, status); st.setLong(2, jsId); st.setInt(3, expectedVersion);
            return st.executeUpdate() > 0;
        }
    }

    public boolean updateField(long jsId, String field, Object value) throws SQLException {
        String safeField = switch (field) {
            case "actual_start_time" -> "actual_start_time";
            case "actual_end_time" -> "actual_end_time";
            default -> throw new IllegalArgumentException("Unknown job-service field: " + field);
        };
        String sql = "UPDATE job_services SET " + safeField + " = ? WHERE js_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setObject(1, value);
            st.setLong(2, jsId);
            return st.executeUpdate() > 0;
        }
    }

    public List<JobService_model> getAll(String status, Long sid) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ALL + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append(" AND status = ?"); params.add(status); }
        if (sid != null) { sql.append(" AND sid = ?"); params.add(sid); }
        return query(sql.toString(), params);
    }

    public int count(Long gid, Long sid, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM job_services js
                JOIN jobs j ON js.jid = j.jid
                JOIN service_requests sr ON j.sr_id = sr.sr_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (gid != null) { sql.append(" AND sr.gid = ?"); params.add(gid); }
        if (sid != null) { sql.append(" AND js.sid = ?"); params.add(sid); }
        if (status != null && !status.isBlank()) { sql.append(" AND js.status = ?"); params.add(status); }
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<JobService_model> query(String sql, List<Object> params) throws SQLException {
        List<JobService_model> list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapJs(rs));
            }
        }
        return list;
    }

    private JobService_model mapJs(ResultSet rs) throws SQLException {
        JobService_model js = new JobService_model();
        js.setJsId(rs.getLong("js_id"));
        js.setJid(rs.getLong("jid"));
        js.setSid(rs.getLong("sid"));
        js.setStatus(rs.getString("status"));
        js.setSelectionType(rs.getString("selection_type"));
        js.setServicePartType(rs.getString("service_part_type"));
        Timestamp ast = rs.getTimestamp("actual_start_time");
        if (ast != null) js.setActualStartTime(ast.toLocalDateTime());
        Timestamp aet = rs.getTimestamp("actual_end_time");
        if (aet != null) js.setActualEndTime(aet.toLocalDateTime());
        return js;
    }
}
