package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.VehicleModel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleModelDAO {

    public long createModel(VehicleModel model) throws SQLException {

        String sql = "INSERT INTO models (bid, model_name) VALUES (?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            st.setLong(1, model.getBid());
            st.setString(2, model.getModelName());
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Model ID was not generated");
    }

    public List<VehicleModel> getAllModels(Long bid) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT model_id, bid, model_name FROM models WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (bid != null) {
            sql.append(" AND bid = ?");
            params.add(bid);
        }

        sql.append(" ORDER BY model_name ASC");

        List<VehicleModel> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapModel(rs));
            }
        }

        return list;
    }

    public VehicleModel getModelById(long modelId) throws SQLException {

        String sql = "SELECT model_id, bid, model_name FROM models WHERE model_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, modelId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapModel(rs);
            }
        }

        return null;
    }

    public boolean updateModel(VehicleModel model) throws SQLException {

        String sql = "UPDATE models SET bid = ?, model_name = ? WHERE model_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, model.getBid());
            st.setString(2, model.getModelName());
            st.setLong(3, model.getModelId());
            return st.executeUpdate() > 0;
        }
    }

    public boolean deleteModel(long modelId) throws SQLException {

        String sql = "DELETE FROM models WHERE model_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, modelId);
            return st.executeUpdate() > 0;
        }
    }

    private VehicleModel mapModel(ResultSet rs) throws SQLException {
        VehicleModel m = new VehicleModel();
        m.setModelId(rs.getLong("model_id"));
        m.setBid(rs.getLong("bid"));
        m.setModelName(rs.getString("model_name"));
        return m;
    }
}
