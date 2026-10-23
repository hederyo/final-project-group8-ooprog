package pharmacy.orders.ui;

import pharmacy.common.exception.PharmacyException;
import pharmacy.common.ui.BaseMenu;
import pharmacy.common.util.ConsoleInput;
import pharmacy.common.util.Format;
import pharmacy.common.util.InputValidator;
import pharmacy.orders.AnalyticsService;
import pharmacy.orders.model.SalesReport;

import java.time.LocalDate;

/**
 * Menu displaying sales, transaction volumes, discount totals, and inventory shrinkage analytics.
 *
 * Demonstrates:
 * - Executive Reporting: Generates financial summaries aggregated over custom or standard calendar periods.
 */
public class AnalyticsMenu extends BaseMenu {

    private final AnalyticsService analyticsService;

    public AnalyticsMenu() {
        this.analyticsService = new AnalyticsService();
    }

    public AnalyticsMenu(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    protected String getTitle() {
        return "Sales & Inventory Analytics (Admin)";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "Daily sales report",
                "Monthly sales report",
                "Yearly sales report",
                "Custom date range report"
        };
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> handleDaily();
            case 2 -> handleMonthly();
            case 3 -> handleYearly();
            case 4 -> handleCustomRange();
        }
    }

    private void handleDaily() throws PharmacyException {
        String input = ConsoleInput.readOptional("Enter date (yyyy-MM-dd, or press Enter for today): ");
        LocalDate target = input.isEmpty() ? LocalDate.now() : InputValidator.requireDate(input);
        renderReport(target, target, "Daily Sales Report (" + Format.DATE.format(target) + ")");
    }

    private void handleMonthly() throws PharmacyException {
        int currentYear = LocalDate.now().getYear();
        int year = ConsoleInput.readInt("Enter year (2000-" + currentYear + "): ", 2000, currentYear);
        int month = ConsoleInput.readInt("Enter month (1-12): ", 1, 12);

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        renderReport(from, to, "Monthly Sales Report (" + year + "-" + String.format("%02d", month) + ")");
    }

    private void handleYearly() throws PharmacyException {
        int currentYear = LocalDate.now().getYear();
        int year = ConsoleInput.readInt("Enter year (2000-" + currentYear + "): ", 2000, currentYear);

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        renderReport(from, to, "Annual Sales Report (" + year + ")");
    }

    private void handleCustomRange() throws PharmacyException {
        LocalDate from = ConsoleInput.readDate("Start date (yyyy-MM-dd): ");
        LocalDate to = ConsoleInput.readDate("End date (yyyy-MM-dd): ");
        renderReport(from, to, "Custom Period Report (" + Format.DATE.format(from) + " to " + Format.DATE.format(to) + ")");
    }

    private void renderReport(LocalDate from, LocalDate to, String heading) throws PharmacyException {
        SalesReport r = analyticsService.report(from, to);

        System.out.println("\n" + "=".repeat(65));
        System.out.println(heading.toUpperCase());
        System.out.println("Period: " + Format.DATE.format(r.getFrom()) + " to " + Format.DATE.format(r.getTo()));
        System.out.println("-".repeat(65));

        System.out.printf("%-30s : %s%n", "Gross Revenue (Completed)", Format.peso(r.getGrossRevenue()));
        System.out.printf("%-30s : %d%n", "Completed Transactions", r.getTransactionCount());
        System.out.printf("%-30s : %s%n", "Total Discounts Given", Format.peso(r.getTotalDiscounts()));
        System.out.printf("%-30s : %d%n", "Cancelled Orders", r.getCancelledCount());
        System.out.printf("%-30s : %d%n", "Voided Orders", r.getVoidedCount());

        System.out.println("-".repeat(65));
        System.out.println("INVENTORY SHRINKAGE (Discrepancies from Audits):");
        System.out.printf("%-30s : %d units%n", "Shrinkage Quantity Lost", r.getShrinkageUnits());
        System.out.printf("%-30s : %s%n", "Shrinkage Estimated Cost", Format.peso(r.getShrinkagePesos()));

        System.out.println("-".repeat(65));
        System.out.println("TOP 5 MEDICINES BY UNITS SOLD:");
        if (r.getTopMedicines().isEmpty()) {
            System.out.println("(no sales recorded in this period)");
        } else {
            System.out.printf("%-4s | %-12s | %-25s | %-6s | %-12s%n", "Rank", "Medicine ID", "Brand & Strength", "Units", "Revenue");
            System.out.println("-".repeat(65));
            int rank = 1;
            for (SalesReport.TopMedicine tm : r.getTopMedicines()) {
                System.out.printf("%-4d | %-12s | %-25s | %-6d | %-12s%n",
                        rank++,
                        tm.getMedicineId(),
                        truncate(tm.getMedicineName(), 25),
                        tm.getUnitsSold(),
                        Format.peso(tm.getTotalRevenue()));
            }
        }
        System.out.println("=".repeat(65));
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }
}
