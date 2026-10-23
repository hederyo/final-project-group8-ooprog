package pharmacy.orders.model;

import pharmacy.common.model.Storable;
import pharmacy.common.util.Format;

import java.time.LocalDateTime;

/**
 * Immutable audit record logged whenever an administrator voids a completed order.
 *
 * Demonstrates:
 * - Audit Trail / Compliance: Preserves accountability for cancelled/voided financial records.
 */
public class VoidRecord implements Storable {

    private final String voidId;
    private final String orderId;
    private final String adminId;
    private final String reason;
    private final LocalDateTime voidedAt;

    public VoidRecord(String voidId, String orderId, String adminId, String reason, LocalDateTime voidedAt) {
        this.voidId = voidId;
        this.orderId = orderId;
        this.adminId = adminId;
        this.reason = reason != null ? reason : "";
        this.voidedAt = voidedAt != null ? voidedAt : LocalDateTime.now();
    }

    @Override
    public String getId() {
        return voidId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }

    @Override
    public String toFileLine() {
        return String.join("|",
                voidId,
                orderId,
                adminId,
                reason,
                Format.DATE_TIME.format(voidedAt)
        );
    }
}
