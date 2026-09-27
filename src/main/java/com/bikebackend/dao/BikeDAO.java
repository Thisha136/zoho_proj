package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Bike;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BikeDAO {
    private static final String COLUMNS = "bike_id, uid, variant_id, manufacturing_year, purchase_date, registration_no, color, image_url, created_at, updated_at";

    public long createBike(Bike bike) throws SQLException {
        String sql = """
                INSERT INTO bikes
                (uid, variant_id, manufacturing_year, purchase_date, registration_no, color, image_url)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setLong(1, bike.getUid());
            st.setLong(2, bike.getVariantId());
            st.setObject(3, bike.getManufacturingYear());
            st.setObject(4, bike.getPurchaseDate());
            st.setString(5, bike.getRegistrationNo());
            st.setString(6, bike.getColor());
            st.setString(7, bike.getImageUrl());
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) { if (rs.next()) return rs.getLong(1); }
        }
        throw new SQLException("Bike ID was not generated");
    }

    public List<Bike> getBikesByUser(long uid) throws SQLException {
        return query("SELECT " + COLUMNS + " FROM bikes WHERE uid = ? ORDER BY created_at DESC", List.of(uid));
    }

    public Bike getBikeById(long bikeId) throws SQLException {
        List<Bike> bikes = query("SELECT " + COLUMNS + " FROM bikes WHERE bike_id = ?", List.of(bikeId));
        return bikes.isEmpty() ? null : bikes.get(0);
    }

    public List<Bike> getAllBikes(Long uid, Long variantId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM bikes WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (uid != null) { sql.append(" AND uid = ?"); params.add(uid); }
        if (variantId != null) { sql.append(" AND variant_id = ?"); params.add(variantId); }
        sql.append(" ORDER BY created_at DESC");
        return query(sql.toString(), params);
    }

    public boolean updateBike(Bike bike) throws SQLException {
        String sql = """
                UPDATE bikes
                SET variant_id = ?, manufacturing_year = ?, purchase_date = ?,
                    registration_no = ?, color = ?, image_url = ?
                WHERE bike_id = ? AND uid = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, bike.getVariantId());
            st.setObject(2, bike.getManufacturingYear());
            st.setObject(3, bike.getPurchaseDate());
            st.setString(4, bike.getRegistrationNo());
            st.setString(5, bike.getColor());
            st.setString(6, bike.getImageUrl());
            st.setLong(7, bike.getBikeId());
            st.setLong(8, bike.getUid());
            return st.executeUpdate() > 0;
        }
    }

    public boolean deleteBike(long bikeId, long uid) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement("DELETE FROM bikes WHERE bike_id = ? AND uid = ?")) {
            st.setLong(1, bikeId);
            st.setLong(2, uid);
            return st.executeUpdate() > 0;
        }
    }

    private List<Bike> query(String sql, List<?> params) throws SQLException {
        List<Bike> list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) list.add(mapBike(rs)); }
        }
        return list;
    }

    private Bike mapBike(ResultSet rs) throws SQLException {
        Bike b = new Bike();
        b.setBikeId(rs.getLong("bike_id"));
        b.setUid(rs.getLong("uid"));
        b.setVariantId(rs.getLong("variant_id"));
        Number year = (Number) rs.getObject("manufacturing_year");
        if (year != null) b.setManufacturingYear(year.intValue());
        Date pd = rs.getDate("purchase_date");
        if (pd != null) b.setPurchaseDate(pd.toLocalDate());
        b.setRegistrationNo(rs.getString("registration_no"));
        b.setColor(rs.getString("color"));
        b.setImageUrl(rs.getString("image_url"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) b.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) b.setUpdatedAt(ua.toLocalDateTime());
        return b;
    }
}
