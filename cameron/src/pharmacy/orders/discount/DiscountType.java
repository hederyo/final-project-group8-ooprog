package pharmacy.orders.discount;

/**
 * Enumeration of available discount types, serving as the factory mapping to concrete strategies.
 */
public enum DiscountType {
    REGULAR("Regular (0%)"),
    SENIOR_PWD("Senior Citizen / PWD (20%)"),
    BULK("Bulk Wholesale (10%)");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Factory method instantiating the appropriate DiscountStrategy for this type.
     */
    public DiscountStrategy toStrategy() {
        return switch (this) {
            case REGULAR -> new RegularDiscount();
            case SENIOR_PWD -> new SeniorPwdDiscount();
            case BULK -> new BulkDiscount();
        };
    }
}
