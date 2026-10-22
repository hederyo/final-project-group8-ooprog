package pharmacy.common.exception;

/**
 * Thrown when an authenticated user attempts to perform an operation not permitted for their role.
 *
 * Demonstrates: Inheritance (extends PharmacyException).
 */
public class AccessDeniedException extends PharmacyException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
