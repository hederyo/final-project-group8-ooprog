package pharmacy.orders;

import pharmacy.common.exception.AccessDeniedException;
import pharmacy.common.exception.AuthenticationException;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.ValidationException;
import pharmacy.common.model.Role;
import pharmacy.common.session.Session;
import pharmacy.common.util.InputValidator;
import pharmacy.inventory.MedicineRepository;
import pharmacy.inventory.StockAuditRepository;
import pharmacy.inventory.model.Medicine;
import pharmacy.inventory.model.StockAudit;
import pharmacy.orders.model.Order;
import pharmacy.orders.model.OrderItem;
import pharmacy.orders.model.OrderStatus;
import pharmacy.orders.model.SalesReport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service aggregating sales revenue, discount amounts, top performers, and inventory shrinkage metrics.
 *
 * Demonstrates:
 * - Cross-module analytical synthesis: Merges transaction history with Karlo's stock audits to derive net shrinkage.
 * - Business intelligence rule enforcement: Only COMPLETED orders by completedAt timestamp contribute to revenue.
 */
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final MedicineRepository medicineRepository;
    private final StockAuditRepository stockAuditRepository;

    public AnalyticsService() {
        this(new OrderRepository(), new MedicineRepository(), new StockAuditRepository());
    }

    public AnalyticsService(OrderRepository orderRepository, MedicineRepository medicineRepository,
                            StockAuditRepository stockAuditRepository) {
        this.orderRepository = orderRepository;
        this.medicineRepository = medicineRepository;
        this.stockAuditRepository = stockAuditRepository;
    }

    public SalesReport report(LocalDate from, LocalDate to)
            throws AuthenticationException, AccessDeniedException, ValidationException, DataAccessException {
        Session.getInstance().requireRole(Role.ADMIN);
        InputValidator.requireDateRange(from, to);

        List<Order> allOrders = orderRepository.findAll();

        double grossRevenue = 0.0;
        int transactionCount = 0;
        double totalDiscounts = 0.0;
        int cancelledCount = 0;
        int voidedCount = 0;

        // Map medicineId -> int[2] where [0]=unitsSold, [1]=revenueInCents
        Map<String, int[]> medicineAggregates = new HashMap<>();

        for (Order o : allOrders) {
            LocalDate refDate = o.getCreatedAt().toLocalDate();

            if (o.getStatus() == OrderStatus.CANCELLED) {
                if (!refDate.isBefore(from) && !refDate.isAfter(to)) {
                    cancelledCount++;
                }
                continue;
            }

            if (o.getStatus() == OrderStatus.VOIDED) {
                if (!refDate.isBefore(from) && !refDate.isAfter(to)) {
                    voidedCount++;
                }
                continue;
            }

            if (o.getStatus() == OrderStatus.COMPLETED && o.getCompletedAt() != null) {
                LocalDate compDate = o.getCompletedAt().toLocalDate();
                if (!compDate.isBefore(from) && !compDate.isAfter(to)) {
                    transactionCount++;
                    grossRevenue += o.getTotal();
                    totalDiscounts += o.getDiscountAmount();

                    for (OrderItem item : o.getItems()) {
                        int[] agg = medicineAggregates.computeIfAbsent(item.getMedicineId(), k -> new int[2]);
                        agg[0] += item.getQuantity();
                        agg[1] += (int) Math.round(item.getLineTotal() * 100);
                    }
                }
            }
        }

        // Determine Top 5 Medicines by units sold
        List<Map.Entry<String, int[]>> sortedEntries = new ArrayList<>(medicineAggregates.entrySet());
        sortedEntries.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));

        List<SalesReport.TopMedicine> topMedicines = new ArrayList<>();
        int limit = Math.min(5, sortedEntries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, int[]> entry = sortedEntries.get(i);
            String medId = entry.getKey();
            int units = entry.getValue()[0];
            double rev = entry.getValue()[1] / 100.0;

            String medName = medId;
            try {
                Medicine med = medicineRepository.findById(medId);
                medName = med.getBrandName() + " " + med.getDosageStrength();
            } catch (Exception ignored) {}

            topMedicines.add(new SalesReport.TopMedicine(medId, medName, units, rev));
        }

        // Calculate shrinkage from Karlo's stock audits (negative difference values)
        int shrinkageUnits = 0;
        double shrinkagePesos = 0.0;

        List<StockAudit> audits = stockAuditRepository.findAll();
        for (StockAudit audit : audits) {
            if (audit.getAuditedAt() == null) continue;
            LocalDate auditDate = audit.getAuditedAt().toLocalDate();
            if (!auditDate.isBefore(from) && !auditDate.isAfter(to)) {
                if (audit.getDifference() < 0) {
                    int lostUnits = -audit.getDifference();
                    shrinkageUnits += lostUnits;
                    try {
                        Medicine m = medicineRepository.findById(audit.getMedicineId());
                        shrinkagePesos += lostUnits * m.getUnitPrice();
                    } catch (Exception ignored) {}
                }
            }
        }

        return new SalesReport(from, to, grossRevenue, transactionCount, totalDiscounts,
                topMedicines, shrinkageUnits, shrinkagePesos, cancelledCount, voidedCount);
    }
}
