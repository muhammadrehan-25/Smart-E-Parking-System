package dao;

import model.Booking;
import model.ParkingSlot;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {
    private final Connection conn;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BookingDAO() { 
        this.conn = DatabaseManager.getInstance().getConnection(); 
        cleanupOldHistory();
    }

    private void cleanupOldHistory() {
        try {
            // Delete only COMPLETED bookings older than 7 days
            String sql = "DELETE FROM bookings WHERE status='completed' AND date(check_in) < date('now', '-7 days')";
            conn.createStatement().execute(sql);
        } catch (SQLException e) { System.err.println("Cleanup error: " + e.getMessage()); }
    }

    public List<ParkingSlot> getAllSlots() {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_slots ORDER BY floor_number, slot_number";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ParkingSlot s = new ParkingSlot(
                    rs.getInt("slot_id"), rs.getInt("floor_number"),
                    rs.getString("slot_number"), toSlotType(rs.getString("slot_type")), rs.getString("vehicle_type"));
                s.forceSetStatus(toStatus(rs.getString("status")));
                list.add(s);
            }
        } catch (SQLException e) { System.err.println("getAllSlots error: " + e.getMessage()); }
        return list;
    }

    public boolean updateSlotStatus(int slotId, String status) {
        String sql = "UPDATE parking_slots SET status=? WHERE slot_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, slotId);
            ps.executeUpdate(); return true;
        } catch (SQLException e) {
            System.err.println("UpdateSlotStatus error: " + e.getMessage());
            return false;
        }
    }

    public ParkingSlot findFreeSlot(int floor) {
        String sql = "SELECT * FROM parking_slots WHERE floor_number=? AND status='free' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, floor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new ParkingSlot(rs.getInt("slot_id"), rs.getInt("floor_number"),
                    rs.getString("slot_number"), toSlotType(rs.getString("slot_type")), rs.getString("vehicle_type"));
            }
        } catch (SQLException e) { System.err.println("FindFreeSlot error: " + e.getMessage()); }
        return null;
    }

    public int createBooking(int vehicleId, int slotId, int employeeId,
                             String vehiclePlate, String slotNumber) {
        try {
            updateSlotStatus(slotId, "occupied");
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bookings (vehicle_id,slot_id,employee_id,vehicle_plate,slot_number," +
                "check_in,status,conf_code) VALUES (?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, vehicleId); ps.setInt(2, slotId); ps.setInt(3, employeeId);
            ps.setString(4, vehiclePlate); ps.setString(5, slotNumber);
            ps.setString(6, LocalDateTime.now().format(FMT));
            ps.setString(7, "active"); ps.setString(8, "EP0000");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMM"));
                int todayCount = 0;
                try (PreparedStatement countPs = conn.prepareStatement(
                        "SELECT COUNT(*) FROM bookings WHERE date(check_in) = date('now')")) {
                    ResultSet rsCount = countPs.executeQuery();
                    if (rsCount.next()) todayCount = rsCount.getInt(1);
                }
                String newConf = String.format("EP-%s-%02d", datePart, todayCount);
                
                PreparedStatement u = conn.prepareStatement(
                    "UPDATE bookings SET conf_code=? WHERE booking_id=?");
                u.setString(1, newConf); u.setInt(2, id);
                u.executeUpdate();
                return id;
            }
        } catch (SQLException e) { System.err.println("createBooking: " + e.getMessage()); }
        return -1;
    }

    public boolean checkoutBooking(int bookingId, double totalHours, double baseFee,
                                   double tax, double discount, double total, String method) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE bookings SET check_out=?,total_hours=?,base_fee=?,tax=?,discount=?," +
                "total_amount=?,pay_method=?,status='completed' WHERE booking_id=?");
            ps.setString(1, LocalDateTime.now().format(FMT));
            ps.setDouble(2, totalHours); ps.setDouble(3, baseFee);
            ps.setDouble(4, tax); ps.setDouble(5, discount);
            ps.setDouble(6, total); ps.setString(7, method); ps.setInt(8, bookingId);
            ps.executeUpdate(); return true;
        } catch (SQLException e) { System.err.println("checkout: " + e.getMessage()); return false; }
    }

    public List<Booking> getActiveBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, v.vehicle_type FROM bookings b " +
                     "JOIN vehicles v ON b.vehicle_id = v.vehicle_id " +
                     "WHERE b.status='active' ORDER BY b.check_in DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapBooking(rs));
        } catch (SQLException e) { System.err.println("GetActiveBookings error: " + e.getMessage()); }
        return list;
    }

    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT b.*, v.vehicle_type FROM bookings b " +
                "JOIN vehicles v ON b.vehicle_id = v.vehicle_id " +
                "ORDER BY b.check_in DESC LIMIT 200");
            while (rs.next()) list.add(mapBooking(rs));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    /** Fetches the latest N transactions (Check-In or Check-Out) for live feed */
    public List<Booking> getRecentActivity(int limit) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, v.vehicle_type FROM bookings b " +
                     "JOIN vehicles v ON b.vehicle_id = v.vehicle_id " +
                     "ORDER BY COALESCE(b.check_out, b.check_in) DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBooking(rs));
            }
        } catch (SQLException e) { System.err.println("RecentActivity error: " + e.getMessage()); }
        return list;
    }

    /** Customer bookings = bookings for vehicles registered under that customer_id. */
    public List<Booking> getBookingsByCustomer(int customerId) {
        List<Booking> list = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT b.*, v.vehicle_type FROM bookings b " +
                "JOIN vehicles v ON v.vehicle_id = b.vehicle_id " +
                "WHERE v.customer_id=? " +
                "ORDER BY b.check_in DESC LIMIT 200");
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBooking(rs));
        } catch (SQLException e) { System.err.println("getBookingsByCustomer: " + e.getMessage()); }
        return list;
    }

    public Booking findActiveByPlate(String plate) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT b.*, v.vehicle_type FROM bookings b " +
                "JOIN vehicles v ON b.vehicle_id = v.vehicle_id " +
                "WHERE b.vehicle_plate=? AND b.status='active' LIMIT 1");
            ps.setString(1, plate.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapBooking(rs);
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return null;
    }

    public double getTodayRevenue() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COALESCE(SUM(total_amount),0) FROM bookings WHERE status='completed' " +
                "AND date(check_out)=date('now')");
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    public int getTodayCount() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM bookings WHERE date(check_in)=date('now')");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    public int getTodayBookingsCount() {
        return getTodayCount();
    }

    public int[] getOccupiedCounts() {
        int[] counts = new int[2]; // [car, bike]
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT vehicle_type, COUNT(*) FROM parking_slots WHERE status='occupied' GROUP BY vehicle_type")) {
            while (rs.next()) {
                String type = rs.getString(1);
                int count = rs.getInt(2);
                if ("car".equalsIgnoreCase(type)) counts[0] = count;
                else if ("bike".equalsIgnoreCase(type)) counts[1] = count;
            }
        } catch (SQLException e) { /* ignore */ }
        return counts;
    }

    public double getTotalRevenueAllTime() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COALESCE(SUM(total_amount),0) FROM bookings WHERE status='completed'");
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {}
        return 0;
    }

    public int getTotalBookingsAllTime() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM bookings");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {}
        return 0;
    }

    public String getMostUsedSlot() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT slot_number, COUNT(*) as c FROM bookings GROUP BY slot_number ORDER BY c DESC LIMIT 1");
            if (rs.next()) return rs.getString("slot_number");
        } catch (SQLException e) {}
        return "N/A";
    }

    /** Returns revenue per day for last 7 days. Key = "Mon\n21" style label, value = amount. */
    public java.util.LinkedHashMap<String, Double> getDailyRevenueLast7Days1() {
        java.util.LinkedHashMap<String, Double> map = new java.util.LinkedHashMap<>();
        try {
            // Build 7-day map (fill with 0 first so all days show even if no revenue)
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter dayFmt = java.time.format.DateTimeFormatter.ofPattern("EEE\ndd");
            for (int i = 6; i >= 0; i--) {
                java.time.LocalDate d = today.minusDays(i);
                map.put(d.format(dayFmt), 0.0);
            }
            // Query actual revenue
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT date(check_out) as d, SUM(total_amount) as rev " +
                "FROM bookings WHERE status='completed' " +
                "AND check_out >= date('now','-6 days') " +
                "GROUP BY date(check_out)");
            while (rs.next()) {
                String dbDate = rs.getString("d"); // yyyy-MM-dd
                if (dbDate == null) continue;
                java.time.LocalDate d = java.time.LocalDate.parse(dbDate);
                String key = d.format(dayFmt);
                if (map.containsKey(key)) map.put(key, rs.getDouble("rev"));
            }
        } catch (Exception e) { System.err.println("dailyRevenue: " + e.getMessage()); }
        return map;
    }

    /** Returns revenue per day for the last N days: key = "Mon 28", value = Rs total */
    public java.util.LinkedHashMap<String, Double> getDailyRevenueLast7Days() {
        java.util.LinkedHashMap<String, Double> map = new java.util.LinkedHashMap<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT date(check_out) as d, SUM(total_amount) as rev " +
                "FROM bookings WHERE status='completed' " +
                "AND check_out >= date('now','-6 days') " +
                "GROUP BY date(check_out) ORDER BY d ASC");
            // Pre-fill last 7 days with 0
            java.time.LocalDate today = java.time.LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                java.time.LocalDate d = today.minusDays(i);
                String key = d.format(java.time.format.DateTimeFormatter.ofPattern("EEE dd"));
                map.put(key, 0.0);
            }
            while (rs.next()) {
                java.time.LocalDate d = java.time.LocalDate.parse(rs.getString("d"));
                String key = d.format(java.time.format.DateTimeFormatter.ofPattern("EEE dd"));
                map.put(key, rs.getDouble("rev"));
            }
        } catch (SQLException e) { System.err.println("dailyRevenue: " + e.getMessage()); }
        return map;
    }

    public String getBusiestRow() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT p.floor_number, COUNT(b.booking_id) as c " +
                "FROM bookings b JOIN parking_slots p ON b.slot_id = p.slot_id " +
                "GROUP BY p.floor_number ORDER BY c DESC LIMIT 1");
            if (rs.next()) {
                // Derive row label from slot data
                ResultSet rs2 = conn.createStatement().executeQuery(
                    "SELECT slot_number FROM parking_slots WHERE floor_number=" + rs.getInt("floor_number") + " LIMIT 1");
                if (rs2.next()) {
                    String sn = rs2.getString("slot_number");
                    return "Row " + (sn != null && sn.length() > 0 ? sn.substring(0, 1) : "A");
                }
            }
        } catch (SQLException e) {}
        return "N/A";
    }

    public int getVehicleIdByPlate(String plate) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT vehicle_id FROM vehicles WHERE license_plate=?");
            ps.setString(1, plate.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* ignore */ }
        return -1;
    }

    public int saveVehicleIfNew(String plate, String type, int customerId) {
        int existing = getVehicleIdByPlate(plate);
        if (existing > 0) return existing;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO vehicles (customer_id,license_plate,vehicle_type,model,color) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, customerId); ps.setString(2, plate.toUpperCase());
            ps.setString(3, type); ps.setString(4, "Unknown"); ps.setString(5, "Unknown");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) { System.err.println("saveVehicle: " + e.getMessage()); }
        return -1;
    }

    /** Find first free slot for given vehicle type, ordered by slot_number (series: A1,A2..B1,B2..) */
    public ParkingSlot findFreeSlotByVehicleType(String vehicleType) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM parking_slots WHERE vehicle_type=? AND status='free' ORDER BY slot_number ASC LIMIT 1");
            ps.setString(1, vehicleType.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ParkingSlot s = new ParkingSlot(rs.getInt("slot_id"), rs.getInt("floor_number"),
                    rs.getString("slot_number"), toSlotType(rs.getString("slot_type")), rs.getString("vehicle_type"));
                s.forceSetStatus(toStatus(rs.getString("status")));
                return s;
            }
        } catch (SQLException e) { System.err.println("findFreeSlotByType: " + e.getMessage()); }
        return null;
    }

    /** Create booking with owner name and contact number */
    public int createBookingWithDetails(int vehicleId, int slotId, int employeeId,
                                        String vehiclePlate, String slotNumber,
                                        String ownerName, String contactNumber) {
        try {
            updateSlotStatus(slotId, "occupied");
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bookings (vehicle_id,slot_id,employee_id,vehicle_plate,slot_number," +
                "check_in,status,conf_code,owner_name,contact_number) VALUES (?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, vehicleId); ps.setInt(2, slotId); ps.setInt(3, employeeId);
            ps.setString(4, vehiclePlate); ps.setString(5, slotNumber);
            ps.setString(6, LocalDateTime.now().format(FMT));
            ps.setString(7, "active"); ps.setString(8, "EP0000");
            ps.setString(9, ownerName); ps.setString(10, contactNumber);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                
                // New logic for Daily Resetting Confirmation Code: EP-DDMM-SERIAL
                String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMM"));
                int todayCount = 0;
                try (PreparedStatement countPs = conn.prepareStatement(
                        "SELECT COUNT(*) FROM bookings WHERE date(check_in) = date('now')")) {
                    ResultSet rsCount = countPs.executeQuery();
                    if (rsCount.next()) todayCount = rsCount.getInt(1);
                }
                
                String newConf = String.format("EP-%s-%02d", datePart, todayCount);
                
                PreparedStatement u = conn.prepareStatement(
                    "UPDATE bookings SET conf_code=? WHERE booking_id=?");
                u.setString(1, newConf); u.setInt(2, id);
                u.executeUpdate();
                return id;
            }
        } catch (SQLException e) { System.err.println("createBookingWithDetails: " + e.getMessage()); }
        return -1;
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        int bookingId   = rs.getInt("booking_id");
        int vehicleId   = rs.getInt("vehicle_id");
        int slotId      = rs.getInt("slot_id");
        int employeeId  = rs.getInt("employee_id");
        String plate    = rs.getString("vehicle_plate");
        String slotNum  = rs.getString("slot_number");

        String vt       = rs.getString("vehicle_type");
        Booking b = new Booking(bookingId, vehicleId, slotId, employeeId, plate, slotNum, vt);

        // Hydrate from DB (otherwise constructor defaults like now() break live UI)
        b.setConfCode(rs.getString("conf_code"));
        b.setPaymentMethod(rs.getString("pay_method"));

        String st = rs.getString("status");
        b.setStatus(toBookingStatus(st));

        b.setTotalHours(rs.getDouble("total_hours"));
        b.setBaseFee(rs.getDouble("base_fee"));
        b.setTax(rs.getDouble("tax"));
        b.setDiscount(rs.getDouble("discount"));
        b.setTotalAmount(rs.getDouble("total_amount"));

        b.setCheckIn(parseDateTime(rs.getString("check_in")));
        b.setCheckOut(parseDateTime(rs.getString("check_out")));
        return b;
    }

    private Booking.BookingStatus toBookingStatus(String s) {
        String v = (s != null ? s.toLowerCase() : "");
        return switch (v) {
            case "completed" -> Booking.BookingStatus.COMPLETED;
            case "cancelled", "canceled" -> Booking.BookingStatus.CANCELLED;
            default -> Booking.BookingStatus.ACTIVE;
        };
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            // Expected: yyyy-MM-dd HH:mm:ss (this DAO writes with FMT)
            return LocalDateTime.parse(raw, FMT);
        } catch (Exception ignored) {
            try {
                // Fallback if stored as ISO format
                return LocalDateTime.parse(raw);
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private ParkingSlot.SlotType toSlotType(String s) {
        return switch (s != null ? s.toLowerCase() : "") {
            case "ev" -> ParkingSlot.SlotType.EV;
            case "handicap" -> ParkingSlot.SlotType.HANDICAP;
            default -> ParkingSlot.SlotType.STANDARD;
        };
    }

    private ParkingSlot.Status toStatus(String s) {
        return switch (s != null ? s.toLowerCase() : "") {
            case "occupied" -> ParkingSlot.Status.OCCUPIED;
            case "reserved" -> ParkingSlot.Status.RESERVED;
            default -> ParkingSlot.Status.FREE;
        };
    }
}
