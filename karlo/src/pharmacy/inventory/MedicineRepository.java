package pharmacy.inventory;

import pharmacy.common.data.DataFiles;
import pharmacy.common.data.DataStore;
import pharmacy.common.exception.DataAccessException;
import pharmacy.common.exception.NotFoundException;
import pharmacy.common.util.Format;
import pharmacy.inventory.model.Medicine;
import pharmacy.inventory.model.MedicineCategory;
import pharmacy.inventory.model.MedicineStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateless data access repository managing medicine entity persistence.
 *
 * Demonstrates:
 * - Stateless Repository: Directly queries DataStore on each call to prevent cache drift.
 * - Robust Error Propagation: Throws checked DataAccessException with line numbers on file corruption.
 */
public class MedicineRepository {

    private final DataStore dataStore;

    public MedicineRepository() {
        this.dataStore = DataStore.getInstance();
    }

    public List<Medicine> findAll() throws DataAccessException {
        List<String[]> lines = dataStore.readAll(DataFiles.MEDICINES);
        List<Medicine> list = new ArrayList<>();
        int lineNum = 0;

        for (String[] cols : lines) {
            lineNum++;
            if (cols.length != 14) {
                throw new DataAccessException(
                        "Corrupted line " + lineNum + " in " + DataFiles.MEDICINES +
                        ": expected 14 columns, found " + cols.length);
            }

            try {
                String id = cols[0].trim();
                String brand = cols[1].trim();
                String generic = cols[2].trim();
                MedicineCategory cat = MedicineCategory.valueOf(cols[3].trim());
                String dosage = cols[4].trim();
                double price = Double.parseDouble(cols[5].trim());
                int stock = Integer.parseInt(cols[6].trim());
                int reorder = Integer.parseInt(cols[7].trim());
                String lot = cols[8].trim();
                LocalDate expiry = LocalDate.parse(cols[9].trim(), Format.DATE);
                boolean rx = Boolean.parseBoolean(cols[10].trim());
                String sideEffects = cols[11];
                String storage = cols[12];
                MedicineStatus status = MedicineStatus.valueOf(cols[13].trim());

                list.add(new Medicine(id, brand, generic, cat, dosage, price, stock, reorder,
                        lot, expiry, rx, sideEffects, storage, status));
            } catch (Exception e) {
                throw new DataAccessException(
                        "Error parsing line " + lineNum + " in " + DataFiles.MEDICINES + ": " + e.getMessage(), e);
            }
        }
        return list;
    }

    public Medicine findById(String medicineId) throws NotFoundException, DataAccessException {
        if (medicineId == null || medicineId.trim().isEmpty()) {
            throw new NotFoundException("Medicine ID cannot be blank.");
        }
        String target = medicineId.trim();
        for (Medicine m : findAll()) {
            if (m.getId().equalsIgnoreCase(target)) {
                return m;
            }
        }
        throw new NotFoundException("Medicine " + target + " not found.");
    }

    public void save(Medicine medicine) throws DataAccessException {
        List<Medicine> all = findAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equalsIgnoreCase(medicine.getId())) {
                all.set(i, medicine);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(medicine);
        }
        dataStore.writeAll(DataFiles.MEDICINES, all);
    }

    public void delete(String medicineId) throws NotFoundException, DataAccessException {
        List<Medicine> all = findAll();
        boolean removed = all.removeIf(m -> m.getId().equalsIgnoreCase(medicineId));
        if (!removed) {
            throw new NotFoundException("Medicine " + medicineId + " not found.");
        }
        dataStore.writeAll(DataFiles.MEDICINES, all);
    }

    public String nextId() throws DataAccessException {
        return dataStore.nextId(DataFiles.MEDICINES, "MED-", 4);
    }

    public boolean existsByBrandGenericDosage(String brand, String generic, String dosage, String excludeId)
            throws DataAccessException {
        for (Medicine m : findAll()) {
            if (excludeId != null && m.getId().equalsIgnoreCase(excludeId)) {
                continue;
            }
            if (m.getBrandName().equalsIgnoreCase(brand.trim()) &&
                m.getGenericName().equalsIgnoreCase(generic.trim()) &&
                m.getDosageStrength().equalsIgnoreCase(dosage.trim())) {
                return true;
            }
        }
        return false;
    }
}
