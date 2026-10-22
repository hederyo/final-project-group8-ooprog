package pharmacy.common.exception;

/**
 * Thrown when attempting to save a record with a unique field value that already exists.
 *
 * Demonstrates: Inheritance (extends PharmacyException).
 */
public class DuplicateRecordException extends PharmacyException {

    public DuplicateRecordException(String message) {
        super(message);
    }

    public DuplicateRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
