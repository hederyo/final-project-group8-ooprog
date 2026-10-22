package pharmacy.auth;

import pharmacy.common.exception.AuthenticationException;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.DuplicateRecordException;
import pharmacy.common.exception.ValidationException;
import pharmacy.common.model.AccountStatus;
import pharmacy.common.model.Pharmacist;
import pharmacy.common.model.User;
import pharmacy.common.util.InputValidator;

/**
 * Service orchestrating authentication and account registration workflows.
 *
 * Demonstrates:
 * - Service Layer Abstraction: Business logic isolated from persistence and UI presentation.
 * - Defensive validation: Re-validates all fields regardless of prior UI checks.
 */
public class AuthService {

    public static final int MAX_LOGIN_ATTEMPTS = 3;

    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticates a user using their username or email and password.
     *
     * @param usernameOrEmail entered username or email
     * @param password        entered password
     * @return authenticated User entity
     * @throws ValidationException     if inputs are blank
     * @throws AuthenticationException if credentials fail or account is deactivated
     * @throws DataAccessException     if file access fails
     */
    public User login(String usernameOrEmail, String password)
            throws ValidationException, AuthenticationException, DataAccessException {

        // Rule A1: Login fields must not be blank
        InputValidator.requireNonBlank("Username or email", usernameOrEmail);
        InputValidator.requireNonBlank("Password", password);

        User user = userRepository.findByLogin(usernameOrEmail);

        // Rule A2: Generic message for both unknown user and bad password
        if (user == null || !user.checkPassword(password)) {
            throw new AuthenticationException("Invalid username/email or password.");
        }

        // Rule A3: Deactivated accounts cannot log in
        if (!user.isActive()) {
            throw new AuthenticationException("This account is deactivated. Please contact an admin.");
        }

        return user;
    }

    /**
     * Registers a new staff pharmacist account.
     *
     * @return newly created Pharmacist record
     * @throws ValidationException      if any field fails validation or passwords mismatch
     * @throws DuplicateRecordException if username, email, or license is already in use
     * @throws DataAccessException      if persistence fails
     */
    public Pharmacist register(String fullName, String email, String contact, String license,
                               String username, String password, String confirmPassword)
            throws ValidationException, DuplicateRecordException, DataAccessException {

        // Format validation on every field
        String validName = InputValidator.requireName("Full name", fullName);
        String validEmail = InputValidator.requireEmail(email);
        String validContact = InputValidator.requirePhone(contact);
        String validLicense = InputValidator.requireLicenseNumber(license);
        String validUsername = InputValidator.requireUsername(username);
        String validPassword = InputValidator.requirePassword(password);

        // Rule A7: Password confirm match
        if (!validPassword.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        // Rule A5: Uniqueness checks (case-insensitive)
        if (userRepository.usernameTaken(validUsername, null)) {
            throw new DuplicateRecordException("Username \"" + validUsername + "\" is already taken.");
        }
        if (userRepository.emailTaken(validEmail, null)) {
            throw new DuplicateRecordException("Email \"" + validEmail + "\" is already registered.");
        }
        if (userRepository.licenseTaken(validLicense, null)) {
            throw new DuplicateRecordException("License number \"" + validLicense + "\" is already registered.");
        }

        // Rule A6: Self-registration always creates an ACTIVE Pharmacist
        String userId = userRepository.nextId();
        Pharmacist pharmacist = new Pharmacist(
                userId, validUsername, validPassword, validName,
                validEmail, validContact, validLicense, AccountStatus.ACTIVE
        );

        userRepository.save(pharmacist);
        return pharmacist;
    }
}
