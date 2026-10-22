package pharmacy.common.model;

/**
 * Enumeration of user account operational statuses.
 */
public enum AccountStatus {
    ACTIVE("Active"),
    DEACTIVATED("Deactivated");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
