package exceptions;

/** Thrown when an unrecognised vehicle type string is provided. */
public class InvalidVehicleTypeException extends Exception {
    public InvalidVehicleTypeException(String type) {
        super("Invalid vehicle type: '" + type + "'. Allowed: car, bike, ev");
    }
}
