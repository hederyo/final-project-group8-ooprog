package pharmacy.orders.discount;

import pharmacy.common.util.Format;

/**
 * 20% discount strategy for Senior Citizens and Persons with Disabilities (PWD).
 *
 * Demonstrates:
 * - Strategy Pattern (GoF): Encapsulates Republic Act statutory discounts for vulnerable sectors.
 */
public class SeniorPwdDiscount implements DiscountStrategy {

    public static final double RATE = 0.20;

    @Override
    public DiscountType getType() {
        return DiscountType.SENIOR_PWD;
    }

    @Override
    public String getLabel() {
        return DiscountType.SENIOR_PWD.getLabel();
    }

    @Override
    public boolean isEligible(int totalUnits) {
        // Eligibility requires an ID verified by OrderService/ConsoleInput
        return true;
    }

    @Override
    public double computeDiscount(double subtotal, int totalUnits) {
        return Format.round2(subtotal * RATE);
    }
}
