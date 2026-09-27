package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;

public class GarageServiceDAO {

    public boolean addService(long gid, long sid) throws SQLException {
        return addService(gid, sid, null, null);
    }

    public boolean addService(long gid, long sid, Integer duration, BigDecimal price) throws SQLException {
        String sql = "INSERT INTO garage_services (gid, sid, duration, price) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, gid);
            st.setLong(2, sid);
            st.setObject(3, duration);
            st.setBigDecimal(4, price);
            return st.executeUpdate() > 0;
        }
    }

    public boolean removeService(long gid, long sid) throws SQLException {
        String sql = "DELETE FROM garage_services WHERE gid = ? AND sid = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, gid);
            st.setLong(2, sid);
            return st.executeUpdate() > 0;
        }
    }

    public boolean exists(long gid, long sid) throws SQLException {
        String sql = "SELECT 1 FROM garage_services WHERE gid = ? AND sid = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, gid);
            st.setLong(2, sid);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
    }
}
