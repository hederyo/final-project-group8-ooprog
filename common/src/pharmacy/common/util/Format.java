package pharmacy.common.util;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Utility class for uniform date, time, and currency formatting across the application.
 */
public final class Format {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private Format() {
        // Utility class
    }

    /**
     * Formats a monetary amount using US locale formatting and the standard PHP prefix.
     *
     * @param amount the double monetary value
     * @return formatted string e.g. "PHP 1,234.50"
     */
    public static String peso(double amount) {
        return String.format(Locale.US, "PHP %,.2f", amount);
    }

    /**
     * Rounds a double value to two decimal places to avoid binary floating point anomalies.
     *
     * @param amount the unrounded value
     * @return value rounded to 2 decimal places
     */
    public static double round2(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
