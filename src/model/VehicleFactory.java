package model;

import exceptions.InvalidVehicleTypeException;

/** Factory to create Vehicle instances by type string. */
public class VehicleFactory {
    public static Vehicle create(String type, int vehicleId, int customerId,
                                 String plate, String model, String color)
            throws InvalidVehicleTypeException {
        return switch (type.toLowerCase().trim()) {
            case "car"   -> new Car(vehicleId, customerId, plate, model, color);
            case "bike"  -> new Bike(vehicleId, customerId, plate, model, color);
            case "ev"    -> new ElectricVehicle(vehicleId, customerId, plate, model, color, 100);
            default      -> throw new InvalidVehicleTypeException(type);
        };
    }
}
