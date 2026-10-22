package pharmacy.auth;

import pharmacy.common.exception.AccessDeniedException;
import pharmacy.common.exception.AuthenticationException;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.DuplicateRecordException;
import pharmacy.common.exception.NotFoundException;
import pharmacy.common.exception.ValidationException;
import pharmacy.common.model.AccountStatus;
import pharmacy.common.model.Admin;
import pharmacy.common.model.Pharmacist;
import pharmacy.common.model.Role;
import pharmacy.common.model.User;
import pharmacy.common.session.Session;
import pharmacy.common.util.InputValidator;
import pharmacy.orders.OrderService;

import java.util.ArrayList;
import java.util.List;

/**
 * Service managing user profile operations and administrator user management.
 *
 * Demonstrates:
 * - Session-based authorization: Profile operations use Session.requireUser(); admin operations guard with requireRole(ADMIN).
 * - Business rule enforcement: Guarantees active admin minimums and soft-delete/deactivation constraints.
 */
public class UserService {

    private final UserRepository userRepository;
    private final OrderService orderService;

    public UserService() {
        this.userRepository = new UserRepository();
        this.orderService = new OrderService();
    }

    public UserService(UserRepository userRepository, OrderService orderService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    // ==========================================
    // Profile Management (A3) — Available to all
    // ==========================================

    /**
     * Retrieves the profile of the currently logged-in user.
     * Rule A9: Profile is always obtained from Session, never by typing an ID.
     */
    public User getMyProfile() throws AuthenticationException {
        return Session.getInstance().requireUser();
    }

    /**
     * Updates the logged-in user's profile details. Blank inputs retain the current value.
     */
    public void updateMyProfile(String fullName, String email, String contact)
            throws AuthenticationException, ValidationException, DuplicateRecordException, DataAccessException {

        User currentUser = Session.getInstance().requireUser();

        if (fullName != null && !fullName.trim().isEmpty()) {
            String validName = InputValidator.requireName("Full name", fullName);
            currentUser.setFullName(validName);
        }

        if (email != null && !email.trim().isEmpty()) {
            String validEmail = InputValidator.requireEmail(email);
            // Rule A5: Uniqueness excludes user's own record
            if (userRepository.emailTaken(validEmail, currentUser.getId())) {
                throw new DuplicateRecordException("Email \"" + validEmail + "\" is already in use.");
            }
            currentUser.setEmail(validEmail);
        }

        if (contact != null && !contact.trim().isEmpty()) {
            String validContact = InputValidator.requirePhone(contact);
            currentUser.setContactNumber(validContact);
        }

        userRepository.save(currentUser);
    }

    /**
     * Changes the logged-in user's password after verifying their current password.
     */
    public void changeMyPassword(String currentPassword, String newPassword, String confirmPassword)
            throws AuthenticationException, ValidationException, DataAccessException {

        User currentUser = Session.getInstance().requireUser();

        // Rule A8: Must verify current password
        if (!currentUser.checkPassword(currentPassword)) {
            throw new AuthenticationException("Current password is incorrect.");
        }

        // Validate new password rules
        String validNewPass = InputValidator.requirePassword(newPassword);

        // Rule A8: New password must differ from current
        if (validNewPass.equals(currentPassword)) {
            throw new ValidationException("New password must be different from current password.");
        }

        // Rule A7: New password and confirm password must match
        if (!validNewPass.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        currentUser.setPassword(validNewPass);
        userRepository.save(currentUser);
    }

    // ==========================================
    // User Management (A4) — Administrator Only
    // ==========================================

    private void requireAdmin() throws AuthenticationException, AccessDeniedException {
        // Rule A10: Admin-only actions require Role.ADMIN
        Session.getInstance().requireRole(Role.ADMIN);
    }

    public List<User> listUsers() throws AuthenticationException, AccessDeniedException, DataAccessException {
        requireAdmin();
        return userRepository.findAll();
    }

    public User getUserById(String userId)
            throws AuthenticationException, AccessDeniedException, NotFoundException, DataAccessException {
        requireAdmin();
        return userRepository.findById(userId);
    }

    /**
     * Searches and filters users by keyword (partial username, name, or license) and optional role/status.
     */
    public List<User> searchUsers(String keyword, Role roleFilter, AccountStatus statusFilter)
            throws AuthenticationException, AccessDeniedException, DataAccessException {
        requireAdmin();
        List<User> all = userRepository.findAll();
        List<User> results = new ArrayList<>();
        String query = keyword == null ? "" : keyword.trim().toLowerCase();

        for (User user : all) {
            if (roleFilter != null && user.getRole() != roleFilter) {
                continue;
            }
            if (statusFilter != null && user.getStatus() != statusFilter) {
                continue;
            }
            if (!query.isEmpty()) {
                boolean matchName = user.getFullName().toLowerCase().contains(query);
                boolean matchUser = user.getUsername().toLowerCase().contains(query);
                boolean matchLicense = (user instanceof Pharmacist p && p.getLicenseNumber() != null &&
                        p.getLicenseNumber().toLowerCase().contains(query));
                if (!matchName && !matchUser && !matchLicense) {
                    continue;
                }
            }
            results.add(user);
        }
        return results;
    }

    /**
     * Admin creates a new staff account (Admin or Pharmacist).
     */
    public User createUser(Role role, String fullName, String email, String contact, String license,
                           String username, String password, String confirmPassword)
            throws AuthenticationException, AccessDeniedException, ValidationException, DuplicateRecordException, DataAccessException {

        requireAdmin();

        String validName = InputValidator.requireName("Full name", fullName);
        String validEmail = InputValidator.requireEmail(email);
        String validContact = InputValidator.requirePhone(contact);
        String validUsername = InputValidator.requireUsername(username);
        String validPassword = InputValidator.requirePassword(password);

        if (!validPassword.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        if (userRepository.usernameTaken(validUsername, null)) {
            throw new DuplicateRecordException("Username \"" + validUsername + "\" is already taken.");
        }
        if (userRepository.emailTaken(validEmail, null)) {
            throw new DuplicateRecordException("Email \"" + validEmail + "\" is already registered.");
        }

        String userId = userRepository.nextId();
        User newUser;

        if (role == Role.ADMIN) {
            newUser = new Admin(userId, validUsername, validPassword, validName, validEmail, validContact, AccountStatus.ACTIVE);
        } else {
            String validLicense = InputValidator.requireLicenseNumber(license);
            if (userRepository.licenseTaken(validLicense, null)) {
                throw new DuplicateRecordException("License number \"" + validLicense + "\" is already registered.");
            }
            newUser = new Pharmacist(userId, validUsername, validPassword, validName, validEmail, validContact, validLicense, AccountStatus.ACTIVE);
        }

        userRepository.save(newUser);
        return newUser;
    }

    /**
     * Admin updates a user's details. Blank inputs retain existing values.
     */
    public void updateUser(String userId, String fullName, String email, String contact, String license)
            throws AuthenticationException, AccessDeniedException, NotFoundException, ValidationException, DuplicateRecordException, DataAccessException {

        requireAdmin();
        User target = userRepository.findById(userId);

        if (fullName != null && !fullName.trim().isEmpty()) {
            target.setFullName(InputValidator.requireName("Full name", fullName));
        }

        if (email != null && !email.trim().isEmpty()) {
            String validEmail = InputValidator.requireEmail(email);
            if (userRepository.emailTaken(validEmail, target.getId())) {
                throw new DuplicateRecordException("Email \"" + validEmail + "\" is already in use.");
            }
            target.setEmail(validEmail);
        }

        if (contact != null && !contact.trim().isEmpty()) {
            target.setContactNumber(InputValidator.requirePhone(contact));
        }

        if (target instanceof Pharmacist p && license != null && !license.trim().isEmpty()) {
            String validLicense = InputValidator.requireLicenseNumber(license);
            if (userRepository.licenseTaken(validLicense, target.getId())) {
                throw new DuplicateRecordException("License number \"" + validLicense + "\" is already in use.");
            }
            p.setLicenseNumber(validLicense);
        }

        userRepository.save(target);
    }

    /**
     * Admin deactivates or reactivates a user account.
     */
    public void setStatus(String userId, AccountStatus newStatus)
            throws AuthenticationException, AccessDeniedException, NotFoundException, ValidationException, DataAccessException {

        requireAdmin();
        User currentUser = Session.getInstance().requireUser();
        User target = userRepository.findById(userId);

        if (newStatus == AccountStatus.DEACTIVATED) {
            // Rule A11: Cannot deactivate yourself
            if (target.getId().equalsIgnoreCase(currentUser.getId())) {
                throw new ValidationException("You cannot deactivate your own account.");
            }

            // Rule A11: Last active admin cannot be deactivated
            if (target.getRole() == Role.ADMIN && target.getStatus() == AccountStatus.ACTIVE) {
                int activeAdminCount = 0;
                for (User u : userRepository.findAll()) {
                    if (u.getRole() == Role.ADMIN && u.getStatus() == AccountStatus.ACTIVE) {
                        activeAdminCount++;
                    }
                }
                if (activeAdminCount <= 1) {
                    throw new ValidationException("Cannot deactivate the last active administrator.");
                }
            }
        }

        target.setStatus(newStatus);
        userRepository.save(target);
    }

    /**
     * Admin deletes a user account.
     */
    public void deleteUser(String userId)
            throws AuthenticationException, AccessDeniedException, NotFoundException, ValidationException, DataAccessException {

        requireAdmin();
        User currentUser = Session.getInstance().requireUser();
        User target = userRepository.findById(userId);

        // Rule A11: Cannot delete yourself
        if (target.getId().equalsIgnoreCase(currentUser.getId())) {
            throw new ValidationException("You cannot delete your own account.");
        }

        // Rule A12: Admins can only be deactivated, never deleted (ADR-012)
        if (target.getRole() == Role.ADMIN) {
            throw new ValidationException("Administrator accounts cannot be deleted. Deactivate the account instead.");
        }

        // Rule A12: Pharmacists with order history can only be deactivated, never deleted
        if (target.getRole() == Role.PHARMACIST) {
            if (orderService.hasOrdersForPharmacist(target.getId())) {
                throw new ValidationException(
                        "Pharmacist has existing order records and cannot be deleted. Deactivate the account instead.");
            }
        }

        userRepository.delete(target.getId());
    }
}
