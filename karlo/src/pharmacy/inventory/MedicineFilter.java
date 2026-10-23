package pharmacy.inventory;

import pharmacy.common.exception.ValidationException;
import pharmacy.inventory.model.Medicine;
import pharmacy.inventory.model.MedicineCategory;

import java.time.LocalDate;

/**
 * Encapsulates multi-criteria search and filter parameters for medicines.
 *
 * Demonstrates:
 * - Abstraction: Hides compound query predicate evaluation behind a single matches() method.
 * - Validation: Enforces search constraint rules (e.g. minPrice <= maxPrice).
 */
public class MedicineFilter {

    private String keyword;
    private MedicineCategory category;
    private Double minPrice;
    private Double maxPrice;
    private Boolean prescriptionRequired;
    private boolean sellableOnly;

    public MedicineFilter() {
        this.sellableOnly = false;
    }

    public void validate() throws ValidationException {
        if (minPrice != null && minPrice < 0) {
            throw new ValidationException("Minimum price cannot be negative.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new ValidationException("Maximum price cannot be negative.");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new ValidationException("Minimum price cannot exceed maximum price.");
        }
    }

    public boolean matches(Medicine m, LocalDate today) {
        if (m == null) {
            return false;
        }

        if (sellableOnly && !m.isSellable(today)) {
            return false;
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            boolean matchBrand = m.getBrandName().toLowerCase().contains(kw);
            boolean matchGeneric = m.getGenericName().toLowerCase().contains(kw);
            if (!matchBrand && !matchGeneric) {
                return false;
            }
        }

        if (category != null && m.getCategory() != category) {
            return false;
        }

        if (minPrice != null && m.getUnitPrice() < minPrice) {
            return false;
        }

        if (maxPrice != null && m.getUnitPrice() > maxPrice) {
            return false;
        }

        if (prescriptionRequired != null && m.isPrescriptionRequired() != prescriptionRequired) {
            return false;
        }

        return true;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public MedicineCategory getCategory() {
        return category;
    }

    public void setCategory(MedicineCategory category) {
        this.category = category;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Boolean getPrescriptionRequired() {
        return prescriptionRequired;
    }

    public void setPrescriptionRequired(Boolean prescriptionRequired) {
        this.prescriptionRequired = prescriptionRequired;
    }

    public boolean isSellableOnly() {
        return sellableOnly;
    }

    public void setSellableOnly(boolean sellableOnly) {
        this.sellableOnly = sellableOnly;
    }
}
