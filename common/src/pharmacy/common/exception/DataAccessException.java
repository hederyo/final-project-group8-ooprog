package pharmacy.common.exception;

/**
 * Thrown when a file access or persistence error occurs (e.g. wrapping an IOException).
 *
 * Demonstrates: Inheritance (extends PharmacyException).
 */
public class DataAccessException extends PharmacyException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
