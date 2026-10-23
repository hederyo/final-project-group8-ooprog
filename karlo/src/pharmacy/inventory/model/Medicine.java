package pharmacy.inventory.model;

import pharmacy.common.model.Storable;
import pharmacy.common.util.Format;

import java.time.LocalDate;

/**
 * Domain entity representing a medicine record in the pharmacy catalog.
 *
 * Demonstrates:
 * - Encapsulation: All state fields are strictly private with invariant-guarding mutators.
 * - Abstraction / Interface implementation: Implements Storable to support flat-file persistence.
 * - Business Domain Logic: Computes time-sensitive states (expiry, sellability, low stock) dynamically.
 */
public class Medicine implements Storable {

    public static final int DEFAULT_REORDER_LEVEL = 10;

    private final String medicineId;
    private String brandName;
    private String genericName;
    private MedicineCategory category;
    private String dosageStrength;
    private double unitPrice;
    private int stockQuantity;
    private int reorderLevel;
    private String lotNumber;
    private LocalDate expiryDate;
    private boolean prescriptionRequired;
    private String sideEffects;
    private String storageInstructions;
    private MedicineStatus status;

    public Medicine(String medicineId, String brandName, String genericName, MedicineCategory category,
                    String dosageStrength, double unitPrice, int stockQuantity, int reorderLevel,
                    String lotNumber, LocalDate expiryDate, boolean prescriptionRequired,
                    String sideEffects, String storageInstructions, MedicineStatus status) {
        this.medicineId = medicineId;
        this.brandName = brandName;
        this.genericName = genericName;
        this.category = (category != null) ? category : MedicineCategory.OTHER;
        this.dosageStrength = dosageStrength;
        setUnitPrice(unitPrice);
        setStockQuantity(stockQuantity);
        setReorderLevel(reorderLevel);
        this.lotNumber = lotNumber;
        this.expiryDate = expiryDate;
        this.prescriptionRequired = prescriptionRequired;
        this.sideEffects = (sideEffects != null) ? sideEffects : "";
        this.storageInstructions = (storageInstructions != null) ? storageInstructions : "";
        this.status = (status != null) ? status : MedicineStatus.ACTIVE;
    }

    @Override
    public String getId() {
        return medicineId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public MedicineCategory getCategory() {
        return category;
    }

    public void setCategory(MedicineCategory category) {
        if (category != null) {
            this.category = category;
        }
    }

    public String getDosageStrength() {
        return dosageStrength;
    }

    public void setDosageStrength(String dosageStrength) {
        this.dosageStrength = dosageStrength;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        if (unitPrice <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero.");
        }
        this.unitPrice = Format.round2(unitPrice);
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative: " + stockQuantity);
        }
        this.stockQuantity = stockQuantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative: " + reorderLevel);
        }
        this.reorderLevel = reorderLevel;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isPrescriptionRequired() {
        return prescriptionRequired;
    }

    public void setPrescriptionRequired(boolean prescriptionRequired) {
        this.prescriptionRequired = prescriptionRequired;
    }

    public String getSideEffects() {
        return sideEffects;
    }

    public void setSideEffects(String sideEffects) {
        this.sideEffects = (sideEffects != null) ? sideEffects : "";
    }

    public String getStorageInstructions() {
        return storageInstructions;
    }

    public void setStorageInstructions(String storageInstructions) {
        this.storageInstructions = (storageInstructions != null) ? storageInstructions : "";
    }

    public MedicineStatus getStatus() {
        return status;
    }

    public void setStatus(MedicineStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    /**
     * Checks if medicine is expired on or before the given reference date.
     * Note: A medicine cannot be sold on its expiry date.
     */
    public boolean isExpired(LocalDate today) {
        return expiryDate != null && !expiryDate.isAfter(today);
    }

    /**
     * Checks if medicine is near expiry within the specified window in days.
     */
    public boolean isNearExpiry(LocalDate today, int days) {
        if (isExpired(today) || expiryDate == null) {
            return false;
        }
        return !expiryDate.isAfter(today.plusDays(days));
    }

    /**
     * Checks if available physical stock is at or below the reorder threshold.
     */
    public boolean isLowStock() {
        return stockQuantity <= reorderLevel;
    }

    /**
     * Determines whether the medicine is legally sellable to a patient today:
     * active lifecycle status and not past its expiration date.
     */
    public boolean isSellable(LocalDate today) {
        return status == MedicineStatus.ACTIVE && !isExpired(today);
    }

    @Override
    public String toFileLine() {
        return String.join("|",
                medicineId,
                brandName,
                genericName,
                category.name(),
                dosageStrength,
                String.format(java.util.Locale.US, "%.2f", unitPrice),
                String.valueOf(stockQuantity),
                String.valueOf(reorderLevel),
                lotNumber,
                expiryDate != null ? expiryDate.format(Format.DATE) : "",
                String.valueOf(prescriptionRequired),
                sideEffects,
                storageInstructions,
                status.name()
        );
    }
}
