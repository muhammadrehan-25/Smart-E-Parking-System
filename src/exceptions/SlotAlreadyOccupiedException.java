package exceptions;

/** Thrown when trying to book an already occupied parking slot. */
public class SlotAlreadyOccupiedException extends Exception {
    private final String slotNumber;

    public SlotAlreadyOccupiedException(String slotNumber) {
        super("Slot " + slotNumber + " is already occupied or reserved.");
        this.slotNumber = slotNumber;
    }

    public String getSlotNumber() { return slotNumber; }
}
