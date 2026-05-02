package model;

import java.io.Serializable;

/**
 * Abstract base class for all vehicle types.
 * Demonstrates ABSTRACTION (abstract method calculateFee).
 * Car, Bike, ElectricVehicle INHERIT from this.
 */
public abstract class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;

    private int vehicleId;
    private int customerId;
    private String licensePlate;
    private String model;
    private String color;

    public Vehicle(int vehicleId, int customerId, String licensePlate,
                   String model, String color) {
        this.vehicleId = vehicleId;
        this.customerId = customerId;
        this.licensePlate = licensePlate.toUpperCase().trim();
        this.model = model;
        this.color = color;
    }

    /** ABSTRACTION: each vehicle type defines its own hourly rate */
    public abstract double calculateFee(double hours);
    public abstract String getVehicleType();
    public abstract String getSlotTypeRequired(); // standard / ev / any

    // Getters (ENCAPSULATION)
    public int getVehicleId()      { return vehicleId; }
    public int getCustomerId()     { return customerId; }
    public String getLicensePlate(){ return licensePlate; }
    public String getModel()       { return model; }
    public String getColor()       { return color; }

    public void setModel(String m)  { this.model = m; }
    public void setColor(String c)  { this.color = c; }

    @Override
    public String toString() {
        return "[" + getVehicleType().toUpperCase() + "] " + licensePlate + " - " + model;
    }
}
