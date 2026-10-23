package pharmacy.inventory.ui;

import pharmacy.common.exception.PharmacyException;
import pharmacy.common.ui.BaseMenu;
import pharmacy.common.util.ConsoleInput;
import pharmacy.inventory.InventoryService;
import pharmacy.inventory.MedicineFilter;
import pharmacy.inventory.model.Medicine;
import pharmacy.inventory.model.MedicineCategory;

import java.util.List;

/**
 * Pharmacist interface for browsing, searching, and inspecting sellable medicines.
 *
 * Demonstrates:
 * - Role-Based Data Isolation: Exposes strictly active, non-expired pharmaceutical records.
 * - Inheritance: Subclasses BaseMenu for standardized terminal workflow.
 */
public class MedicineBrowserMenu extends BaseMenu {

    private final InventoryService inventoryService;

    public MedicineBrowserMenu() {
        this(new InventoryService());
    }

    public MedicineBrowserMenu(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    protected String getTitle() {
        return "Medicine Browser (Pharmacist)";
    }

    @Override
    protected String[] getOptions() {
        return new String[] {
            "Browse all medicines",
            "Search & filter",
            "View details"
        };
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> handleBrowseAll();
            case 2 -> handleSearchAndFilter();
            case 3 -> handleViewDetails();
        }
    }

    private void handleBrowseAll() throws PharmacyException {
        List<Medicine> list = inventoryService.browseSellable();
        System.out.println("\n--- Sellable Medicines Catalog ---");
        MedicineTable.printTable(list, false);
        ConsoleInput.pause();
    }

    private void handleSearchAndFilter() throws PharmacyException {
        System.out.println("\n--- Search & Filter Medicines ---");
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
        MedicineTable.printTable(results, false);
        ConsoleInput.pause();
    }

    private void handleViewDetails() throws PharmacyException {
        String id = ConsoleInput.readRequired("Enter Medicine ID (e.g. MED-0001): ");
        Medicine med = inventoryService.getDetails(id);
        MedicineTable.printDetails(med, false);
        ConsoleInput.pause();
    }
}
