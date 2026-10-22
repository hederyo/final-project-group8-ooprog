package pharmacy.orders.model;

/**
 * State machine enumeration controlling lifecycle transitions of an order.
 *
 * Demonstrates:
 * - State pattern / Transition enforcement: Centralizes valid lifecycle changes to guarantee data integrity.
 */
public enum OrderStatus {
    PENDING("Pending"),
    ON_HOLD("On Hold"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    VOIDED("Voided");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Checks if this status can legitimately transition to the requested next status.
     * Enforces the status matrix defined in ARCHITECTURE §9.1.
     */
    public boolean canTransitionTo(OrderStatus next) {
        if (next == null || next == this) {
            return false;
        }

        return switch (this) {
            case PENDING -> next == ON_HOLD || next == COMPLETED || next == CANCELLED;
            case ON_HOLD -> next == PENDING || next == CANCELLED;
            case COMPLETED -> next == VOIDED;
            case CANCELLED, VOIDED -> false;
        };
    }

    /**
     * True if the order's items, quantities, or notes can still be modified.
     */
    public boolean isEditable() {
        return this == PENDING || this == ON_HOLD;
    }

    /**
     * True if the order is in a final, immutable terminal state.
     */
    public boolean isTerminal() {
        return this == CANCELLED || this == VOIDED;
    }
}
