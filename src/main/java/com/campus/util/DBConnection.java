package com.campus.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Factory for MySQL JDBC connections.
 *
 * <p>Configuration is resolved once, in this order (later wins) so the same JAR
 * runs unchanged on any machine:</p>
 * <ol>
 *   <li>bundled {@code db.properties} on the classpath (defaults shipped in the JAR);</li>
 *   <li>an external {@code db.properties} in the working directory (or the path in
 *       the {@code -Ddb.config} system property) — lets a judge supply their own DB;</li>
 *   <li>per-value overrides via system properties ({@code -Ddb.url=…}) or environment
 *       variables ({@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}).</li>
 * </ol>
 *
 * <p>Each {@link #getConnection()} returns a fresh connection so the caller (the
 * service layer, via {@code Tx}) owns its transaction boundary.</p>
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private static final String url;
    private static final String user;
    private static final String password;

    private DBConnection() {
    }

    static {
        Properties properties = loadProperties();

        String driver = resolve("db.driver", "DB_DRIVER", properties.getProperty("db.driver"));
        try {
            if (driver != null && !driver.isBlank()) {
                Class.forName(driver);
            }
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("JDBC driver not found: " + e.getMessage());
        }

        url = resolve("db.url", "DB_URL", properties.getProperty("db.url"));
        user = resolve("db.user", "DB_USER", properties.getProperty("db.user"));
        password = resolve("db.password", "DB_PASSWORD", properties.getProperty("db.password", ""));

        if (url == null || url.isBlank()) {
            throw new ExceptionInInitializerError(
                    "Database URL not configured. Set db.url in db.properties, or pass -Ddb.url=… / DB_URL.");
        }
    }

    /** Loads bundled defaults, then overlays an external db.properties if present. */
    private static Properties loadProperties() {
        Properties properties = new Properties();

        // 1. bundled defaults shipped inside the JAR (classpath)
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to read bundled db.properties: " + e.getMessage());
        }

        // 2. external file beside the JAR (or -Ddb.config=/path) overrides the defaults
        Path external = Paths.get(System.getProperty("db.config", PROPERTIES_FILE));
        if (Files.isRegularFile(external)) {
            try (InputStream input = Files.newInputStream(external)) {
                properties.load(input);
            } catch (IOException e) {
                throw new ExceptionInInitializerError("Failed to read " + external + ": " + e.getMessage());
            }
        }
        return properties;
    }

    /** System property wins over environment variable, which wins over the file value. */
    private static String resolve(String systemProperty, String envVar, String fileValue) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVar);
        }
        return (value == null || value.isBlank()) ? fileValue : value;
    }

    /**
     * Opens a new connection to the configured database.
     *
     * @return an open {@link Connection} the caller must close
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Opens a connection to the MySQL <em>server</em> with no database selected,
     * so a script can run {@code CREATE DATABASE}. Used only by schema setup.
     */
    public static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(serverUrl(), user, password);
    }

    /** Strips the {@code /databaseName} path from the JDBC URL, keeping the query string. */
    private static String serverUrl() {
        int queryAt = url.indexOf('?');
        String base = (queryAt < 0) ? url : url.substring(0, queryAt);
        String query = (queryAt < 0) ? "" : url.substring(queryAt);
        int lastSlash = base.lastIndexOf('/');
        String server = (lastSlash < 0) ? base : base.substring(0, lastSlash + 1);
        return server + query;
    }
}
