package pharmacy.orders;

import pharmacy.auth.UserRepository;
import pharmacy.common.exception.AccessDeniedException;
import pharmacy.common.exception.AuthenticationException;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.NotFoundException;
import pharmacy.common.exception.PharmacyException;
import pharmacy.common.exception.ValidationException;
import pharmacy.common.model.Role;
import pharmacy.common.model.User;
import pharmacy.common.session.Session;
import pharmacy.common.util.InputValidator;
import pharmacy.inventory.InventoryService;
import pharmacy.inventory.MedicineRepository;
import pharmacy.inventory.exception.InsufficientStockException;
import pharmacy.inventory.model.Medicine;
import pharmacy.orders.discount.BulkDiscount;
import pharmacy.orders.discount.DiscountType;
import pharmacy.orders.exception.InvalidStatusTransitionException;
import pharmacy.orders.model.Order;
import pharmacy.orders.model.OrderItem;
import pharmacy.orders.model.OrderStatus;
import pharmacy.orders.model.VoidRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service orchestrating order transactions, stock movement, discount validations, and lifecycle operations.
 *
 * Demonstrates:
 * - Business logic layer isolation: Mediates between UI menus and persistent repositories without console I/O.
 * - Compensating Transactions / Rollback: Restores reserved inventory if an order save fails across flat files.
 * - Cross-member contracts: Exposes hasOrdersForPharmacist and hasOrdersForMedicine for Heather and Karlo.
 */
public class OrderService {

    public static final int MAX_ITEM_QUANTITY = 1000;

    private final OrderRepository orderRepository;
    private final VoidRecordRepository voidRecordRepository;
    private final InventoryService inventoryService;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;

    public OrderService() {
        this(new OrderRepository(), new VoidRecordRepository(), new InventoryService(),
                new MedicineRepository(), new UserRepository());
    }

    public OrderService(OrderRepository orderRepository, VoidRecordRepository voidRecordRepository,
                        InventoryService inventoryService, MedicineRepository medicineRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.voidRecordRepository = voidRecordRepository;
        this.inventoryService = inventoryService;
        this.medicineRepository = medicineRepository;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // 1. Cross-member contracts (Heather & Karlo)
    // =========================================================================

    public boolean hasOrdersForPharmacist(String userId) throws DataAccessException {
        return orderRepository.existsByPharmacist(userId);
    }

    public boolean hasOrdersForMedicine(String medicineId) throws DataAccessException {
        return orderRepository.existsByMedicine(medicineId);
    }

    // =========================================================================
    // 2. Pharmacist Order Placement (O1, O2)
    // =========================================================================

    /**
     * Checks if a medicine is sellable and has sufficient stock for an in-memory cart item.
     * Does NOT reserve inventory.
     */
    public Medicine checkItem(String medicineId, int quantity)
            throws NotFoundException, InsufficientStockException, ValidationException, DataAccessException {
        if (quantity < 1 || quantity > MAX_ITEM_QUANTITY) {
            throw new ValidationException("Item quantity must be between 1 and " + MAX_ITEM_QUANTITY + ".");
        }
        Medicine med = inventoryService.findSellable(medicineId);
        if (quantity > med.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Only " + med.getStockQuantity() + " units of " + med.getBrandName() + " " + med.getDosageStrength() + " left.");
        }
        return med;
    }

    /**
     * Places a dispensing order: validates rules O1-O7, reserves inventory with rollback, and saves.
     */
    public void placeOrder(Order order, boolean completeNow)
            throws ValidationException, InsufficientStockException, NotFoundException, DataAccessException, PharmacyException {

        // Validate Customer Name (O1)
        InputValidator.requireName("Customer name", order.getCustomerName());

        // Validate Optional Phone (O1)
        if (order.getCustomerContact() != null && !order.getCustomerContact().trim().isEmpty()) {
            InputValidator.requirePhone(order.getCustomerContact());
        }

        // Validate Non-empty items (O2)
        if (order.getItems().isEmpty()) {
            throw new ValidationException("Order must have at least one item.");
        }

        // Validate items and verify prescription requirement (O3, O4, O5)
        boolean rxNeeded = false;
        for (OrderItem item : order.getItems()) {
            if (item.getQuantity() < 1 || item.getQuantity() > MAX_ITEM_QUANTITY) {
                throw new ValidationException("Item quantity must be between 1 and " + MAX_ITEM_QUANTITY + ".");
            }
            Medicine med = inventoryService.findSellable(item.getMedicineId());
            if (med.isPrescriptionRequired()) {
                rxNeeded = true;
            }
        }

        if (rxNeeded) {
            InputValidator.requirePrescriptionId(order.getPrescriptionId());
            InputValidator.requireLicenseNumber(order.getDoctorLicense());
        }

        // Validate Discount rules (O6, O7)
        if (order.getDiscountType() == DiscountType.SENIOR_PWD) {
            InputValidator.requireSeniorPwdId(order.getDiscountIdNumber());
        } else if (order.getDiscountType() == DiscountType.BULK) {
            if (!order.getDiscount().isEligible(order.getTotalUnits())) {
                throw new ValidationException(
                        "Bulk discount requires at least " + BulkDiscount.MIN_UNITS + " total units.");
            }
        }

        // Reserve stock for every item with atomic rollback on failure (ARCHITECTURE §9.2-9.3)
        List<OrderItem> successfullyReserved = new ArrayList<>();
        try {
            for (OrderItem item : order.getItems()) {
                inventoryService.reserveStock(item.getMedicineId(), item.getQuantity());
                successfullyReserved.add(item);
            }
        } catch (Exception e) {
            for (OrderItem res : successfullyReserved) {
                try {
                    inventoryService.restoreStock(res.getMedicineId(), res.getQuantity());
                } catch (Exception ignored) {}
            }
            throw e;
        }

        // Save order across flat files, rolling back all reserved stock if disk I/O fails
        try {
            if (completeNow) {
                order.setStatusDirectly(OrderStatus.COMPLETED);
                order.setCompletedAt(LocalDateTime.now());
            } else {
                order.setStatusDirectly(OrderStatus.PENDING);
            }
            orderRepository.save(order);
        } catch (DataAccessException dae) {
            for (OrderItem res : order.getItems()) {
                try {
                    inventoryService.restoreStock(res.getMedicineId(), res.getQuantity());
                } catch (Exception ignored) {}
            }
            throw dae;
        }
    }

    // =========================================================================
    // 3. Own Order Management (O3 - O6)
    // =========================================================================

    public List<Order> getMyOrders(OrderStatus statusOrNull) throws AuthenticationException, DataAccessException {
        User user = Session.getInstance().requireUser();
        List<Order> orders = orderRepository.findByPharmacist(user.getId());
        List<Order> filtered = new ArrayList<>();
        for (Order o : orders) {
            if (statusOrNull == null || o.getStatus() == statusOrNull) {
                filtered.add(o);
            }
        }
        filtered.sort(Comparator.comparing(Order::getCreatedAt).reversed());
        return filtered;
    }

    public Order getMyOrder(String orderId)
            throws AuthenticationException, NotFoundException, AccessDeniedException, DataAccessException {
        Order order = orderRepository.findById(orderId);
        checkOwnership(order);
        return order;
    }

    public void addItem(String orderId, String medicineId, int quantity)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            InsufficientStockException, ValidationException, DataAccessException {
        Order order = getMyOrder(orderId);
        ensureEditable(order);

        if (quantity < 1 || quantity > MAX_ITEM_QUANTITY) {
            throw new ValidationException("Quantity must be between 1 and " + MAX_ITEM_QUANTITY + ".");
        }

        Medicine med = inventoryService.findSellable(medicineId);
        if (med.isPrescriptionRequired()) {
            if (order.getPrescriptionId().isEmpty() || order.getDoctorLicense().isEmpty()) {
                throw new ValidationException(
                        "Adding a prescription drug requires a prescription ID and doctor license on the order.");
            }
        }

        inventoryService.reserveStock(medicineId, quantity);
        order.addOrMergeItem(medicineId, quantity, med.getUnitPrice());
        order.setUpdatedAt(LocalDateTime.now());

        try {
            orderRepository.save(order);
        } catch (DataAccessException dae) {
            try {
                inventoryService.restoreStock(medicineId, quantity);
            } catch (Exception ignored) {}
            throw dae;
        }
    }

    public void changeQuantity(String orderId, String medicineId, int newQuantity)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            InsufficientStockException, ValidationException, DataAccessException {
        Order order = getMyOrder(orderId);
        ensureEditable(order);

        OrderItem item = order.findItem(medicineId);
        if (item == null) {
            throw new NotFoundException("Medicine " + medicineId + " is not in order " + orderId + ".");
        }
        if (newQuantity < 1 || newQuantity > MAX_ITEM_QUANTITY) {
            throw new ValidationException("Quantity must be between 1 and " + MAX_ITEM_QUANTITY + ".");
        }

        int diff = newQuantity - item.getQuantity();
        if (diff == 0) return;

        int newTotalUnits = order.getTotalUnits() + diff;
        if (order.getDiscountType() == DiscountType.BULK && newTotalUnits < BulkDiscount.MIN_UNITS) {
            throw new ValidationException(
                    "Order units would drop below " + BulkDiscount.MIN_UNITS + ". Please change the discount type before lowering the quantity.");
        }

        if (diff > 0) {
            inventoryService.reserveStock(medicineId, diff);
        } else {
            inventoryService.restoreStock(medicineId, -diff);
        }

        int oldQuantity = item.getQuantity();
        item.setQuantity(newQuantity);
        order.setUpdatedAt(LocalDateTime.now());

        try {
            orderRepository.save(order);
        } catch (DataAccessException dae) {
            // rollback inventory adjustment
            if (diff > 0) {
                try { inventoryService.restoreStock(medicineId, diff); } catch (Exception ignored) {}
            } else {
                try { inventoryService.reserveStock(medicineId, -diff); } catch (Exception ignored) {}
            }
            item.setQuantity(oldQuantity);
            throw dae;
        }
    }

    public void removeItem(String orderId, String medicineId)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            ValidationException, DataAccessException {
        Order order = getMyOrder(orderId);
        ensureEditable(order);

        OrderItem item = order.findItem(medicineId);
        if (item == null) {
            throw new NotFoundException("Medicine " + medicineId + " is not in order " + orderId + ".");
        }

        if (order.getItems().size() <= 1) {
            throw new ValidationException("Cannot remove the only item in the order. Cancel the order instead.");
        }

        int newTotalUnits = order.getTotalUnits() - item.getQuantity();
        if (order.getDiscountType() == DiscountType.BULK && newTotalUnits < BulkDiscount.MIN_UNITS) {
            throw new ValidationException(
                    "Removing this item drops total units below " + BulkDiscount.MIN_UNITS + ". Please change discount type first.");
        }

        inventoryService.restoreStock(medicineId, item.getQuantity());
        order.removeItem(medicineId);
        order.setUpdatedAt(LocalDateTime.now());

        try {
            orderRepository.save(order);
        } catch (DataAccessException dae) {
            try {
                inventoryService.reserveStock(medicineId, item.getQuantity());
            } catch (Exception ignored) {}
            order.addOrMergeItem(medicineId, item.getQuantity(), item.getUnitPriceAtSale());
            throw dae;
        }
    }

    public void changeDiscount(String orderId, DiscountType newType, String discountIdNum)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            ValidationException, DataAccessException {
        Order order = getMyOrder(orderId);
        ensureEditable(order);

        if (newType == DiscountType.SENIOR_PWD) {
            InputValidator.requireSeniorPwdId(discountIdNum);
            order.setDiscountIdNumber(discountIdNum);
        } else if (newType == DiscountType.BULK) {
            if (order.getTotalUnits() < BulkDiscount.MIN_UNITS) {
                throw new ValidationException(
                        "Order has only " + order.getTotalUnits() + " units; Bulk requires at least " + BulkDiscount.MIN_UNITS + ".");
            }
            order.setDiscountIdNumber("");
        } else {
            order.setDiscountIdNumber("");
        }

        order.setDiscountType(newType);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    public void updateNotes(String orderId, String notes)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            ValidationException, DataAccessException {
        Order order = getMyOrder(orderId);
        ensureEditable(order);

        String clean = InputValidator.requireText("Notes", notes, 200);
        order.setNotes(clean);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    public void putOnHold(String orderId)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            DataAccessException {
        Order order = getMyOrder(orderId);
        order.transitionTo(OrderStatus.ON_HOLD);
        orderRepository.save(order);
    }

    public void resume(String orderId)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            DataAccessException {
        Order order = getMyOrder(orderId);
        order.transitionTo(OrderStatus.PENDING);
        orderRepository.save(order);
    }

    public void complete(String orderId)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            ValidationException, DataAccessException {
        Order order = getMyOrder(orderId);

        // Re-check expiry on every item before completion (Rule O11)
        for (OrderItem item : order.getItems()) {
            Medicine med = medicineRepository.findById(item.getMedicineId());
            if (med.isExpired(LocalDate.now())) {
                throw new ValidationException(
                        med.getBrandName() + " " + med.getDosageStrength() + " has expired and cannot be completed.");
            }
        }

        order.transitionTo(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    public void cancel(String orderId)
            throws AuthenticationException, NotFoundException, AccessDeniedException, InvalidStatusTransitionException,
            ValidationException, DataAccessException {
        Order order = getMyOrder(orderId);

        order.transitionTo(OrderStatus.CANCELLED);

        // Restore all reserved stock (Rule O12)
        for (OrderItem item : order.getItems()) {
            try {
                inventoryService.restoreStock(item.getMedicineId(), item.getQuantity());
            } catch (Exception ignored) {}
        }

        try {
            orderRepository.save(order);
        } catch (DataAccessException dae) {
            // Re-reserve on disk failure
            for (OrderItem item : order.getItems()) {
                try {
                    inventoryService.reserveStock(item.getMedicineId(), item.getQuantity());
                } catch (Exception ignored) {}
            }
            throw dae;
        }
    }

    // =========================================================================
    // 4. Admin Order Management (O7 - O10)
    // =========================================================================

    public List<Order> allOrders()
            throws AuthenticationException, AccessDeniedException, DataAccessException {
        Session.getInstance().requireRole(Role.ADMIN);
        List<Order> list = orderRepository.findAll();
        list.sort(Comparator.comparing(Order::getCreatedAt).reversed());
        return list;
    }

    public List<Order> searchOrders(String keyword, OrderStatus status, String pharmacistId,
                                    LocalDate from, LocalDate to)
            throws AuthenticationException, AccessDeniedException, DataAccessException {
        Session.getInstance().requireRole(Role.ADMIN);
        List<Order> list = orderRepository.findAll();
        List<Order> matches = new ArrayList<>();

        String kw = keyword != null ? keyword.trim().toLowerCase() : "";
        String phId = pharmacistId != null ? pharmacistId.trim() : "";

        for (Order o : list) {
            if (!kw.isEmpty()) {
                boolean hit = o.getId().toLowerCase().contains(kw)
                        || o.getCustomerName().toLowerCase().contains(kw)
                        || o.getDoctorLicense().toLowerCase().contains(kw);
                if (!hit) continue;
            }

            if (status != null && o.getStatus() != status) {
                continue;
            }

            if (!phId.isEmpty() && !o.getPharmacistId().equalsIgnoreCase(phId)) {
                continue;
            }

            LocalDate orderDate = o.getCreatedAt().toLocalDate();
            if (from != null && orderDate.isBefore(from)) {
                continue;
            }
            if (to != null && orderDate.isAfter(to)) {
                continue;
            }

            matches.add(o);
        }

        matches.sort(Comparator.comparing(Order::getCreatedAt).reversed());
        return matches;
    }

    public void updateStatus(String orderId, OrderStatus nextStatus)
            throws AuthenticationException, AccessDeniedException, NotFoundException, InvalidStatusTransitionException,
            ValidationException, DataAccessException {
        Session.getInstance().requireRole(Role.ADMIN);
        if (nextStatus == OrderStatus.VOIDED) {
            throw new ValidationException("Use Void Order to void a completed order.");
        }

        Order order = orderRepository.findById(orderId);

        if (nextStatus == OrderStatus.CANCELLED) {
            order.transitionTo(OrderStatus.CANCELLED);
            for (OrderItem item : order.getItems()) {
                try {
                    inventoryService.restoreStock(item.getMedicineId(), item.getQuantity());
                } catch (Exception ignored) {}
            }
            orderRepository.save(order);
            return;
        }

        if (nextStatus == OrderStatus.COMPLETED) {
            for (OrderItem item : order.getItems()) {
                Medicine med = medicineRepository.findById(item.getMedicineId());
                if (med.isExpired(LocalDate.now())) {
                    throw new ValidationException(
                            med.getBrandName() + " " + med.getDosageStrength() + " has expired.");
                }
            }
        }

        order.transitionTo(nextStatus);
        orderRepository.save(order);
    }

    public void voidOrder(String orderId, String reason)
            throws AuthenticationException, AccessDeniedException, NotFoundException, InvalidStatusTransitionException,
            ValidationException, DataAccessException {
        Session.getInstance().requireRole(Role.ADMIN);
        User admin = Session.getInstance().requireUser();

        InputValidator.requireNonBlank("Void reason", reason);
        String cleanReason = InputValidator.requireText("Void reason", reason, 200);
        if (cleanReason.length() < 5) {
            throw new ValidationException("Void reason must be between 5 and 200 characters.");
        }

        Order order = orderRepository.findById(orderId);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ValidationException("Only COMPLETED orders can be voided.");
        }

        order.transitionTo(OrderStatus.VOIDED);

        // Restore reserved inventory for every item
        for (OrderItem item : order.getItems()) {
            try {
                inventoryService.restoreStock(item.getMedicineId(), item.getQuantity());
            } catch (Exception ignored) {}
        }

        VoidRecord voidRecord = new VoidRecord(voidRecordRepository.nextId(), orderId, admin.getId(), cleanReason, LocalDateTime.now());
        voidRecordRepository.add(voidRecord);
        orderRepository.save(order);
    }

    public List<Order> prescriptionAudit(String doctorLicenseOrNull, LocalDate from, LocalDate to)
            throws AuthenticationException, AccessDeniedException, DataAccessException {
        Session.getInstance().requireRole(Role.ADMIN);
        List<Order> list = orderRepository.findAll();
        List<Order> rxOrders = new ArrayList<>();

        String docFilter = doctorLicenseOrNull != null ? doctorLicenseOrNull.trim() : "";

        for (Order o : list) {
            if (o.getPrescriptionId().isEmpty()) {
                continue;
            }
            if (!docFilter.isEmpty() && !o.getDoctorLicense().equalsIgnoreCase(docFilter)) {
                continue;
            }
            LocalDate orderDate = o.getCreatedAt().toLocalDate();
            if (from != null && orderDate.isBefore(from)) {
                continue;
            }
            if (to != null && orderDate.isAfter(to)) {
                continue;
            }
            rxOrders.add(o);
        }

        rxOrders.sort(Comparator.comparing(Order::getCreatedAt).reversed());
        return rxOrders;
    }

    public String getPharmacistName(String userId) {
        try {
            User u = userRepository.findById(userId);
            return u.getFullName();
        } catch (Exception e) {
            return userId;
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private void checkOwnership(Order order) throws AuthenticationException, AccessDeniedException {
        User current = Session.getInstance().requireUser();
        if (current.getRole() == Role.ADMIN) {
            return;
        }
        if (!order.getPharmacistId().equalsIgnoreCase(current.getId())) {
            throw new AccessDeniedException("Access denied: Order " + order.getId() + " belongs to another pharmacist.");
        }
    }

    private void ensureEditable(Order order) throws InvalidStatusTransitionException {
        if (!order.getStatus().isEditable()) {
            throw new InvalidStatusTransitionException(
                    "Order " + order.getId() + " is " + order.getStatus().getLabel() + " and cannot be modified.");
        }
    }
}
