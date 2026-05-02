package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Data Access Object for parking slots. */
public class SlotDAO {
    private final Connection conn;

    public SlotDAO() { this.conn = DatabaseManager.getInstance().getConnection(); }

    public boolean updateStatus(int slotId, String status) {
        String sql = "UPDATE parking_slots SET status=? WHERE slot_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, slotId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UpdateStatus error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatusByNumber(String slotNumber, String status) {
        String sql = "UPDATE parking_slots SET status=? WHERE slot_number=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, slotNumber);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UpdateStatusByNumber error: " + e.getMessage());
            return false;
        }
    }

    /** Returns [slot_id, floor, slot_number, slot_type, status] rows */
    public List<Object[]> getAllSlots() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT slot_id, floor_number, slot_number, slot_type, status " +
                     "FROM parking_slots ORDER BY floor_number, slot_number";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("slot_id"),
                    rs.getInt("floor_number"),
                    rs.getString("slot_number"),
                    rs.getString("slot_type"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) { System.err.println("getAllSlots: " + e.getMessage()); }
        return list;
    }

    /** Find first free slot for given floor */
    public int[] findFreeSlot(int floor) {
        String sql = "SELECT slot_id, slot_number FROM parking_slots " +
                     "WHERE floor_number=? AND status='free' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, floor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new int[]{rs.getInt("slot_id")};
            }
        } catch (SQLException e) { System.err.println("findFreeSlot error: " + e.getMessage()); }
        return null;
    }

    public int getTotalFree() {
        String sql = "SELECT COUNT(*) FROM parking_slots WHERE status='free'";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("getTotalFree error: " + e.getMessage()); }
        return 0;
    }

    public int getTotal() {
        String sql = "SELECT COUNT(*) FROM parking_slots";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("getTotal error: " + e.getMessage()); }
        return 0;
    }
}
