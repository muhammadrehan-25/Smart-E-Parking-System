package model;

/**
 * Employee/Operator user — daily parking operations.
 * Demonstrates INHERITANCE from User.
 */
public class Employee extends User {
    private static final long serialVersionUID = 1L;

    private String shift;       // "morning" / "evening" / "night"
    private int processedToday; // bookings handled in current shift

    public Employee(int userId, String username, String passwordHash,
                    String fullName, String email, String phone, String shift) {
        super(userId, username, passwordHash, fullName, email, phone);
        this.shift = shift;
        this.processedToday = 0;
    }

    @Override public String getRole()           { return "EMPLOYEE"; }
    @Override public String getDashboardTitle() { return "Operator Dashboard"; }

    public String getShift()         { return shift; }
    public int getProcessedToday()   { return processedToday; }
    public void incrementProcessed() { processedToday++; }
    public void resetShift()         { processedToday = 0; }
    public void setShift(String s)   { this.shift = s; }
}
