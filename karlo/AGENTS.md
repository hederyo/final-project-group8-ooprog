# karlo/ — Karlo's workspace

**Member:** Silang, Karlo
**Area:** Medicines and inventory: Browse, View Details, Search & Filter, Admin Catalog CRUD, Stock & Inventory Audit.
**Package:** `pharmacy.inventory`

## Read in this order
1. Root [AGENTS.md](../AGENTS.md): project rules and doc map.
2. This file.
3. [docs/CONTEXT.md](docs/CONTEXT.md): your features, classes, contracts, checklist.
   Your member runs the steps in [docs/PROMPTS.md](docs/PROMPTS.md). Follow its **Working rules**.
4. [docs/GOTCHAS.md](docs/GOTCHAS.md) and the last few entries of [docs/SESSION_LOG.md](docs/SESSION_LOG.md).
5. Shared docs as needed: [FEATURES §Medicines](../docs/FEATURES.md#medicines--karlo), [VALIDATION §3.2](../docs/VALIDATION.md#32-medicines-and-stock--karlo), [DATA §3.2–3.3](../docs/DATA.md#32-medicinestxt--owner-karlo), [ARCHITECTURE §6](../docs/ARCHITECTURE.md#6-cross-member-contracts).

## What you may edit
- `karlo/**`.
- Shared `docs/*.md`, only when your change makes them wrong, in the same PR.
- **Never** `common/**`, `heather/**`, or `cameron/**`. Use `common/` exactly as documented. If something is missing, stop and tell Karlo to ask the owner ([CONVENTIONS §5](../docs/CONVENTIONS.md#5-changing-common-or-a-cross-member-contract)).

## Rules
- Cameron's order code depends on `InventoryService.findSellable`, `reserveStock`, and `restoreStock`, plus `MedicineRepository.findById` and `StockAuditRepository.findAll`. Keep those signatures exactly as in [ARCHITECTURE §6.3](../docs/ARCHITECTURE.md#63-provided-by-karlo--used-by-cameron-and-common).
- Follow [CONVENTIONS](../docs/CONVENTIONS.md). Every input needs a validation rule from [VALIDATION](../docs/VALIDATION.md).
- Karlo must be able to explain every line in the defense. Prefer clear code to clever code.

## Before ending a session
1. Tick finished items in [docs/CONTEXT.md](docs/CONTEXT.md).
2. Append an entry to [docs/SESSION_LOG.md](docs/SESSION_LOG.md).
3. Add anything surprising to [docs/GOTCHAS.md](docs/GOTCHAS.md).
4. If you changed a contract or the data format, also log it in the shared [docs/SESSION_LOG.md](../docs/SESSION_LOG.md).
