package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ParkingRow — COMPOSITION: has ArrayList<ParkingSlot>
 * Replaces the old "ParkingFloor" concept — parking is now organized by ROWS (A, B, C...).
 */
public class ParkingRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private String rowLabel;   // e.g. "A", "B", "C"
    private String rowName;    // e.g. "Car Row A"
    private List<ParkingSlot> slots; // OBJECT COMPOSITION

    public ParkingRow(String rowLabel, String rowName) {
        this.rowLabel = rowLabel;
        this.rowName  = rowName;
        this.slots    = new ArrayList<>();
    }

    public void addSlot(ParkingSlot slot) { slots.add(slot); }

    public List<ParkingSlot> getSlots()  { return slots; }
    public String getRowLabel()          { return rowLabel; }
    public String getRowName()           { return rowName; }

    public int getFreeCount() {
        return (int) slots.stream().filter(ParkingSlot::isFree).count();
    }

    public int getTotalCount() { return slots.size(); }

    public ParkingSlot findFreeSlot(ParkingSlot.SlotType preferred) {
        // Try preferred type first
        for (ParkingSlot s : slots)
            if (s.isFree() && s.getSlotType() == preferred) return s;
        // Fall back to any free slot
        for (ParkingSlot s : slots)
            if (s.isFree()) return s;
        return null;
    }
}
