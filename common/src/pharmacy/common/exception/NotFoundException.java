package pharmacy.common.exception;

/**
 * Thrown when an entity cannot be found by its identifier or query criteria.
 *
 * Demonstrates: Inheritance (extends PharmacyException).
 */
public class NotFoundException extends PharmacyException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
