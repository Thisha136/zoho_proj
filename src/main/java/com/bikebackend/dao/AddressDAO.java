package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.Address;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AddressDAO {

    // =========================
    // CREATE
    // =========================

    public long createAddress(Address address) throws SQLException {

        // If this address is marked default, clear existing defaults first
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefault(address.getUid());
        }

        String sql = """
                INSERT INTO addresses
                (uid, address_type, address_line, city, state, pincode, is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setLong(1, address.getUid());
            statement.setString(2, address.getAddressType());
            statement.setString(3, address.getAddressLine());
            statement.setString(4, address.getCity());
            statement.setString(5, address.getState());
            statement.setString(6, address.getPincode());
            statement.setBoolean(7, Boolean.TRUE.equals(address.getIsDefault()));

            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Address ID was not generated");
    }

    // =========================
    // GET ALL FOR USER
    // =========================

    public List<Address> getAddressesByUser(long uid) throws SQLException {

        String sql = """
                SELECT address_id, uid, address_type, address_line,
                       city, state, pincode, is_default, created_at, updated_at
                FROM addresses
                WHERE uid = ?
                ORDER BY is_default DESC, address_id ASC
                """;

        List<Address> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, uid);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) list.add(mapAddress(rs));
            }
        }

        return list;
    }

    // =========================
    // GET ONE
    // =========================

    public Address getAddressById(long addressId) throws SQLException {

        String sql = """
                SELECT address_id, uid, address_type, address_line,
                       city, state, pincode, is_default, created_at, updated_at
                FROM addresses
                WHERE address_id = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, addressId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return mapAddress(rs);
            }
        }

        return null;
    }

    // =========================
    // FULL UPDATE
    // =========================

    public boolean updateAddress(Address address) throws SQLException {

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefault(address.getUid());
        }

        String sql = """
                UPDATE addresses
                SET address_type = ?, address_line = ?, city = ?,
                    state = ?, pincode = ?, is_default = ?
                WHERE address_id = ? AND uid = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, address.getAddressType());
            statement.setString(2, address.getAddressLine());
            statement.setString(3, address.getCity());
            statement.setString(4, address.getState());
            statement.setString(5, address.getPincode());
            statement.setBoolean(6, Boolean.TRUE.equals(address.getIsDefault()));
            statement.setLong(7, address.getAddressId());
            statement.setLong(8, address.getUid());

            return statement.executeUpdate() > 0;
        }
    }

    // =========================
    // SET DEFAULT
    // =========================

    public boolean setDefault(long uid, long addressId) throws SQLException {

        // Use a transaction: clear old default then set new one atomically
        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Step 1: clear all defaults for this user
                String clearSql = "UPDATE addresses SET is_default = false WHERE uid = ?";
                try (PreparedStatement st = connection.prepareStatement(clearSql)) {
                    st.setLong(1, uid);
                    st.executeUpdate();
                }

                // Step 2: set the new default
                String setSql = "UPDATE addresses SET is_default = true WHERE address_id = ? AND uid = ?";
                try (PreparedStatement st = connection.prepareStatement(setSql)) {
                    st.setLong(1, addressId);
                    st.setLong(2, uid);
                    int rows = st.executeUpdate();

                    if (rows == 0) {
                        connection.rollback();
                        return false;
                    }
                }

                connection.commit();
                return true;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // =========================
    // DELETE
    // =========================

    public boolean deleteAddress(long addressId, long uid) throws SQLException {

        String sql = "DELETE FROM addresses WHERE address_id = ? AND uid = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, addressId);
            statement.setLong(2, uid);
            return statement.executeUpdate() > 0;
        }
    }

    // =========================
    // HELPER — clear defaults
    // =========================

    private void clearDefault(long uid) throws SQLException {

        String sql = "UPDATE addresses SET is_default = false WHERE uid = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, uid);
            statement.executeUpdate();
        }
    }

    // =========================
    // MAP ROW → Address
    // =========================

    private Address mapAddress(ResultSet rs) throws SQLException {

        Address a = new Address();

        a.setAddressId(rs.getLong("address_id"));
        a.setUid(rs.getLong("uid"));
        a.setAddressType(rs.getString("address_type"));
        a.setAddressLine(rs.getString("address_line"));
        a.setCity(rs.getString("city"));
        a.setState(rs.getString("state"));
        a.setPincode(rs.getString("pincode"));
        a.setIsDefault(rs.getBoolean("is_default"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) a.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) a.setUpdatedAt(updatedAt.toLocalDateTime());

        return a;
    }
}
