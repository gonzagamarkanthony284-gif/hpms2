package Util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Minimal DB helper using plain JDBC DriverManager for development.
 * Defaults to an embedded H2 file database if no config provided.
 *
 * Notes:
 * - Add the H2 JDBC jar (or your chosen DB driver) to the project's classpath in Eclipse.
 * - The helper will attempt to execute `sql/schema.sql` once when `initDatabase()` is called.
 */
public class DB {
    private static final Properties cfg = new Properties();
    private static final String CONFIG_PATH = "config/db.properties"; // relative to project root
    private static final String DEFAULT_DRIVER = "org.h2.Driver";
    private static final String DEFAULT_URL = "jdbc:h2:file:./data/hpms;AUTO_SERVER=TRUE";
    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_PASS = "";

    static {
        // load properties if available, otherwise use defaults
        try (InputStream is = new FileInputStream(CONFIG_PATH)) {
            cfg.load(is);
        } catch (Exception e) {
            // no config found - proceed with defaults
        }
        // ensure driver is loaded when possible (silently ignore if missing)
        try {
            String driver = cfg.getProperty("db.driver", DEFAULT_DRIVER);
            Class.forName(driver);
        } catch (Throwable ignored) {
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = cfg.getProperty("db.url", DEFAULT_URL);
        String user = cfg.getProperty("db.user", DEFAULT_USER);
        String pass = cfg.getProperty("db.password", DEFAULT_PASS);
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Initialize the database by executing sql/schema.sql (if present) only when db.init=true.
     */
    public static void initDatabase() {
        String initFlag = cfg.getProperty("db.init", "false");
        if (!"true".equalsIgnoreCase(initFlag)) {
            // initialization disabled by config
            return;
        }

        Path schema = Paths.get("sql", "schema.sql");
        if (!Files.exists(schema)) return;
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = Files.newBufferedReader(schema, StandardCharsets.UTF_8)) {
                String line;
                while ((line = r.readLine()) != null) {
                    // skip SQL comments starting with --
                    if (line.trim().startsWith("--")) continue;
                    sb.append(line).append('\n');
                }
            }
            // naive split on semicolon - suitable for simple DDL scripts
            String[] parts = sb.toString().split(";\\s*\\n");
            for (String p : parts) {
                String sql = p.trim();
                if (sql.isEmpty()) continue;
                try {
                    st.execute(sql);
                } catch (SQLException ex) {
                    // log and continue - many scripts include IF NOT EXISTS so failures may be harmless
                    System.err.println("[DB] Failed to execute SQL statement: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            System.err.println("[DB] initDatabase error: " + ex.getMessage());
        }
    }

    /**
     * Quick check that connection can be obtained and a simple query runs.
     */
    public static boolean smokeTest() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT 1")) {
                if (rs.next()) return true;
            }
        } catch (SQLException ex) {
            System.err.println("[DB] smokeTest failed: " + ex.getMessage());
        }
        return false;
    }
}