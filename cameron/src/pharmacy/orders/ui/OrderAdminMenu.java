package pharmacy.orders.ui;

import pharmacy.common.exception.PharmacyException;
import pharmacy.common.ui.BaseMenu;
import pharmacy.common.util.ConsoleInput;
import pharmacy.common.util.Format;
import pharmacy.inventory.MedicineRepository;
import pharmacy.inventory.model.Medicine;
import pharmacy.orders.OrderRepository;
import pharmacy.orders.OrderService;
import pharmacy.orders.model.Order;
import pharmacy.orders.model.OrderItem;
import pharmacy.orders.model.OrderStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Administrator menu providing full system-wide order auditing, search, status overrides, and voiding.
 *
 * Demonstrates:
 * - Role-Based UI Presentation: Exposes administrative controls strictly guarded by Session.requireRole(ADMIN).
 * - Integration with Heather's module: Displays human-friendly pharmacist full names via UserRepository.findById.
 */
public class OrderAdminMenu extends BaseMenu {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final MedicineRepository medicineRepository;

    public OrderAdminMenu() {
        this.orderService = new OrderService();
        this.orderRepository = new OrderRepository();
        this.medicineRepository = new MedicineRepository();
    }

    public OrderAdminMenu(OrderService orderService, OrderRepository orderRepository, MedicineRepository medicineRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.medicineRepository = medicineRepository;
    }

    @Override
    protected String getTitle() {
        return "Orders & Prescriptions Administration (Admin)";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "All orders (view recent transactions)",
                "Search & filter orders",
                "View order details",
                "Update order status",
                "Void order (completed transactions only)",
                "Prescription audit"
        };
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> handleAllOrders();
            case 2 -> handleSearchOrders();
            case 3 -> handleViewOrderDetails();
            case 4 -> handleUpdateStatus();
            case 5 -> handleVoidOrder();
            case 6 -> handlePrescriptionAudit();
        }
    }

    // =========================================================================
    // Admin Features
    // =========================================================================

    private void handleAllOrders() throws PharmacyException {
        List<Order> orders = orderService.allOrders();
        printOrderTable(orders, "All Orders (System-wide)");
    }

    private void handleSearchOrders() throws PharmacyException {
        System.out.println("\n--- Search & Filter Orders ---");
        String kw = ConsoleInput.readOptional("Keyword (Order ID, Customer, Doctor License, or Enter to skip): ");

        System.out.println("Filter by status: 1 All · 2 Pending · 3 On Hold · 4 Completed · 5 Cancelled · 6 Voided");
        int stChoice = ConsoleInput.readInt("Choice (1-6): ", 1, 6);
        OrderStatus status = switch (stChoice) {
            case 2 -> OrderStatus.PENDING;
            case 3 -> OrderStatus.ON_HOLD;
            case 4 -> OrderStatus.COMPLETED;
            case 5 -> OrderStatus.CANCELLED;
            case 6 -> OrderStatus.VOIDED;
            default -> null;
        };

        String phId = ConsoleInput.readOptional("Pharmacist User ID (e.g. U-0002, or Enter to skip): ");

        LocalDate from = null;
        LocalDate to = null;
        if (ConsoleInput.readYesNo("Filter by date range? (y/n): ")) {
            from = ConsoleInput.readDate("Start date (yyyy-MM-dd): ");
            to = ConsoleInput.readDate("End date (yyyy-MM-dd): ");
            if (from.isAfter(to)) {
                System.out.println("Start date cannot be after end date. Ignoring date filter.");
                from = null;
                to = null;
            }
        }

        List<Order> results = orderService.searchOrders(kw, status, phId, from, to);
        printOrderTable(results, "Search Results (" + results.size() + " matches)");
    }

    private void handleViewOrderDetails() throws PharmacyException {
        String orderId = ConsoleInput.readRequired("Enter Order ID to view: ").toUpperCase();
        Order order = orderRepository.findById(orderId);
        printFullOrderDetails(order);
    }

    private void handleUpdateStatus() throws PharmacyException {
        String orderId = ConsoleInput.readRequired("Enter Order ID to update: ").toUpperCase();
        Order order = orderRepository.findById(orderId);

        if (order.getStatus().isTerminal()) {
            System.out.println("Order " + order.getId() + " is in terminal status " + order.getStatus().getLabel() + " and cannot be modified.");
            return;
        }

        System.out.println("\nCurrent status of " + order.getId() + ": " + order.getStatus().getLabel());
        List<OrderStatus> eligible = new ArrayList<>();
        for (OrderStatus st : OrderStatus.values()) {
            if (order.getStatus().canTransitionTo(st) && st != OrderStatus.VOIDED) {
                eligible.add(st);
            }
        }

        if (eligible.isEmpty()) {
            System.out.println("No status transitions available from " + order.getStatus().getLabel() + ".");
            return;
        }

        System.out.println("Available transitions:");
        for (int i = 0; i < eligible.size(); i++) {
            System.out.println((i + 1) + ". " + eligible.get(i).getLabel());
        }
        System.out.println("0. Cancel");

        int choice = ConsoleInput.readInt("Choose new status: ", 0, eligible.size());
        if (choice == 0) return;

        OrderStatus target = eligible.get(choice - 1);
        if (ConsoleInput.readYesNo("Confirm updating status of " + order.getId() + " to " + target.getLabel() + "? (y/n): ")) {
            orderService.updateStatus(order.getId(), target);
            System.out.println("SUCCESS: Status of order " + order.getId() + " updated to " + target.getLabel() + ".");
        }
    }

    private void handleVoidOrder() throws PharmacyException {
        String orderId = ConsoleInput.readRequired("Enter Order ID to VOID: ").toUpperCase();
        Order order = orderRepository.findById(orderId);

        if (order.getStatus() != OrderStatus.COMPLETED) {
            System.out.println("Only COMPLETED orders can be voided. (Current status: " + order.getStatus().getLabel() + ")");
            return;
        }

        printFullOrderDetails(order);
        System.out.println("\nCAUTION: Voiding is irreversible. It will mark the order as VOIDED, return all "
                + order.getTotalUnits() + " unit(s) back into active stock, and log an administrative memo.");

        if (!ConsoleInput.readYesNo("Are you sure you want to void order " + order.getId() + "? (y/n): ")) {
            System.out.println("Void operation aborted.");
            return;
        }

        String reason = ConsoleInput.readRequired("Administrative reason for voiding (5-200 chars): ");
        orderService.voidOrder(order.getId(), reason);
        System.out.println("SUCCESS: Order " + order.getId() + " has been VOIDED. Stock restored and void record logged.");
    }

    private void handlePrescriptionAudit() throws PharmacyException {
        System.out.println("\n--- Prescription Audit Report ---");
        String docLicense = ConsoleInput.readOptional("Doctor PRC License filter (or press Enter to view all): ");

        LocalDate from = null;
        LocalDate to = null;
        if (ConsoleInput.readYesNo("Filter by date range? (y/n): ")) {
            from = ConsoleInput.readDate("Start date (yyyy-MM-dd): ");
            to = ConsoleInput.readDate("End date (yyyy-MM-dd): ");
            if (from.isAfter(to)) {
                System.out.println("Invalid date range. Start date must be before end date. Ignoring date filter.");
                from = null;
                to = null;
            }
        }

        List<Order> rxOrders = orderService.prescriptionAudit(docLicense, from, to);
        if (rxOrders.isEmpty()) {
            System.out.println("No prescription orders matched the criteria.");
            return;
        }

        System.out.println("\n" + "=".repeat(110));
        System.out.printf("%-12s | %-12s | %-14s | %-18s | %-20s | %-10s%n",
                "Order ID", "Date", "Doctor License", "Prescription ID", "Dispensing Pharmacist", "Status");
        System.out.println("-".repeat(110));

        for (Order o : rxOrders) {
            String phName = orderService.getPharmacistName(o.getPharmacistId());
            System.out.printf("%-12s | %-12s | %-14s | %-18s | %-20s | %-10s%n",
                    o.getId(),
                    Format.DATE.format(o.getCreatedAt()),
                    o.getDoctorLicense(),
                    truncate(o.getPrescriptionId(), 18),
                    truncate(phName, 20),
                    o.getStatus().getLabel());
        }
        System.out.println("=".repeat(110));
    }

    // =========================================================================
    // Table Output Formatting Helpers
    // =========================================================================

    private void printOrderTable(List<Order> orders, String title) {
        System.out.println("\n=== " + title + " ===");
        if (orders.isEmpty()) {
            System.out.println("No orders to display.");
            return;
        }

        System.out.println("-".repeat(105));
        System.out.printf("%-12s | %-19s | %-18s | %-18s | %-6s | %-12s | %-10s%n",
                "Order ID", "Date / Time", "Pharmacist", "Customer", "Units", "Total", "Status");
        System.out.println("-".repeat(105));

        for (Order o : orders) {
            String phName = orderService.getPharmacistName(o.getPharmacistId());
            System.out.printf("%-12s | %-19s | %-18s | %-18s | %-6d | %-12s | %-10s%n",
                    o.getId(),
                    Format.DATE_TIME.format(o.getCreatedAt()),
                    truncate(phName, 18),
                    truncate(o.getCustomerName(), 18),
                    o.getTotalUnits(),
                    Format.peso(o.getTotal()),
                    o.getStatus().getLabel());
        }
        System.out.println("-".repeat(105));
    }

    private void printFullOrderDetails(Order order) {
        System.out.println("\n" + "=".repeat(75));
        System.out.println("TRANSACTION DETAILS: " + order.getId());
        System.out.println("Status:       " + order.getStatus().getLabel());
        System.out.println("Pharmacist:   " + orderService.getPharmacistName(order.getPharmacistId()) + " (" + order.getPharmacistId() + ")");
        System.out.println("Customer:     " + order.getCustomerName() + (order.getCustomerContact().isEmpty() ? "" : " [" + order.getCustomerContact() + "]"));
        System.out.println("Created At:   " + Format.DATE_TIME.format(order.getCreatedAt()));
        System.out.println("Updated At:   " + Format.DATE_TIME.format(order.getUpdatedAt()));
        if (order.getCompletedAt() != null) {
            System.out.println("Completed At: " + Format.DATE_TIME.format(order.getCompletedAt()));
        }
        if (!order.getPrescriptionId().isEmpty()) {
            System.out.println("Prescription: " + order.getPrescriptionId() + " (Doctor PRC License: " + order.getDoctorLicense() + ")");
        }

        System.out.println("\nLine Items:");
        System.out.printf("%-12s | %-25s | %-6s | %-12s | %-12s%n", "Medicine ID", "Brand & Strength", "Qty", "Unit Price", "Total");
        System.out.println("-".repeat(75));
        for (OrderItem item : order.getItems()) {
            String medLabel = item.getMedicineId();
            try {
                Medicine m = medicineRepository.findById(item.getMedicineId());
                medLabel = m.getBrandName() + " " + m.getDosageStrength();
            } catch (Exception ignored) {}

            System.out.printf("%-12s | %-25s | %-6d | %-12s | %-12s%n",
                    item.getMedicineId(),
                    truncate(medLabel, 25),
                    item.getQuantity(),
                    Format.peso(item.getUnitPriceAtSale()),
                    Format.peso(item.getLineTotal()));
        }
        System.out.println("-".repeat(75));
        System.out.printf("Total Units:     %-6d    Subtotal: %s%n", order.getTotalUnits(), Format.peso(order.getSubtotal()));
        System.out.printf("Discount Applied: %-30s%n", order.getDiscount().getLabel());
        if (!order.getDiscountIdNumber().isEmpty()) {
            System.out.printf("Discount ID:      %-30s%n", order.getDiscountIdNumber());
        }
        System.out.printf("Discount Amount: -%s%n", Format.peso(order.getDiscountAmount()));
        System.out.printf("NET TOTAL PAID:   %s%n", Format.peso(order.getTotal()));
        if (!order.getNotes().isEmpty()) {
            System.out.println("Customer Notes:   " + order.getNotes());
        }
        System.out.println("=".repeat(75));
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }
}
