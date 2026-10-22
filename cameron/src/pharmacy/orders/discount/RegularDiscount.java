package pharmacy.orders.discount;

/**
 * Standard regular pricing strategy offering zero discount.
 *
 * Demonstrates:
 * - Strategy Pattern (GoF): Null-object style concrete strategy representing default pricing.
 */
public class RegularDiscount implements DiscountStrategy {

    @Override
    public DiscountType getType() {
        return DiscountType.REGULAR;
    }

    @Override
    public String getLabel() {
        return DiscountType.REGULAR.getLabel();
    }

    @Override
    public boolean isEligible(int totalUnits) {
        return true;
    }

    @Override
    public double computeDiscount(double subtotal, int totalUnits) {
        return 0.0;
    }
}
