package pharmacy.orders;

import pharmacy.common.data.DataFiles;
import pharmacy.common.data.DataStore;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.NotFoundException;
import pharmacy.common.util.Format;
import pharmacy.orders.discount.DiscountType;
import pharmacy.orders.model.Order;
import pharmacy.orders.model.OrderItem;
import pharmacy.orders.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository orchestrating relational persistence of orders across orders.txt and order_items.txt.
 *
 * Demonstrates:
 * - Aggregate Root persistence: Order acts as the aggregate root for OrderItem collections, ensuring
 *   atomic persistence across two separate flat data files.
 */
public class OrderRepository {

    private final DataStore dataStore;

    public OrderRepository() {
        this.dataStore = DataStore.getInstance();
    }

    public List<Order> findAll() throws DataAccessException {
        List<String[]> orderRows = dataStore.readAll(DataFiles.ORDERS);
        List<String[]> itemRows = dataStore.readAll(DataFiles.ORDER_ITEMS);

        Map<String, List<OrderItem>> itemsByOrderId = new HashMap<>();
        for (String[] cols : itemRows) {
            if (cols.length < 4) continue;
            try {
                String orderId = cols[0].trim();
                String medicineId = cols[1].trim();
                int qty = Integer.parseInt(cols[2].trim());
                double price = Double.parseDouble(cols[3].trim());

                itemsByOrderId.computeIfAbsent(orderId.toUpperCase(), k -> new ArrayList<>())
                        .add(new OrderItem(orderId, medicineId, qty, price));
            } catch (Exception ignored) {
                // skip corrupted item rows
            }
        }

        List<Order> orders = new ArrayList<>();
        for (String[] cols : orderRows) {
            if (cols.length < 12) continue;
            try {
                String orderId = cols[0].trim();
                String pharmacistId = cols[1].trim();
                String customerName = cols[2].trim();
                String customerContact = cols[3].trim();
                DiscountType discountType = DiscountType.valueOf(cols[4].trim());
                String discountIdNum = cols[5].trim();
                String rxId = cols[6].trim();
                String docLicense = cols[7].trim();
                String notes = cols[8].trim();
                OrderStatus status = OrderStatus.valueOf(cols[9].trim());
                LocalDateTime createdAt = parseDateTime(cols[10].trim());
                LocalDateTime updatedAt = parseDateTime(cols[11].trim());
                LocalDateTime completedAt = (cols.length >= 13 && !cols[12].trim().isEmpty())
                        ? parseDateTime(cols[12].trim())
                        : null;

                Order order = new Order(orderId, pharmacistId, customerName, customerContact,
                        discountType, discountIdNum, rxId, docLicense, notes, status,
                        createdAt, updatedAt, completedAt);

                List<OrderItem> items = itemsByOrderId.get(orderId.toUpperCase());
                if (items != null) {
                    for (OrderItem item : items) {
                        order.addOrMergeItem(item.getMedicineId(), item.getQuantity(), item.getUnitPriceAtSale());
                    }
                }
                orders.add(order);
            } catch (Exception ignored) {
                // skip corrupted order rows
            }
        }
        return orders;
    }

    public Order findById(String orderId) throws NotFoundException, DataAccessException {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new NotFoundException("Order ID cannot be empty.");
        }
        String target = orderId.trim();
        for (Order order : findAll()) {
            if (order.getId().equalsIgnoreCase(target)) {
                return order;
            }
        }
        throw new NotFoundException("Order " + target + " not found.");
    }

    public List<Order> findByPharmacist(String userId) throws DataAccessException {
        if (userId == null) return new ArrayList<>();
        String target = userId.trim();
        List<Order> matching = new ArrayList<>();
        for (Order order : findAll()) {
            if (order.getPharmacistId().equalsIgnoreCase(target)) {
                matching.add(order);
            }
        }
        return matching;
    }

    public boolean existsByMedicine(String medicineId) throws DataAccessException {
        if (medicineId == null) return false;
        String target = medicineId.trim();
        List<String[]> itemRows = dataStore.readAll(DataFiles.ORDER_ITEMS);
        for (String[] cols : itemRows) {
            if (cols.length >= 2 && cols[1].trim().equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    public boolean existsByPharmacist(String userId) throws DataAccessException {
        if (userId == null) return false;
        String target = userId.trim();
        List<String[]> orderRows = dataStore.readAll(DataFiles.ORDERS);
        for (String[] cols : orderRows) {
            if (cols.length >= 2 && cols[1].trim().equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    public void save(Order order) throws DataAccessException {
        List<Order> orders = findAll();
        boolean replaced = false;
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getId().equalsIgnoreCase(order.getId())) {
                orders.set(i, order);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            orders.add(order);
        }

        // 1. Rewrite orders.txt
        dataStore.writeAll(DataFiles.ORDERS, orders);

        // 2. Rewrite order_items.txt by purging this order's lines and appending current lines
        List<String[]> rawItems = dataStore.readAll(DataFiles.ORDER_ITEMS);
        List<OrderItem> survivingItems = new ArrayList<>();
        for (String[] cols : rawItems) {
            if (cols.length < 4) continue;
            String oid = cols[0].trim();
            if (!oid.equalsIgnoreCase(order.getId())) {
                try {
                    survivingItems.add(new OrderItem(oid, cols[1].trim(),
                            Integer.parseInt(cols[2].trim()), Double.parseDouble(cols[3].trim())));
                } catch (Exception ignored) {}
            }
        }
        // Append current items
        survivingItems.addAll(order.getItems());
        dataStore.writeAll(DataFiles.ORDER_ITEMS, survivingItems);
    }

    public String nextId() throws DataAccessException {
        return dataStore.nextId(DataFiles.ORDERS, "ORD-", 6);
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(text.trim(), Format.DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.parse(text.trim());
        }
    }
}
