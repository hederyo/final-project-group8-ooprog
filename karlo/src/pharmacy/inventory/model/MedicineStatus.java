package pharmacy.inventory.model;

/**
 * Lifecycle status of a medicine record.
 */
public enum MedicineStatus {
    ACTIVE("Active"),
    DISCONTINUED("Discontinued");

    private final String label;

    MedicineStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
