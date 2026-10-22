package pharmacy.common.ui;

import pharmacy.common.exception.PharmacyException;
import pharmacy.common.util.ConsoleInput;

/**
 * Abstract template for all console menu screens.
 *
 * Demonstrates:
 * - Abstraction: Outlines the template method for menu presentation and execution.
 * - Exception Handling: Centrally catches PharmacyException from handlers, printing readable errors.
 * - Polymorphism: Each menu subclass defines its own title, options list, and handle logic.
 */
public abstract class BaseMenu implements Menu {

    protected abstract String getTitle();

    protected abstract String[] getOptions();

    protected abstract void handle(int choice) throws PharmacyException;

    protected String getExitLabel() {
        return "Back";
    }

    @Override
    public final void show() {
        while (true) {
            System.out.println();
            System.out.println("=== " + getTitle() + " ===");
            String[] options = getOptions();
            for (int i = 0; i < options.length; i++) {
                System.out.println((i + 1) + ". " + options[i]);
            }
            System.out.println("0. " + getExitLabel());

            int choice = ConsoleInput.readInt("Choose an option: ", 0, options.length);
            if (choice == 0) {
                return;
            }

            try {
                handle(choice);
            } catch (PharmacyException e) {
                System.out.println("\nError: " + e.getMessage());
                ConsoleInput.pause();
            }
        }
    }
}
