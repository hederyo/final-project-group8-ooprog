package pharmacy.orders.model;

import pharmacy.common.model.Storable;
import pharmacy.common.util.Format;
import pharmacy.orders.discount.DiscountStrategy;
import pharmacy.orders.discount.DiscountType;
import pharmacy.orders.discount.RegularDiscount;
import pharmacy.orders.exception.InvalidStatusTransitionException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Order entity representing a sales transaction header along with its line items.
 *
 * Demonstrates:
 * - Encapsulation: Internal line items list is returned unmodifiable; totals are dynamically calculated
 *   from atomic line items and polymorphic discount strategies rather than persisted directly.
 * - Polymorphism: Delegates discount computation directly to the injected DiscountStrategy.
 */
public class Order implements Storable {

    private final String orderId;
    private final String pharmacistId;
    private String customerName;
    private String customerContact;
    private DiscountType discountType;
    private String discountIdNumber;
    private String prescriptionId;
    private String doctorLicense;
    private String notes;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    private final List<OrderItem> items = new ArrayList<>();
    private DiscountStrategy discount;

    public Order(String orderId, String pharmacistId, String customerName, String customerContact,
                 DiscountType discountType, String discountIdNumber, String prescriptionId,
                 String doctorLicense, String notes, OrderStatus status,
                 LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime completedAt) {
        this.orderId = orderId;
        this.pharmacistId = pharmacistId;
        this.customerName = customerName;
        this.customerContact = customerContact != null ? customerContact : "";
        this.discountType = discountType != null ? discountType : DiscountType.REGULAR;
        this.discountIdNumber = discountIdNumber != null ? discountIdNumber : "";
        this.prescriptionId = prescriptionId != null ? prescriptionId : "";
        this.doctorLicense = doctorLicense != null ? doctorLicense : "";
        this.notes = notes != null ? notes : "";
        this.status = status != null ? status : OrderStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.completedAt = completedAt;
        this.discount = this.discountType.toStrategy();
    }

    @Override
    public String getId() {
        return orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPharmacistId() {
        return pharmacistId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact != null ? customerContact : "";
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public String getDiscountIdNumber() {
        return discountIdNumber;
    }

    public void setDiscountIdNumber(String discountIdNumber) {
        this.discountIdNumber = discountIdNumber != null ? discountIdNumber : "";
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId != null ? prescriptionId : "";
    }

    public String getDoctorLicense() {
        return doctorLicense;
    }

    public void setDoctorLicense(String doctorLicense) {
        this.doctorLicense = doctorLicense != null ? doctorLicense : "";
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatusDirectly(OrderStatus status) {
        this.status = status;
    }

    /**
     * Transitions order to next status, validating against the state machine rules.
     */
    public void transitionTo(OrderStatus next) throws InvalidStatusTransitionException {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition order " + orderId + " from " + status.getLabel() + " to " + next.getLabel() + ".");
        }
        this.status = next;
        this.updatedAt = LocalDateTime.now();
        if (next == OrderStatus.COMPLETED) {
            this.completedAt = this.updatedAt;
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public DiscountStrategy getDiscount() {
        if (discount == null) {
            discount = discountType != null ? discountType.toStrategy() : new RegularDiscount();
        }
        return discount;
    }

    public void setDiscount(DiscountStrategy discount) {
        this.discount = discount;
        this.discountType = discount != null ? discount.getType() : DiscountType.REGULAR;
    }

    public void setDiscountType(DiscountType type) {
        this.discountType = type != null ? type : DiscountType.REGULAR;
        this.discount = this.discountType.toStrategy();
    }

    /**
     * Unmodifiable view of line items preventing unauthorized direct collection mutation.
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addOrMergeItem(String medicineId, int qty, double unitPrice) {
        for (OrderItem item : items) {
            if (item.getMedicineId().equalsIgnoreCase(medicineId)) {
                item.setQuantity(item.getQuantity() + qty);
                return;
            }
        }
        items.add(new OrderItem(orderId, medicineId, qty, unitPrice));
    }

    public void setItemQuantity(String medicineId, int qty) {
        if (qty <= 0) {
            removeItem(medicineId);
            return;
        }
        for (OrderItem item : items) {
            if (item.getMedicineId().equalsIgnoreCase(medicineId)) {
                item.setQuantity(qty);
                return;
            }
        }
    }

    public boolean removeItem(String medicineId) {
        return items.removeIf(item -> item.getMedicineId().equalsIgnoreCase(medicineId));
    }

    public OrderItem findItem(String medicineId) {
        for (OrderItem item : items) {
            if (item.getMedicineId().equalsIgnoreCase(medicineId)) {
                return item;
            }
        }
        return null;
    }

    public void clearItems() {
        items.clear();
    }

    public int getTotalUnits() {
        int sum = 0;
        for (OrderItem item : items) {
            sum += item.getQuantity();
        }
        return sum;
    }

    public double getSubtotal() {
        double sum = 0.0;
        for (OrderItem item : items) {
            sum += item.getLineTotal();
        }
        return Format.round2(sum);
    }

    public double getDiscountAmount() {
        return getDiscount().computeDiscount(getSubtotal(), getTotalUnits());
    }

    public double getTotal() {
        return Format.round2(getSubtotal() - getDiscountAmount());
    }

    @Override
    public String toFileLine() {
        return String.join("|",
                orderId,
                pharmacistId,
                customerName,
                customerContact,
                discountType.name(),
                discountIdNumber,
                prescriptionId,
                doctorLicense,
                notes,
                status.name(),
                Format.DATE_TIME.format(createdAt),
                Format.DATE_TIME.format(updatedAt),
                completedAt != null ? Format.DATE_TIME.format(completedAt) : ""
        );
    }
}
