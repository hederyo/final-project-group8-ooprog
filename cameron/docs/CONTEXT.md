# Cameron — Orders & Transactions: context and checklist

Features:
- **O1** Process Order
- **O2** Apply Discount
- **O3** View Orders
- **O4** Update Order
- **O5** Complete
- **O6** Cancel
- **O7** Update Status
- **O8** Void
- **O9** Audit Prescriptions
- **O10** Search Orders
- **O11** Analytics

The full behaviour is in [FEATURES §Orders](../../docs/FEATURES.md#orders--cameron) and the rules in [VALIDATION §3.3](../../docs/VALIDATION.md#33-orders-discounts-prescriptions-analytics--cameron). Data formats: [DATA §3.4–3.6](../../docs/DATA.md#34-orderstxt--owner-cameron). Status and stock rules: [ARCHITECTURE §9](../../docs/ARCHITECTURE.md#9-orders-stock-and-status).

## Folder layout
Every file you create, with its purpose and milestone: [BLUEPRINT §5](../../docs/BLUEPRINT.md#5-cameron--orders-discounts-analytics-cameron).

## Classes to build

### Strategy pattern (`discount/`): build this first, it is graded on its own
- [ ] `DiscountStrategy` interface ([ARCHITECTURE §5.2](../../docs/ARCHITECTURE.md#52-strategy--discounts)).
- [ ] `RegularDiscount`, `SeniorPwdDiscount` (`RATE = 0.20`), `BulkDiscount` (`RATE = 0.10`, `MIN_UNITS = 50`). Each `computeDiscount` returns `Format.round2(...)`.
- [ ] `DiscountType` enum (`REGULAR`, `SENIOR_PWD`, `BULK`) with `getLabel()` and `DiscountStrategy toStrategy()`.
- [ ] **Never** write `if (type == SENIOR_PWD) total *= 0.8` anywhere. `Order` just calls `discount.computeDiscount(...)`. That call is the polymorphism the graders look for.

### Model
- [ ] `OrderStatus` enum:
  - `boolean canTransitionTo(OrderStatus next)` (the table in [ARCHITECTURE §9.1](../../docs/ARCHITECTURE.md#91-status-machine))
  - `boolean isEditable()` (PENDING, ON_HOLD)
  - `boolean isTerminal()`
- [ ] `OrderItem implements Storable`: `orderId`, `medicineId`, `quantity`, `unitPriceAtSale`, and `getLineTotal()`. `getId()` returns `orderId + ":" + medicineId`.
- [ ] `Order implements Storable`:
  - Fields from [DATA §3.4](../../docs/DATA.md#34-orderstxt--owner-cameron), plus a `private final List<OrderItem> items` and a `private DiscountStrategy discount`.
  - Methods:
    - `getItems()` returns an unmodifiable list.
    - `addOrMergeItem(medicineId, qty, unitPrice)`
    - `setItemQuantity(medicineId, qty)`
    - `removeItem(medicineId)`
    - `getTotalUnits()`
    - `getSubtotal()`
    - `getDiscountAmount()`, which delegates to the strategy
    - `getTotal()`
    - `hasPrescriptionItem(...)`, which needs medicine info, so the **service** decides this. Keep it out of the model if that is simpler.
  - `setStatus(...)` is package-private or guarded. Status only changes through `OrderService`, which checks `canTransitionTo`.
- [ ] `VoidRecord implements Storable` ([DATA §3.6](../../docs/DATA.md#36-void_logtxt--owner-cameron)).
- [ ] `InvalidStatusTransitionException extends PharmacyException`.

### Data
- [ ] `OrderRepository`:
  - `findAll()`: loads `orders.txt`, then attaches the lines from `order_items.txt` to each order.
  - `findById(id)`: throws `NotFoundException`.
  - `findByPharmacist(userId)`
  - `save(order)`: replaces the order line **and** all its item lines, then writes both files.
  - `existsByMedicine(id)`, `existsByPharmacist(id)`
  - `nextId()`: `"ORD-", 6`.
- [ ] `VoidRecordRepository`: `add(record)` (append), `findAll()`, `nextId()` (`"VOID-", 4`).

### Logic
- [ ] `OrderService`:
  - `public static final int MAX_ITEM_QUANTITY = 1000;`
  - **Contracts for Heather and Karlo (build early):** `hasOrdersForPharmacist(userId)`, `hasOrdersForMedicine(medicineId)`.
  - **Pharmacist.** Each method uses `Session.requireUser()` and **checks ownership** (O8).
    - `checkItem(medicineId, qty)`: used while building the O1 cart. Calls `findSellable` and compares stock, but does not reserve.
    - `placeOrder(draft, boolean completeNow)`: validates O1–O7, reserves every item (rolling back on failure, [§9.2](../../docs/ARCHITECTURE.md#92-stock-movement)), then saves.
    - `myOrders(statusOrNull)` and `getMyOrder(id)`
    - `addItem`, `changeQuantity`, `removeItem`, `changeDiscount`, `updateNotes`: each reserves or restores the difference, then saves. On a save failure it undoes the stock change ([§9.3](../../docs/ARCHITECTURE.md#93-writing-safely-across-two-files)).
    - `putOnHold`, `resume`, `complete` (O11), `cancel` (restores all stock)
  - **Admin.** Each method starts with `requireRole(ADMIN)`.
    - `allOrders()`
    - `searchOrders(keyword, status, pharmacistId, from, to)`
    - `updateStatus(id, next)`: never VOIDED.
    - `voidOrder(id, reason)`: restores stock and appends a `VoidRecord`.
    - `prescriptionAudit(doctorLicenseOrNull, from, to)`
- [ ] `AnalyticsService` (admin): `report(LocalDate from, LocalDate to)` returns:
  - revenue
  - transaction count
  - total discounts
  - top 5 medicines by units
  - shrinkage units and pesos (from Karlo's `StockAuditRepository.findAll()` and `MedicineRepository.findById` for prices)
  - cancelled and voided counts

  Day, month and year are just different `from`/`to` pairs.

### UI (all `extends BaseMenu`)
- [ ] `OrderMenu` (pharmacist): New order · My orders · Update order · Complete order · Cancel order.
- [ ] `OrderAdminMenu` (admin): All orders · Search & filter · View order · Update status · Void order · Prescription audit.
- [ ] `AnalyticsMenu` (admin): Daily · Monthly · Yearly · Custom range.

## Contracts

**You provide** ([ARCHITECTURE §6.4](../../docs/ARCHITECTURE.md#64-provided-by-cameron--used-by-heather-karlo-and-common)):
- `OrderService.hasOrdersForPharmacist`, `hasOrdersForMedicine`
- `OrderMenu`, `OrderAdminMenu`, `AnalyticsMenu`

**You use:**
- `common/`
- From Karlo ([§6.3](../../docs/ARCHITECTURE.md#63-provided-by-karlo--used-by-cameron-and-common)): `InventoryService.findSellable`, `reserveStock`, `restoreStock`; `MedicineRepository.findById`; `StockAuditRepository.findAll`
- From Heather ([§6.2](../../docs/ARCHITECTURE.md#62-provided-by-heather--used-by-cameron-and-common)): `UserRepository.findById`, `findAll`, used to show pharmacist names

Until Karlo's inventory is merged, you can still build and test the discount strategies and `OrderStatus` on their own.

## Your OOP and pattern evidence (for the defense)
- **Strategy:** `DiscountStrategy` and its three implementations. `Order` depends only on the interface.
- **Encapsulation:**
  - `Order.items` is private and returned unmodifiable.
  - Totals are computed, never set.
  - Status changes are guarded.
- **Abstraction:** the `DiscountStrategy` interface; `Storable`.
- **Inheritance:** your menus `extend BaseMenu`; `InvalidStatusTransitionException extends PharmacyException`.
- **Polymorphism:** `discount.computeDiscount(...)` runs different code for each strategy at runtime; menu `handle(...)` overrides.
- **Exception handling:** `InvalidStatusTransitionException`, plus handling Karlo's `InsufficientStockException` with rollback.

## Definition of done (per feature)
- [ ] Compiles together with all four folders.
- [ ] Every rule O1–O15 that applies is enforced in the **service**.
- [ ] Stock is correct after every path: save, update, cancel, void, and discard. Check `medicines.txt` before and after.
- [ ] Passes the [hostile-input checklist](../../docs/VALIDATION.md#4-hostile-input-checklist).
- [ ] The `data/orders.txt`, `order_items.txt`, and `void_log.txt` lines match DATA.md.
- [ ] PR reviewed by Heather or Karlo.

## Progress
| Milestone | Status |
|---|---|
| M0 Skeleton: empty `OrderMenu`, `OrderAdminMenu`, `AnalyticsMenu` | ☐ |
| Discount strategies + `OrderStatus` | ☐ |
| Model + repositories + `hasOrdersFor*` | ☐ |
| O1 Process order + O2 Discount | ☐ |
| O3 View · O4 Update · O5 Complete · O6 Cancel | ☐ |
| O7 Status · O8 Void · O9 Rx audit · O10 Search | ☐ |
| O11 Analytics | ☐ |
