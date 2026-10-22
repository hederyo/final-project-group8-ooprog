package pharmacy.common.util;

import pharmacy.common.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes input data according to system-wide format and business rules.
 * All require* methods return the cleaned (trimmed, lowercased where appropriate) value.
 */
public final class InputValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,19}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^09\\d{9}$");
    private static final Pattern LICENSE_PATTERN = Pattern.compile("^\\d{7}$");
    private static final Pattern PRESCRIPTION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{4,20}$");
    private static final Pattern SENIOR_PWD_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{4,20}$");
    private static final Pattern LOT_NUMBER_PATTERN = Pattern.compile("^[A-Za-z0-9-]{3,20}$");
    private static final Pattern DOSAGE_PATTERN = Pattern.compile(
            "^\\d+(?:\\.\\d+)?\\s*(?:mg|g|mcg|mL|ml|IU|%)(?:\\s*/\\s*\\d+(?:\\.\\d+)?\\s*(?:mg|g|mcg|mL|ml|IU|%)?)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PRICE_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    private InputValidator() {
        // Utility class
    }

    public static String requireNonBlank(String field, String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    public static String requireNoDelimiter(String field, String value) throws ValidationException {
        if (value != null && value.contains("|")) {
            throw new ValidationException(field + " cannot contain the \"|\" character.");
        }
        return value;
    }

    public static String requireText(String field, String value, int maxLength) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim();
        requireNoDelimiter(field, trimmed);
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    public static String requireName(String field, String value) throws ValidationException {
        String trimmed = requireNonBlank(field, value);
        requireNoDelimiter(field, trimmed);
        if (trimmed.length() < 2 || trimmed.length() > 60) {
            throw new ValidationException(field + " must be between 2 and 60 characters.");
        }
        boolean hasLetter = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (c != ' ' && c != '.' && c != '-' && c != '\'') {
                throw new ValidationException(field + " may contain only letters, spaces, . - '");
            }
        }
        if (!hasLetter) {
            throw new ValidationException(field + " must contain at least one letter.");
        }
        return trimmed;
    }

    public static String requireUsername(String value) throws ValidationException {
        String trimmed = requireNonBlank("Username", value);
        requireNoDelimiter("Username", trimmed);
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Username must be 4-20 letters, digits or _ and start with a letter.");
        }
        return trimmed;
    }

    public static String requirePassword(String value) throws ValidationException {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("Password is required.");
        }
        if (value.contains("|")) {
            throw new ValidationException("Password cannot contain the \"|\" character.");
        }
        if (value.contains(" ")) {
            throw new ValidationException("Password cannot contain spaces.");
        }
        if (value.length() < 8 || value.length() > 30) {
            throw new ValidationException("Password must be between 8 and 30 characters.");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper) {
            throw new ValidationException("Password needs at least one uppercase letter.");
        }
        if (!hasLower) {
            throw new ValidationException("Password needs at least one lowercase letter.");
        }
        if (!hasDigit) {
            throw new ValidationException("Password needs at least one digit.");
        }
        return value;
    }

    public static String requireEmail(String value) throws ValidationException {
        String trimmed = requireNonBlank("Email", value);
        requireNoDelimiter("Email", trimmed);
        if (trimmed.length() > 100 || !EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Enter a valid email, e.g. name@example.com.");
        }
        return trimmed.toLowerCase();
    }

    public static String requirePhone(String value) throws ValidationException {
        String trimmed = requireNonBlank("Contact number", value);
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Contact number must be 11 digits starting with 09.");
        }
        return trimmed;
    }

    public static String requireLicenseNumber(String value) throws ValidationException {
        String trimmed = requireNonBlank("License number", value);
        if (!LICENSE_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("License number must be exactly 7 digits.");
        }
        return trimmed;
    }

    public static String requirePrescriptionId(String value) throws ValidationException {
        String trimmed = requireNonBlank("Prescription ID", value);
        if (!PRESCRIPTION_ID_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Prescription ID must be 4-20 letters, digits or dashes.");
        }
        return trimmed;
    }

    public static String requireSeniorPwdId(String value) throws ValidationException {
        String trimmed = requireNonBlank("Senior/PWD ID", value);
        if (!SENIOR_PWD_ID_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Senior/PWD ID must be 4-20 letters, digits or dashes.");
        }
        return trimmed;
    }

    public static String requireLotNumber(String value) throws ValidationException {
        String trimmed = requireNonBlank("Lot number", value);
        if (!LOT_NUMBER_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Lot number must be 3-20 letters, digits or dashes.");
        }
        return trimmed;
    }

    public static String requireDosageStrength(String value) throws ValidationException {
        String trimmed = requireNonBlank("Dosage strength", value);
        requireNoDelimiter("Dosage strength", trimmed);
        if (!DOSAGE_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Dosage must look like 500mg or 250mg/5mL.");
        }
        return trimmed;
    }

    public static double requirePrice(String value) throws ValidationException {
        String trimmed = requireNonBlank("Price", value);
        if (!PRICE_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Price must be a valid positive amount with up to 2 decimal places.");
        }
        try {
            double price = Double.parseDouble(trimmed);
            if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0) {
                throw new ValidationException("Price must be greater than 0.");
            }
            if (price > 100000.0) {
                throw new ValidationException("Price must be at most 100,000.");
            }
            return Format.round2(price);
        } catch (NumberFormatException e) {
            throw new ValidationException("Price must be a valid number.");
        }
    }

    public static int requireQuantity(String field, String value, int min, int max) throws ValidationException {
        String trimmed = requireNonBlank(field, value);
        try {
            int qty = Integer.parseInt(trimmed);
            if (qty < min || qty > max) {
                throw new ValidationException(field + " must be between " + min + " and " + max + ".");
            }
            return qty;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a whole number between " + min + " and " + max + ".");
        }
    }

    public static LocalDate requireDate(String value) throws ValidationException {
        String trimmed = requireNonBlank("Date", value);
        try {
            return LocalDate.parse(trimmed, Format.DATE);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Enter a real date as yyyy-MM-dd.");
        }
    }

    public static LocalDate requireFutureDate(String field, LocalDate date) throws ValidationException {
        if (date == null) {
            throw new ValidationException(field + " is required.");
        }
        if (!date.isAfter(LocalDate.now())) {
            throw new ValidationException(field + " must be after today.");
        }
        return date;
    }

    public static void requireDateRange(LocalDate from, LocalDate to) throws ValidationException {
        if (from == null || to == null) {
            throw new ValidationException("Both start date and end date are required.");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("Start date must be on or before end date.");
        }
    }
}
