package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Feedback;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeedbackDAO {

    private static final String SELECT_ALL =
            "SELECT feedback_id, jid, rating, comments, created_at FROM feedback";

    public long createFeedback(Feedback fb) throws SQLException {
        String sql = "INSERT INTO feedback (jid, rating, comments) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setLong(1, fb.getJid());
            st.setInt(2, fb.getRating());
            st.setString(3, fb.getComments());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Feedback ID was not generated");
    }

    public Feedback getByJob(long jid) throws SQLException {
        return first(SELECT_ALL + " WHERE jid = ?", List.of(jid));
    }

    public Feedback getById(long feedbackId) throws SQLException {
        return first(SELECT_ALL + " WHERE feedback_id = ?", List.of(feedbackId));
    }

    public List<Feedback> getAll(Long gid, Integer rating) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT f.feedback_id, f.jid, f.rating, f.comments, f.created_at
                FROM feedback f
                JOIN jobs j ON j.jid = f.jid
                JOIN service_requests sr ON sr.sr_id = j.sr_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (gid != null) { sql.append(" AND sr.gid = ?"); params.add(gid); }
        if (rating != null) { sql.append(" AND f.rating = ?"); params.add(rating); }
        sql.append(" ORDER BY f.created_at DESC");
        return query(sql.toString(), params);
    }

    public List<Feedback> getByGarage(long gid) throws SQLException {
        return getAll(gid == 0 ? null : gid, null);
    }

    public boolean updateFeedback(long feedbackId, Integer rating, String comments) throws SQLException {
        String sql = "UPDATE feedback SET rating = ?, comments = ? WHERE feedback_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, rating);
            st.setString(2, comments);
            st.setLong(3, feedbackId);
            return st.executeUpdate() > 0;
        }
    }

    public Map<String, Object> getSummary(long gid) throws SQLException {
        String sql = gid == 0 ? "SELECT COUNT(*) AS total, COALESCE(AVG(rating), 0) AS avg_rating FROM feedback" : """
                SELECT COUNT(*) AS total, COALESCE(AVG(f.rating), 0) AS avg_rating
                FROM feedback f JOIN jobs j ON j.jid = f.jid
                JOIN service_requests sr ON sr.sr_id = j.sr_id WHERE sr.gid = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            if (gid != 0) st.setLong(1, gid);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return Map.of("total", rs.getInt("total"), "avg_rating", rs.getDouble("avg_rating"));
            }
        }
        return Map.of("total", 0, "avg_rating", 0.0);
    }

    private Feedback first(String sql, List<Object> params) throws SQLException {
        List<Feedback> list = query(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Feedback> query(String sql, List<Object> params) throws SQLException {
        List<Feedback> list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapFeedback(rs));
            }
        }
        return list;
    }

    private Feedback mapFeedback(ResultSet rs) throws SQLException {
        Feedback fb = new Feedback();
        fb.setFeedbackId(rs.getLong("feedback_id"));
        fb.setJid(rs.getLong("jid"));
        fb.setRating(rs.getInt("rating"));
        fb.setComments(rs.getString("comments"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) fb.setCreatedAt(ca.toLocalDateTime());
        return fb;
    }
}
