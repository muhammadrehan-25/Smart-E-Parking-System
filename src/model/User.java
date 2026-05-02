package model;

import java.io.Serializable;

/**
 * Abstract base class for all system users.
 * Demonstrates ABSTRACTION and ENCAPSULATION.
 * Admin and Employee inherit from this class.
 */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    // ENCAPSULATION: all fields private
    private int userId;
    private String username;
    private String passwordHash; // SHA-256 hash, never exposed raw
    private String fullName;
    private String email;
    private String phone;
    private boolean active;

    public User(int userId, String username, String passwordHash,
                String fullName, String email, String phone) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.active = true;
    }

    // ABSTRACTION: subclasses must define their role
    public abstract String getRole();
    public abstract String getDashboardTitle();

    // Getters & Setters (ENCAPSULATION)
    public int getUserId()           { return userId; }
    public String getUsername()      { return username; }
    public String getPasswordHash()  { return passwordHash; }
    public String getFullName()      { return fullName; }
    public String getEmail()         { return email; }
    public String getPhone()         { return phone; }
    public boolean isActive()        { return active; }

    public void setFullName(String n) { this.fullName = n; }
    public void setEmail(String e)    { this.email = e; }
    public void setPhone(String p)    { this.phone = p; }
    public void setActive(boolean a)  { this.active = a; }
    public void setPasswordHash(String h) { this.passwordHash = h; }

    @Override
    public String toString() {
        return "[" + getRole() + "] " + fullName + " (" + username + ")";
    }
}
