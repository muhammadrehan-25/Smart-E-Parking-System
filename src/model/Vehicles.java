package model;

/**
 * Concrete vehicle classes demonstrating POLYMORPHISM.
 * Each overrides calculateFee() with its own rate.
 */

// ── Car ──────────────────────────────────────────────────
class Car extends Vehicle {
    private static double getRate() { return util.Settings.getDouble("carRatePerHour", 100); }

    public Car(int vehicleId, int customerId, String licensePlate, String model, String color) {
        super(vehicleId, customerId, licensePlate, model, color);
    }

    @Override public double calculateFee(double hours)  { return hours * getRate(); }
    @Override public String getVehicleType()            { return "car"; }
    @Override public String getSlotTypeRequired()       { return "standard"; }
}

// ── Bike ─────────────────────────────────────────────────
class Bike extends Vehicle {
    private static double getRate() { return util.Settings.getDouble("bikeRatePerHour", 50); }

    public Bike(int vehicleId, int customerId, String licensePlate, String model, String color) {
        super(vehicleId, customerId, licensePlate, model, color);
    }

    @Override public double calculateFee(double hours)  { return hours * getRate(); }
    @Override public String getVehicleType()            { return "bike"; }
    @Override public String getSlotTypeRequired()       { return "standard"; }
}

// ── Electric Vehicle ──────────────────────────────────────
class ElectricVehicle extends Vehicle {
    private static final double RATE_PER_HOUR = 30.0; // Rs. 30/hr (discounted)
    private int batteryLevel;

    public ElectricVehicle(int vehicleId, int customerId, String licensePlate,
                           String model, String color, int batteryLevel) {
        super(vehicleId, customerId, licensePlate, model, color);
        this.batteryLevel = batteryLevel;
    }

    @Override public double calculateFee(double hours)  { return hours * RATE_PER_HOUR; }
    @Override public String getVehicleType()            { return "ev"; }
    @Override public String getSlotTypeRequired()       { return "ev"; } // priority EV slot

    public int getBatteryLevel()               { return batteryLevel; }
    public void setBatteryLevel(int level)     { this.batteryLevel = level; }
}
