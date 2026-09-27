package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Variant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VariantDAO {

    public long createVariant(Variant variant) throws SQLException {

        String sql = "INSERT INTO variants (model_id, variant_name) VALUES (?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            st.setLong(1, variant.getModelId());
            st.setString(2, variant.getVariantName());
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Variant ID was not generated");
    }

    public List<Variant> getAllVariants(Long modelId) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT variant_id, model_id, variant_name FROM variants WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (modelId != null) {
            sql.append(" AND model_id = ?");
            params.add(modelId);
        }

        sql.append(" ORDER BY variant_name ASC");

        List<Variant> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapVariant(rs));
            }
        }

        return list;
    }

    public Variant getVariantById(long variantId) throws SQLException {

        String sql = "SELECT variant_id, model_id, variant_name FROM variants WHERE variant_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, variantId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapVariant(rs);
            }
        }

        return null;
    }

    public boolean updateVariant(Variant variant) throws SQLException {

        String sql = "UPDATE variants SET model_id = ?, variant_name = ? WHERE variant_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, variant.getModelId());
            st.setString(2, variant.getVariantName());
            st.setLong(3, variant.getVariantId());
            return st.executeUpdate() > 0;
        }
    }

    public boolean deleteVariant(long variantId) throws SQLException {

        String sql = "DELETE FROM variants WHERE variant_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, variantId);
            return st.executeUpdate() > 0;
        }
    }

    private Variant mapVariant(ResultSet rs) throws SQLException {
        Variant v = new Variant();
        v.setVariantId(rs.getLong("variant_id"));
        v.setModelId(rs.getLong("model_id"));
        v.setVariantName(rs.getString("variant_name"));
        return v;
    }
}
