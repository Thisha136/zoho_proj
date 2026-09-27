package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Service;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDAO {
    public long createService(Service service) throws SQLException {
        String sql = "INSERT INTO services (service_name, specialization, service_type) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, service.getServiceName());
            st.setString(2, service.getSpecialization());
            st.setString(3, service.getServiceType());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) { if (rs.next()) return rs.getLong(1); }
        }
        throw new SQLException("Service ID was not generated");
    }
    public List<Service> getAllServices(String serviceType, String sortOrder) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT sid, service_name, specialization, service_type FROM services WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (serviceType != null && !serviceType.isBlank()) { sql.append(" AND service_type = ?"); params.add(serviceType); }
        sql.append(" ORDER BY sid ASC");
        return query(sql.toString(), params);
    }
    public Service getServiceById(long sid) throws SQLException { return one("SELECT sid, service_name, specialization, service_type FROM services WHERE sid = ?", List.of(sid)); }
    public List<Service> getServicesByGarage(long gid) throws SQLException { return query("SELECT s.sid, s.service_name, s.specialization, gs.duration, gs.price, s.service_type FROM services s JOIN garage_services gs ON s.sid = gs.sid WHERE gs.gid = ? ORDER BY s.service_name ASC", List.of(gid)); }
    public Service getServiceByGarage(long gid, long sid) throws SQLException { return one("SELECT s.sid, s.service_name, s.specialization, gs.duration, gs.price, s.service_type FROM services s JOIN garage_services gs ON s.sid = gs.sid WHERE gs.gid = ? AND s.sid = ?", List.of(gid, sid)); }
    public boolean updateService(Service service) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement st = c.prepareStatement("UPDATE services SET service_name = ?, specialization = ?, service_type = ? WHERE sid = ?")) {
            st.setString(1, service.getServiceName()); st.setString(2, service.getSpecialization()); st.setString(3, service.getServiceType()); st.setLong(4, service.getSid()); return st.executeUpdate() > 0;
        }
    }
    public boolean deleteService(long sid) throws SQLException { try (Connection c = DatabaseConnection.getConnection(); PreparedStatement st = c.prepareStatement("DELETE FROM services WHERE sid = ?")) { st.setLong(1, sid); return st.executeUpdate() > 0; } }
    private Service one(String sql, List<?> params) throws SQLException { List<Service> result = query(sql, params); return result.isEmpty() ? null : result.get(0); }
    private List<Service> query(String sql, List<?> params) throws SQLException {
        List<Service> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement st = c.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) result.add(map(rs)); }
        }
        return result;
    }
    private Service map(ResultSet rs) throws SQLException {
        Service s = new Service(); s.setSid(rs.getLong("sid")); s.setServiceName(rs.getString("service_name")); s.setSpecialization(rs.getString("specialization"));
        try { s.setDuration((Integer) rs.getObject("duration")); } catch (SQLException ignored) { }
        try { s.setPrice(rs.getBigDecimal("price")); } catch (SQLException ignored) { }
        s.setServiceType(rs.getString("service_type")); return s;
    }
}
