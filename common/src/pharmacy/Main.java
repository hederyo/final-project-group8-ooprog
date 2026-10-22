package pharmacy;

import pharmacy.auth.ui.AuthMenu;
import pharmacy.common.data.DataStore;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.model.User;
import pharmacy.common.session.Session;
import pharmacy.common.util.ConsoleInput;

/**
 * Main application entry point for the Pharmacy Management System.
 *
 * Demonstrates:
 * - Program bootstrap: Seeds default storage on startup.
 * - Polymorphism: Calls user.createMenu().show(), launching the appropriate menu dynamically by role.
 * - Robustness: Implements a global safety net for unhandled runtime exceptions.
 */
public class Main {

    public static void main(String[] args) {
        // Step 1: Initialize data store and seed defaults
        try {
            DataStore.getInstance().seedDefaults();
        } catch (DataAccessException e) {
            System.err.println("Fatal: Failed to initialize data storage: " + e.getMessage());
            System.exit(1);
        }

        AuthMenu authMenu = new AuthMenu();

        // Step 2: Main welcome loop
        while (true) {
            System.out.println();
            System.out.println("=== Pharmacy Management System ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");

            int choice = ConsoleInput.readInt("Choose an option: ", 0, 2);
            if (choice == 0) {
                System.out.println("\nThank you for using Pharmacy Management System. Goodbye!");
                break;
            }

            try {
                if (choice == 1) {
                    authMenu.login();
                    if (Session.getInstance().isLoggedIn()) {
                        User user = Session.getInstance().getCurrentUser();
                        // Polymorphism: Launches AdminMenu or PharmacistMenu based on actual User subclass
                        user.createMenu().show();
                        Session.getInstance().logout();
                        System.out.println("\nYou have been logged out.");
                    }
                } else if (choice == 2) {
                    authMenu.register();
                }
            } catch (RuntimeException e) {
                System.out.println("\nUnexpected error: " + e.getMessage());
                Session.getInstance().logout();
            }
        }
    }
}
