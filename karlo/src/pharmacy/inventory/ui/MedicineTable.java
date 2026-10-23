package pharmacy.inventory.ui;

import pharmacy.common.util.Format;
import pharmacy.inventory.InventoryService;
import pharmacy.inventory.model.Medicine;
import pharmacy.inventory.model.MedicineStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Console presentation utility for formatting medicine collections and detail sheets.
 *
 * Demonstrates:
 * - Separation of Concerns: Encapsulates all terminal table rendering logic away from menus and services.
 */
public final class MedicineTable {

    private MedicineTable() {}

    public static void printTable(List<Medicine> list, boolean isAdmin) {
        if (list == null || list.isEmpty()) {
            System.out.println("No medicines available.");
            return;
        }

        LocalDate today = LocalDate.now();

        System.out.println("-----------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s %-16s %-18s %-16s %-10s %-12s %-6s %-7s %s%n",
                "ID", "Brand", "Generic", "Category", "Dosage", "Price", "Type", "Stock", "Status / Alerts");
        System.out.println("-----------------------------------------------------------------------------------------------------------------------");

        for (Medicine m : list) {
            String type = m.isPrescriptionRequired() ? "Rx" : "OTC";
            StringBuilder flags = new StringBuilder();

            if (m.getStatus() == MedicineStatus.DISCONTINUED) {
                flags.append("[DISCONTINUED] ");
            }
            if (m.isExpired(today)) {
                flags.append("[EXPIRED] ");
            } else if (m.isNearExpiry(today, InventoryService.NEAR_EXPIRY_DAYS)) {
                flags.append("[NEAR EXPIRY] ");
            }
            if (m.isLowStock()) {
                flags.append("[LOW STOCK] ");
            }
            if (flags.length() == 0) {
                flags.append("OK");
            }

            System.out.printf("%-10s %-16s %-18s %-16s %-10s %-12s %-6s %-7d %s%n",
                    m.getId(),
                    truncate(m.getBrandName(), 15),
                    truncate(m.getGenericName(), 17),
                    truncate(m.getCategory().getLabel(), 15),
                    m.getDosageStrength(),
                    Format.peso(m.getUnitPrice()),
                    type,
                    m.getStockQuantity(),
                    flags.toString().trim()
            );
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------------");
        System.out.println("Total records: " + list.size());
    }

    public static void printDetails(Medicine m, boolean isAdmin) {
        if (m == null) {
            System.out.println("No medicine details to display.");
            return;
        }

        LocalDate today = LocalDate.now();
        StringBuilder alerts = new StringBuilder();
        if (m.getStatus() == MedicineStatus.DISCONTINUED) {
            alerts.append("[DISCONTINUED] ");
        }
        if (m.isExpired(today)) {
            alerts.append("[EXPIRED] ");
        } else if (m.isNearExpiry(today, InventoryService.NEAR_EXPIRY_DAYS)) {
            alerts.append("[NEAR EXPIRY] ");
        }
        if (m.isLowStock()) {
            alerts.append("[LOW STOCK] ");
        }
        if (alerts.length() == 0) {
            alerts.append("Normal");
        }

        System.out.println("==================================================");
        System.out.println("  Medicine Details — " + m.getBrandName() + " (" + m.getId() + ")");
        System.out.println("==================================================");
        System.out.println("  Medicine ID           : " + m.getId());
        System.out.println("  Brand Name            : " + m.getBrandName());
        System.out.println("  Generic Name          : " + m.getGenericName());
        System.out.println("  Category              : " + m.getCategory().getLabel());
        System.out.println("  Dosage Strength       : " + m.getDosageStrength());
        System.out.println("  Unit Price            : " + Format.peso(m.getUnitPrice()));
        System.out.println("  Stock Quantity        : " + m.getStockQuantity() + " units");
        System.out.println("  Reorder Level         : " + m.getReorderLevel() + " units");
        System.out.println("  Lot Number            : " + m.getLotNumber());
        System.out.println("  Expiry Date           : " + m.getExpiryDate().format(Format.DATE));
        System.out.println("  Prescription Required : " + (m.isPrescriptionRequired() ? "Yes (Prescription-Only / Rx)" : "No (Over-The-Counter / OTC)"));
        System.out.println("  Side Effects          : " + (m.getSideEffects().isEmpty() ? "None reported" : m.getSideEffects()));
        System.out.println("  Storage Instructions  : " + (m.getStorageInstructions().isEmpty() ? "Standard room storage" : m.getStorageInstructions()));
        System.out.println("  Status                : " + m.getStatus().getLabel());
        System.out.println("  Alerts                : " + alerts.toString().trim());
        System.out.println("==================================================");
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 1) + "…";
    }
}
