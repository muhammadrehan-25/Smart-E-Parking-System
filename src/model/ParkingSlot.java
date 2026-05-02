package model;

import exceptions.SlotAlreadyOccupiedException;
import java.io.Serializable;

/**
 * Represents a single parking slot.
 * ENCAPSULATION: setStatus() validates before changing state.
 */
public class ParkingSlot implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { FREE, OCCUPIED, RESERVED }
    public enum SlotType { STANDARD, EV, HANDICAP }

    private int slotId;
    private String rowLabel;   // e.g. "A", "B" (Row concept — replaces old floorNumber)
    private String slotNumber; // e.g. "A1", "B3"
    private SlotType slotType;
    private Status status;
    private String vehicleType; // "car", "bike"

    /** Primary constructor with rowLabel */
    public ParkingSlot(int slotId, String rowLabel, String slotNumber, SlotType slotType, String vehicleType) {
        this.slotId = slotId;
        this.rowLabel = rowLabel;
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.vehicleType = vehicleType;
        this.status = Status.FREE;
    }

    /** Legacy/DB-compat constructor (int floor_number → derive row label from slot_number) */
    public ParkingSlot(int slotId, int legacyFloor, String slotNumber, SlotType slotType, String vehicleType) {
        this(slotId, slotNumber != null && slotNumber.length() > 0 ? slotNumber.substring(0, 1) : "A",
             slotNumber, slotType, vehicleType);
    }

    /** ENCAPSULATION: validates state transition before allowing change */
    public void setStatus(Status newStatus) throws SlotAlreadyOccupiedException {
        if (this.status != Status.FREE && newStatus == Status.OCCUPIED) {
            throw new SlotAlreadyOccupiedException(slotNumber);
        }
        this.status = newStatus;
    }

    public void forceSetStatus(Status s) { this.status = s; } // admin override

    public int getSlotId()        { return slotId; }
    public String getRowLabel()   { return rowLabel; }
    /** Legacy getter — returns 1 always (no multi-floor; use getRowLabel() instead) */
    public int getFloorNumber()   { return 1; }
    public String getSlotNumber() { return slotNumber; }
    public SlotType getSlotType() { return slotType; }
    public Status getStatus()     { return status; }
    public String getVehicleType(){ return vehicleType; }
    public void setVehicleType(String vt) { this.vehicleType = vt; }
    public boolean isFree()       { return status == Status.FREE; }

    @Override
    public String toString() {
        return "Slot " + slotNumber + " [Row " + rowLabel + "] - " +
               (vehicleType != null ? vehicleType.substring(0, 1).toUpperCase() + vehicleType.substring(1) : "Car") +
               " - " + status;
    }
}
