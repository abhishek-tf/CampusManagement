package com.campus.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Factory for MySQL JDBC connections.
 *
 * <p>Connection settings are read once from {@code db.properties} on the
 * classpath. Each call to {@link #getConnection()} returns a fresh connection
 * so the caller (the service layer) owns its transaction boundary and is
 * responsible for committing/rolling back and closing it.</p>
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private static final String url;
    private static final String user;
    private static final String password;

    private DBConnection() {
        // Utility class - no instances.
    }

    static {
        Properties properties = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Unable to find " + PROPERTIES_FILE + " on the classpath");
            }
            properties.load(input);

            // Loading the driver class is optional on modern JDBC, but we honour
            // the configured driver so the setup is explicit and self-documenting.
            String driver = properties.getProperty("db.driver");
            if (driver != null && !driver.isBlank()) {
                Class.forName(driver);
            }

            url = properties.getProperty("db.url");
            user = properties.getProperty("db.user");
            password = properties.getProperty("db.password", "");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "Failed to initialise database configuration: " + e.getMessage());
        }
    }

    /**
     * Opens a new connection to the configured MySQL database.
     *
     * @return an open {@link Connection} the caller must close
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
