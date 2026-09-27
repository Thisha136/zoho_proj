package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Job;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobDAO {
    private static final String SELECT_ALL = """
            SELECT jobs.jid, jobs.sr_id, jobs.status, jobs.job_date, jobs.service_start_date, jobs.service_end_date,
                   jobs.expected_delivery_date, jobs.actual_delivery_date, jobs.pickup_status, jobs.delivery_status
            FROM jobs
            """;

    public long createJob(Job job) throws SQLException {
        String sql = """
                INSERT INTO jobs (sr_id, status, job_date, expected_delivery_date)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setLong(1, job.getSrId());
            st.setString(2, job.getStatus());
            st.setObject(3, job.getJobDate());
            st.setObject(4, job.getExpectedDeliveryDate());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Job ID was not generated");
    }

    public Job getById(long jid) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(SELECT_ALL + " WHERE jid = ?")) {
            st.setLong(1, jid);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapJob(rs);
            }
        }
        return null;
    }

    public Job getByIdForUser(long jid, long uid) throws SQLException {
        String sql = SELECT_ALL + """
                JOIN service_requests sr ON jobs.sr_id = sr.sr_id
                JOIN bikes b ON sr.bike_id = b.bike_id
                WHERE jobs.jid = ? AND b.uid = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, jid);
            st.setLong(2, uid);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapJob(rs);
            }
        }
        return null;
    }

    public List<Job> getByGarage(long gid, String status, String from, String to) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ALL + """
                JOIN service_requests sr ON jobs.sr_id = sr.sr_id
                WHERE sr.gid = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(gid);
        if (status != null && !status.isBlank()) { sql.append(" AND jobs.status = ?"); params.add(status); }
        if (from != null && !from.isBlank()) { sql.append(" AND job_date >= ?"); params.add(from); }
        if (to != null && !to.isBlank()) { sql.append(" AND job_date <= ?"); params.add(to); }
        sql.append(" ORDER BY job_date DESC");
        return query(sql.toString(), params);
    }

    public List<Job> getByUser(long uid) throws SQLException {
        String sql = SELECT_ALL + """
                JOIN service_requests sr ON jobs.sr_id = sr.sr_id
                JOIN bikes b ON sr.bike_id = b.bike_id
                WHERE b.uid = ?
                ORDER BY jobs.job_date DESC
                """;
        return query(sql, List.of(uid));
    }

    public List<Job> getAll(String status, Long gid, String from, String to) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ALL);
        List<Object> params = new ArrayList<>();
        if (gid != null) {
            sql.append(" JOIN service_requests sr ON jobs.sr_id = sr.sr_id WHERE sr.gid = ?");
            params.add(gid);
        } else {
            sql.append(" WHERE 1=1");
        }
        if (status != null && !status.isBlank()) { sql.append(" AND jobs.status = ?"); params.add(status); }
        if (from != null && !from.isBlank()) { sql.append(" AND job_date >= ?"); params.add(from); }
        if (to != null && !to.isBlank()) { sql.append(" AND job_date <= ?"); params.add(to); }
        sql.append(" ORDER BY job_date DESC");
        return query(sql.toString(), params);
    }

    public boolean updateStatus(long jid, String status) throws SQLException {
        String sql = "UPDATE jobs SET status = ?, version = version + 1 WHERE jid = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, status);
            st.setLong(2, jid);
            return st.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(long jid, String status, int expectedVersion) throws SQLException {
        String sql = "UPDATE jobs SET status = ?, version = version + 1 WHERE jid = ? AND version = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, status); st.setLong(2, jid); st.setInt(3, expectedVersion);
            return st.executeUpdate() > 0;
        }
    }

    public boolean updateField(long jid, String field, Object value) throws SQLException {
        String safeField = switch (field) {
            case "service_start" -> "service_start_date";
            case "service_end" -> "service_end_date";
            case "expected_delivery_date" -> "expected_delivery_date";
            case "actual_delivery_date" -> "actual_delivery_date";
            case "pickup_status" -> "pickup_status";
            case "delivery_status" -> "delivery_status";
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        };
        String sql = "UPDATE jobs SET " + safeField + " = ? WHERE jid = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setObject(1, value);
            st.setLong(2, jid);
            return st.executeUpdate() > 0;
        }
    }

    public int countByStatus(Long gid, Long uid, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM jobs j
                JOIN service_requests sr ON j.sr_id = sr.sr_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (gid != null) { sql.append(" AND sr.gid = ?"); params.add(gid); }
        if (uid != null) { sql.append(" AND EXISTS (SELECT 1 FROM bikes b WHERE b.bike_id = sr.bike_id AND b.uid = ?)"); params.add(uid); }
        if (status != null && !status.isBlank()) { sql.append(" AND j.status = ?"); params.add(status); }
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private List<Job> query(String sql, List<?> params) throws SQLException {
        List<Job> list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapJob(rs));
            }
        }
        return list;
    }

    private Job mapJob(ResultSet rs) throws SQLException {
        Job j = new Job();
        j.setJid(rs.getLong("jid"));
        j.setSrId(rs.getLong("sr_id"));
        j.setStatus(rs.getString("status"));
        Date jd = rs.getDate("job_date");
        if (jd != null) j.setJobDate(jd.toLocalDate());
        Timestamp ss = rs.getTimestamp("service_start_date");
        if (ss != null) j.setServiceStart(ss.toLocalDateTime());
        Timestamp se = rs.getTimestamp("service_end_date");
        if (se != null) j.setServiceEnd(se.toLocalDateTime());
        Date edd = rs.getDate("expected_delivery_date");
        if (edd != null) j.setExpectedDeliveryDate(edd.toLocalDate());
        Date add = rs.getDate("actual_delivery_date");
        if (add != null) j.setActualDeliveryDate(add.toLocalDate());
        j.setPickupStatus(rs.getString("pickup_status"));
        j.setDeliveryStatus(rs.getString("delivery_status"));
        return j;
    }
}
