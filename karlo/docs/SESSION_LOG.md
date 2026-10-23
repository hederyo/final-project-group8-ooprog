# Karlo — Session Log

Append-only log of Karlo's work sessions, written by Karlo or Karlo's agent. This is how the next agent session knows where work stopped. Changes that affect other members also go in the shared [docs/SESSION_LOG.md](../../docs/SESSION_LOG.md).

**Rules**
- New entries go at the **bottom**. Never edit or delete old entries.
- Write prose, not a copy of `git log`: what was done, what is unfinished, what to do next, and any decision made. Link commits/PRs.
- If a session ends mid-feature, say exactly where it stopped (file, method, what is missing).

Format:
```
## yyyy-MM-dd — <short title>
**Done:** …
**Not done / next:** …
**Notes:** decisions, problems, links
```

---

## 2026-10-22 — Workspace created
**Done:** Documentation only; no code yet. Features and checklist are in [CONTEXT.md](CONTEXT.md).
**Not done / next:** Milestone 0. See the [README build order](../../README.md#build-order).

## 2026-10-23 — Solo Mode: Karlo's Full Medicines and Inventory Module
**Done:**
- Built Group 1: Stand-in infrastructure and development launcher in `stubs/` (`DataFiles`, `DataStore` Singleton with UTF-8 flat-file persistence and auto-seeding, and `DevLauncher` standalone interactive testbed with rapid role switching).
- Built Group 2: Domain models, repositories, and presentation (`Medicine` with invariant-guarding setters and dynamic status predicates `isExpired`, `isNearExpiry`, `isLowStock`, `isSellable`; `MedicineCategory` and `MedicineStatus` enums with display labels; immutable `StockAudit` entity; `InsufficientStockException`; stateless `MedicineRepository` and `StockAuditRepository` enforcing exact column counts and line-numbered `DataAccessException`s; `MedicineTable` standardized table and detail sheet renderer).
- Built Group 3: Admin Catalog CRUD M4 (`InventoryService` list, view, add, update with blank-to-keep, discontinue, reactivate, delete guarded by `OrderService.hasOrdersForMedicine`; `CatalogMenu` with full input validation and confirmation dialogs). Fixed circular constructor recursion between `InventoryService` and `OrderService` using lazy dependency binding. Seeded 7 realistic medicines (OTC/Rx, low stock, near expiry) verified in `data/medicines.txt`.
- Built Group 4: Cross-member stock contracts for Cameron (`findSellable`, `reserveStock`, `restoreStock`) matching exact signatures in ARCHITECTURE §6.3 with no role gates and full Javadoc.
- Built Group 5: Pharmacist Browse M1, View Details M2, and Search & Filter M3 (`MedicineFilter` compound predicate with `matches(Medicine, today)` abstraction; `MedicineBrowserMenu` ensuring pharmacists strictly view sellable active items).
- Built Group 6: Stock & Inventory Audit M5 (`lowStockReport`, `expiryReport`, `recordPhysicalCount` reading system quantity before overwriting to guarantee audit fidelity, `updateLotExpiryReorder`, `auditHistory`, and `StockAuditMenu` with alert banner). Verified audit logging to `data/stock_audits.txt`.
- Verified whole repository compilation across all four modules (`common`, `heather`, `cameron`, `karlo`) and verified resilient re-seeding on deleted `data/` directory.
**Not done / next:** Ready for Karlo's branch commit, PR creation against `integration`, and code defense.
**Notes:** Git-ignored `stubs/` cleanly encapsulates solo test harnesses without modifying other members' folders.
