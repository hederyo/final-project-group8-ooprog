package pharmacy.common.exception;

/**
 * Thrown on authentication failures such as invalid credentials, deactivated accounts,
 * or accessing protected operations without being logged in.
 *
 * Demonstrates: Inheritance (extends PharmacyException).
 */
public class AuthenticationException extends PharmacyException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
