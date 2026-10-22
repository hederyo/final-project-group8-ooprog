package pharmacy.common.exception;

/**
 * Abstract checked base exception for all custom domain exceptions in the application.
 * Forces callers to handle business and data errors explicitly.
 *
 * Demonstrates: Inheritance (extends Exception) and Abstraction (abstract base).
 */
public abstract class PharmacyException extends Exception {

    public PharmacyException(String message) {
        super(message);
    }

    public PharmacyException(String message, Throwable cause) {
        super(message, cause);
    }
}
