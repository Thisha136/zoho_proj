package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Garage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GarageDAO {

    public long createGarage(Garage garage) throws SQLException {

        String sql = """
                INSERT INTO garages (garage_name, address_line, city, state, pincode)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            st.setString(1, garage.getGarageName());
            st.setString(2, garage.getAddressLine());
            st.setString(3, garage.getCity());
            st.setString(4, garage.getState());
            st.setString(5, garage.getPincode());
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Garage ID was not generated");
    }

    public List<Garage> getAllGarages(Long variantId) throws SQLException {

        StringBuilder sql = new StringBuilder();

        List<Object> params = new ArrayList<>();

        if (variantId != null) {
            // Find garages that support the given variant
            sql.append("""
                    SELECT g.gid, g.garage_name, g.address_line, g.city, g.state, g.pincode
                    FROM garages g
                    JOIN garage_variants gv ON g.gid = gv.gid
                    WHERE gv.variant_id = ?
                    ORDER BY g.garage_name ASC
                    """);
            params.add(variantId);
        } else {
            sql.append("""
                    SELECT gid, garage_name, address_line, city, state, pincode
                    FROM garages
                    ORDER BY garage_name ASC
                    """);
        }

        List<Garage> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < params.size(); i++) st.setObject(i + 1, params.get(i));
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapGarage(rs));
            }
        }

        return list;
    }

    public Garage getGarageById(long gid) throws SQLException {

        String sql = """
                SELECT gid, garage_name, address_line, city, state, pincode
                FROM garages WHERE gid = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, gid);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapGarage(rs);
            }
        }

        return null;
    }

    public boolean updateGarage(Garage garage) throws SQLException {

        String sql = """
                UPDATE garages
                SET garage_name = ?, address_line = ?, city = ?, state = ?, pincode = ?
                WHERE gid = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setString(1, garage.getGarageName());
            st.setString(2, garage.getAddressLine());
            st.setString(3, garage.getCity());
            st.setString(4, garage.getState());
            st.setString(5, garage.getPincode());
            st.setLong(6, garage.getGid());
            return st.executeUpdate() > 0;
        }
    }

    private Garage mapGarage(ResultSet rs) throws SQLException {
        Garage g = new Garage();
        g.setGid(rs.getLong("gid"));
        g.setGarageName(rs.getString("garage_name"));
        g.setAddressLine(rs.getString("address_line"));
        g.setCity(rs.getString("city"));
        g.setState(rs.getString("state"));
        g.setPincode(rs.getString("pincode"));
        return g;
    }
}
