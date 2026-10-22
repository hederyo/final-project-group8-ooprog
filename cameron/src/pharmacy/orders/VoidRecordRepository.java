package pharmacy.orders;

import pharmacy.common.data.DataFiles;
import pharmacy.common.data.DataStore;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.util.Format;
import pharmacy.orders.model.VoidRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only repository managing persistent void audit logs in void_log.txt.
 */
public class VoidRecordRepository {

    private final DataStore dataStore;

    public VoidRecordRepository() {
        this.dataStore = DataStore.getInstance();
    }

    public List<VoidRecord> findAll() throws DataAccessException {
        List<String[]> rows = dataStore.readAll(DataFiles.VOID_LOG);
        List<VoidRecord> records = new ArrayList<>();

        for (String[] cols : rows) {
            if (cols.length < 5) continue;
            try {
                String voidId = cols[0].trim();
                String orderId = cols[1].trim();
                String adminId = cols[2].trim();
                String reason = cols[3].trim();
                LocalDateTime voidedAt = parseDateTime(cols[4].trim());

                records.add(new VoidRecord(voidId, orderId, adminId, reason, voidedAt));
            } catch (Exception ignored) {
                // skip corrupted lines gracefully
            }
        }
        return records;
    }

    public void add(VoidRecord record) throws DataAccessException {
        dataStore.append(DataFiles.VOID_LOG, record);
    }

    public String nextId() throws DataAccessException {
        return dataStore.nextId(DataFiles.VOID_LOG, "VOID-", 4);
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(text, Format.DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.parse(text);
        }
    }
}
