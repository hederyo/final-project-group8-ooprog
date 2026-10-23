package pharmacy.inventory.ui;

import pharmacy.common.exception.PharmacyException;
import pharmacy.common.exception.ValidationException;
import pharmacy.common.ui.BaseMenu;
import pharmacy.common.util.ConsoleInput;
import pharmacy.common.util.Format;
import pharmacy.common.util.InputValidator;
import pharmacy.inventory.InventoryService;
import pharmacy.inventory.MedicineFilter;
import pharmacy.inventory.model.Medicine;
import pharmacy.inventory.model.MedicineCategory;
import pharmacy.inventory.model.MedicineStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Administrative medicine catalog management menu.
 *
 * Demonstrates:
 * - Inheritance: Subclasses BaseMenu to reuse menu loop structure, choice handling, and error catching.
 * - Layered Architecture: Delegates all catalog persistence and validation to InventoryService without direct file I/O.
 */
public class CatalogMenu extends BaseMenu {

    private final InventoryService inventoryService;

    public CatalogMenu() {
        this(new InventoryService());
    }

    public CatalogMenu(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    protected String getTitle() {
        return "Medicine Catalog";
    }

    @Override
    protected String[] getOptions() {
        return new String[] {
            "List all medicines",
            "Search & filter",
            "View details",
            "Add medicine",
            "Update medicine",
            "Discontinue / Reactivate",
            "Delete medicine"
        };
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> handleListAll();
            case 2 -> handleSearchAndFilter();
            case 3 -> handleViewDetails();
            case 4 -> handleAddMedicine();
            case 5 -> handleUpdateMedicine();
            case 6 -> handleDiscontinueOrReactivate();
            case 7 -> handleDeleteMedicine();
        }
    }

    private void handleListAll() throws PharmacyException {
        List<Medicine> all = inventoryService.listAll();
        System.out.println("\n--- All Catalog Medicines (Admin View) ---");
        MedicineTable.printTable(all, true);
        ConsoleInput.pause();
    }

    private void handleSearchAndFilter() throws PharmacyException {
        System.out.println("\n--- Search & Filter Catalog ---");
        MedicineFilter filter = new MedicineFilter();

        String kw = ConsoleInput.readOptional("Search keyword (brand or generic, Enter to skip): ");
        if (!kw.isEmpty()) {
            filter.setKeyword(kw);
        }

        if (ConsoleInput.readYesNo("Filter by category? (y/n): ")) {
            MedicineCategory[] categories = MedicineCategory.values();
            System.out.println("Categories:");
            for (int i = 0; i < categories.length; i++) {
                System.out.println("  " + (i + 1) + ". " + categories[i].getLabel());
            }
            int catChoice = ConsoleInput.readInt("Select category (1-" + categories.length + "): ", 1, categories.length);
            filter.setCategory(categories[catChoice - 1]);
        }

        if (ConsoleInput.readYesNo("Filter by price range? (y/n): ")) {
            double min = ConsoleInput.readDouble("Minimum price: ", 0.0, 100000.0);
            double max = ConsoleInput.readDouble("Maximum price: ", min, 100000.0);
            filter.setMinPrice(min);
            filter.setMaxPrice(max);
        }

        if (ConsoleInput.readYesNo("Filter by prescription requirement (Rx / OTC)? (y/n): ")) {
            boolean rxOnly = ConsoleInput.readYesNo("Show only Prescription-Required (Rx) medicines? (y/n): ");
            filter.setPrescriptionRequired(rxOnly);
        }

        List<Medicine> results = inventoryService.search(filter);
        System.out.println("\nSearch Results (" + results.size() + " matches):");
        MedicineTable.printTable(results, true);
        ConsoleInput.pause();
    }

    private void handleViewDetails() throws PharmacyException {
        String id = ConsoleInput.readRequired("Enter Medicine ID (e.g. MED-0001): ");
        Medicine med = inventoryService.getDetails(id);
        MedicineTable.printDetails(med, true);
        ConsoleInput.pause();
    }

    private void handleAddMedicine() throws PharmacyException {
        System.out.println("\n--- Add New Medicine ---");
        String brand = ConsoleInput.readValidated("Brand name: ", v -> {
            InputValidator.requireNonBlank("Brand name", v);
            InputValidator.requireNoDelimiter("Brand name", v);
        });

        String generic = ConsoleInput.readValidated("Generic name: ", v -> {
            InputValidator.requireNonBlank("Generic name", v);
            InputValidator.requireNoDelimiter("Generic name", v);
        });

        MedicineCategory[] categories = MedicineCategory.values();
        System.out.println("Select Category:");
        for (int i = 0; i < categories.length; i++) {
            System.out.println("  " + (i + 1) + ". " + categories[i].getLabel());
        }
        int catChoice = ConsoleInput.readInt("Category choice (1-" + categories.length + "): ", 1, categories.length);
        MedicineCategory category = categories[catChoice - 1];

        String dosage = ConsoleInput.readValidated("Dosage strength (e.g. 500mg, 250mg/5mL): ",
                InputValidator::requireDosageStrength);

        double price = ConsoleInput.readDouble("Unit price (> 0): ", 0.01, 100000.0);
        int stock = ConsoleInput.readInt("Initial stock quantity (0-100000): ", 0, 100000);
        int reorder = ConsoleInput.readInt("Reorder level threshold (default 10): ", 0, 10000);

        String lot = ConsoleInput.readValidated("Lot number: ", InputValidator::requireLotNumber);
        LocalDate expiry = ConsoleInput.readDate("Expiry date (yyyy-MM-dd, must be future): ");
        InputValidator.requireFutureDate("Expiry date", expiry);

        boolean rx = ConsoleInput.readYesNo("Requires doctor prescription (Rx)? (y/n): ");

        String sideEffects = ConsoleInput.readOptional("Side effects / contraindications (Enter to skip): ");
        String storage = ConsoleInput.readOptional("Storage instructions (Enter to skip): ");

        Medicine created = inventoryService.addMedicine(brand, generic, category, dosage, price, stock,
                reorder, lot, expiry, rx, sideEffects, storage);

        System.out.println("\nSuccess: Medicine " + created.getId() + " (" + created.getBrandName() +
                " " + created.getDosageStrength() + ") registered successfully.");
        ConsoleInput.pause();
    }

    private void handleUpdateMedicine() throws PharmacyException {
        System.out.println("\n--- Update Medicine ---");
        String id = ConsoleInput.readRequired("Enter Medicine ID to update: ");
        Medicine med = inventoryService.getDetails(id);

        System.out.println("Updating " + med.getBrandName() + " (" + med.getId() + "). Press Enter to keep current value.");

        String promptBrand = "Brand name [" + med.getBrandName() + "]: ";
        String newBrand = ConsoleInput.readOptional(promptBrand);

        String promptGeneric = "Generic name [" + med.getGenericName() + "]: ";
        String newGeneric = ConsoleInput.readOptional(promptGeneric);

        MedicineCategory newCategory = null;
        if (ConsoleInput.readYesNo("Change category from [" + med.getCategory().getLabel() + "]? (y/n): ")) {
            MedicineCategory[] categories = MedicineCategory.values();
            for (int i = 0; i < categories.length; i++) {
                System.out.println("  " + (i + 1) + ". " + categories[i].getLabel());
            }
            int catChoice = ConsoleInput.readInt("Select category: ", 1, categories.length);
            newCategory = categories[catChoice - 1];
        }

        String promptDosage = "Dosage strength [" + med.getDosageStrength() + "]: ";
        String newDosage = ConsoleInput.readOptional(promptDosage);

        Double newPrice = null;
        String priceStr = ConsoleInput.readOptional("Unit price [" + Format.peso(med.getUnitPrice()) + "]: ");
        if (!priceStr.isEmpty()) {
            newPrice = Double.parseDouble(priceStr);
        }

        Integer newStock = null;
        String stockStr = ConsoleInput.readOptional("Stock quantity [" + med.getStockQuantity() + "]: ");
        if (!stockStr.isEmpty()) {
            newStock = Integer.parseInt(stockStr);
        }

        Integer newReorder = null;
        String reorderStr = ConsoleInput.readOptional("Reorder level [" + med.getReorderLevel() + "]: ");
        if (!reorderStr.isEmpty()) {
            newReorder = Integer.parseInt(reorderStr);
        }

        String lotStr = ConsoleInput.readOptional("Lot number [" + med.getLotNumber() + "]: ");
        String newLot = lotStr.isEmpty() ? null : lotStr;

        LocalDate newExpiry = null;
        String expiryStr = ConsoleInput.readOptional("Expiry date [" + med.getExpiryDate().format(Format.DATE) + "] (yyyy-MM-dd): ");
        if (!expiryStr.isEmpty()) {
            newExpiry = LocalDate.parse(expiryStr, Format.DATE);
        }

        Boolean newRx = null;
        if (ConsoleInput.readYesNo("Change prescription requirement (currently " + (med.isPrescriptionRequired() ? "Rx" : "OTC") + ")? (y/n): ")) {
            newRx = ConsoleInput.readYesNo("Requires prescription (Rx)? (y/n): ");
        }

        String sideStr = ConsoleInput.readOptional("Side effects [" + med.getSideEffects() + "]: ");
        String newSide = sideStr.isEmpty() ? null : sideStr;

        String storeStr = ConsoleInput.readOptional("Storage instructions [" + med.getStorageInstructions() + "]: ");
        String newStore = storeStr.isEmpty() ? null : storeStr;

        Medicine updated = inventoryService.updateMedicine(id,
                newBrand.isEmpty() ? null : newBrand,
                newGeneric.isEmpty() ? null : newGeneric,
                newCategory,
                newDosage.isEmpty() ? null : newDosage,
                newPrice, newStock, newReorder, newLot, newExpiry, newRx, newSide, newStore);

        System.out.println("\nSuccess: Medicine " + updated.getId() + " updated successfully.");
        ConsoleInput.pause();
    }

    private void handleDiscontinueOrReactivate() throws PharmacyException {
        String id = ConsoleInput.readRequired("Enter Medicine ID: ");
        Medicine med = inventoryService.getDetails(id);

        if (med.getStatus() == MedicineStatus.ACTIVE) {
            System.out.println("Medicine " + med.getBrandName() + " is currently ACTIVE.");
            boolean confirm = ConsoleInput.readYesNo("Are you sure you want to discontinue this medicine? (y/n): ");
            if (confirm) {
                inventoryService.discontinue(id);
                System.out.println("Medicine " + med.getBrandName() + " (" + id + ") is now DISCONTINUED.");
            } else {
                System.out.println("Discontinuation cancelled.");
            }
        } else {
            System.out.println("Medicine " + med.getBrandName() + " is currently DISCONTINUED.");
            boolean confirm = ConsoleInput.readYesNo("Are you sure you want to reactivate this medicine? (y/n): ");
            if (confirm) {
                inventoryService.reactivate(id);
                System.out.println("Medicine " + med.getBrandName() + " (" + id + ") is now ACTIVE.");
            } else {
                System.out.println("Reactivation cancelled.");
            }
        }
        ConsoleInput.pause();
    }

    private void handleDeleteMedicine() throws PharmacyException {
        String id = ConsoleInput.readRequired("Enter Medicine ID to delete: ");
        Medicine med = inventoryService.getDetails(id);

        System.out.println("WARNING: Deletion is permanent and permitted only if no orders exist.");
        boolean confirm = ConsoleInput.readYesNo("Are you sure you want to delete " + med.getBrandName() + " (" + id + ")? (y/n): ");
        if (!confirm) {
            System.out.println("Deletion cancelled.");
            ConsoleInput.pause();
            return;
        }

        inventoryService.deleteMedicine(id);
        System.out.println("Medicine " + med.getBrandName() + " (" + id + ") has been permanently deleted.");
        ConsoleInput.pause();
    }
}
