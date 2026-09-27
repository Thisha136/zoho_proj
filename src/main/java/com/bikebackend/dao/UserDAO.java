package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // =========================
    // CREATE USER
    // =========================

    public long createUser(User user) throws SQLException {

        String sql = """
                INSERT INTO users
                (username, email, password, ph_no, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getPhNo());
            statement.setString(5, user.getStatus());

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {

                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }

        throw new SQLException("User ID was not generated");
    }


    // =========================
    // GET USER BY ID
    // =========================

    public User getUserById(long uid) throws SQLException {

        String sql = """
                SELECT uid, username, email, password,
                       ph_no, status, created_at, updated_at, version
                FROM users
                WHERE uid = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, uid);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }

        return null;
    }


    // =========================
    // UPDATE USER
    // =========================

    public boolean updateUser(User user) throws SQLException {

        String sql = """
                UPDATE users
                SET username = ?, email = ?, ph_no = ?
                WHERE uid = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhNo());
            statement.setLong(4, user.getUid());

            return statement.executeUpdate() > 0;
        }
    }


    // =========================
    // CHANGE PASSWORD
    // =========================

    public boolean updatePassword(long uid, String password)
            throws SQLException {

        String sql = """
                UPDATE users
                SET password = ?
                WHERE uid = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, password);
            statement.setLong(2, uid);

            return statement.executeUpdate() > 0;
        }
    }


    // =========================
    // UPDATE STATUS
    // =========================

    public boolean updateStatus(long uid, String status)
            throws SQLException {

        String sql = """
                UPDATE users
                SET status = ?, version = version + 1
                WHERE uid = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, status);
            statement.setLong(2, uid);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(long uid, String status, int expectedVersion) throws SQLException {
        String sql = "UPDATE users SET status = ?, version = version + 1 WHERE uid = ? AND version = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status); statement.setLong(2, uid); statement.setInt(3, expectedVersion);
            return statement.executeUpdate() > 0;
        }
    }


    // =========================
    // GET ALL USERS
    // =========================

    public List<User> getAllUsers(
            String search,
            String status,
            String sortField,
            String order
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT uid, username, email, password,
                       ph_no, status, created_at, updated_at, version
                FROM users
                WHERE 1 = 1
                """);

        List<Object> parameters = new ArrayList<>();

        if (search != null && !search.isBlank()) {

            sql.append("""
                    AND (
                        username LIKE ?
                        OR email LIKE ?
                        OR ph_no LIKE ?
                    )
                    """);

            String value = "%" + search + "%";

            parameters.add(value);
            parameters.add(value);
            parameters.add(value);
        }

        if (status != null && !status.isBlank()) {

            sql.append(" AND status = ? ");

            parameters.add(status);
        }

        /*
         * IMPORTANT:
         * Column names cannot safely be supplied using ?.
         * Therefore, only allow known column names.
         */

        String safeSortField =
                switch (sortField == null ? "" : sortField) {
                    case "username"   -> "username";
                    case "email"      -> "email";
                    case "status"     -> "status";
                    case "created_at" -> "created_at";
                    case "updated_at" -> "updated_at";
                    default           -> "uid";
                };

        String safeOrder =
                "asc".equalsIgnoreCase(order)
                        ? "ASC"
                        : "DESC";

        sql.append(" ORDER BY ")
                .append(safeSortField)
                .append(" ")
                .append(safeOrder);

        List<User> users = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql.toString())
        ) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }
        }

        return users;
    }


    // =========================
    // MAP RESULTSET → USER
    // =========================

    private User mapUser(ResultSet resultSet)
            throws SQLException {

        User user = new User();

        user.setUid(
                resultSet.getLong("uid")
        );

        user.setUsername(
                resultSet.getString("username")
        );

        user.setEmail(
                resultSet.getString("email")
        );

        user.setPassword(
                resultSet.getString("password")
        );

        user.setPhNo(
                resultSet.getString("ph_no")
        );

        user.setStatus(
                resultSet.getString("status")
        );

        Timestamp createdAt =
                resultSet.getTimestamp("created_at");

        if (createdAt != null) {
            user.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        Timestamp updatedAt =
                resultSet.getTimestamp("updated_at");

        if (updatedAt != null) {
            user.setUpdatedAt(
                    updatedAt.toLocalDateTime()
            );
        }

        user.setVersion(
                resultSet.getInt("version")
        );

        return user;
    }
}
