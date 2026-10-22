package pharmacy.common.model;

/**
 * Enumeration of system user roles.
 */
public enum Role {
    ADMIN("Administrator"),
    PHARMACIST("Pharmacist");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
