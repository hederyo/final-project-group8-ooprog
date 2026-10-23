package pharmacy.orders.exception;

import pharmacy.common.exception.PharmacyException;

/**
 * Checked exception thrown when an illegal status change is attempted on an order.
 *
 * Demonstrates:
 * - Custom Checked Exceptions: Enforces explicit handling across layers per ARCHITECTURE §7.
 */
public class InvalidStatusTransitionException extends PharmacyException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
