package pharmacy.inventory;

import pharmacy.common.data.DataFiles;
import pharmacy.common.data.DataStore;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.util.Format;
import pharmacy.inventory.model.StockAudit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless data access repository managing stock audit log persistence.
 *
 * Demonstrates:
 * - Append-Only Audit Logging: Captures history safely using DataStore.append.
 * - Stateless Consistency: Reads stock_audits.txt directly without local caching.
 */
public class StockAuditRepository {

    private final DataStore dataStore;

    public StockAuditRepository() {
        this.dataStore = DataStore.getInstance();
    }

    public List<StockAudit> findAll() throws DataAccessException {
        List<String[]> lines = dataStore.readAll(DataFiles.STOCK_AUDITS);
        List<StockAudit> list = new ArrayList<>();
        int lineNum = 0;

        for (String[] cols : lines) {
            lineNum++;
            if (cols.length != 8) {
                throw new DataAccessException(
                        "Corrupted line " + lineNum + " in " + DataFiles.STOCK_AUDITS +
                        ": expected 8 columns, found " + cols.length);
            }

            try {
                String auditId = cols[0].trim();
                String medicineId = cols[1].trim();
                int sysQty = Integer.parseInt(cols[2].trim());
                int countedQty = Integer.parseInt(cols[3].trim());
                int diff = Integer.parseInt(cols[4].trim());
                String reason = cols[5];
                String auditedBy = cols[6].trim();
                LocalDateTime auditedAt = LocalDateTime.parse(cols[7].trim(), Format.DATE_TIME);

                list.add(new StockAudit(auditId, medicineId, sysQty, countedQty, diff, reason, auditedBy, auditedAt));
            } catch (Exception e) {
                throw new DataAccessException(
                        "Error parsing line " + lineNum + " in " + DataFiles.STOCK_AUDITS + ": " + e.getMessage(), e);
            }
        }
        return list;
    }

    public List<StockAudit> findByMedicine(String medicineId) throws DataAccessException {
        List<StockAudit> all = findAll();
        List<StockAudit> matches = new ArrayList<>();
        if (medicineId == null) {
            return matches;
        }
        String target = medicineId.trim();
        for (StockAudit audit : all) {
            if (audit.getMedicineId().equalsIgnoreCase(target)) {
                matches.add(audit);
            }
        }
        return matches;
    }

    public void add(StockAudit audit) throws DataAccessException {
        dataStore.append(DataFiles.STOCK_AUDITS, audit);
    }

    public String nextId() throws DataAccessException {
        return dataStore.nextId(DataFiles.STOCK_AUDITS, "AUD-", 6);
    }
}
