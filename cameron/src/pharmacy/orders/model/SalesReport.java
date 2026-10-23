package pharmacy.orders.model;

import pharmacy.common.util.Format;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Report data transfer model containing aggregated sales and shrinkage analytics for a date window.
 *
 * Demonstrates:
 * - Data Transfer Object (DTO) pattern: Packages multi-repository business metrics into an immutable view model.
 */
public class SalesReport {

    public static class TopMedicine {
        private final String medicineId;
        private final String medicineName;
        private final int unitsSold;
        private final double totalRevenue;

        public TopMedicine(String medicineId, String medicineName, int unitsSold, double totalRevenue) {
            this.medicineId = medicineId;
            this.medicineName = medicineName;
            this.unitsSold = unitsSold;
            this.totalRevenue = Format.round2(totalRevenue);
        }

        public String getMedicineId() { return medicineId; }
        public String getMedicineName() { return medicineName; }
        public int getUnitsSold() { return unitsSold; }
        public double getTotalRevenue() { return totalRevenue; }
    }

    private final LocalDate from;
    private final LocalDate to;
    private final double grossRevenue;
    private final int transactionCount;
    private final double totalDiscounts;
    private final List<TopMedicine> topMedicines;
    private final int shrinkageUnits;
    private final double shrinkagePesos;
    private final int cancelledCount;
    private final int voidedCount;

    public SalesReport(LocalDate from, LocalDate to, double grossRevenue, int transactionCount,
                       double totalDiscounts, List<TopMedicine> topMedicines,
                       int shrinkageUnits, double shrinkagePesos,
                       int cancelledCount, int voidedCount) {
        this.from = from;
        this.to = to;
        this.grossRevenue = Format.round2(grossRevenue);
        this.transactionCount = transactionCount;
        this.totalDiscounts = Format.round2(totalDiscounts);
        this.topMedicines = topMedicines != null ? topMedicines : Collections.emptyList();
        this.shrinkageUnits = shrinkageUnits;
        this.shrinkagePesos = Format.round2(shrinkagePesos);
        this.cancelledCount = cancelledCount;
        this.voidedCount = voidedCount;
    }

    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
    public double getGrossRevenue() { return grossRevenue; }
    public int getTransactionCount() { return transactionCount; }
    public double getTotalDiscounts() { return totalDiscounts; }
    public List<TopMedicine> getTopMedicines() { return Collections.unmodifiableList(topMedicines); }
    public int getShrinkageUnits() { return shrinkageUnits; }
    public double getShrinkagePesos() { return shrinkagePesos; }
    public int getCancelledCount() { return cancelledCount; }
    public int getVoidedCount() { return voidedCount; }
}
