# Karlo — Gotchas (medicines + inventory)

Area-specific traps for `pharmacy.inventory`. Shared Java pitfalls are in [docs/GOTCHAS.md](../../docs/GOTCHAS.md). The entries below were written before coding as warnings. Mark them `Confirmed: <date>` when you hit them, and add new ones as you find them.

Format: `### Title` → **Symptom** / **Cause** / **Fix**.

---

### Expired medicine still shows up for pharmacists
**Symptom:** A pharmacist can browse or order a medicine whose expiry date has passed.
**Cause:** The expiry check ran when the medicine was **added**, not when it is **listed or sold**. Dates move forward but the record doesn't change.
**Fix:** Compute "expired" every time with `isExpired(LocalDate.now())`. Never store it as a field. Pharmacist lists use `isSellable(today)`.

### Admin can't find an expired medicine to fix it
**Symptom:** The catalog hides a medicine the admin needs to update or discontinue.
**Cause:** The admin list reused the pharmacist filter.
**Fix:** Admin views (`listAll`, catalog search) show **everything**, with `[EXPIRED]`, `[LOW STOCK]`, `[DISCONTINUED]` flags.

### Cancelling an order of a discontinued medicine fails
**Symptom:** Cameron's cancel or void throws `Medicine is discontinued`.
**Cause:** `restoreStock` used `findSellable` instead of `findById`.
**Fix:** `restoreStock` must work for any existing medicine (discontinued or expired). Only `findSellable` and `reserveStock` check sellability.

### Stock audit "loses" the system quantity
**Symptom:** Every audit row shows `difference = 0`.
**Cause:** The stock was overwritten with the counted value *before* `systemQuantity` was read.
**Fix:** Read `systemQuantity` first, build the `StockAudit`, then set the new stock and save.
