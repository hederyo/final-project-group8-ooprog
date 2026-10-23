package pharmacy.inventory.model;

/**
 * Standard pharmaceutical categories for medicines.
 *
 * Demonstrates:
 * - Encapsulated Domain Enums: Associates machine constants with human-readable display labels.
 */
public enum MedicineCategory {
    ANALGESIC("Pain Reliever / Analgesic"),
    ANTIBIOTIC("Antibiotic"),
    ANTIHISTAMINE("Antihistamine / Allergy"),
    ANTIPYRETIC("Fever Reducer / Antipyretic"),
    ANTIHYPERTENSIVE("Antihypertensive"),
    ANTIDIABETIC("Antidiabetic"),
    ANTACID("Antacid / Gastrointestinal"),
    COUGH_AND_COLD("Cough & Cold"),
    VITAMINS_SUPPLEMENTS("Vitamins & Supplements"),
    OTHER("Other");

    private final String label;

    MedicineCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
