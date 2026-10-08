package hr;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** JDBC access with short-lived, independently closed connections. */
public final class Repository {
    public List<Map<String,Object>> query(String sql, Object... values) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String,Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1;i<=rs.getMetaData().getColumnCount();i++)
                        row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT), rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
    public Map<String,Object> one(String sql, Object... values) throws SQLException {
        var rows = query(sql,values); return rows.isEmpty() ? null : rows.get(0);
    }
    public int update(String sql, Object... values) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps,values); return ps.executeUpdate();
        }
    }
    static void bind(PreparedStatement ps, Object... values) throws SQLException {
        for (int i=0;i<values.length;i++) ps.setObject(i+1,values[i]);
    }
    /** A rotation regenerates one week atomically; unrelated weeks are preserved. */
    public void generateWeek(LocalDate monday, int offset) throws SQLException {
        String[] tasks = {"Reception","Support","Operations","Training","Administration"};
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement employees = c.prepareStatement("SELECT id FROM employee WHERE role='EMPLOYEE' ORDER BY id");
                 PreparedStatement clear = c.prepareStatement("DELETE FROM schedule_slot WHERE work_date BETWEEN ? AND ?");
                 PreparedStatement insert = c.prepareStatement("INSERT INTO schedule_slot(employee_id,work_date,task) VALUES(?,?,?)")) {
                bind(clear,monday,monday.plusDays(4)); clear.executeUpdate();
                try (ResultSet rs=employees.executeQuery()) {
                    int employeeIndex=0;
                    while (rs.next()) {
                        for(int day=0;day<5;day++) {
                            bind(insert,rs.getInt(1),monday.plusDays(day),tasks[Math.floorMod(employeeIndex+day+offset,tasks.length)]);
                            insert.addBatch();
                        }
                        employeeIndex++;
                    }
                }
                insert.executeBatch(); c.commit();
            } catch (SQLException ex) { c.rollback(); throw ex; }
        }
    }
}
