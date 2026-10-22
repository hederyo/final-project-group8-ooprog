package pharmacy.auth.ui;

import pharmacy.auth.AuthService;
import pharmacy.common.exception.AuthenticationException;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.DuplicateRecordException;
import pharmacy.common.exception.ValidationException;
import pharmacy.common.model.User;
import pharmacy.common.session.Session;
import pharmacy.common.util.ConsoleInput;
import pharmacy.common.util.InputValidator;

/**
 * Handles console screens for user Login (A2) and self-service Pharmacist Registration (A1).
 *
 * Demonstrates:
 * - UI input validation: Uses ConsoleInput.readValidated to assist users interactively.
 * - Lockout policy enforcement: Tracks consecutive failed login attempts up to MAX_LOGIN_ATTEMPTS.
 */
public class AuthMenu {

    private final AuthService authService;

    public AuthMenu() {
        this.authService = new AuthService();
    }

    /**
     * Executes the login workflow with a 3-attempt lockout limit.
     */
    public void login() {
        int attempts = 0;

        System.out.println("\n--- Login ---");
        while (attempts < AuthService.MAX_LOGIN_ATTEMPTS) {
            String loginId = ConsoleInput.readRequired("Username or email: ");
            String password = ConsoleInput.readRequired("Password: ");

            try {
                User user = authService.login(loginId, password);
                Session.getInstance().login(user);
                System.out.println("\nWelcome, " + user.getFullName() + " (" + user.getRole().getLabel() + ")");
                return;
            } catch (AuthenticationException | ValidationException e) {
                attempts++;
                System.out.println("  ✗ " + e.getMessage());
                if (attempts < AuthService.MAX_LOGIN_ATTEMPTS) {
                    System.out.println("  (" + (AuthService.MAX_LOGIN_ATTEMPTS - attempts) + " attempt(s) remaining)");
                }
            } catch (DataAccessException e) {
                System.out.println("\nError accessing user data: " + e.getMessage());
                ConsoleInput.pause();
                return;
            }
        }

        // Rule A4: 3 failed attempts returns to welcome screen
        System.out.println("\nToo many failed login attempts. Returning to welcome menu.");
        ConsoleInput.pause();
    }

    /**
     * Executes the pharmacist self-registration workflow.
     */
    public void register() {
        System.out.println("\n--- Pharmacist Registration ---");

        String fullName = ConsoleInput.readValidated(
                "Full name (2-60 letters): ",
                val -> InputValidator.requireName("Full name", val)
        );

        String email = ConsoleInput.readValidated(
                "Email address: ",
                InputValidator::requireEmail
        );

        String contact = ConsoleInput.readValidated(
                "Contact number (09XXXXXXXXX): ",
                InputValidator::requirePhone
        );

        String license = ConsoleInput.readValidated(
                "PRC License number (7 digits): ",
                InputValidator::requireLicenseNumber
        );

        String username = ConsoleInput.readValidated(
                "Username (4-20 chars, start with letter): ",
                InputValidator::requireUsername
        );

        String password = ConsoleInput.readValidated(
                "Password (8-30 chars, uppercase, lowercase, digit): ",
                InputValidator::requirePassword
        );

        String confirmPassword = ConsoleInput.readRequired("Confirm password: ");

        try {
            authService.register(fullName, email, contact, license, username, password, confirmPassword);
            System.out.println("\nRegistration successful! Please log in with your credentials.");
            ConsoleInput.pause();
        } catch (ValidationException | DuplicateRecordException e) {
            System.out.println("\nRegistration failed: " + e.getMessage());
            ConsoleInput.pause();
        } catch (DataAccessException e) {
            System.out.println("\nData storage error: " + e.getMessage());
            ConsoleInput.pause();
        }
    }
}
