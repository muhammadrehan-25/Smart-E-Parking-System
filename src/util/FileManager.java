package util;

import model.Booking;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handles all FILE I/O operations.
 * Satisfies OOP project DATA PERSISTENCE requirement.
 */
public class FileManager {
    public static final String BASE_DIR    = System.getProperty("user.home") + java.io.File.separator + ".smartepark" + java.io.File.separator;
    public static final String DATA_DIR    = BASE_DIR + "data/";
    public static final String RECEIPT_DIR = BASE_DIR + "data/receipts/";
    public static final String LOG_DIR     = BASE_DIR + "data/logs/";
    public static final String REPORT_DIR  = BASE_DIR + "data/reports/";

    static {
        new File(DATA_DIR).mkdirs();
        new File(RECEIPT_DIR).mkdirs();
        new File(LOG_DIR).mkdirs();
        new File(REPORT_DIR).mkdirs();
    }

    public static String saveReceipt(Booking b) {
        String filename = RECEIPT_DIR + "receipt_" + b.getConfCode() + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.print(b.generateReceipt(b.getBookingId()));
            return filename;
        } catch (IOException e) {
            System.err.println("Receipt save error: " + e.getMessage());
            return null;
        }
    }

    public static void logEvent(String user, String action) {
        String date     = LocalDate.now().toString();
        String filename = LOG_DIR + "parking_log_" + date + ".txt";
        try (FileWriter fw = new FileWriter(filename, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    + " | " + user + " | " + action);
        } catch (IOException e) { System.err.println("Log write error: " + e.getMessage()); }
    }

    public static String exportBookingsCSV(List<Booking> bookings) {
        String filename = REPORT_DIR + "revenue_report_" + LocalDate.now() + ".csv";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        double totalRev = 0, carRev = 0, bikeRev = 0, totalTax = 0;
        int carCount = 0, bikeCount = 0;

        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("PARKNOVA ADVANCED REVENUE REPORT");
            pw.println("Generated on: " + LocalDateTime.now().format(fmt));
            pw.println();
            pw.println("BookingID,Plate,Type,Slot,CheckIn,CheckOut,Hours,BaseAmount,Tax,Total,Status");
            
            for (Booking b : bookings) {
                String in  = b.getCheckIn()  != null ? b.getCheckIn().format(fmt)  : "-";
                String out = b.getCheckOut() != null ? b.getCheckOut().format(fmt) : "-";
                
                double tax = b.getTotalAmount() * 0.1; // Simple fallback tax calc for report
                double base = b.getTotalAmount() - tax;
                
                pw.printf("%s,%s,%s,%s,%s,%s,%.1f,%.0f,%.0f,%.0f,%s%n",
                    b.getConfCode(), b.getVehiclePlate(), b.getVehicleType(), 
                    b.getSlotNumber(), in, out, b.getTotalHours(), 
                    base, tax, b.getTotalAmount(), b.getStatus());
                
                totalRev += b.getTotalAmount();
                totalTax += tax;
                if ("car".equalsIgnoreCase(b.getVehicleType())) {
                    carRev += b.getTotalAmount();
                    carCount++;
                } else {
                    bikeRev += b.getTotalAmount();
                    bikeCount++;
                }
            }
            
            pw.println();
            pw.println("--- REVENUE SUMMARY ---");
            pw.println("Total Bookings," + bookings.size());
            pw.println("Car Bookings," + carCount);
            pw.println("Bike Bookings," + bikeCount);
            pw.println("Total Base Revenue,Rs. " + String.format("%.0f", totalRev - totalTax));
            pw.println("Total Tax Collected,Rs. " + String.format("%.0f", totalTax));
            pw.println("GRAND TOTAL REVENUE,Rs. " + String.format("%.0f", totalRev));
            
            return filename;
        } catch (IOException e) {
            System.err.println("CSV error: " + e.getMessage());
            return null;
        }
    }

    public static String backupDatabase() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File backupDir = new File(BASE_DIR + "data/backups");
        if (!backupDir.exists()) backupDir.mkdirs();
        
        File source = new File(BASE_DIR + "data/epark.db");
        if (!source.exists()) source = new File(BASE_DIR + "data/parking.db"); // Fallback check
        
        String destPath = BASE_DIR + "data/backups/parknova_backup_" + timestamp + ".db";
        File dest = new File(destPath);
        
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            return destPath;
        } catch (IOException e) {
            System.err.println("Backup error: " + e.getMessage());
            return null;
        }
    }

    public static boolean serializeObject(Object obj, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(DATA_DIR + filename))) {
            oos.writeObject(obj); return true;
        } catch (IOException e) {
            System.err.println("Serialize error: " + e.getMessage()); return false;
        }
    }

    public static Object deserializeObject(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(DATA_DIR + filename))) {
            return ois.readObject();
        } catch (Exception e) {
            System.err.println("Deserialize error: " + e.getMessage()); return null;
        }
    }

    public static String readTextFile(String path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line; while ((line = br.readLine()) != null) sb.append(line).append("\n");
        } catch (IOException e) { return "Error reading file: " + e.getMessage(); }
        return sb.toString();
    }
}
