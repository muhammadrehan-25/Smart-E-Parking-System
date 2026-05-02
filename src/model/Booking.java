package model;

import interfaces.Payable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Represents a parking booking session. */
public class Booking implements Serializable, Payable {
    private static final long serialVersionUID = 1L;

    public enum BookingStatus { ACTIVE, COMPLETED, CANCELLED }

    private int bookingId;
    private int vehicleId;
    private int slotId;
    private int employeeId;
    private String vehiclePlate;
    private String slotNumber;
    private String vehicleType;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private double totalHours;
    private double baseFee;
    private double tax;
    private double discount;
    private double totalAmount;
    private String paymentMethod;
    private BookingStatus status;
    private String confirmationCode;

    public Booking(int bookingId, int vehicleId, int slotId, int employeeId,
                   String vehiclePlate, String slotNumber, String vehicleType) {
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.slotId    = slotId;
        this.employeeId = employeeId;
        this.vehiclePlate = vehiclePlate;
        this.slotNumber   = slotNumber;
        this.vehicleType  = vehicleType;
        this.checkIn   = LocalDateTime.now();
        this.status    = BookingStatus.ACTIVE;
        this.confirmationCode = "EP" + String.format("%04d", bookingId);
    }

    /**
     * DAO-friendly constructor. Prefer using setters from BookingDAO to hydrate
     * objects from the database to avoid "now()" defaults overwriting real data.
     */
    public Booking(int bookingId, int vehicleId, int slotId, int employeeId,
                   String vehiclePlate, String slotNumber,
                   LocalDateTime checkIn, LocalDateTime checkOut,
                   double totalHours, double baseFee, double tax, double discount,
                   double totalAmount, String paymentMethod,
                   BookingStatus status, String confirmationCode) {
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.slotId = slotId;
        this.employeeId = employeeId;
        this.vehiclePlate = vehiclePlate;
        this.slotNumber = slotNumber;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalHours = totalHours;
        this.baseFee = baseFee;
        this.tax = tax;
        this.discount = discount;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status != null ? status : BookingStatus.ACTIVE;
        this.confirmationCode = (confirmationCode != null && !confirmationCode.isEmpty())
            ? confirmationCode
            : ("EP" + String.format("%04d", bookingId));
    }

    /** Calculate total duration in hours */
    public double computeHours() {
        LocalDateTime end = (checkOut != null) ? checkOut : LocalDateTime.now();
        return ChronoUnit.MINUTES.between(checkIn, end) / 60.0;
    }

    public void checkout(Vehicle vehicle, double memberDiscountPct) {
        this.checkOut    = LocalDateTime.now();
        this.totalHours  = computeHours();
        this.baseFee     = vehicle.calculateFee(totalHours); // POLYMORPHISM
        this.tax         = baseFee * 0.13; // 13% tax
        this.discount    = baseFee * (memberDiscountPct / 100.0);
        this.totalAmount = baseFee + tax - discount;
        this.status      = BookingStatus.COMPLETED;
    }

    // ── Payable interface ──────────────────────────────────
    @Override
    public double processPayment(double amount, String method) {
        this.paymentMethod = method;
        this.totalAmount   = amount;
        return amount;
    }

    @Override
    public String generateReceipt(int bookingId) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        return  "=========================================\n"
              + "          SMART ePARK - RECEIPT          \n"
              + "=========================================\n"
              + "Booking # : " + confirmationCode + "\n"
              + "Vehicle   : " + vehiclePlate + "\n"
              + "Slot      : " + slotNumber + "\n"
              + "Check-In  : " + (checkIn  != null ? checkIn.format(fmt)  : "-") + "\n"
              + "Check-Out : " + (checkOut != null ? checkOut.format(fmt) : "-") + "\n"
              + "Duration  : " + String.format("%.2f", totalHours) + " hrs\n"
              + "-----------------------------------------\n"
              + "Base Fee  : Rs. " + String.format("%.2f", baseFee) + "\n"
              + "Tax (13%) : Rs. " + String.format("%.2f", tax) + "\n"
              + "Discount  : Rs. " + String.format("%.2f", discount) + "\n"
              + "TOTAL     : Rs. " + String.format("%.2f", totalAmount) + "\n"
              + "Method    : " + (paymentMethod != null ? paymentMethod : "-") + "\n"
              + "=========================================\n"
              + "      Thank you for using Smart ePark!   \n"
              + "=========================================\n";
    }

    @Override
    public boolean applyDiscount(double discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) return false;
        this.discount = baseFee * (discountPercent / 100.0);
        this.totalAmount = baseFee + tax - discount;
        return true;
    }

    // Getters
    public int getBookingId()         { return bookingId; }
    public int getVehicleId()         { return vehicleId; }
    public int getSlotId()            { return slotId; }
    public int getEmployeeId()        { return employeeId; }
    public String getVehiclePlate()   { return vehiclePlate; }
    public String getSlotNumber()     { return slotNumber; }
    public LocalDateTime getCheckIn() { return checkIn; }
    public LocalDateTime getCheckOut(){ return checkOut; }
    public double getTotalHours()     { return totalHours; }
    public double getBaseFee()        { return baseFee; }
    public double getTax()            { return tax; }
    public double getDiscount()       { return discount; }
    public double getTotalAmount()    { return totalAmount; }
    public String getPaymentMethod()  { return paymentMethod; }
    public BookingStatus getStatus()  { return status; }
    public String getConfCode()       { return confirmationCode; }
    public String getVehicleType()   { return vehicleType; }
    public void setVehicleType(String vt) { this.vehicleType = vt; }

    public void setPaymentMethod(String m) { this.paymentMethod = m; }
    public void setStatus(BookingStatus s) { this.status = s; }

    // Setters used by DAO hydration (live DB data)
    public void setCheckIn(LocalDateTime t) { this.checkIn = t; }
    public void setCheckOut(LocalDateTime t) { this.checkOut = t; }
    public void setTotalHours(double h) { this.totalHours = h; }
    public void setBaseFee(double f) { this.baseFee = f; }
    public void setTax(double t) { this.tax = t; }
    public void setDiscount(double d) { this.discount = d; }
    public void setTotalAmount(double a) { this.totalAmount = a; }
    public void setConfCode(String c) { this.confirmationCode = c; }
}
