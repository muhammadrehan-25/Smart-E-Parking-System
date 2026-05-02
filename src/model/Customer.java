package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer/Driver user — self-service portal.
 * Demonstrates INHERITANCE from User.
 * COMPOSITION: has ArrayList of Booking history and registered vehicles.
 */
public class Customer extends User {
    private static final long serialVersionUID = 1L;

    private List<Booking> bookingHistory;  // COMPOSITION: ArrayList<Booking>
    private List<Vehicle> registeredVehicles; // COMPOSITION: ArrayList<Vehicle>
    private boolean hasMembership;
    private double membershipDiscount; // percentage e.g. 20.0

    public Customer(int userId, String username, String passwordHash,
                    String fullName, String email, String phone) {
        super(userId, username, passwordHash, fullName, email, phone);
        this.bookingHistory = new ArrayList<>();
        this.registeredVehicles = new ArrayList<>();
        this.hasMembership = false;
        this.membershipDiscount = 0.0;
    }

    @Override public String getRole()           { return "CUSTOMER"; }
    @Override public String getDashboardTitle() { return "My Parking Portal"; }

    public void addBooking(Booking b)   { bookingHistory.add(b); }
    public void addVehicle(Vehicle v)   { registeredVehicles.add(v); }
    public List<Booking> getBookingHistory()     { return bookingHistory; }
    public List<Vehicle> getRegisteredVehicles() { return registeredVehicles; }
    public boolean hasMembership()               { return hasMembership; }
    public double getMembershipDiscount()        { return membershipDiscount; }

    public void activateMembership(double discountPct) {
        this.hasMembership = true;
        this.membershipDiscount = discountPct;
    }
}
