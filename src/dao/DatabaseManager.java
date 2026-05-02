package dao;

import java.sql.*;

/**
 * Manages SQLite database connection and schema initialization.
 * All operations use try-catch for ROBUSTNESS (Exception Handling requirement).
 */
public class DatabaseManager {
    private static final String BASE_DIR = System.getProperty("user.home") + java.io.File.separator + ".smartepark" + java.io.File.separator;
    private static final String DB_URL = "jdbc:sqlite:" + BASE_DIR + "data/epark.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            new java.io.File(BASE_DIR + "data").mkdirs();
            System.setProperty("org.sqlite.tmpdir", new java.io.File(BASE_DIR + "data").getAbsolutePath());
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initSchema();
        } catch (Exception e) {
            System.err.println("DB init error: " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.err.println("DB: connection was closed — reopening...");
                connection = DriverManager.getConnection(DB_URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON");
            }
        } catch (SQLException e) {
            System.err.println("DB: reconnect failed: " + e.getMessage());
        }
        return connection;
    }

    private void initSchema() throws SQLException {
        Statement st = connection.createStatement();

        st.execute("""
            CREATE TABLE IF NOT EXISTS users (
                user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                full_name     TEXT,
                email         TEXT,
                phone         TEXT,
                role          TEXT NOT NULL,
                shift         TEXT,
                active        INTEGER DEFAULT 1
            )""");

        st.execute("""
            CREATE TABLE IF NOT EXISTS vehicles (
                vehicle_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id   INTEGER REFERENCES users(user_id),
                license_plate TEXT UNIQUE NOT NULL,
                vehicle_type  TEXT NOT NULL,
                model         TEXT,
                color         TEXT
            )""");

        st.execute("""
            CREATE TABLE IF NOT EXISTS parking_slots (
                slot_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                floor_number INTEGER NOT NULL,
                slot_number  TEXT UNIQUE NOT NULL,
                slot_type    TEXT DEFAULT 'standard',
                status       TEXT DEFAULT 'free',
                vehicle_type TEXT DEFAULT 'car'
            )""");

        // CLEANUP DUPLICATES: Delete extra slots with same number, PREFERRING 'occupied' slots
        // Using a more compatible subquery approach instead of Window Functions
        st.execute("""
            DELETE FROM parking_slots 
            WHERE slot_id NOT IN (
                SELECT s1.slot_id 
                FROM parking_slots s1
                WHERE s1.slot_id = (
                    SELECT s2.slot_id 
                    FROM parking_slots s2 
                    WHERE s2.slot_number = s1.slot_number 
                    ORDER BY s2.status DESC, s2.slot_id ASC 
                    LIMIT 1
                )
            )""");

        try {
            // Force unique index creation
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_slot_number ON parking_slots(slot_number)");
        } catch (SQLException e) { 
            System.err.println("Note: Could not create unique index on slot_number. Duplicates might exist.");
        }

        // REPAIR: Dynamically assign free slots based on the current settings
        int carSlots = util.Settings.getInt("carSlots", 30);
        int numCarRows = (carSlots + 9) / 10;
        char bikeStartRow = (char)('A' + numCarRows);
        
        // Mark everything before bikeStartRow as 'car' if free, and everything after as 'bike' if free
        st.execute(String.format(
            "UPDATE parking_slots SET vehicle_type='car' WHERE slot_number < '%c1' AND status='free'", bikeStartRow));
        st.execute(String.format(
            "UPDATE parking_slots SET vehicle_type='bike' WHERE slot_number >= '%c1' AND status='free'", bikeStartRow));

        try {
            st.execute("ALTER TABLE parking_slots ADD COLUMN vehicle_type TEXT DEFAULT 'car'");
        } catch (SQLException e) { /* column exists */ }

        try {
            st.execute("ALTER TABLE bookings ADD COLUMN owner_name TEXT");
        } catch (SQLException e) { /* already exists */ }

        try {
            st.execute("ALTER TABLE bookings ADD COLUMN contact_number TEXT");
        } catch (SQLException e) { /* already exists */ }

        st.execute("""
            CREATE TABLE IF NOT EXISTS bookings (
                booking_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                vehicle_id   INTEGER REFERENCES vehicles(vehicle_id),
                slot_id      INTEGER REFERENCES parking_slots(slot_id),
                employee_id  INTEGER REFERENCES users(user_id),
                vehicle_plate TEXT,
                slot_number  TEXT,
                check_in     TEXT NOT NULL,
                check_out    TEXT,
                total_hours  REAL DEFAULT 0,
                base_fee     REAL DEFAULT 0,
                tax          REAL DEFAULT 0,
                discount     REAL DEFAULT 0,
                total_amount REAL DEFAULT 0,
                pay_method   TEXT,
                status       TEXT DEFAULT 'active',
                conf_code    TEXT,
                owner_name   TEXT,
                contact_number TEXT
            )""");

        st.execute("""
            CREATE TABLE IF NOT EXISTS memberships (
                membership_id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id   INTEGER REFERENCES users(user_id),
                plan_type     TEXT,
                expiry_date   TEXT,
                discount_pct  REAL DEFAULT 20.0
            )""");

        st.execute("""
            CREATE TABLE IF NOT EXISTS activity_log (
                log_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER REFERENCES users(user_id),
                action     TEXT,
                logged_at  TEXT DEFAULT (datetime('now'))
            )""");

        // Seed Users
        String adminHash = UserDAO.hash("admin123");
        String empHash   = UserDAO.hash("emp123");
        String custHash  = UserDAO.hash("cust123");

        PreparedStatement psUser = connection.prepareStatement(
            "INSERT OR IGNORE INTO users (username, password_hash, full_name, email, role) VALUES (?,?,?,?,?)");
        psUser.setString(1, "admin"); psUser.setString(2, adminHash);
        psUser.setString(3, "System Admin"); psUser.setString(4, "admin@epark.com");
        psUser.setString(5, "ADMIN"); psUser.executeUpdate();

        PreparedStatement psEmp = connection.prepareStatement(
            "INSERT OR IGNORE INTO users (username, password_hash, full_name, email, role, shift) VALUES (?,?,?,?,?,?)");
        psEmp.setString(1, "emp1"); psEmp.setString(2, empHash);
        psEmp.setString(3, "Ali Raza"); psEmp.setString(4, "ali@epark.com");
        psEmp.setString(5, "EMPLOYEE"); psEmp.setString(6, "morning"); psEmp.executeUpdate();

        // Seed slots if table is empty
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM parking_slots");
        if (rs.next() && rs.getInt(1) == 0) {
            String[] carRows = {"A", "B", "C"};
            String[] bikeRows = {"D", "E"};
            PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO parking_slots (floor_number, slot_number, slot_type, status, vehicle_type) VALUES (?,?,?,?,?)");
            for (String row : carRows) {
                for (int n = 1; n <= 10; n++) {
                    ps.setInt(1, 1); ps.setString(2, row + n);
                    ps.setString(3, "standard"); ps.setString(4, "free");
                    ps.setString(5, "car"); ps.addBatch();
                }
            }
            for (String row : bikeRows) {
                for (int n = 1; n <= 10; n++) {
                    ps.setInt(1, 1); ps.setString(2, row + n);
                    ps.setString(3, "standard"); ps.setString(4, "free");
                    ps.setString(5, "bike"); ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }
}