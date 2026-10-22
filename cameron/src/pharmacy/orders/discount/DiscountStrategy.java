package pharmacy.orders.discount;

/**
 * Strategy pattern interface defining discount calculation algorithms for orders.
 *
 * Demonstrates:
 * - Strategy Pattern (GoF): Encapsulates different discount algorithms behind a common contract.
 * - Abstraction: The Order model interacts with this interface without coupling to concrete discount rules.
 */
public interface DiscountStrategy {

    /**
     * Identifies the category of this discount.
     */
    DiscountType getType();

    /**
     * User-facing label displayed in summaries and menus.
     */
    String getLabel();

    /**
     * Checks if the given total units qualify for this discount policy.
     */
    boolean isEligible(int totalUnits);

    /**
     * Computes the total peso discount amount, rounded to two decimal places.
     */
    double computeDiscount(double subtotal, int totalUnits);
}
