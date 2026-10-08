package hr;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.annotation.WebListener;

@WebListener
public final class Database implements ServletContextListener {
    static String setting(String name, String fallback) {
        return System.getProperty(name, System.getenv().getOrDefault(name, fallback));
    }
    public static Connection open() throws SQLException {
        return DriverManager.getConnection(setting("DB_URL", "jdbc:h2:file:./data/hr;MODE=MySQL;DATABASE_TO_LOWER=TRUE"),
                setting("DB_USER", "sa"), setting("DB_PASSWORD", ""));
    }
    @Override public void contextInitialized(ServletContextEvent event) {
        try {
            Class.forName(setting("DB_URL", "jdbc:h2:").startsWith("jdbc:mysql:") ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver");
            try (Connection connection = open(); Statement statement = connection.createStatement();
                 var stream = Database.class.getResourceAsStream("/schema.sql")) {
                if (stream == null) throw new IllegalStateException("Missing schema.sql");
                for (String sql : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
                    if (!sql.isBlank()) statement.execute(sql);
                }
                if (setting("APP_DEMO", "true").equalsIgnoreCase("true")) seed(connection);
                else {
                    String email = setting("ADMIN_EMAIL", "");
                    String password = setting("ADMIN_PASSWORD", "");
                    try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM employee")) {
                        rs.next();
                        if (rs.getInt(1) == 0) {
                            if (email.isBlank() || password.length() < 12)
                                throw new IllegalStateException("Set ADMIN_EMAIL and ADMIN_PASSWORD (12+ characters) for first startup");
                            add(connection, "Administrator", email, "ADMIN", password);
                        }
                    }
                }
            }
        } catch (Exception ex) { throw new IllegalStateException("Database initialization failed", ex); }
    }
    private static void seed(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM employee")) {
            rs.next(); if (rs.getInt(1) != 0) return;
        }
        connection.setAutoCommit(false);
        try {
            add(connection, "Demo Admin", "admin@example.test", "ADMIN", "DemoAdmin!2026");
            add(connection, "Demo Employee", "employee@example.test", "EMPLOYEE", "DemoEmployee!2026");
            connection.commit();
        } catch (SQLException ex) { connection.rollback(); throw ex; }
        finally { connection.setAutoCommit(true); }
    }
    private static void add(Connection connection, String name, String email, String role, String password) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO employee(name,email,phone,salary,position,role,password_hash) VALUES(?,?,'',2500,'General',?,?)")) {
            ps.setString(1,name); ps.setString(2,email); ps.setString(3,role); ps.setString(4,Passwords.hash(password)); ps.executeUpdate();
        }
    }
}
