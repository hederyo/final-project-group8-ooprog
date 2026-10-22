# Karlo — Medicines & Inventory: context and checklist

Features: **M1 Browse · M2 View Details · M3 Search & Filter · M4 Manage Catalog · M5 Stock & Inventory Audit**. The full behaviour is in [FEATURES §Medicines](../../docs/FEATURES.md#medicines--karlo) and the rules in [VALIDATION §3.2](../../docs/VALIDATION.md#32-medicines-and-stock--karlo). Data formats: [DATA §3.2–3.3](../../docs/DATA.md#32-medicinestxt--owner-karlo).

## Folder layout
Every file you create, with its purpose and milestone: [BLUEPRINT §4](../../docs/BLUEPRINT.md#4-karlo--medicines--inventory-karlo).

## Classes to build

### Model
- [ ] `Medicine implements Storable`:
  - Every field from [DATA §3.2](../../docs/DATA.md#32-medicinestxt--owner-karlo) is private.
  - Setters guard invariants (no negative price or stock).
  - `public static final int DEFAULT_REORDER_LEVEL = 10;`
  - `boolean isExpired(LocalDate today)` → `!expiryDate.isAfter(today)`. A medicine is not sold on its expiry date.
  - `boolean isNearExpiry(LocalDate today, int days)`: not expired, and the expiry date is within `days` days.
  - `boolean isLowStock()` → `stockQuantity <= reorderLevel`.
  - `boolean isSellable(LocalDate today)` → `ACTIVE` and not expired.
  - `toFileLine()`
- [ ] `MedicineCategory` (with `getLabel()`) and `MedicineStatus`. Constants are in [DATA §4](../../docs/DATA.md#4-enums).
- [ ] `StockAudit implements Storable`, with fields from [DATA §3.3](../../docs/DATA.md#33-stock_auditstxt--owner-karlo).

### Data
- [ ] `MedicineRepository`: `findAll()`, `findById(id)` (`NotFoundException`), `save(medicine)` (replace or add), `delete(id)`, `nextId()` → `"MED-", 4`. A wrong column count throws `DataAccessException`.
- [ ] `StockAuditRepository`: `findAll()`, `findByMedicine(id)`, `add(audit)` via `DataStore.append`, `nextId()` → `"AUD-", 6`.

### Logic
- [ ] `MedicineFilter`:
  - Optional `keyword`, `MedicineCategory category`, `Double minPrice`, `Double maxPrice`, `Boolean prescriptionRequired`, and `boolean sellableOnly`. `null` means "any".
  - `boolean matches(Medicine m, LocalDate today)` applies every non-null criterion (AND).
  - `validate()` enforces M9.
- [ ] `InventoryService`. Admin methods start with `Session.getInstance().requireRole(Role.ADMIN)`.
  - `public static final int NEAR_EXPIRY_DAYS = 30;`
  - Pharmacist + admin:
    - `browseSellable()`: sorted by brand.
    - `search(MedicineFilter)`: forces `sellableOnly` for pharmacists.
    - `getDetails(id)`: a pharmacist asking for a non-sellable medicine gets `NotFoundException`.
  - Admin:
    - `listAll()`
    - `addMedicine(...)` (M1–M4)
    - `updateMedicine(id, ...)`: a blank value keeps the current one.
    - `discontinue(id)` / `reactivate(id)`
    - `deleteMedicine(id)` (M11): calls Cameron's `OrderService.hasOrdersForMedicine`.
    - `lowStockReport()`
    - `expiryReport()`: returns expired items plus those near expiry.
    - `recordPhysicalCount(id, counted, reason)` (M10)
    - `updateLotExpiryReorder(id, lot, expiry, reorder)`
    - `auditHistory(idOrNull)`
  - **For Cameron** ([ARCHITECTURE §6.3](../../docs/ARCHITECTURE.md#63-provided-by-karlo--used-by-cameron-and-common)):
    - `findSellable(id)` (M7)
    - `reserveStock(id, qty)` (M8)
    - `restoreStock(id, qty)`

    No role check on these three: both roles reach them through orders.
- [ ] `InsufficientStockException extends PharmacyException`.

### UI (all `extends BaseMenu`)
- [ ] `MedicineBrowserMenu` (pharmacist): Browse all · Search & filter · View details.
- [ ] `CatalogMenu` (admin): List all · Search & filter · View details · Add · Update · Discontinue/Reactivate · Delete.
- [ ] `StockAuditMenu` (admin): Low-stock report · Expiry report · Record physical count · Update lot/expiry/reorder level · Audit history. Prints the alert summary line when opened.
- [ ] Shared table printing: one helper in your folder (e.g. `ui/MedicineTable`) so every list looks the same.

## Contracts

**You provide** ([ARCHITECTURE §6.3](../../docs/ARCHITECTURE.md#63-provided-by-karlo--used-by-cameron-and-common)):
- `InventoryService.findSellable`, `reserveStock`, `restoreStock`
- `MedicineRepository.findById`
- `StockAuditRepository.findAll`
- The `Medicine` and `StockAudit` getters
- `MedicineBrowserMenu`, `CatalogMenu`, `StockAuditMenu`

**You use:**
- `common/` (`DataStore`, `Session`, `ConsoleInput`, `InputValidator`, `Format`, `BaseMenu`, exceptions)
- Cameron's `OrderService.hasOrdersForMedicine(id)`

## Your OOP and pattern evidence (for the defense)
- **Encapsulation:** `Medicine` private fields with guarded setters; stock changes only through `InventoryService`.
- **Abstraction:** `MedicineFilter.matches(...)` hides all the search rules behind one call; `Storable`.
- **Inheritance:** your three menus `extend BaseMenu`; `InsufficientStockException extends PharmacyException`.
- **Polymorphism:** each menu overrides `getTitle`/`getOptions`/`handle`; `toFileLine()` overrides.
- **Exception handling:** `InsufficientStockException`, `NotFoundException`, `DuplicateRecordException`, `ValidationException` thrown from the service.
- **Singleton usage:** `DataStore.getInstance()`, `Session.getInstance()`.

## Definition of done (per feature)
- [ ] Compiles together with all four folders.
- [ ] Every rule M1–M12 that applies is enforced in the **service**.
- [ ] Pharmacists never see expired or discontinued medicines; admins see them with flags.
- [ ] Passes the [hostile-input checklist](../../docs/VALIDATION.md#4-hostile-input-checklist).
- [ ] `data/medicines.txt` and `data/stock_audits.txt` lines match DATA.md.
- [ ] PR reviewed by Heather or Cameron.

## Progress
| Milestone | Status |
|---|---|
| M0 Skeleton: empty `MedicineBrowserMenu`, `CatalogMenu`, `StockAuditMenu` | ☐ |
| Model + repositories | ☐ |
| M4 Catalog CRUD (add/list first, so others have test data) | ☐ |
| `findSellable` / `reserveStock` / `restoreStock` for Cameron | ☐ |
| M1 Browse · M2 Details · M3 Search & Filter | ☐ |
| M5 Stock & Inventory Audit | ☐ |
