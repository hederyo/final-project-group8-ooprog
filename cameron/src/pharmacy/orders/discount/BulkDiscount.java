package pharmacy.orders.discount;

import pharmacy.common.util.Format;

/**
 * 10% volume discount strategy applicable strictly for purchases of 50 or more total units.
 *
 * Demonstrates:
 * - Strategy Pattern (GoF): Dynamic eligibility validation based on cart state.
 */
public class BulkDiscount implements DiscountStrategy {

    public static final double RATE = 0.10;
    public static final int MIN_UNITS = 50;

    @Override
    public DiscountType getType() {
        return DiscountType.BULK;
    }

    @Override
    public String getLabel() {
        return DiscountType.BULK.getLabel();
    }

    @Override
    public boolean isEligible(int totalUnits) {
        return totalUnits >= MIN_UNITS;
    }

    @Override
    public double computeDiscount(double subtotal, int totalUnits) {
        if (!isEligible(totalUnits)) {
            return 0.0;
        }
        return Format.round2(subtotal * RATE);
    }
}
