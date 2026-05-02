package interfaces;

/**
 * Interface for report generation.
 * Demonstrates ABSTRACTION through interface.
 */
public interface Reportable {
    String generateReport();
    boolean exportCSV(String filePath);
    String getSummary();
}
