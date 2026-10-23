package pharmacy.inventory.ui;

import pharmacy.common.exception.PharmacyException;
import pharmacy.common.ui.BaseMenu;
import pharmacy.common.util.ConsoleInput;
import pharmacy.common.util.Format;
import pharmacy.common.util.InputValidator;
import pharmacy.inventory.InventoryService;
import pharmacy.inventory.model.Medicine;
import pharmacy.inventory.model.StockAudit;

import java.time.LocalDate;
import java.util.List;

/**
 * Administrative stock reconciliation and inventory audit menu.
 *
 * Demonstrates:
 * - Inheritance: Subclasses BaseMenu to inherit loop flow and centralized error handling.
 * - Accountability / Auditing: Provides end-to-end stock verification, shrinkage tracking, and batch updates.
 */
public class StockAuditMenu extends BaseMenu {

    private final InventoryService inventoryService;

    public StockAuditMenu() {
        this(new InventoryService());
    }

    public StockAuditMenu(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    protected String getTitle() {
        return "Stock & Inventory Audit\n  [" + inventoryService.getAlertSummary() + "]";
    }

    @Override
    protected String[] getOptions() {
        return new String[] {
            "Low-stock report",
            "Expiry report",
            "Record physical count",
            "Update lot / expiry / reorder level",
            "Audit history"
        };
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> handleLowStockReport();
            case 2 -> handleExpiryReport();
            case 3 -> handleRecordPhysicalCount();
            case 4 -> handleUpdateBatchDetails();
            case 5 -> handleAuditHistory();
        }
    }

    private void handleLowStockReport() throws PharmacyException {
        List<Medicine> low = inventoryService.lowStockReport();
        System.out.println("\n--- Low-Stock Report (Stock <= Reorder Level) ---");
        MedicineTable.printTable(low, true);
        ConsoleInput.pause();
    }

    private void handleExpiryReport() throws PharmacyException {
        List<Medicine> expiring = inventoryService.expiryReport();
        System.out.println("\n--- Expiry & Near-Expiry Report (Expired or within " + InventoryService.NEAR_EXPIRY_DAYS + " days) ---");
        MedicineTable.printTable(expiring, true);
        ConsoleInput.pause();
    }

    private void handleRecordPhysicalCount() throws PharmacyException {
        System.out.println("\n--- Record Physical Count Audit ---");
        String id = ConsoleInput.readRequired("Enter Medicine ID to audit: ");
        Medicine med = inventoryService.getDetails(id);

        System.out.println("Auditing: " + med.getBrandName() + " " + med.getDosageStrength() + " (" + med.getId() + ")");
        System.out.println("System Recorded Stock: " + med.getStockQuantity() + " units");

        int counted = ConsoleInput.readInt("Enter actual counted quantity (0-100000): ", 0, 100000);
        int difference = counted - med.getStockQuantity();

        String reason = "";
        if (difference != 0) {
            System.out.println("Discrepancy detected: " + (difference > 0 ? "+" + difference : difference) + " units (" +
                    (difference < 0 ? "Shrinkage / Missing" : "Overage") + ").");
            reason = ConsoleInput.readValidated("Reason for discrepancy (required): ", v -> {
                InputValidator.requireNonBlank("Discrepancy reason", v);
                InputValidator.requireNoDelimiter("Discrepancy reason", v);
            });
        } else {
            System.out.println("Physical count matches system quantity exactly.");
            reason = ConsoleInput.readOptional("Audit note (optional, Enter to skip): ");
        }

        StockAudit audit = inventoryService.recordPhysicalCount(id, counted, reason);

        System.out.println("\nSuccess: Physical audit " + audit.getId() + " saved.");
        System.out.println("Medicine stock updated: " + audit.getSystemQuantity() + " -> " + audit.getCountedQuantity() +
                " (Net difference: " + audit.getDifference() + ").");
        ConsoleInput.pause();
    }

    private void handleUpdateBatchDetails() throws PharmacyException {
        System.out.println("\n--- Update Lot Number / Expiry Date / Reorder Level ---");
        String id = ConsoleInput.readRequired("Enter Medicine ID: ");
        Medicine med = inventoryService.getDetails(id);

        System.out.println("Updating batch info for " + med.getBrandName() + " (" + med.getId() + "). Press Enter to keep current value.");

        String lotStr = ConsoleInput.readOptional("Lot number [" + med.getLotNumber() + "]: ");
        String newLot = lotStr.isEmpty() ? null : lotStr;

        LocalDate newExpiry = null;
        String expiryStr = ConsoleInput.readOptional("Expiry date [" + med.getExpiryDate().format(Format.DATE) + "] (yyyy-MM-dd): ");
        if (!expiryStr.isEmpty()) {
            newExpiry = LocalDate.parse(expiryStr, Format.DATE);
        }

        Integer newReorder = null;
        String reorderStr = ConsoleInput.readOptional("Reorder level [" + med.getReorderLevel() + "]: ");
        if (!reorderStr.isEmpty()) {
            newReorder = Integer.parseInt(reorderStr);
        }

        inventoryService.updateLotExpiryReorder(id, newLot, newExpiry, newReorder);
        System.out.println("\nSuccess: Batch metadata for " + med.getBrandName() + " updated successfully.");
        ConsoleInput.pause();
    }

    private void handleAuditHistory() throws PharmacyException {
        System.out.println("\n--- Stock Audit History ---");
        String medId = ConsoleInput.readOptional("Filter by Medicine ID (Enter to view all): ");
        List<StockAudit> audits = inventoryService.auditHistory(medId.isEmpty() ? null : medId);

        if (audits.isEmpty()) {
            System.out.println("No stock audit records found.");
            ConsoleInput.pause();
            return;
        }

        System.out.println("-------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-12s %-10s %-8s %-8s %-12s %-10s %-20s %s%n",
                "Audit ID", "Med ID", "System", "Counted", "Difference", "Auditor", "Date / Time", "Reason");
        System.out.println("-------------------------------------------------------------------------------------------------------------------");

        for (StockAudit a : audits) {
            String diffStr = (a.getDifference() > 0 ? "+" : "") + a.getDifference();
            System.out.printf("%-12s %-10s %-8d %-8d %-12s %-10s %-20s %s%n",
                    a.getId(),
                    a.getMedicineId(),
                    a.getSystemQuantity(),
                    a.getCountedQuantity(),
                    diffStr,
                    a.getAuditedBy(),
                    a.getAuditedAt().format(Format.DATE_TIME),
                    a.getReason()
            );
        }
        System.out.println("-------------------------------------------------------------------------------------------------------------------");
        System.out.println("Total audit records: " + audits.size());
        ConsoleInput.pause();
    }
}
