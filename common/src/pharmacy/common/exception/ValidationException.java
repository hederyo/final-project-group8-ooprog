package pharmacy.common.exception;

/**
 * Thrown when an input format or business validation rule is violated.
 *
 * Demonstrates: Inheritance (extends PharmacyException).
 */
public class ValidationException extends PharmacyException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
