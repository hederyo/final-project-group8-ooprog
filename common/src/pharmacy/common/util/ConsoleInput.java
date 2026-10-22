package pharmacy.common.util;

import pharmacy.common.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Handles all interactive console input from System.in.
 *
 * Demonstrates:
 * - Robust input handling: Wraps all parsing in retry loops to prevent crashes from hostile inputs.
 * - Resource safety: Holds a single Scanner on System.in that is never closed, preventing System.in shutdown.
 */
public final class ConsoleInput {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    private ConsoleInput() {
        // Utility class
    }

    private static String nextRawLine() {
        try {
            return SCANNER.nextLine();
        } catch (NoSuchElementException e) {
            // Documented exception: graceful exit on stdin closure/piping EOF
            System.out.println("\nInput closed. Exiting.");
            System.exit(0);
            return "";
        }
    }

    /**
     * Reads a mandatory non-blank line that does not contain the delimiter '|'.
     */
    public static String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = nextRawLine().trim();
            if (input.isEmpty()) {
                System.out.println("  x This field cannot be blank.");
                continue;
            }
            if (input.contains("|")) {
                System.out.println("  x Input cannot contain the \"|\" character.");
                continue;
            }
            return input;
        }
    }

    /**
     * Reads an optional line. Pressing Enter returns an empty string "".
     */
    public static String readOptional(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = nextRawLine().trim();
            if (input.isEmpty()) {
                return "";
            }
            if (input.contains("|")) {
                System.out.println("  x Input cannot contain the \"|\" character.");
                continue;
            }
            return input;
        }
    }

    /**
     * Reads a line and validates it against the provided InputRule until valid.
     */
    public static String readValidated(String prompt, InputRule rule) {
        while (true) {
            System.out.print(prompt);
            String input = nextRawLine().trim();
            try {
                rule.check(input);
                return input;
            } catch (ValidationException e) {
                System.out.println("  x " + e.getMessage());
            }
        }
    }

    /**
     * Reads an integer within [min, max], handling non-numeric and huge number overflows.
     */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = nextRawLine().trim();
            if (input.isEmpty()) {
                System.out.println("  x Input is required.");
                continue;
            }
            try {
                int val = Integer.parseInt(input);
                if (val < min || val > max) {
                    System.out.println("  x Please enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("  x Please enter a valid whole number.");
            }
        }
    }

    /**
     * Reads a monetary double within [min, max], rejecting NaN, Infinity, scientific notation,
     * and more than 2 decimal places.
     */
    public static double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String input = nextRawLine().trim();
            if (input.isEmpty()) {
                System.out.println("  x Input is required.");
                continue;
            }
            if (!DOUBLE_PATTERN.matcher(input).matches()) {
                System.out.println("  x Please enter a valid decimal number with up to 2 decimal places.");
                continue;
            }
            try {
                double val = Double.parseDouble(input);
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    System.out.println("  x Please enter a valid number.");
                    continue;
                }
                if (val < min || val > max) {
                    System.out.println("  x Please enter an amount between " + min + " and " + max + ".");
                    continue;
                }
                return Format.round2(val);
            } catch (NumberFormatException e) {
                System.out.println("  x Please enter a valid number.");
            }
        }
    }

    /**
     * Reads a real calendar date in strict yyyy-MM-dd format.
     */
    public static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = nextRawLine().trim();
            if (input.isEmpty()) {
                System.out.println("  x Date is required.");
                continue;
            }
            try {
                return LocalDate.parse(input, Format.DATE);
            } catch (DateTimeParseException e) {
                System.out.println("  x Enter a real date as yyyy-MM-dd (e.g. 2026-12-31).");
            }
        }
    }

    /**
     * Reads a yes/no response (y, yes, n, no; case-insensitive).
     */
    public static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = nextRawLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            }
            if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("  ✗ Please answer 'y' or 'n'.");
        }
    }

    /**
     * Prompts the user to press Enter to continue navigation.
     */
    public static void pause() {
        System.out.print("\nPress Enter to continue...");
        nextRawLine();
    }
}
