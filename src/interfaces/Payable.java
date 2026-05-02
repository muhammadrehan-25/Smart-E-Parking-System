package interfaces;

/**
 * Interface for payment-related operations.
 * Demonstrates ABSTRACTION through interface.
 */
public interface Payable {
    double processPayment(double amount, String method);
    String generateReceipt(int bookingId);
    boolean applyDiscount(double discountPercent);
}
