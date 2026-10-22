package pharmacy.auth.ui;

import pharmacy.auth.UserService;
import pharmacy.common.exception.PharmacyException;
import pharmacy.common.model.AccountStatus;
import pharmacy.common.model.Pharmacist;
import pharmacy.common.model.Role;
import pharmacy.common.model.User;
import pharmacy.common.ui.BaseMenu;
import pharmacy.common.util.ConsoleInput;
import pharmacy.common.util.InputValidator;

import java.util.List;

/**
 * Administrative console menu for managing staff accounts (A4).
 *
 * Demonstrates:
 * - Admin authorization: Calls UserService methods guarded with requireRole(ADMIN).
 * - Confirmation workflows: Enforces yes/no confirmation before deactivation and deletion (Rule A13).
 */
public class UserManagementMenu extends BaseMenu {

    private final UserService userService;

    public UserManagementMenu() {
        this.userService = new UserService();
    }

    @Override
    protected String getTitle() {
        return "Manage Users";
    }

    @Override
    protected String[] getOptions() {
        return new String[] {
            "List all users",
            "Search and filter users",
            "View user details",
            "Create user account",
            "Update user details",
            "Deactivate / Reactivate user",
            "Delete user"
        };
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> listAllUsers();
            case 2 -> searchAndFilterUsers();
            case 3 -> viewUserDetails();
            case 4 -> createUser();
            case 5 -> updateUser();
            case 6 -> toggleUserStatus();
            case 7 -> deleteUser();
        }
    }

    private void printUsersTable(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("\nNo user records found.");
            return;
        }

        System.out.println();
        System.out.printf("%-8s | %-12s | %-24s | %-12s | %-12s%n",
                "ID", "Username", "Full Name", "Role", "Status");
        System.out.println("-".repeat(78));

        for (User u : users) {
            System.out.printf("%-8s | %-12s | %-24s | %-12s | %-12s%n",
                    u.getId(),
                    u.getUsername(),
                    u.getFullName().length() > 24 ? u.getFullName().substring(0, 21) + "..." : u.getFullName(),
                    u.getRole().getLabel(),
                    u.getStatus().getLabel());
        }
    }

    private void listAllUsers() throws PharmacyException {
        List<User> users = userService.listUsers();
        printUsersTable(users);
        ConsoleInput.pause();
    }

    private void searchAndFilterUsers() throws PharmacyException {
        System.out.println("\n--- Search and Filter Users ---");
        String keyword = ConsoleInput.readOptional("Search keyword (name/username/license, Enter to skip): ");

        System.out.println("Filter by role: 0: All, 1: Administrator, 2: Pharmacist");
        int roleChoice = ConsoleInput.readInt("Choose role filter: ", 0, 2);
        Role roleFilter = switch (roleChoice) {
            case 1 -> Role.ADMIN;
            case 2 -> Role.PHARMACIST;
            default -> null;
        };

        System.out.println("Filter by status: 0: All, 1: Active, 2: Deactivated");
        int statusChoice = ConsoleInput.readInt("Choose status filter: ", 0, 2);
        AccountStatus statusFilter = switch (statusChoice) {
            case 1 -> AccountStatus.ACTIVE;
            case 2 -> AccountStatus.DEACTIVATED;
            default -> null;
        };

        List<User> results = userService.searchUsers(keyword, roleFilter, statusFilter);
        printUsersTable(results);
        ConsoleInput.pause();
    }

    private void viewUserDetails() throws PharmacyException {
        System.out.println("\n--- View User Details ---");
        String userId = ConsoleInput.readRequired("Enter User ID (e.g. U-0001): ");
        User user = userService.getUserById(userId);

        System.out.println("\nUser ID:        " + user.getId());
        System.out.println("Role:           " + user.getRole().getLabel());
        System.out.println("Full Name:      " + user.getFullName());
        System.out.println("Username:       " + user.getUsername());
        System.out.println("Email:          " + user.getEmail());
        System.out.println("Contact Number: " + user.getContactNumber());
        if (user instanceof Pharmacist p) {
            System.out.println("PRC License:    " + p.getLicenseNumber());
        }
        System.out.println("Status:         " + user.getStatus().getLabel());
        ConsoleInput.pause();
    }

    private void createUser() throws PharmacyException {
        System.out.println("\n--- Create Staff Account ---");
        System.out.println("Select account role:");
        System.out.println("1. Administrator");
        System.out.println("2. Pharmacist");
        int roleChoice = ConsoleInput.readInt("Choose role: ", 1, 2);
        Role role = (roleChoice == 1) ? Role.ADMIN : Role.PHARMACIST;

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

        String license = "";
        if (role == Role.PHARMACIST) {
            license = ConsoleInput.readValidated(
                    "PRC License number (7 digits): ",
                    InputValidator::requireLicenseNumber
            );
        }

        String username = ConsoleInput.readValidated(
                "Username (4-20 chars, start with letter): ",
                InputValidator::requireUsername
        );

        String password = ConsoleInput.readValidated(
                "Password (8-30 chars, uppercase, lowercase, digit): ",
                InputValidator::requirePassword
        );

        String confirmPassword = ConsoleInput.readRequired("Confirm password: ");

        User created = userService.createUser(role, fullName, email, contact, license, username, password, confirmPassword);
        System.out.println("\nStaff account created successfully for " + created.getFullName() + " (ID: " + created.getId() + ").");
        ConsoleInput.pause();
    }

    private void updateUser() throws PharmacyException {
        System.out.println("\n--- Update User Details ---");
        String userId = ConsoleInput.readRequired("Enter User ID to update: ");
        User user = userService.getUserById(userId);

        System.out.println("Updating " + user.getFullName() + " (" + user.getId() + ")");
        System.out.println("(Press Enter to keep current value)");

        String name = ConsoleInput.readOptional("Full name [" + user.getFullName() + "]: ");
        String email = ConsoleInput.readOptional("Email address [" + user.getEmail() + "]: ");
        String contact = ConsoleInput.readOptional("Contact number [" + user.getContactNumber() + "]: ");

        String license = "";
        if (user instanceof Pharmacist p) {
            license = ConsoleInput.readOptional("PRC License [" + p.getLicenseNumber() + "]: ");
        }

        userService.updateUser(userId, name, email, contact, license);
        System.out.println("\nUser details updated successfully.");
        ConsoleInput.pause();
    }

    private void toggleUserStatus() throws PharmacyException {
        System.out.println("\n--- Deactivate / Reactivate User ---");
        String userId = ConsoleInput.readRequired("Enter User ID: ");
        User user = userService.getUserById(userId);

        AccountStatus newStatus = (user.getStatus() == AccountStatus.ACTIVE)
                ? AccountStatus.DEACTIVATED
                : AccountStatus.ACTIVE;

        String action = (newStatus == AccountStatus.DEACTIVATED) ? "deactivate" : "reactivate";

        // Rule A13: Confirm before deactivate/reactivate
        boolean confirmed = ConsoleInput.readYesNo("Are you sure you want to " + action + " user " +
                user.getUsername() + " (" + user.getFullName() + ")? (y/n): ");

        if (!confirmed) {
            System.out.println("Operation cancelled.");
            ConsoleInput.pause();
            return;
        }

        userService.setStatus(userId, newStatus);
        System.out.println("\nUser " + user.getUsername() + " has been " + newStatus.getLabel().toLowerCase() + ".");
        ConsoleInput.pause();
    }

    private void deleteUser() throws PharmacyException {
        System.out.println("\n--- Delete User Account ---");
        String userId = ConsoleInput.readRequired("Enter User ID to permanently delete: ");
        User user = userService.getUserById(userId);

        // Rule A13: Confirm before delete
        boolean confirmed = ConsoleInput.readYesNo(
                "Are you sure you want to permanently delete user " + user.getUsername() +
                " (" + user.getFullName() + ")? (y/n): ");

        if (!confirmed) {
            System.out.println("Deletion cancelled.");
            ConsoleInput.pause();
            return;
        }

        userService.deleteUser(userId);
        System.out.println("\nUser " + userId + " (" + user.getUsername() + ") has been deleted.");
        ConsoleInput.pause();
    }
}
