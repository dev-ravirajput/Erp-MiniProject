package com.erp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Database settings may be overridden with JVM system properties. */
public final class DBConnection {
    private static final String URL = System.getProperty("erp.db.url", "jdbc:mysql://localhost:3306/erp_db?useSSL=false&serverTimezone=UTC");
    private static final String USER = System.getProperty("erp.db.user", "root");
    private static final String PASSWORD = System.getProperty("erp.db.password", "root");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MySQL Connector/J is missing from WEB-INF/lib.", exception);
        }
    }

    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
