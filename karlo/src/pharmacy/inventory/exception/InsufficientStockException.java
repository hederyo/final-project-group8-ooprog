package pharmacy.inventory.exception;

import pharmacy.common.exception.PharmacyException;

/**
 * Thrown when an order or reservation requests more medicine units than currently in stock.
 *
 * Demonstrates:
 * - Inheritance: Subclasses custom checked PharmacyException to force explicit handling.
 */
public class InsufficientStockException extends PharmacyException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
