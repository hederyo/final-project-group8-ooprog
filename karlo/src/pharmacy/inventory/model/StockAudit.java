package pharmacy.inventory.model;

import pharmacy.common.model.Storable;
import pharmacy.common.util.Format;

import java.time.LocalDateTime;

/**
 * Domain entity capturing an immutable physical stock audit event.
 *
 * Demonstrates:
 * - Encapsulation: Immutable audit record with read-only accessors.
 * - Abstraction: Implements Storable for append-only transaction logging.
 */
public class StockAudit implements Storable {

    private final String auditId;
    private final String medicineId;
    private final int systemQuantity;
    private final int countedQuantity;
    private final int difference;
    private final String reason;
    private final String auditedBy;
    private final LocalDateTime auditedAt;

    public StockAudit(String auditId, String medicineId, int systemQuantity, int countedQuantity,
                      int difference, String reason, String auditedBy, LocalDateTime auditedAt) {
        this.auditId = auditId;
        this.medicineId = medicineId;
        this.systemQuantity = systemQuantity;
        this.countedQuantity = countedQuantity;
        this.difference = difference;
        this.reason = (reason != null) ? reason : "";
        this.auditedBy = auditedBy;
        this.auditedAt = (auditedAt != null) ? auditedAt : LocalDateTime.now();
    }

    @Override
    public String getId() {
        return auditId;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public int getSystemQuantity() {
        return systemQuantity;
    }

    public int getCountedQuantity() {
        return countedQuantity;
    }

    public int getDifference() {
        return difference;
    }

    public String getReason() {
        return reason;
    }

    public String getAuditedBy() {
        return auditedBy;
    }

    public LocalDateTime getAuditedAt() {
        return auditedAt;
    }

    @Override
    public String toFileLine() {
        return String.join("|",
                auditId,
                medicineId,
                String.valueOf(systemQuantity),
                String.valueOf(countedQuantity),
                String.valueOf(difference),
                reason,
                auditedBy,
                auditedAt.format(Format.DATE_TIME)
        );
    }
}
