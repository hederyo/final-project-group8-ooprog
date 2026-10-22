# Karlo — Prompts & Git steps

Copy-paste prompts for your AI agent, in order, with the git commands for each step. You do the git commands yourself; the agent writes code only in `karlo/`. Background: [GIT-GUIDE](../../docs/GIT-GUIDE.md).

## How to use this file
1. Open your agent **at the repository root** (Claude Code: run `claude` inside `pharmacy-management-system/`; it loads `AGENTS.md` automatically).
2. For each step: run **Git: start** → paste **[Start of session](#start-of-session)** → paste the **step prompt** → review and test → paste **[End of session](#end-of-session)** → run **Git: finish**.
3. A step can take several sessions. Just start each new session with the *Start of session* prompt again.
4. Read what the agent writes. Ask it "explain this line" whenever you don't understand. You will defend this code.

## Your order and who waits for whom

| Step | Branch | Wait for | Others waiting for you |
|---|---|---|---|
| 1 | `karlo/menu-skeleton` | Heather's `common-foundation` merged | Heather's `wire-menus` |
| 2 | `karlo/medicine-model` | — | Cameron's `analytics` (needs `StockAuditRepository`) |
| 3 | `karlo/catalog-crud` | — | Cameron needs medicines to test with |
| 4 | `karlo/stock-contract` | — | **Cameron's `process-order`** |
| 5 | `karlo/browse-search` | — | — |
| 6 | `karlo/stock-audit` | — | — |
| 7 | `karlo/catalog-delete` | Cameron's `order-data` merged | — |

---

## Working rules
*(The step prompts tell the agent to follow this section.)*

1. **Plan first.** List the files and methods you will create or change, then wait for my OK.
2. Edit only `karlo/**`. Shared `docs/` only if the change makes them wrong. Never touch `common/**`, `heather/**`, or `cameron/**`. If something is missing there, stop and tell me what to ask for.
3. Follow docs/ARCHITECTURE.md (layers, §6 contracts), docs/CONVENTIONS.md, docs/VALIDATION.md, docs/DATA.md, and docs/BLUEPRINT.md §4 exactly. If the spec is unclear or seems wrong, **ask me**; don't invent.
4. Write clear, simple Java 17 with no libraries. After each class, explain in 2–3 plain sentences what it does and which OOP pillar it shows.
5. Compile all four folders: `javac -d out $(find common heather karlo cameron -name "*.java")`. Fix every error in my folder. Report errors in other folders to me instead of fixing them.
6. Test by running `java -cp out pharmacy.Main` (log in as admin / Admin123). You may script input with `printf '...' | java -cp out pharmacy.Main`. Then give me the manual test steps, including the hostile inputs from docs/VALIDATION.md §4.
7. **No git commands that change anything** (add, commit, push, merge, reset, switch). Read-only ones (`git status`, `git diff`, `git log`) are fine. I commit and push myself.

## Start of session
```text
I'm Karlo (Group #8). Read AGENTS.md, karlo/AGENTS.md, karlo/docs/CONTEXT.md,
karlo/docs/GOTCHAS.md, karlo/docs/PROMPTS.md (Working rules), and the last 3 entries of
karlo/docs/SESSION_LOG.md and docs/SESSION_LOG.md.
Then tell me in at most 6 lines: what is done, what is next, and anything blocking.
Don't write code yet.
```

## End of session
```text
Before we stop:
1. Tick finished items in karlo/docs/CONTEXT.md (checklists and Progress table).
2. Append an entry to karlo/docs/SESSION_LOG.md (Done / Not done-next / Notes).
3. Add anything surprising to karlo/docs/GOTCHAS.md.
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
git switch -c karlo/menu-skeleton
```

**Prompt**
```text
Step 1, branch karlo/menu-skeleton. Follow the Working rules in karlo/docs/PROMPTS.md.
Create MedicineBrowserMenu, CatalogMenu and StockAuditMenu in karlo/src/pharmacy/inventory/ui/,
each extending pharmacy.common.ui.BaseMenu with its title and NO options yet
(getOptions returns an empty array, handle does nothing). Explain how BaseMenu's loop works
and which OOP pillars these three classes show. Make sure all four folders compile.
```

**Git: finish**
```bash
git add karlo/
git commit -m "feat(inventory): add menu skeletons"
git fetch origin
git merge origin/main
git push -u origin karlo/menu-skeleton
gh pr create --base main --title "feat(inventory): menu skeletons (M0b)"
```

---

## Step 2 — Medicine model and repositories

**Git: start**
```bash
git switch main
git pull
git switch -c karlo/medicine-model
```

**Prompt**
```text
Step 2, branch karlo/medicine-model. Follow the Working rules in karlo/docs/PROMPTS.md.
Build, as in karlo/docs/CONTEXT.md and docs/DATA.md §3.2-3.3 and §4:
- model: Medicine (private fields, guarded setters, isExpired/isNearExpiry/isLowStock/isSellable,
  toFileLine), MedicineCategory (with getLabel), MedicineStatus, StockAudit
- MedicineRepository and StockAuditRepository (stateless; a wrong column count throws
  DataAccessException with the line number — never skip a line)
- exception/InsufficientStockException
- ui/MedicineTable helper that prints a medicine list and a medicine detail view
Show me a sample medicines.txt line produced by toFileLine and parsed back.
```

**Git: finish**
```bash
git add karlo/
git commit -m "feat(inventory): add medicine model and repositories"
git fetch origin
git merge origin/main
git push -u origin karlo/medicine-model
gh pr create --base main --title "feat(inventory): medicine model and repositories"
```

---

## Step 3 — Catalog CRUD, Admin (M4: add, list, view, update, discontinue)

**Git: start**
```bash
git switch main
git pull
git switch -c karlo/catalog-crud
```

**Prompt**
```text
Step 3, branch karlo/catalog-crud. Follow the Working rules in karlo/docs/PROMPTS.md.
Implement M4 Manage Medicine Catalog from docs/FEATURES.md except Delete (that is Step 7),
with rules M1-M5 and M12 from docs/VALIDATION.md §3.2:
- InventoryService (admin methods start with Session.requireRole(Role.ADMIN)): listAll,
  getDetails, addMedicine, updateMedicine (blank keeps current), discontinue, reactivate
- CatalogMenu options: List all · View details · Add · Update · Discontinue/Reactivate
  (Search & filter is added in Step 5)
Admin lists show [EXPIRED] [LOW STOCK] [NEAR EXPIRY] [DISCONTINUED] flags.
Then add 5-8 realistic medicines through the menu (mix of OTC and Rx, one near expiry)
and show me the resulting data/medicines.txt. (data/ is not committed: once this is merged,
Cameron adds his own test medicines through the same menu.)
```

**Git: finish**
```bash
git add karlo/
git commit -m "feat(inventory): add medicine catalog management"
git fetch origin
git merge origin/main
git push -u origin karlo/catalog-crud
gh pr create --base main --title "feat(inventory): medicine catalog CRUD (M4)"
```

---

## Step 4 — Stock contract for Cameron

**Git: start**
```bash
git switch main
git pull
git switch -c karlo/stock-contract
```

**Prompt**
```text
Step 4, branch karlo/stock-contract. Follow the Working rules in karlo/docs/PROMPTS.md.
Implement the three methods Cameron depends on, with the EXACT signatures in
docs/ARCHITECTURE.md §6.3:
- findSellable(medicineId): ACTIVE and not expired, else ValidationException (rule M7)
- reserveStock(medicineId, quantity): quantity > 0 and <= stock, else ValidationException /
  InsufficientStockException with a message like "Only 4 units of Biogesic 500mg left." (M8)
- restoreStock(medicineId, quantity): works for ANY existing medicine, even discontinued or
  expired (see karlo/docs/GOTCHAS.md)
No role check on these three. Add Javadoc to each explaining what Cameron can rely on.
Since there is no order screen yet, show me a small throwaway check (not committed) that
reserves then restores stock and prints the stock before/after.
```

**Git: finish**
```bash
git add karlo/
git commit -m "feat(inventory): add findSellable, reserveStock and restoreStock"
git fetch origin
git merge origin/main
git push -u origin karlo/stock-contract
gh pr create --base main --title "feat(inventory): stock reservation contract for orders"
```
Then tell Cameron: "stock-contract is merged".

---

## Step 5 — Browse, View Details, Search & Filter (M1, M2, M3)

**Git: start**
```bash
git switch main
git pull
git switch -c karlo/browse-search
```

**Prompt**
```text
Step 5, branch karlo/browse-search. Follow the Working rules in karlo/docs/PROMPTS.md.
Implement M1 Browse, M2 View Details and M3 Search & Filter from docs/FEATURES.md
with rules M5, M6, M9:
- MedicineFilter (keyword, category, min/max price, Rx/OTC, sellableOnly; matches(m, today);
  validate())
- InventoryService.browseSellable, search(filter) — forces sellableOnly for pharmacists,
  getDetails hides non-sellable medicines from pharmacists
- MedicineBrowserMenu (pharmacist): Browse all · Search & filter · View details
- add "Search & filter" to CatalogMenu (admin sees everything)
Every filter can be skipped; chosen filters combine (AND). Show empty states.
```

**Git: finish**
```bash
git add karlo/
git commit -m "feat(inventory): add medicine browse, details and search"
git fetch origin
git merge origin/main
git push -u origin karlo/browse-search
gh pr create --base main --title "feat(inventory): browse, details, search & filter (M1-M3)"
```

---

## Step 6 — Stock & Inventory Audit, Admin (M5)

**Git: start**
```bash
git switch main
git pull
git switch -c karlo/stock-audit
```

**Prompt**
```text
Step 6, branch karlo/stock-audit. Follow the Working rules in karlo/docs/PROMPTS.md.
Implement M5 Stock & Inventory Audit from docs/FEATURES.md with rule M10:
- InventoryService: lowStockReport, expiryReport (expired + within NEAR_EXPIRY_DAYS),
  recordPhysicalCount(id, counted, reason) — read systemQuantity BEFORE overwriting,
  write a StockAudit row; updateLotExpiryReorder; auditHistory(idOrNull)
- StockAuditMenu: Low-stock report · Expiry report · Record physical count ·
  Update lot/expiry/reorder level · Audit history; print "N low-stock, M near-expiry" on open
Show me data/stock_audits.txt after recording one count that is lower than the system stock.
```

**Git: finish**
```bash
git add karlo/
git commit -m "feat(inventory): add stock audit and expiry reports"
git fetch origin
git merge origin/main
git push -u origin karlo/stock-audit
gh pr create --base main --title "feat(inventory): stock & inventory audit (M5)"
```

---

## Step 7 — Delete medicine (finishes M4)

**Git: start** (after Cameron's `order-data` is merged)
```bash
git switch main
git pull
git switch -c karlo/catalog-delete
```

**Prompt**
```text
Step 7, branch karlo/catalog-delete. Follow the Working rules in karlo/docs/PROMPTS.md.
Add Delete to CatalogMenu and InventoryService.deleteMedicine(id) with rules M11, M12:
a medicine with order history (Cameron's OrderService.hasOrdersForMedicine) cannot be
deleted — tell the admin to discontinue it instead. Confirm before deleting.
```

**Git: finish**
```bash
git add karlo/
git commit -m "feat(inventory): add medicine delete with order-history guard"
git fetch origin
git merge origin/main
git push -u origin karlo/catalog-delete
gh pr create --base main --title "feat(inventory): delete medicine (M4)"
```

---

## Any time

**Fix a bug**
```text
Follow the Working rules in karlo/docs/PROMPTS.md. Bug: <what I did> → <what happened>
→ <what should happen>. Find the root cause before changing anything, explain it to me,
then fix it properly. If the cause is in another member's folder, don't edit it — tell me
what to report to them. Add the trap to karlo/docs/GOTCHAS.md if it could happen again.
```

**Prepare for the defense**
```text
Quiz me on my code. For each of: encapsulation, abstraction, inheritance, polymorphism,
exception handling, Singleton usage, and validation — show me the exact file and lines in
karlo/, explain it simply, then ask me one question about it and correct my answer.
Use docs/ARCHITECTURE.md §10 as the list.
```

**Review my branch before the PR**
```text
Review everything changed on this branch compared to main (git diff main --stat, then read
the files). Check against: docs/VALIDATION.md §3.2, docs/CONVENTIONS.md §2-3, the contracts
in docs/ARCHITECTURE.md §6.3, and that only karlo/ (and intended docs) changed.
List problems by severity. Don't fix anything until I say so.
```
