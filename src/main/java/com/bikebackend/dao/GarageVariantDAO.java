package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.GarageVariant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GarageVariantDAO {

    public boolean addVariant(long gid, long variantId) throws SQLException {

        String sql = "INSERT INTO garage_variants (gid, variant_id) VALUES (?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, gid);
            st.setLong(2, variantId);
            return st.executeUpdate() > 0;
        }
    }

    public List<GarageVariant> getVariantsByGarage(long gid) throws SQLException {

        String sql = "SELECT gid, variant_id FROM garage_variants WHERE gid = ?";

        List<GarageVariant> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, gid);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    GarageVariant gv = new GarageVariant();
                    gv.setGid(rs.getLong("gid"));
                    gv.setVariantId(rs.getLong("variant_id"));
                    list.add(gv);
                }
            }
        }

        return list;
    }

    public boolean removeVariant(long gid, long variantId) throws SQLException {

        String sql = "DELETE FROM garage_variants WHERE gid = ? AND variant_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, gid);
            st.setLong(2, variantId);
            return st.executeUpdate() > 0;
        }
    }

    public boolean removeVariant(Connection connection, long gid, long variantId)
            throws SQLException {
        String sql = "DELETE FROM garage_variants WHERE gid = ? AND variant_id = ?";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, gid);
            st.setLong(2, variantId);
            return st.executeUpdate() > 0;
        }
    }

    public boolean exists(long gid, long variantId) throws SQLException {

        String sql = "SELECT 1 FROM garage_variants WHERE gid = ? AND variant_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, gid);
            st.setLong(2, variantId);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
    }
}
