package dao;

import model.*;
import java.sql.*;
import java.security.MessageDigest;

/** Data Access Object for User-related DB operations. */
public class UserDAO {
    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    /** SHA-256 hash utility */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }

    /** Authenticate user: returns User object or null */
    public User authenticate(String username, String password) {
        String hashed = hash(password);
        String sql = "SELECT * FROM users WHERE username=? AND password_hash=? AND active=1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Auth error: " + e.getMessage());
        }
        return null;
    }

    /** Get user by ID */
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Find user error: " + e.getMessage());
        }
        return null;
    }

    /** Save new user */
    public boolean save(String username, String password, String fullName,
            String email, String phone, String role, String shift) {
        String sql = "INSERT INTO users (username,password_hash,full_name,email,phone,role,shift) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.setString(6, role);
            ps.setString(7, shift);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Save user error: " + e.getMessage());
            return false;
        }
    }

    public java.util.List<User> getAllUsers() {
        java.util.List<User> list = new java.util.ArrayList<>();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM users")) {
            while (rs.next())
                list.add(mapUser(rs));
        } catch (SQLException e) {
            System.err.println("GetAllUsers error: " + e.getMessage());
        }
        return list;
    }

    public boolean updateUser(int userId, String fullName, String email, String role, String shift) {
        String sql = "UPDATE users SET full_name=?, email=?, role=?, shift=? WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, role);
            ps.setString(4, shift);
            ps.setInt(5, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("UpdateUser error: " + e.getMessage());
            return false;
        }
    }

    public boolean deactivateUser(int userId) {
        String sql = "UPDATE users SET active=0 WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("DeactivateUser error: " + e.getMessage());
            return false;
        }
    }

    public boolean isUserActive(int userId) {
        String sql = "SELECT active FROM users WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("active") == 1;
            }
        } catch (SQLException e) {
            System.err.println("IsUserActive error: " + e.getMessage());
        }
        return false;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String uname = rs.getString("username");
        String hash = rs.getString("password_hash");
        String name = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String role = rs.getString("role");
        String shift = rs.getString("shift");

        User u = switch (role) {
            case "ADMIN" -> new Admin(id, uname, hash, name, email, phone);
            case "EMPLOYEE" -> new Employee(id, uname, hash, name, email, phone,
                    shift != null ? shift : "morning");
            default -> new Customer(id, uname, hash, name, email, phone);
        };
        u.setActive(rs.getInt("active") == 1);
        return u;
    }

    public String getPhone(int userId) {
        String sql = "SELECT phone FROM users WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("phone");
            }
        } catch (SQLException e) {
            System.err.println("GetPhone error: " + e.getMessage());
        }
        return "";
    }
    // ── UserDAO mein add karo ──────────────────────────────────────

    // User ko activate karna
    // User ko activate karna
    public boolean activateUser(int userId) {
        String sql = "UPDATE users SET active = 1 WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ActivateUser error: " + e.getMessage());
            return false;
        }
    }

    // User ko permanently delete karna
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("DeleteUser error: " + e.getMessage());
            return false;
        }
    }

    /** Reset password to a new value */
    public boolean resetPassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password_hash=? WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ResetPassword error: " + e.getMessage());
            return false;
        }
    }

    /** Change user role */
    public boolean changeRole(int userId, String newRole) {
        String sql = "UPDATE users SET role=? WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ChangeRole error: " + e.getMessage());
            return false;
        }
    }

}