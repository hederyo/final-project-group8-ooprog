# Cameron — Gotchas (orders + discounts + analytics)

Area-specific traps for `pharmacy.orders`. Shared Java pitfalls are in [docs/GOTCHAS.md](../../docs/GOTCHAS.md). The entries below were written before coding as warnings. Mark them `Confirmed: <date>` when you hit them, and add new ones as you find them.

Format: `### Title` → **Symptom** / **Cause** / **Fix**.

---

### Stock goes missing after cancel or void
**Symptom:** After cancelling or voiding, the medicine stock stays low.
**Cause:** The status changed but `restoreStock` was never called for the items.
**Fix:** `cancel` and `voidOrder` restore **every** item's quantity before saving the new status ([ARCHITECTURE §9.2](../../docs/ARCHITECTURE.md#92-stock-movement)).

### Stock reserved twice, or lost, on failed saves
**Symptom:** Stock drops even though the order wasn't saved, or one item was reserved and the next failed.
**Cause:** No rollback after a partial failure.
**Fix:** Keep a list of what you reserved. On any exception, `restoreStock` exactly those, then re-throw ([§9.3](../../docs/ARCHITECTURE.md#93-writing-safely-across-two-files)).

### Old orders change total when a price changes
**Symptom:** An order's total changed after the admin edited a medicine price.
**Cause:** The total was computed from `Medicine.getUnitPrice()` instead of the stored `unitPriceAtSale`.
**Fix:** Copy the price into `OrderItem` when the item is added. Never look it up again for totals.

### Order items duplicated or orphaned
**Symptom:** `order_items.txt` has the same item twice, or items of a deleted order remain.
**Cause:** `save(order)` appended items instead of replacing that order's lines.
**Fix:** In `save`, remove **all** existing lines with this `orderId` from the items list, add the current items, then write both files.

### Bulk discount survives a quantity drop
**Symptom:** An order with 20 units still gets 10% off.
**Cause:** Eligibility was only checked when the discount was chosen.
**Fix:** Re-check `discount.isEligible(order.getTotalUnits())` after every quantity change (rule O7).

### Pharmacist sees or edits someone else's order
**Symptom:** Typing another pharmacist's order ID works.
**Cause:** `findById` was used without an ownership check.
**Fix:** Every pharmacist method compares `order.getPharmacistId()` with `Session.requireUser().getId()` and throws `AccessDeniedException` if they differ (rule O8).

### Revenue includes cancelled or voided orders
**Symptom:** Analytics totals are too high.
**Cause:** All orders were summed.
**Fix:** Revenue, transaction count, and top sellers use **COMPLETED** orders only, by `completedAt`. A voided order no longer counts.
