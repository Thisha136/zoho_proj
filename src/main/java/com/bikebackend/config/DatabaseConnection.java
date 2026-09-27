package com.bikebackend.config;

import com.mysql.cj.jdbc.Driver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/VRM";

    private static final String USER =
            "root";

    private static final String PASSWORD =
            "Abcdef@T";

    public static Connection getConnection() throws SQLException {

        Driver driver = new Driver();

        Properties properties = new Properties();
        properties.setProperty("user", USER);
        properties.setProperty("password", PASSWORD);

        Connection connection =
                driver.connect(URL, properties);

        if (connection == null) {
            throw new SQLException("MySQL driver rejected the JDBC URL");
        }

        return connection;
    }
}