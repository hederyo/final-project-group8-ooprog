package pharmacy.orders.model;

import pharmacy.common.model.Storable;
import pharmacy.common.util.Format;

import java.util.Locale;

/**
 * Line item in an order, snapshotting the medicine ID, quantity, and unit price at time of sale.
 *
 * Demonstrates:
 * - Immutability of financial historical snapshot: Captures unitPriceAtSale so future catalog price changes
 *   never distort past transactions.
 */
public class OrderItem implements Storable {

    private final String orderId;
    private final String medicineId;
    private int quantity;
    private final double unitPriceAtSale;

    public OrderItem(String orderId, String medicineId, int quantity, double unitPriceAtSale) {
        this.orderId = orderId;
        this.medicineId = medicineId;
        this.quantity = quantity;
        this.unitPriceAtSale = Format.round2(unitPriceAtSale);
    }

    @Override
    public String getId() {
        return orderId + ":" + medicineId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPriceAtSale() {
        return unitPriceAtSale;
    }

    public double getLineTotal() {
        return Format.round2(quantity * unitPriceAtSale);
    }

    @Override
    public String toFileLine() {
        return String.join("|",
                orderId,
                medicineId,
                String.valueOf(quantity),
                String.format(Locale.US, "%.2f", unitPriceAtSale)
        );
    }
}
