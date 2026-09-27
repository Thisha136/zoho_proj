package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Brand;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BrandDAO {

    public long createBrand(Brand brand) throws SQLException {

        String sql = "INSERT INTO brands (brand_name) VALUES (?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            st.setString(1, brand.getBrandName());
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Brand ID was not generated");
    }

    public List<Brand> getAllBrands(String search) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT bid, brand_name FROM brands WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND brand_name LIKE ?");
            params.add("%" + search + "%");
        }

        sql.append(" ORDER BY brand_name ASC");

        List<Brand> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapBrand(rs));
            }
        }

        return list;
    }

    public Brand getBrandById(long bid) throws SQLException {

        String sql = "SELECT bid, brand_name FROM brands WHERE bid = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, bid);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapBrand(rs);
            }
        }

        return null;
    }

    public boolean updateBrand(Brand brand) throws SQLException {

        String sql = "UPDATE brands SET brand_name = ? WHERE bid = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setString(1, brand.getBrandName());
            st.setLong(2, brand.getBid());
            return st.executeUpdate() > 0;
        }
    }

    public boolean deleteBrand(long bid) throws SQLException {

        String sql = "DELETE FROM brands WHERE bid = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, bid);
            return st.executeUpdate() > 0;
        }
    }

    private Brand mapBrand(ResultSet rs) throws SQLException {
        Brand b = new Brand();
        b.setBid(rs.getLong("bid"));
        b.setBrandName(rs.getString("brand_name"));
        return b;
    }
}
