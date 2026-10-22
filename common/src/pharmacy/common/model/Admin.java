package pharmacy.common.model;

import pharmacy.common.ui.AdminMenu;
import pharmacy.common.ui.Menu;

/**
 * Represents an administrator account with access to system-wide management operations.
 *
 * Demonstrates:
 * - Inheritance: Extends User, inheriting identity and state while supplying admin behavior.
 * - Polymorphism: Overrides getRole(), createMenu() to return an AdminMenu, and toFileLine().
 */
public class Admin extends User {

    public Admin(String userId, String username, String password, String fullName,
                 String email, String contactNumber, AccountStatus status) {
        super(userId, username, password, fullName, email, contactNumber, status);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public Menu createMenu() {
        return new AdminMenu();
    }

    @Override
    public String toFileLine() {
        // Must preserve empty license column (column 7) to maintain 9 columns in users.txt
        return String.join("|",
                getId(),
                getRole().name(),
                getUsername(),
                getPassword(),
                getFullName(),
                getEmail(),
                getContactNumber(),
                "",
                getStatus().name()
        );
    }
}
