package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.GarageBay;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GarageBayDAO {

    public long createBay(GarageBay bay) throws SQLException {

        String sql = "INSERT INTO garage_bays (gid, bay_name) VALUES (?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            st.setLong(1, bay.getGid());
            st.setString(2, bay.getBayName());
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Bay ID was not generated");
    }

    public List<GarageBay> getByGarage(long gid) throws SQLException {

        String sql = "SELECT bay_id, gid, bay_name FROM garage_bays WHERE gid = ? ORDER BY bay_id ASC";

        List<GarageBay> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, gid);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapBay(rs));
            }
        }

        return list;
    }

    public GarageBay getById(long bayId) throws SQLException {

        String sql = "SELECT bay_id, gid, bay_name FROM garage_bays WHERE bay_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, bayId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapBay(rs);
            }
        }

        return null;
    }

    public boolean updateBay(GarageBay bay) throws SQLException {

        String sql = "UPDATE garage_bays SET bay_name = ? WHERE bay_id = ? AND gid = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setString(1, bay.getBayName());
            st.setLong(2, bay.getBayId());
            st.setLong(3, bay.getGid());
            return st.executeUpdate() > 0;
        }
    }

    public boolean deleteBay(long bayId, long gid) throws SQLException {

        String sql = "DELETE FROM garage_bays WHERE bay_id = ? AND gid = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, bayId);
            st.setLong(2, gid);
            return st.executeUpdate() > 0;
        }
    }

    private GarageBay mapBay(ResultSet rs) throws SQLException {
        GarageBay b = new GarageBay();
        b.setBayId(rs.getLong("bay_id"));
        b.setGid(rs.getLong("gid"));
        b.setBayName(rs.getString("bay_name"));
        return b;
    }
}
