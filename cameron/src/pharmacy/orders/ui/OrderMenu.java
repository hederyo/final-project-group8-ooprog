package pharmacy.orders.ui;

import pharmacy.common.exception.PharmacyException;
import pharmacy.common.session.Session;
import pharmacy.common.ui.BaseMenu;
import pharmacy.common.util.ConsoleInput;
import pharmacy.common.util.Format;
import pharmacy.common.util.InputValidator;
import pharmacy.inventory.MedicineRepository;
import pharmacy.inventory.model.Medicine;
import pharmacy.orders.OrderRepository;
import pharmacy.orders.OrderService;
import pharmacy.orders.discount.BulkDiscount;
import pharmacy.orders.discount.DiscountType;
import pharmacy.orders.model.Order;
import pharmacy.orders.model.OrderItem;
import pharmacy.orders.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pharmacist order management menu handling dispensing, carts, discounts, and order lifecycle.
 *
 * Demonstrates:
 * - Presentation Layer separation: Delegates all validation, stock movement, and persistence to OrderService.
 * - Polymorphic exception handling: Catches PharmacyException through BaseMenu's execution loop.
 */
public class OrderMenu extends BaseMenu {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final MedicineRepository medicineRepository;

    public OrderMenu() {
        this.orderService = new OrderService();
        this.orderRepository = new OrderRepository();
        this.medicineRepository = new MedicineRepository();
    }

    public OrderMenu(OrderService orderService, OrderRepository orderRepository, MedicineRepository medicineRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.medicineRepository = medicineRepository;
    }

    @Override
    protected String getTitle() {
        return "Dispensing & Orders (Pharmacist)";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "New order (dispense medication)",
                "My orders (view & filter)",
                "Update order (pending / on hold)",
                "Complete order",
                "Cancel order"
        };
    }

    @Override
    protected void handle(int choice) throws PharmacyException {
        switch (choice) {
            case 1 -> handleNewOrder();
            case 2 -> handleMyOrders();
            case 3 -> handleUpdateOrder();
            case 4 -> handleCompleteOrder();
            case 5 -> handleCancelOrder();
        }
    }

    // =========================================================================
    // O1 & O2: Process Dispensing Order & Apply Discount
    // =========================================================================

    private void handleNewOrder() throws PharmacyException {
        System.out.println("\n--- Step 1: Customer Information ---");
        String customerName = ConsoleInput.readValidated("Customer name: ",
                val -> InputValidator.requireName("Customer name", val));

        String rawContact = ConsoleInput.readOptional("Contact number (09XXXXXXXXX or press Enter to skip): ");
        String customerContact = "";
        if (!rawContact.isEmpty()) {
            customerContact = InputValidator.requirePhone(rawContact);
        }

        String orderId = orderRepository.nextId();
        String pharmacistId = Session.getInstance().requireUser().getId();
        Order draft = new Order(orderId, pharmacistId, customerName, customerContact,
                DiscountType.REGULAR, "", "", "", "", OrderStatus.PENDING,
                LocalDateTime.now(), LocalDateTime.now(), null);

        System.out.println("\n--- Step 2: Medication Cart ---");
        while (true) {
            printCart(draft);
            System.out.println("Cart actions: 1 Add item · 2 Remove item · 3 Proceed to discount · 0 Discard draft");
            int act = ConsoleInput.readInt("Choice: ", 0, 3);

            if (act == 0) {
                if (ConsoleInput.readYesNo("Are you sure you want to discard this draft? Stock will not be affected. (y/n): ")) {
                    System.out.println("Draft discarded. Stock remains unchanged.");
                    return;
                }
                continue;
            }

            if (act == 1) {
                String medId = ConsoleInput.readRequired("Enter Medicine ID (e.g. MED-0001): ").toUpperCase();
                int qty = ConsoleInput.readInt("Quantity (1-1000): ", 1, OrderService.MAX_ITEM_QUANTITY);
                try {
                    Medicine med = orderService.checkItem(medId, qty);
                    draft.addOrMergeItem(med.getId(), qty, med.getUnitPrice());
                    System.out.println("Added " + qty + "x " + med.getBrandName() + " (" + med.getDosageStrength() + ").");
                } catch (Exception e) {
                    System.out.println("Cannot add item: " + e.getMessage());
                }
            } else if (act == 2) {
                if (draft.getItems().isEmpty()) {
                    System.out.println("Cart is currently empty.");
                    continue;
                }
                String medId = ConsoleInput.readRequired("Enter Medicine ID to remove: ").toUpperCase();
                if (draft.removeItem(medId)) {
                    System.out.println("Removed " + medId + " from cart.");
                } else {
                    System.out.println("Medicine " + medId + " is not in the cart.");
                }
            } else if (act == 3) {
                if (draft.getItems().isEmpty()) {
                    System.out.println("Cart must contain at least one item before proceeding.");
                    continue;
                }
                break;
            }
        }

        // Step 3: Prescription Validation
        boolean rxFound = false;
        for (OrderItem item : draft.getItems()) {
            Medicine med = medicineRepository.findById(item.getMedicineId());
            if (med.isPrescriptionRequired()) {
                rxFound = true;
                break;
            }
        }

        if (rxFound) {
            System.out.println("\n--- Step 3: Prescription Verification (Rx items detected) ---");
            String rxId = ConsoleInput.readValidated("Prescription ID (4-20 alphanumeric): ",
                    InputValidator::requirePrescriptionId);
            String docLicense = ConsoleInput.readValidated("Attending Doctor PRC License (7 digits): ",
                    InputValidator::requireLicenseNumber);
            draft.setPrescriptionId(rxId);
            draft.setDoctorLicense(docLicense);
        }

        // Step 4: Discount Selection (O2)
        System.out.println("\n--- Step 4: Select Discount Policy ---");
        System.out.println("1. Regular (0%)");
        System.out.println("2. Senior Citizen / PWD (20%)");
        System.out.println("3. Bulk Wholesale (10% - requires 50+ total units)");

        int discChoice = ConsoleInput.readInt("Choose discount (1-3): ", 1, 3);
        if (discChoice == 1) {
            draft.setDiscountType(DiscountType.REGULAR);
            draft.setDiscountIdNumber("");
        } else if (discChoice == 2) {
            String idNum = ConsoleInput.readValidated("Senior Citizen / PWD ID number: ",
                    InputValidator::requireSeniorPwdId);
            draft.setDiscountType(DiscountType.SENIOR_PWD);
            draft.setDiscountIdNumber(idNum);
        } else if (discChoice == 3) {
            if (draft.getTotalUnits() < BulkDiscount.MIN_UNITS) {
                System.out.println("Ineligible for Bulk discount (Cart has " + draft.getTotalUnits() + " units, 50 required).");
                System.out.println("Applying Regular (0%) pricing instead.");
                draft.setDiscountType(DiscountType.REGULAR);
                draft.setDiscountIdNumber("");
            } else {
                draft.setDiscountType(DiscountType.BULK);
                draft.setDiscountIdNumber("");
            }
        }

        String notes = ConsoleInput.readOptional("Order Notes (optional, press Enter to skip): ");
        if (!notes.isEmpty()) {
            draft.setNotes(InputValidator.requireText("Notes", notes, 200));
        }

        // Step 5: Summary & Final Decision
        printOrderSummary(draft);
        System.out.println("Final decision: 1 Complete now · 2 Save as pending · 0 Discard draft");
        int decision = ConsoleInput.readInt("Choice: ", 0, 2);

        if (decision == 0) {
            System.out.println("Draft order discarded. Inventory untouched.");
            return;
        }

        boolean completeNow = (decision == 1);
        orderService.placeOrder(draft, completeNow);

        if (completeNow) {
            System.out.println("SUCCESS: Order " + draft.getId() + " completed! Total paid: " + Format.peso(draft.getTotal()));
        } else {
            System.out.println("SUCCESS: Order " + draft.getId() + " saved as PENDING.");
        }
    }

    // =========================================================================
    // O3: View Sales Orders
    // =========================================================================

    private void handleMyOrders() throws PharmacyException {
        System.out.println("\nFilter orders: 1 All · 2 Pending · 3 On Hold · 4 Completed · 5 Cancelled · 0 Back");
        int filterChoice = ConsoleInput.readInt("Choice: ", 0, 5);
        if (filterChoice == 0) return;

        OrderStatus filter = switch (filterChoice) {
            case 2 -> OrderStatus.PENDING;
            case 3 -> OrderStatus.ON_HOLD;
            case 4 -> OrderStatus.COMPLETED;
            case 5 -> OrderStatus.CANCELLED;
            default -> null;
        };

        List<Order> orders = orderService.getMyOrders(filter);
        if (orders.isEmpty()) {
            System.out.println("No matching orders found.");
            return;
        }

        System.out.println("\n" + "-".repeat(95));
        System.out.printf("%-12s | %-19s | %-20s | %-6s | %-12s | %-10s%n",
                "Order ID", "Date / Time", "Customer", "Units", "Total", "Status");
        System.out.println("-".repeat(95));

        for (Order o : orders) {
            System.out.printf("%-12s | %-19s | %-20s | %-6d | %-12s | %-10s%n",
                    o.getId(),
                    Format.DATE_TIME.format(o.getCreatedAt()),
                    truncate(o.getCustomerName(), 20),
                    o.getTotalUnits(),
                    Format.peso(o.getTotal()),
                    o.getStatus().getLabel());
        }
        System.out.println("-".repeat(95));

        String inspectId = ConsoleInput.readOptional("Enter Order ID to view details (or press Enter to return): ");
        if (!inspectId.isEmpty()) {
            Order o = orderService.getMyOrder(inspectId);
            printOrderDetails(o);
        }
    }

    // =========================================================================
    // O4: Update Pending Order
    // =========================================================================

    private void handleUpdateOrder() throws PharmacyException {
        String orderId = ConsoleInput.readRequired("Enter Order ID to update: ").toUpperCase();
        Order order = orderService.getMyOrder(orderId);

        if (!order.getStatus().isEditable()) {
            System.out.println("Only PENDING or ON_HOLD orders can be modified. (Status: " + order.getStatus().getLabel() + ")");
            return;
        }

        while (true) {
            printOrderDetails(order);
            System.out.println("\nUpdate options for " + order.getId() + ":");
            System.out.println("1 Add item");
            System.out.println("2 Change item quantity");
            System.out.println("3 Remove item");
            System.out.println("4 Change discount policy");
            System.out.println("5 Edit customer notes");
            System.out.println("6 " + (order.getStatus() == OrderStatus.PENDING ? "Put on hold" : "Resume to Pending"));
            System.out.println("0 Finished updating");

            int choice = ConsoleInput.readInt("Choice: ", 0, 6);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> {
                    String medId = ConsoleInput.readRequired("Medicine ID: ").toUpperCase();
                    int qty = ConsoleInput.readInt("Quantity to add (1-1000): ", 1, OrderService.MAX_ITEM_QUANTITY);
                    orderService.addItem(order.getId(), medId, qty);
                    System.out.println("Item added successfully.");
                }
                case 2 -> {
                    String medId = ConsoleInput.readRequired("Medicine ID to modify: ").toUpperCase();
                    int newQty = ConsoleInput.readInt("New total quantity (1-1000): ", 1, OrderService.MAX_ITEM_QUANTITY);
                    orderService.changeQuantity(order.getId(), medId, newQty);
                    System.out.println("Quantity updated successfully.");
                }
                case 3 -> {
                    String medId = ConsoleInput.readRequired("Medicine ID to remove: ").toUpperCase();
                    orderService.removeItem(order.getId(), medId);
                    System.out.println("Item removed from order.");
                }
                case 4 -> {
                    System.out.println("1 Regular (0%) · 2 Senior/PWD (20%) · 3 Bulk Wholesale (10%)");
                    int dt = ConsoleInput.readInt("Select new discount: ", 1, 3);
                    DiscountType newType = switch (dt) {
                        case 2 -> DiscountType.SENIOR_PWD;
                        case 3 -> DiscountType.BULK;
                        default -> DiscountType.REGULAR;
                    };
                    String idNum = "";
                    if (newType == DiscountType.SENIOR_PWD) {
                        idNum = ConsoleInput.readValidated("Senior / PWD ID: ", InputValidator::requireSeniorPwdId);
                    }
                    orderService.changeDiscount(order.getId(), newType, idNum);
                    System.out.println("Discount policy updated.");
                }
                case 5 -> {
                    String notes = ConsoleInput.readOptional("New notes (or press Enter to clear): ");
                    orderService.updateNotes(order.getId(), notes);
                    System.out.println("Notes updated.");
                }
                case 6 -> {
                    if (order.getStatus() == OrderStatus.PENDING) {
                        orderService.putOnHold(order.getId());
                        System.out.println("Order " + order.getId() + " is now ON HOLD.");
                    } else {
                        orderService.resume(order.getId());
                        System.out.println("Order " + order.getId() + " has been resumed to PENDING.");
                    }
                }
            }
            // Reload order state after modifications
            order = orderService.getMyOrder(order.getId());
        }
    }

    // =========================================================================
    // O5: Complete Order
    // =========================================================================

    private void handleCompleteOrder() throws PharmacyException {
        String orderId = ConsoleInput.readRequired("Enter Order ID to complete: ").toUpperCase();
        Order order = orderService.getMyOrder(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            System.out.println("Only PENDING orders can be completed. (Current status: " + order.getStatus().getLabel() + ")");
            return;
        }

        printOrderSummary(order);
        if (ConsoleInput.readYesNo("Confirm completion and finalize transaction for " + Format.peso(order.getTotal()) + "? (y/n): ")) {
            orderService.complete(order.getId());
            System.out.println("SUCCESS: Order " + order.getId() + " is now COMPLETED.");
        } else {
            System.out.println("Completion aborted.");
        }
    }

    // =========================================================================
    // O6: Cancel Dispensing Order
    // =========================================================================

    private void handleCancelOrder() throws PharmacyException {
        String orderId = ConsoleInput.readRequired("Enter Order ID to cancel: ").toUpperCase();
        Order order = orderService.getMyOrder(orderId);

        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            System.out.println("Order " + order.getId() + " is in status " + order.getStatus().getLabel() + " and cannot be cancelled.");
            return;
        }

        printOrderSummary(order);
        System.out.println("WARNING: Cancelling will return all " + order.getTotalUnits() + " reserved unit(s) back into active stock.");
        if (ConsoleInput.readYesNo("Are you sure you want to cancel this order? (y/n): ")) {
            orderService.cancel(order.getId());
            System.out.println("SUCCESS: Order " + order.getId() + " has been CANCELLED. All stock returned.");
        } else {
            System.out.println("Cancellation aborted.");
        }
    }

    // =========================================================================
    // Visual Formatting Helpers
    // =========================================================================

    private void printCart(Order order) {
        System.out.println("\n--- Current Cart ---");
        if (order.getItems().isEmpty()) {
            System.out.println("(cart is empty)");
            return;
        }
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
        System.out.printf("Total Units: %-5d   Subtotal: %s%n", order.getTotalUnits(), Format.peso(order.getSubtotal()));
    }

    private void printOrderSummary(Order order) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ORDER SUMMARY: " + order.getId());
        System.out.println("Customer: " + order.getCustomerName() + (order.getCustomerContact().isEmpty() ? "" : " (" + order.getCustomerContact() + ")"));
        if (!order.getPrescriptionId().isEmpty()) {
            System.out.println("Prescription ID: " + order.getPrescriptionId() + " | Doctor License: " + order.getDoctorLicense());
        }
        printCart(order);
        System.out.printf("Discount Applied: %-30s%n", order.getDiscount().getLabel());
        if (!order.getDiscountIdNumber().isEmpty()) {
            System.out.printf("Discount ID:      %-30s%n", order.getDiscountIdNumber());
        }
        System.out.printf("Discount Amount: -%s%n", Format.peso(order.getDiscountAmount()));
        System.out.printf("FINAL TOTAL:      %s%n", Format.peso(order.getTotal()));
        if (!order.getNotes().isEmpty()) {
            System.out.println("Notes: " + order.getNotes());
        }
        System.out.println("=".repeat(60));
    }

    private void printOrderDetails(Order order) {
        printOrderSummary(order);
        System.out.println("Status:       " + order.getStatus().getLabel());
        System.out.println("Created At:   " + Format.DATE_TIME.format(order.getCreatedAt()));
        System.out.println("Updated At:   " + Format.DATE_TIME.format(order.getUpdatedAt()));
        if (order.getCompletedAt() != null) {
            System.out.println("Completed At: " + Format.DATE_TIME.format(order.getCompletedAt()));
        }
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }
}
