# cameron/ — Cameron's workspace

**Member:** Roxas, Cameron
**Area:** Orders and transactions:
- Process Dispensing Order, with the discount **Strategy pattern**
- View, Update, Complete and Cancel orders
- Admin: Update Status, Void, Audit Prescriptions, Search Orders, Sales & Inventory Analytics

**Package:** `pharmacy.orders`

## Read in this order
1. Root [AGENTS.md](../AGENTS.md): project rules and doc map.
2. This file.
3. [docs/CONTEXT.md](docs/CONTEXT.md): your features, classes, contracts, checklist.
   Your member runs the steps in [docs/PROMPTS.md](docs/PROMPTS.md). Follow its **Working rules**.
4. [docs/GOTCHAS.md](docs/GOTCHAS.md) and the last few entries of [docs/SESSION_LOG.md](docs/SESSION_LOG.md).
5. Shared docs as needed: [FEATURES §Orders](../docs/FEATURES.md#orders--cameron), [VALIDATION §3.3](../docs/VALIDATION.md#33-orders-discounts-prescriptions-analytics--cameron), [DATA §3.4–3.6](../docs/DATA.md#34-orderstxt--owner-cameron), [ARCHITECTURE §5.2 and §9](../docs/ARCHITECTURE.md#9-orders-stock-and-status).

## What you may edit
- `cameron/**`.
- Shared `docs/*.md`, only when your change makes them wrong, in the same PR.
- **Never** `common/**`, `heather/**`, or `karlo/**`. Use their classes exactly as documented in [ARCHITECTURE §6](../docs/ARCHITECTURE.md#6-cross-member-contracts). If something is missing, stop and tell Cameron to ask the owner.

## Rules
- **Never change stock yourself.** Always go through Karlo's `InventoryService.reserveStock` / `restoreStock`.
- Heather's and Karlo's delete features depend on your `OrderService.hasOrdersForPharmacist` / `hasOrdersForMedicine`. Build these two early ([ARCHITECTURE §6.4](../docs/ARCHITECTURE.md#64-provided-by-cameron--used-by-heather-karlo-and-common)).
- Follow [CONVENTIONS](../docs/CONVENTIONS.md). Every input needs a validation rule from [VALIDATION](../docs/VALIDATION.md).
- Cameron must be able to explain every line in the defense, especially the Strategy pattern. Prefer clear code to clever code.

## Before ending a session
1. Tick finished items in [docs/CONTEXT.md](docs/CONTEXT.md).
2. Append an entry to [docs/SESSION_LOG.md](docs/SESSION_LOG.md).
3. Add anything surprising to [docs/GOTCHAS.md](docs/GOTCHAS.md).
4. If you changed a contract or the data format, also log it in the shared [docs/SESSION_LOG.md](../docs/SESSION_LOG.md).
