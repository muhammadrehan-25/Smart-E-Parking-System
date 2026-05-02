package model;

import interfaces.Reportable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ParkingLot — top-level COMPOSITION class.
 * Has ArrayList<ParkingRow>, each row has ArrayList<ParkingSlot>.
 * Implements Reportable interface (ABSTRACTION).
 * Layout: Rows A-C = Car (10 each), Rows D-E = Bike (10 each).
 */
public class ParkingLot implements Serializable, Reportable {
    private static final long serialVersionUID = 1L;

    private String lotName;
    private String address;
    private List<ParkingRow> rows; // COMPOSITION: ArrayList<ParkingRow>

    public ParkingLot(String lotName, String address) {
        this.lotName = lotName;
        this.address = address;
        this.rows    = new ArrayList<>();
    }

    public void addRow(ParkingRow row) { rows.add(row); }

    public List<ParkingRow> getRows() { return rows; }
    public String getLotName()        { return lotName; }
    public String getAddress()        { return address; }

    public int getTotalSlots() {
        return rows.stream().mapToInt(ParkingRow::getTotalCount).sum();
    }

    public int getTotalFreeSlots() {
        return rows.stream().mapToInt(ParkingRow::getFreeCount).sum();
    }

    public int getTotalOccupied() { return getTotalSlots() - getTotalFreeSlots(); }

    // POLYMORPHISM via Reportable interface
    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(lotName).append(" Status Report ===\n");
        sb.append("Total Slots  : ").append(getTotalSlots()).append("\n");
        sb.append("Occupied     : ").append(getTotalOccupied()).append("\n");
        sb.append("Available    : ").append(getTotalFreeSlots()).append("\n\n");
        for (ParkingRow r : rows) {
            sb.append("Row ").append(r.getRowLabel()).append(" (").append(r.getRowName()).append("): ")
              .append(r.getFreeCount()).append("/").append(r.getTotalCount()).append(" free\n");
        }
        return sb.toString();
    }

    @Override
    public boolean exportCSV(String filePath) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(filePath))) {
            pw.println("Row,SlotNumber,Type,Status");
            for (ParkingRow r : rows)
                for (ParkingSlot s : r.getSlots())
                    pw.printf("%s,%s,%s,%s%n",
                        r.getRowLabel(), s.getSlotNumber(), s.getSlotType(), s.getStatus());
            return true;
        } catch (Exception e) { return false; }
    }

    @Override
    public String getSummary() {
        return lotName + ": " + getTotalFreeSlots() + " free / " + getTotalSlots() + " total";
    }

    /** Initialize default layout: Rows A-C = Car (10 each), D-E = Bike (10 each) */
    public static ParkingLot createDefault() {
        ParkingLot lot = new ParkingLot("Smart ePark", "City Center, Block-5");
        int slotId = 1;
        String[] carRowLabels  = {"A", "B", "C"};
        String[] bikeRowLabels = {"D", "E"};
        for (String label : carRowLabels) {
            ParkingRow row = new ParkingRow(label, "Car Row " + label);
            for (int n = 1; n <= 10; n++) {
                row.addSlot(new ParkingSlot(slotId++, label, label + n,
                        ParkingSlot.SlotType.STANDARD, "car"));
            }
            lot.addRow(row);
        }
        for (String label : bikeRowLabels) {
            ParkingRow row = new ParkingRow(label, "Bike Row " + label);
            for (int n = 1; n <= 10; n++) {
                row.addSlot(new ParkingSlot(slotId++, label, label + n,
                        ParkingSlot.SlotType.STANDARD, "bike"));
            }
            lot.addRow(row);
        }
        return lot;
    }
}
