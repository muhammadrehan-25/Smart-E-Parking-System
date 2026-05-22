package dao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class LogDAO {
    private final Connection conn;
    public LogDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS system_logs (" +
                     "log_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "user_id INTEGER, " +
                     "username TEXT, " +
                     "action TEXT, " +
                     "details TEXT, " +
                     "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating logs table: " + e.getMessage());
        }
    }

    public void log(int userId, String username, String action, String details) {
        String sql = "INSERT INTO system_logs (user_id, username, action, details) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Log error: " + e.getMessage());
        }
    }

    public List<String[]> getAllLogs() {
        List<String[]> logs = new ArrayList<>();
        String sql = "SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 100";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new String[]{
                    rs.getString("timestamp"),
                    rs.getString("username"),
                    rs.getString("action"),
                    rs.getString("details")
                });
            }
        } catch (SQLException e) {
            System.err.println("Fetch logs error: " + e.getMessage());
        }
        return logs;
    }
}
