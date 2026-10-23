package pharmacy.inventory;

import pharmacy.common.exception.AccessDeniedException;
import pharmacy.common.exception.AuthenticationException;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.DuplicateRecordException;
import pharmacy.common.exception.NotFoundException;
import pharmacy.common.exception.ValidationException;
import pharmacy.common.model.Role;
import pharmacy.common.model.User;
import pharmacy.common.session.Session;
import pharmacy.common.util.Format;
import pharmacy.common.util.InputValidator;
import pharmacy.inventory.exception.InsufficientStockException;
import pharmacy.inventory.model.Medicine;
import pharmacy.inventory.model.MedicineCategory;
import pharmacy.inventory.model.MedicineStatus;
import pharmacy.inventory.model.StockAudit;
import pharmacy.orders.OrderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service managing medicine catalog operations, inventory tracking, and stock contracts.
 *
 * Demonstrates:
 * - Encapsulation / Layering: Isolates business rule enforcement from console I/O and repositories.
 * - Cross-Member Contracts: Provides exact signatures for Cameron's orders and Heather's admin flows.
 * - Guarded State Invariants: Prevents selling expired/discontinued stock and guarantees audit trail fidelity.
 */
public class InventoryService {

    public static final int NEAR_EXPIRY_DAYS = 30;

    private final MedicineRepository medicineRepository;
    private final StockAuditRepository stockAuditRepository;
    private final OrderService orderService;

    public InventoryService() {
        this(new MedicineRepository(), new StockAuditRepository(), null);
    }

    public InventoryService(MedicineRepository medicineRepository, StockAuditRepository stockAuditRepository,
                            OrderService orderService) {
        this.medicineRepository = medicineRepository;
        this.stockAuditRepository = stockAuditRepository;
        this.orderService = orderService;
    }

    private OrderService getOrderService() {
        return (orderService != null) ? orderService : new OrderService(
                new pharmacy.orders.OrderRepository(),
                new pharmacy.orders.VoidRecordRepository(),
                this,
                this.medicineRepository,
                new pharmacy.auth.UserRepository()
        );
    }

    private void requireAdmin() throws AuthenticationException, AccessDeniedException {
        Session.getInstance().requireRole(Role.ADMIN);
    }

    // =========================================================================
    // 1. Cross-Member Stock Contracts for Cameron (ARCHITECTURE §6.3)
    // =========================================================================

    /**
     * Finds a medicine by ID and verifies it is legally sellable (active and not expired).
     * Accessible by both Pharmacist and Admin through order processing without role restrictions.
     *
     * @param medicineId unique ID of the medicine
     * @return sellable Medicine entity
     * @throws NotFoundException if medicine does not exist
     * @throws ValidationException if medicine is discontinued or expired
     * @throws DataAccessException on flat-file storage failure
     */
    public Medicine findSellable(String medicineId)
            throws NotFoundException, ValidationException, DataAccessException {
        Medicine med = medicineRepository.findById(medicineId);
        if (med.getStatus() == MedicineStatus.DISCONTINUED) {
            throw new ValidationException(med.getBrandName() + " " + med.getDosageStrength() + " is discontinued and cannot be sold.");
        }
        if (med.isExpired(LocalDate.now())) {
            throw new ValidationException(med.getBrandName() + " " + med.getDosageStrength() + " is expired and cannot be sold.");
        }
        return med;
    }

    /**
     * Reserves (decrements) physical stock for a validated order item.
     * Enforces sellability and stock availability rules.
     *
     * @param medicineId unique ID of the medicine
     * @param quantity number of units to reserve (must be > 0)
     * @throws NotFoundException if medicine does not exist
     * @throws InsufficientStockException if requested quantity exceeds available stock
     * @throws ValidationException if quantity <= 0 or medicine is not sellable
     * @throws DataAccessException on flat-file storage failure
     */
    public void reserveStock(String medicineId, int quantity)
            throws NotFoundException, InsufficientStockException, ValidationException, DataAccessException {
        if (quantity <= 0) {
            throw new ValidationException("Quantity to reserve must be greater than 0.");
        }
        Medicine med = findSellable(medicineId);
        if (quantity > med.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Only " + med.getStockQuantity() + " units of " + med.getBrandName() + " " + med.getDosageStrength() + " left.");
        }
        med.setStockQuantity(med.getStockQuantity() - quantity);
        medicineRepository.save(med);
    }

    /**
     * Restores (increments) physical stock when an order is updated, cancelled, or voided.
     * Works for ANY existing medicine, including expired or discontinued records.
     *
     * @param medicineId unique ID of the medicine
     * @param quantity number of units to restore (must be > 0)
     * @throws NotFoundException if medicine does not exist
     * @throws ValidationException if quantity <= 0
     * @throws DataAccessException on flat-file storage failure
     */
    public void restoreStock(String medicineId, int quantity)
            throws NotFoundException, ValidationException, DataAccessException {
        if (quantity <= 0) {
            throw new ValidationException("Quantity to restore must be greater than 0.");
        }
        Medicine med = medicineRepository.findById(medicineId);
        med.setStockQuantity(med.getStockQuantity() + quantity);
        medicineRepository.save(med);
    }

    // =========================================================================
    // 2. Browse & Search (M1, M2, M3)
    // =========================================================================

    /**
     * Returns all currently sellable medicines sorted alphabetically by brand name.
     */
    public List<Medicine> browseSellable() throws DataAccessException {
        LocalDate today = LocalDate.now();
        List<Medicine> all = medicineRepository.findAll();
        List<Medicine> sellable = new ArrayList<>();
        for (Medicine m : all) {
            if (m.isSellable(today)) {
                sellable.add(m);
            }
        }
        sellable.sort(Comparator.comparing(m -> m.getBrandName().toLowerCase()));
        return sellable;
    }

    /**
     * Searches medicines matching criteria. Forces sellable-only filter for non-admin sessions.
     */
    public List<Medicine> search(MedicineFilter filter) throws ValidationException, DataAccessException {
        if (filter == null) {
            filter = new MedicineFilter();
        }
        filter.validate();

        User current = Session.getInstance().getCurrentUser();
        if (current == null || current.getRole() != Role.ADMIN) {
            filter.setSellableOnly(true);
        }

        LocalDate today = LocalDate.now();
        List<Medicine> all = medicineRepository.findAll();
        List<Medicine> matches = new ArrayList<>();
        for (Medicine m : all) {
            if (filter.matches(m, today)) {
                matches.add(m);
            }
        }
        matches.sort(Comparator.comparing(m -> m.getBrandName().toLowerCase()));
        return matches;
    }

    /**
     * Retrieves full medicine details. If viewed by a pharmacist, non-sellable records throw NotFoundException.
     */
    public Medicine getDetails(String medicineId) throws NotFoundException, DataAccessException {
        Medicine med = medicineRepository.findById(medicineId);
        User current = Session.getInstance().getCurrentUser();
        if (current != null && current.getRole() == Role.PHARMACIST) {
            if (!med.isSellable(LocalDate.now())) {
                throw new NotFoundException("Medicine " + medicineId + " not found.");
            }
        }
        return med;
    }

    // =========================================================================
    // 3. Admin Catalog CRUD (M4)
    // =========================================================================

    public List<Medicine> listAll() throws AuthenticationException, AccessDeniedException, DataAccessException {
        requireAdmin();
        List<Medicine> all = medicineRepository.findAll();
        all.sort(Comparator.comparing(m -> m.getBrandName().toLowerCase()));
        return all;
    }

    public Medicine addMedicine(String brand, String generic, MedicineCategory category, String dosage,
                                double price, int stock, int reorderLevel, String lot, LocalDate expiry,
                                boolean prescriptionRequired, String sideEffects, String storage)
            throws AuthenticationException, AccessDeniedException, ValidationException,
                   DuplicateRecordException, DataAccessException {
        requireAdmin();

        // M1 Required fields validation
        String cleanBrand = InputValidator.requireNonBlank("Brand name", brand);
        cleanBrand = InputValidator.requireNoDelimiter("Brand name", cleanBrand);
        String cleanGeneric = InputValidator.requireNonBlank("Generic name", generic);
        cleanGeneric = InputValidator.requireNoDelimiter("Generic name", cleanGeneric);
        if (category == null) {
            throw new ValidationException("Category is required.");
        }
        String cleanDosage = InputValidator.requireDosageStrength(dosage);

        // M2 Price, Stock, Reorder validation
        InputValidator.requirePrice(String.valueOf(price));
        InputValidator.requireQuantity("Stock quantity", String.valueOf(stock), 0, 100000);
        InputValidator.requireQuantity("Reorder level", String.valueOf(reorderLevel), 0, 10000);

        // M3 Expiry & Lot validation
        String cleanLot = InputValidator.requireLotNumber(lot);
        if (expiry == null) {
            throw new ValidationException("Expiry date is required.");
        }
        InputValidator.requireFutureDate("Expiry date", expiry);

        String cleanSide = InputValidator.requireText("Side effects", sideEffects, 200);
        String cleanStorage = InputValidator.requireText("Storage instructions", storage, 200);

        // M4 Uniqueness validation
        if (medicineRepository.existsByBrandGenericDosage(cleanBrand, cleanGeneric, cleanDosage, null)) {
            throw new DuplicateRecordException("A medicine with brand '" + cleanBrand + "', generic '" +
                    cleanGeneric + "', and dosage '" + cleanDosage + "' already exists.");
        }

        String nextId = medicineRepository.nextId();
        Medicine medicine = new Medicine(nextId, cleanBrand, cleanGeneric, category, cleanDosage,
                price, stock, reorderLevel, cleanLot, expiry, prescriptionRequired,
                cleanSide, cleanStorage, MedicineStatus.ACTIVE);

        medicineRepository.save(medicine);
        return medicine;
    }

    public Medicine updateMedicine(String id, String brand, String generic, MedicineCategory category,
                                   String dosage, Double price, Integer stock, Integer reorderLevel,
                                   String lot, LocalDate expiry, Boolean prescriptionRequired,
                                   String sideEffects, String storage)
            throws AuthenticationException, AccessDeniedException, NotFoundException,
                   ValidationException, DuplicateRecordException, DataAccessException {
        requireAdmin();

        Medicine existing = medicineRepository.findById(id);

        String newBrand = (brand != null && !brand.trim().isEmpty()) ?
                InputValidator.requireNoDelimiter("Brand name", InputValidator.requireNonBlank("Brand name", brand)) : existing.getBrandName();
        String newGeneric = (generic != null && !generic.trim().isEmpty()) ?
                InputValidator.requireNoDelimiter("Generic name", InputValidator.requireNonBlank("Generic name", generic)) : existing.getGenericName();
        MedicineCategory newCategory = (category != null) ? category : existing.getCategory();
        String newDosage = (dosage != null && !dosage.trim().isEmpty()) ?
                InputValidator.requireDosageStrength(dosage) : existing.getDosageStrength();

        if (price != null) {
            InputValidator.requirePrice(String.valueOf(price));
            existing.setUnitPrice(price);
        }
        if (stock != null) {
            InputValidator.requireQuantity("Stock quantity", String.valueOf(stock), 0, 100000);
            existing.setStockQuantity(stock);
        }
        if (reorderLevel != null) {
            InputValidator.requireQuantity("Reorder level", String.valueOf(reorderLevel), 0, 10000);
            existing.setReorderLevel(reorderLevel);
        }
        if (lot != null && !lot.trim().isEmpty()) {
            existing.setLotNumber(InputValidator.requireLotNumber(lot));
        }
        if (expiry != null) {
            InputValidator.requireFutureDate("Expiry date", expiry);
            existing.setExpiryDate(expiry);
        }
        if (prescriptionRequired != null) {
            existing.setPrescriptionRequired(prescriptionRequired);
        }
        if (sideEffects != null) {
            existing.setSideEffects(InputValidator.requireText("Side effects", sideEffects, 200));
        }
        if (storage != null) {
            existing.setStorageInstructions(InputValidator.requireText("Storage instructions", storage, 200));
        }

        // Check uniqueness if brand, generic, or dosage changed
        if (!newBrand.equalsIgnoreCase(existing.getBrandName()) ||
            !newGeneric.equalsIgnoreCase(existing.getGenericName()) ||
            !newDosage.equalsIgnoreCase(existing.getDosageStrength())) {
            if (medicineRepository.existsByBrandGenericDosage(newBrand, newGeneric, newDosage, existing.getId())) {
                throw new DuplicateRecordException("A medicine with brand '" + newBrand + "', generic '" +
                        newGeneric + "', and dosage '" + newDosage + "' already exists.");
            }
        }

        existing.setBrandName(newBrand);
        existing.setGenericName(newGeneric);
        existing.setCategory(newCategory);
        existing.setDosageStrength(newDosage);

        medicineRepository.save(existing);
        return existing;
    }

    public void discontinue(String medicineId)
            throws AuthenticationException, AccessDeniedException, NotFoundException, DataAccessException {
        requireAdmin();
        Medicine med = medicineRepository.findById(medicineId);
        med.setStatus(MedicineStatus.DISCONTINUED);
        medicineRepository.save(med);
    }

    public void reactivate(String medicineId)
            throws AuthenticationException, AccessDeniedException, NotFoundException, DataAccessException {
        requireAdmin();
        Medicine med = medicineRepository.findById(medicineId);
        med.setStatus(MedicineStatus.ACTIVE);
        medicineRepository.save(med);
    }

    public void deleteMedicine(String medicineId)
            throws AuthenticationException, AccessDeniedException, NotFoundException,
                   ValidationException, DataAccessException {
        requireAdmin();
        Medicine med = medicineRepository.findById(medicineId);

        // M11 Order history check
        if (getOrderService().hasOrdersForMedicine(medicineId)) {
            throw new ValidationException(
                    "Cannot delete " + med.getBrandName() + " (" + medicineId +
                    "): it has associated order history. Discontinue it instead.");
        }

        medicineRepository.delete(medicineId);
    }

    // =========================================================================
    // 4. Stock & Inventory Audit (M5)
    // =========================================================================

    public List<Medicine> lowStockReport()
            throws AuthenticationException, AccessDeniedException, DataAccessException {
        requireAdmin();
        List<Medicine> all = medicineRepository.findAll();
        List<Medicine> low = new ArrayList<>();
        for (Medicine m : all) {
            if (m.isLowStock()) {
                low.add(m);
            }
        }
        low.sort(Comparator.comparingInt(Medicine::getStockQuantity));
        return low;
    }

    public List<Medicine> expiryReport()
            throws AuthenticationException, AccessDeniedException, DataAccessException {
        requireAdmin();
        LocalDate today = LocalDate.now();
        List<Medicine> all = medicineRepository.findAll();
        List<Medicine> expiring = new ArrayList<>();
        for (Medicine m : all) {
            if (m.isExpired(today) || m.isNearExpiry(today, NEAR_EXPIRY_DAYS)) {
                expiring.add(m);
            }
        }
        expiring.sort(Comparator.comparing(Medicine::getExpiryDate));
        return expiring;
    }

    /**
     * Records a physical count audit for a medicine.
     * Crucial Gotcha: Reads system quantity BEFORE overwriting with counted quantity.
     */
    public StockAudit recordPhysicalCount(String medicineId, int countedQuantity, String reason)
            throws AuthenticationException, AccessDeniedException, NotFoundException,
                   ValidationException, DataAccessException {
        requireAdmin();
        User admin = Session.getInstance().requireUser();

        InputValidator.requireQuantity("Physical count", String.valueOf(countedQuantity), 0, 100000);

        Medicine med = medicineRepository.findById(medicineId);
        int systemQuantity = med.getStockQuantity(); // MUST read before overwriting
        int difference = countedQuantity - systemQuantity;

        if (difference != 0) {
            InputValidator.requireNonBlank("Discrepancy reason", reason);
            reason = InputValidator.requireText("Discrepancy reason", reason, 200);
        } else {
            reason = (reason == null || reason.trim().isEmpty()) ? "Routine physical audit - count matched" :
                    InputValidator.requireText("Audit reason", reason, 200);
        }

        // Overwrite stock
        med.setStockQuantity(countedQuantity);
        medicineRepository.save(med);

        // Create and persist audit record
        String auditId = stockAuditRepository.nextId();
        StockAudit audit = new StockAudit(auditId, medicineId, systemQuantity, countedQuantity,
                difference, reason, admin.getId(), LocalDateTime.now());
        stockAuditRepository.add(audit);

        return audit;
    }

    public void updateLotExpiryReorder(String medicineId, String lotNumber, LocalDate expiryDate, Integer reorderLevel)
            throws AuthenticationException, AccessDeniedException, NotFoundException,
                   ValidationException, DataAccessException {
        requireAdmin();
        Medicine med = medicineRepository.findById(medicineId);

        if (lotNumber != null && !lotNumber.trim().isEmpty()) {
            med.setLotNumber(InputValidator.requireLotNumber(lotNumber));
        }
        if (expiryDate != null) {
            InputValidator.requireFutureDate("Expiry date", expiryDate);
            med.setExpiryDate(expiryDate);
        }
        if (reorderLevel != null) {
            InputValidator.requireQuantity("Reorder level", String.valueOf(reorderLevel), 0, 10000);
            med.setReorderLevel(reorderLevel);
        }

        medicineRepository.save(med);
    }

    public List<StockAudit> auditHistory(String medicineIdOrNull)
            throws AuthenticationException, AccessDeniedException, DataAccessException {
        requireAdmin();
        if (medicineIdOrNull != null && !medicineIdOrNull.trim().isEmpty()) {
            return stockAuditRepository.findByMedicine(medicineIdOrNull);
        }
        return stockAuditRepository.findAll();
    }

    public String getAlertSummary() {
        try {
            LocalDate today = LocalDate.now();
            List<Medicine> all = medicineRepository.findAll();
            int lowStockCount = 0;
            int nearExpiryCount = 0;
            int expiredCount = 0;

            for (Medicine m : all) {
                if (m.isLowStock()) lowStockCount++;
                if (m.isExpired(today)) expiredCount++;
                else if (m.isNearExpiry(today, NEAR_EXPIRY_DAYS)) nearExpiryCount++;
            }

            return String.format("Alerts: %d low-stock, %d near-expiry, %d expired",
                    lowStockCount, nearExpiryCount, expiredCount);
        } catch (Exception e) {
            return "Alerts: Unable to compute";
        }
    }
}
