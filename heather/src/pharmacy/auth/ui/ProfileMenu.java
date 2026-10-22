package pharmacy.auth.ui;

import pharmacy.auth.UserService;
import pharmacy.common.exception.PharmacyException;
import pharmacy.common.model.Pharmacist;
import pharmacy.common.model.User;
import pharmacy.common.ui.BaseMenu;
import pharmacy.common.util.ConsoleInput;
import pharmacy.common.util.InputValidator;

/**
 * Console menu for managing the logged-in user's profile and password.
 *
 * Demonstrates:
 * - Encapsulation & Security: Passwords are never displayed; sensitive changes require current password.
 * - Layered separation: Coordinates UI input through ConsoleInput and delegates logic to UserService.
 */
public class ProfileMenu extends BaseMenu {

    private final UserService userService;

    public ProfileMenu() {
        this.userService = new UserService();
    }

    @Override
    protected String getTitle() {
        return "My Profile";
    }

    @Override
    protected String[] getOptions() {
        return new String[] {
            "View my profile",
            "Update details",
            "Change password"
        };
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> viewProfile();
            case 2 -> updateDetails();
            case 3 -> changePassword();
        }
    }

    private void viewProfile() throws PharmacyException {
        User user = userService.getMyProfile();
        System.out.println("\n--- User Profile ---");
        System.out.println("User ID:        " + user.getId());
        System.out.println("Role:           " + user.getRole().getLabel());
        System.out.println("Full Name:      " + user.getFullName());
        System.out.println("Username:       " + user.getUsername());
        System.out.println("Email:          " + user.getEmail());
        System.out.println("Contact Number: " + user.getContactNumber());
        if (user instanceof Pharmacist p) {
            System.out.println("PRC License:    " + p.getLicenseNumber());
        }
        System.out.println("Account Status: " + user.getStatus().getLabel());
        ConsoleInput.pause();
    }

    private void updateDetails() throws PharmacyException {
        User user = userService.getMyProfile();
        System.out.println("\n--- Update Profile Details ---");
        System.out.println("(Press Enter to keep current value)");

        String name = ConsoleInput.readOptional("Full name [" + user.getFullName() + "]: ");
        String email = ConsoleInput.readOptional("Email address [" + user.getEmail() + "]: ");
        String contact = ConsoleInput.readOptional("Contact number [" + user.getContactNumber() + "]: ");

        userService.updateMyProfile(name, email, contact);
        System.out.println("\nProfile updated successfully.");
        ConsoleInput.pause();
    }

    private void changePassword() throws PharmacyException {
        System.out.println("\n--- Change Password ---");

        String currentPassword = ConsoleInput.readRequired("Current password: ");
        String newPassword = ConsoleInput.readValidated(
                "New password (8-30 chars, uppercase, lowercase, digit): ",
                InputValidator::requirePassword
        );
        String confirmPassword = ConsoleInput.readRequired("Confirm new password: ");

        userService.changeMyPassword(currentPassword, newPassword, confirmPassword);
        System.out.println("\nPassword changed successfully.");
        ConsoleInput.pause();
    }
}
