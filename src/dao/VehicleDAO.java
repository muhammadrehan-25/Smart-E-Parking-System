package dao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/** Data Access Object for vehicle operations. */
public class VehicleDAO {
    private final Connection conn;

    public VehicleDAO() { this.conn = DatabaseManager.getInstance().getConnection(); }

    public boolean save(int customerId, String plate, String type, String model, String color) {
        String sql = "INSERT OR IGNORE INTO vehicles (customer_id,license_plate,vehicle_type,model,color) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, plate.toUpperCase());
            ps.setString(3, type);
            ps.setString(4, model);
            ps.setString(5, color);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Save vehicle error: " + e.getMessage());
            return false;
        }
    }
    public int getIdByPlate(String plate) {
        String sql = "SELECT vehicle_id FROM vehicles WHERE license_plate=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plate.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("GetIdByPlate error: " + e.getMessage());
        }
        return -1;
    }

    public String getTypeByPlate(String plate) {
        String sql = "SELECT vehicle_type FROM vehicles WHERE license_plate=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plate.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            System.err.println("GetTypeByPlate error: " + e.getMessage());
        }
        return "car";
    }

    public List<Object[]> getByCustomer(int customerId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT license_plate, vehicle_type, model, color FROM vehicles WHERE customer_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getString("license_plate"),
                        rs.getString("vehicle_type"),
                        rs.getString("model"),
                        rs.getString("color")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("GetByCustomer error: " + e.getMessage());
        }
        return list;
    }

    public boolean plateExists(String plate) {
        String sql = "SELECT 1 FROM vehicles WHERE license_plate=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plate.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("PlateExists error: " + e.getMessage());
            return false;
        }
    }
}
