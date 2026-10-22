# Cameron — Prompts & Git steps

Copy-paste prompts for your AI agent, in order, with the git commands for each step. You do the git commands yourself; the agent writes code only in `cameron/`. Background: [GIT-GUIDE](../../docs/GIT-GUIDE.md).

## How to use this file
1. Open your agent **at the repository root** (Claude Code: run `claude` inside `pharmacy-management-system/`; it loads `AGENTS.md` automatically).
2. For each step: run **Git: start** → paste **[Start of session](#start-of-session)** → paste the **step prompt** → review and test → paste **[End of session](#end-of-session)** → run **Git: finish**.
3. A step can take several sessions. Just start each new session with the *Start of session* prompt again.
4. Read what the agent writes. Ask it "explain this line" whenever you don't understand. You will defend this code, **especially the Strategy pattern**.

## Your order and who waits for whom

| Step | Branch | Wait for | Others waiting for you |
|---|---|---|---|
| 1 | `cameron/menu-skeleton` | Heather's `common-foundation` merged | Heather's `wire-menus` |
| 2 | `cameron/discounts-status` | — | — |
| 3 | `cameron/order-data` | — | **Heather's `manage-users`, Karlo's `catalog-delete`** |
| 4 | `cameron/process-order` | Karlo's `stock-contract` merged | — |
| 5 | `cameron/manage-orders` | — | — |
| 6 | `cameron/admin-orders` | — | — |
| 7 | `cameron/analytics` | Karlo's `stock-audit` merged (to test shrinkage) | — |

Steps 2 and 3 don't need Karlo's code. Do them while you wait for his `stock-contract`.

---

## Working rules
*(The step prompts tell the agent to follow this section.)*

1. **Plan first.** List the files and methods you will create or change, then wait for my OK.
2. Edit only `cameron/**`. Shared `docs/` only if the change makes them wrong. Never touch `common/**`, `heather/**`, or `karlo/**`. If something is missing there, stop and tell me what to ask for.
3. Follow docs/ARCHITECTURE.md (layers, §5.2 Strategy, §6 contracts, §9 orders/stock/status), docs/CONVENTIONS.md, docs/VALIDATION.md, docs/DATA.md, and docs/BLUEPRINT.md §5 exactly. If the spec is unclear or seems wrong, **ask me**; don't invent.
4. Never change stock directly. Always use Karlo's `InventoryService.reserveStock` / `restoreStock`, and roll back on failure (ARCHITECTURE §9.2–9.3).
5. Write clear, simple Java 17 with no libraries. After each class, explain in 2–3 plain sentences what it does and which OOP pillar or pattern it shows.
6. Compile all four folders: `javac -d out $(find common heather karlo cameron -name "*.java")`. Fix every error in my folder. Report errors in other folders to me instead of fixing them.
7. Test by running `java -cp out pharmacy.Main`. You may script input with `printf '...' | java -cp out pharmacy.Main`. Check `data/medicines.txt` stock before and after every order action. Give me the manual test steps, including the hostile inputs from docs/VALIDATION.md §4.
8. **No git commands that change anything** (add, commit, push, merge, reset, switch). Read-only ones (`git status`, `git diff`, `git log`) are fine. I commit and push myself.

## Start of session
```text
I'm Cameron (Group #8). Read AGENTS.md, cameron/AGENTS.md, cameron/docs/CONTEXT.md,
cameron/docs/GOTCHAS.md, cameron/docs/PROMPTS.md (Working rules), and the last 3 entries of
cameron/docs/SESSION_LOG.md and docs/SESSION_LOG.md.
Then tell me in at most 6 lines: what is done, what is next, and anything blocking.
Don't write code yet.
```

## End of session
```text
Before we stop:
1. Tick finished items in cameron/docs/CONTEXT.md (checklists and Progress table).
2. Append an entry to cameron/docs/SESSION_LOG.md (Done / Not done-next / Notes).
3. Add anything surprising to cameron/docs/GOTCHAS.md.
4. If you changed a contract or data format: update docs/ARCHITECTURE.md / docs/DATA.md /
   docs/BLUEPRINT.md and append to docs/SESSION_LOG.md.
5. Give me: the list of changed files, a commit message "type(area): summary",
   and a PR description filled in from .github/pull_request_template.md.
Don't run any git commands that change anything.
```

---

## Step 1 — Menu skeleton (M0b)

**Git: start** (after Heather's `common-foundation` is merged)
```bash
git switch main
git pull
git switch -c cameron/menu-skeleton
```

**Prompt**
```text
Step 1, branch cameron/menu-skeleton. Follow the Working rules in cameron/docs/PROMPTS.md.
Create OrderMenu, OrderAdminMenu and AnalyticsMenu in cameron/src/pharmacy/orders/ui/,
each extending pharmacy.common.ui.BaseMenu with its title and NO options yet
(getOptions returns an empty array, handle does nothing). Explain how BaseMenu's loop works
and which OOP pillars these classes show. Make sure all four folders compile.
```

**Git: finish**
```bash
git add cameron/
git commit -m "feat(orders): add menu skeletons"
git fetch origin
git merge origin/main
git push -u origin cameron/menu-skeleton
gh pr create --base main --title "feat(orders): menu skeletons (M0b)"
```

---

## Step 2 — Discount strategies and order status

**Git: start**
```bash
git switch main
git pull
git switch -c cameron/discounts-status
```

**Prompt**
```text
Step 2, branch cameron/discounts-status. Follow the Working rules in cameron/docs/PROMPTS.md.
Build the Strategy pattern and the status machine:
- discount/: DiscountStrategy interface, RegularDiscount, SeniorPwdDiscount (RATE 0.20),
  BulkDiscount (RATE 0.10, MIN_UNITS 50), DiscountType enum with getLabel() and toStrategy()
  — exactly as docs/ARCHITECTURE.md §5.2 and docs/DECISIONS.md ADR-010
- model/OrderStatus with canTransitionTo, isEditable, isTerminal — the table in
  docs/ARCHITECTURE.md §9.1
- exception/InvalidStatusTransitionException
Then show me a small throwaway check (not committed) that prints the discount of each
strategy on subtotal 1000 with 10 and 60 units, and every allowed/blocked status transition.
Explain the Strategy pattern using my code, as I would explain it in the defense.
```

**Git: finish**
```bash
git add cameron/
git commit -m "feat(orders): add discount strategies and order status rules"
git fetch origin
git merge origin/main
git push -u origin cameron/discounts-status
gh pr create --base main --title "feat(orders): discount strategies and status machine"
```

---

## Step 3 — Order model, repositories, and the `hasOrdersFor*` contract

**Git: start**
```bash
git switch main
git pull
git switch -c cameron/order-data
```

**Prompt**
```text
Step 3, branch cameron/order-data. Follow the Working rules in cameron/docs/PROMPTS.md.
Build, as in cameron/docs/CONTEXT.md and docs/DATA.md §3.4-3.6:
- model: Order (private items list returned unmodifiable, DiscountStrategy field,
  addOrMergeItem/setItemQuantity/removeItem, getTotalUnits/getSubtotal/getDiscountAmount/
  getTotal using Format.round2), OrderItem (unitPriceAtSale snapshot), VoidRecord
- OrderRepository (findAll attaches items; save replaces the order line AND all its item
  lines in both files), VoidRecordRepository (append)
- OrderService.hasOrdersForPharmacist(userId) and hasOrdersForMedicine(medicineId) with the
  exact signatures in docs/ARCHITECTURE.md §6.4 — Heather and Karlo depend on these
Show me an order + its items written to data/ and read back with the same totals.
```

**Git: finish**
```bash
git add cameron/
git commit -m "feat(orders): add order model, repositories and history checks"
git fetch origin
git merge origin/main
git push -u origin cameron/order-data
gh pr create --base main --title "feat(orders): order model and repositories"
```
Then tell Heather and Karlo: "order-data is merged, hasOrdersFor* is available".

---

## Step 4 — Process Dispensing Order + Apply Discount (O1, O2)

**Git: start** (after Karlo's `stock-contract` is merged)
```bash
git switch main
git pull
git switch -c cameron/process-order
```

**Prompt**
```text
Step 4, branch cameron/process-order. Follow the Working rules in cameron/docs/PROMPTS.md.
Implement O1 Process Dispensing Order and O2 Apply Discount Policy from docs/FEATURES.md
with rules O1-O7 from docs/VALIDATION.md §3.3:
- OrderService.checkItem(medicineId, qty): uses Karlo's findSellable + stock check, does NOT reserve
- OrderService.placeOrder(order, completeNow): validate everything, reserveStock for every
  item with rollback on failure (docs/ARCHITECTURE.md §9.2-9.3), then save
- OrderMenu option "New order": customer info → item loop with running cart → Rx ID +
  doctor license if any Rx item → choose discount (Senior/PWD asks for ID, Bulk needs 50+
  units) → summary → Complete now / Save as pending / Discard
I'll first add a few medicines as admin (Karlo's catalog). Test: insufficient stock,
expired medicine, Rx item without prescription, Bulk with 49 units, and Discard leaving
stock unchanged.
```

**Git: finish**
```bash
git add cameron/
git commit -m "feat(orders): add dispensing order creation with discounts"
git fetch origin
git merge origin/main
git push -u origin cameron/process-order
gh pr create --base main --title "feat(orders): process dispensing order + discounts (O1, O2)"
```

---

## Step 5 — View, Update, Complete, Cancel own orders (O3–O6)

**Git: start**
```bash
git switch main
git pull
git switch -c cameron/manage-orders
```

**Prompt**
```text
Step 5, branch cameron/manage-orders. Follow the Working rules in cameron/docs/PROMPTS.md.
Implement O3 View Sales Orders, O4 Update Pending Order, O5 Complete Order and
O6 Cancel Dispensing Order from docs/FEATURES.md with rules O7-O12:
- every method checks the order belongs to Session's user (AccessDeniedException otherwise)
- updates only for PENDING/ON_HOLD; each quantity change reserves/restores the difference
  with rollback; Bulk eligibility re-checked after every change
- putOnHold / resume / complete (re-check expiry) / cancel (restore ALL stock, confirm first)
- OrderMenu options: My orders (filter by status, view details) · Update order ·
  Complete order · Cancel order
For each action, show me data/medicines.txt stock before and after.
```

**Git: finish**
```bash
git add cameron/
git commit -m "feat(orders): add view, update, complete and cancel for own orders"
git fetch origin
git merge origin/main
git push -u origin cameron/manage-orders
gh pr create --base main --title "feat(orders): manage own orders (O3-O6)"
```

---

## Step 6 — Admin: status, void, prescription audit, search (O7–O10)

**Git: start**
```bash
git switch main
git pull
git switch -c cameron/admin-orders
```

**Prompt**
```text
Step 6, branch cameron/admin-orders. Follow the Working rules in cameron/docs/PROMPTS.md.
Implement O7 Update Transaction Status, O8 Void Erroneous Orders, O9 Audit Prescriptions and
O10 Search and Filter Orders from docs/FEATURES.md with rules O10, O12, O13, O15:
- OrderService admin methods (each starts with Session.requireRole(Role.ADMIN)):
  allOrders, searchOrders(keyword, status, pharmacistId, from, to), updateStatus (only
  allowed next statuses, never VOIDED), voidOrder(id, reason) (COMPLETED only, restore stock,
  append VoidRecord, confirm first), prescriptionAudit(doctorLicense, from, to)
- show pharmacist names using Heather's UserRepository.findById
- OrderAdminMenu: All orders · Search & filter · View order · Update status · Void order ·
  Prescription audit
```

**Git: finish**
```bash
git add cameron/
git commit -m "feat(orders): add admin status update, void, Rx audit and search"
git fetch origin
git merge origin/main
git push -u origin cameron/admin-orders
gh pr create --base main --title "feat(orders): admin order management (O7-O10)"
```

---

## Step 7 — Sales & Inventory Analytics (O11)

**Git: start** (after Karlo's `stock-audit` is merged)
```bash
git switch main
git pull
git switch -c cameron/analytics
```

**Prompt**
```text
Step 7, branch cameron/analytics. Follow the Working rules in cameron/docs/PROMPTS.md.
Implement O11 View Sales & Inventory Analytics from docs/FEATURES.md with rule O14:
- model/SalesReport (revenue, transaction count, total discounts, top 5 medicines by units,
  shrinkage units and pesos, cancelled count, voided count)
- AnalyticsService.report(from, to): only COMPLETED orders by completedAt count toward
  revenue/count/top sellers; shrinkage from Karlo's StockAuditRepository.findAll() (negative
  differences) priced with MedicineRepository.findById
- AnalyticsMenu: Daily · Monthly · Yearly · Custom range — each just builds from/to dates
Create a few completed, cancelled and voided orders first and check the numbers by hand.
```

**Git: finish**
```bash
git add cameron/
git commit -m "feat(orders): add sales and inventory analytics"
git fetch origin
git merge origin/main
git push -u origin cameron/analytics
gh pr create --base main --title "feat(orders): sales & inventory analytics (O11)"
```

---

## Any time

**Fix a bug**
```text
Follow the Working rules in cameron/docs/PROMPTS.md. Bug: <what I did> → <what happened>
→ <what should happen>. Find the root cause before changing anything, explain it to me,
then fix it properly. If the cause is in another member's folder, don't edit it — tell me
what to report to them. Add the trap to cameron/docs/GOTCHAS.md if it could happen again.
```

**Prepare for the defense**
```text
Quiz me on my code. For each of: Strategy pattern, encapsulation, abstraction, inheritance,
polymorphism, exception handling (including the stock rollback), and validation — show me
the exact file and lines in cameron/, explain it simply, then ask me one question about it
and correct my answer. Use docs/ARCHITECTURE.md §10 as the list.
```

**Review my branch before the PR**
```text
Review everything changed on this branch compared to main (git diff main --stat, then read
the files). Check against: docs/VALIDATION.md §3.3, docs/CONVENTIONS.md §2-3, docs/ARCHITECTURE.md
§9 (stock and status), the contracts in §6, and that only cameron/ (and intended docs)
changed. List problems by severity. Don't fix anything until I say so.
```
