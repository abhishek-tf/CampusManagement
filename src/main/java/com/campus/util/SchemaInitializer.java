package com.campus.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the database schema (and optional sample data) on startup, so the app
 * is fully runnable from the JAR alone — the grader just runs {@code java -jar}.
 *
 * <p>The {@code schema.sql} and {@code seed.sql} scripts are bundled inside the
 * JAR (from {@code src/main/resources}).</p>
 *
 * <p><b>Idempotent:</b> if the schema is already present it does nothing, so data
 * survives restarts. Control via system properties:</p>
 * <ul>
 *   <li>{@code -Ddb.seed=false} — skip loading sample data on a fresh install
 *       (default is to seed so reports/expenses have data to show);</li>
 *   <li>{@code -Ddb.reset=true} — force a clean re-create (drops &amp; recreates).</li>
 * </ul>
 */
public final class SchemaInitializer {

    private SchemaInitializer() {
    }

    /** Ensures the schema exists; safe to call once at startup. Never throws. */
    public static void initialize() {
        boolean reset = Boolean.parseBoolean(System.getProperty("db.reset", "false"));
        boolean seed = Boolean.parseBoolean(System.getProperty("db.seed", "true"));
        try {
            if (!reset && schemaExists()) {
                Logger.info("Database schema already present; skipping initialization");
                return;
            }
            runScript("schema.sql");
            Logger.info("Database schema created from schema.sql");
            System.out.println("Database schema ready.");

            if (seed) {
                runScript("seed.sql");
                Logger.info("Sample data loaded from seed.sql");
                System.out.println("Sample data loaded.");
            }
        } catch (Exception e) {
            // Never crash the app over setup — report clearly and let the user fix config.
            Logger.error("Database initialization failed", e);
            System.out.println("WARNING: could not initialize the database: " + e.getMessage());
            System.out.println("Check db.properties (url/user/password) and that MySQL is running, then retry.");
        }
    }

    /** True if the database and its core table already exist. */
    private static boolean schemaExists() {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeQuery("SELECT 1 FROM student LIMIT 1");
            return true;
        } catch (SQLException e) {
            return false; // database or table missing → needs initialization
        }
    }

    /** Runs every statement in a bundled .sql script on a server-level connection. */
    private static void runScript(String resource) throws IOException, SQLException {
        List<String> statements = splitStatements(readResource(resource));
        // Server connection (no database selected) so CREATE DATABASE / USE work.
        try (Connection conn = DBConnection.getServerConnection();
             Statement st = conn.createStatement()) {
            for (String sql : statements) {
                st.execute(sql);
            }
        }
    }

    /** Reads a classpath resource, stripping {@code --} line/inline comments. */
    private static String readResource(String name) throws IOException {
        try (InputStream in = SchemaInitializer.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("Bundled SQL script not found on classpath: " + name);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int comment = line.indexOf("--");
                    if (comment >= 0) {
                        line = line.substring(0, comment);
                    }
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
    }

    /** Splits a script into individual statements on {@code ;} (none of our SQL embeds it). */
    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        for (String part : script.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements;
    }
}
