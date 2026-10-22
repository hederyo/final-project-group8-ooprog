package pharmacy.common.session;

import pharmacy.common.exception.AccessDeniedException;
import pharmacy.common.exception.AuthenticationException;
import pharmacy.common.model.Role;
import pharmacy.common.model.User;

/**
 * Singleton holding the currently authenticated session state.
 *
 * Demonstrates:
 * - Singleton Pattern: Exactly one active user session exists in the application lifecycle.
 * - Encapsulation: currentUser reference is protected and guarded with role-based checks.
 */
public final class Session {

    private static final Session INSTANCE = new Session();
    private User currentUser;

    private Session() {
        // Enforce singleton
    }

    public static Session getInstance() {
        return INSTANCE;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return this.currentUser != null;
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Ensures an authenticated user exists, throwing an exception if not logged in.
     *
     * @return the currently logged-in user
     * @throws AuthenticationException if no user is logged in
     */
    public User requireUser() throws AuthenticationException {
        if (this.currentUser == null) {
            throw new AuthenticationException("Please log in first.");
        }
        return this.currentUser;
    }

    /**
     * Ensures the current user possesses the required role.
     *
     * @param role the required role
     * @throws AuthenticationException if not logged in
     * @throws AccessDeniedException   if the user lacks the role
     */
    public void requireRole(Role role) throws AuthenticationException, AccessDeniedException {
        User user = requireUser();
        if (user.getRole() != role) {
            throw new AccessDeniedException("This action is for " + role + " accounts only.");
        }
    }
}
