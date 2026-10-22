# Features

Every feature from the group proposal, with its **owner**, **menu path**, **CRUD operation**, and **rules**. Validation rule IDs (A1, M3, O7…) point to [VALIDATION.md](VALIDATION.md).

Owners: **H** = Heather · **K** = Karlo · **C** = Cameron

## Summary

| # | Feature | Role | Op | Owner |
|---|---|---|---|---|
| A1 | Register Account | Pharmacist | Create | H |
| A2 | Login / Logout | Both | — | H |
| A3 | Manage Profile | Both | Read, Update | H |
| A4 | Manage Users | Admin | CRUD | H |
| M1 | Browse Medicines | Pharmacist | Read | K |
| M2 | View Medicine Details | Both | Read | K |
| M3 | Search and Filter Medicines | Both | — | K |
| M4 | Manage Medicine Catalog | Admin | CRUD | K |
| M5 | Stock & Inventory Audit | Admin | Read, Update | K |
| O1 | Process Dispensing Order | Pharmacist | Create | C |
| O2 | Apply Discount Policy | Pharmacist | — | C |
| O3 | View Sales Orders | Pharmacist | Read | C |
| O4 | Update Pending Order | Pharmacist | Update | C |
| O5 | Complete Order | Pharmacist | Update | C |
| O6 | Cancel Dispensing Order | Pharmacist | Delete (soft) | C |
| O7 | Update Transaction Status | Admin | Update | C |
| O8 | Void Erroneous Orders | Admin | Delete (soft) | C |
| O9 | Audit Prescriptions | Admin | Read | C |
| O10 | Search and Filter Orders | Admin | — | C |
| O11 | View Sales & Inventory Analytics | Admin | Read | C |

"Search and Filter Records" (Admin) is split by record type: users → A4 (H), medicines → M3 (K), orders and prescriptions → O10/O9 (C).

"Admin Login / Logout" is the same screen as A2. The role stored on the account decides which menu opens ([ADR-008](DECISIONS.md)).

---

## Accounts — Heather

### A1 Register Account
**Path:** Welcome → 2 Register
- Asks for full name, email, contact number, PRC license number, username, password, then confirm password.
- Each field is checked as it is typed (`ConsoleInput.readValidated`). `AuthService.register` re-checks everything (A5–A7).
- Creates a `Pharmacist` with status `ACTIVE`. The user returns to the welcome screen and logs in.

### A2 Login / Logout
**Path:** Welcome → 1 Login · any role menu → 0 Logout
- Log in with username **or** email (case-insensitive) and password (case-sensitive) (A1–A4).
- On success: `Session.login(user)`, print `Welcome, <full name> (<role>)`, then `user.createMenu().show()`.
- Logout: `Session.logout()`, print `You have been logged out.`, back to Welcome.

### A3 Manage Profile
**Path:** Pharmacist 3 / Admin 6 → My profile
1. View my profile. Shows every field except the password; the license is shown for pharmacists.
2. Update name / email / contact number. Press Enter to keep a value unchanged (`readOptional`).
3. Change password: current password, then new password, then confirm (A8).

A user cannot change their own username, role, status or user ID (A6, A9).

### A4 Manage Users (Admin)
**Path:** Admin 3 → Manage users
1. List all users: ID, username, name, role, status.
2. Search users by name, username, or license number. Filter by role or status.
3. View user details.
4. Create a staff account (Pharmacist or Admin), using the same fields and rules as A1.
5. Update a user's name, email, contact, or license.
6. Deactivate or reactivate a user (A11).
7. Delete a user (A11–A13).

---

## Medicines — Karlo

### M1 Browse Medicines
**Path:** Pharmacist 1 → Medicines → 1 Browse all
- A table of `ACTIVE`, non-expired medicines with these columns: ID, brand, generic, category, strength, price, Rx/OTC, stock. Sorted by brand name.
- Empty state: `No medicines available.`

### M2 View Medicine Details
**Path:** Medicines → 3 View details (Admin: Catalog → View details)
- Enter a medicine ID. Shows every field, including side effects/contraindications, storage, lot, expiry, Rx requirement, and flags (`[LOW STOCK]`, `[NEAR EXPIRY]`, and for admins also `[EXPIRED]` and `[DISCONTINUED]`).

### M3 Search and Filter Medicines
**Path:** Medicines → 2 Search & filter (Admin: Catalog → Search & filter)
- Keyword: matches part of the brand or generic name, case-insensitive.
- Filters: category, price range (min–max), and Rx-only or OTC-only. Any filter can be skipped. All chosen filters apply together (AND).
- One `MedicineFilter` object holds the criteria. `matches(Medicine)` decides (M9).
- Pharmacists only ever see sellable medicines (M6).

### M4 Manage Medicine Catalog (Admin)
**Path:** Admin 1 → Medicine catalog
1. List all medicines, including expired and discontinued ones, with flags.
2. Search & filter (M3).
3. View details (M2).
4. Add a medicine (M1–M4).
5. Update a medicine: any field except the ID. Press Enter to keep a value (M2–M4).
6. Discontinue or reactivate a medicine. Discontinued medicines are hidden from pharmacists and cannot be sold.
7. Delete a medicine. Only allowed when it has no order history (M11, M12).

### M5 Stock & Inventory Audit (Admin)
**Path:** Admin 2 → Stock & inventory
1. **Low-stock report:** medicines where `stock <= reorderLevel`.
2. **Expiry report:** medicines already expired, plus those expiring within 30 days.
3. **Record physical count:** enter the medicine ID and the counted quantity (plus a reason if it differs). This sets the stock and writes an audit row (M10).
4. **Update lot number / expiry / reorder level** for one medicine.
5. **Audit history:** list `stock_audits.txt`, optionally for one medicine.

A short alert line (`3 low-stock, 2 near-expiry`) is shown when this menu opens.

---

## Orders — Cameron

### O1 Process Dispensing Order
**Path:** Pharmacist 2 → Dispensing orders → 1 New order
1. Enter the customer name and contact (optional) (O1).
2. Add items in a loop: medicine ID, then quantity. After each item, show the running cart with line totals. Each item is checked with `findSellable` and against current stock (O3, O4), but stock is **not** reserved yet: the cart lives in memory. Adding the same medicine again increases its quantity.
3. If any item is Rx-only, ask for the prescription ID and doctor license (O5).
4. Choose a discount (O2).
5. Show the summary (subtotal, discount, total). The pharmacist then picks one:
   - **Complete now**, which reserves stock and sets COMPLETED
   - **Save as pending**, which reserves stock and keeps it PENDING
   - **Discard**, which saves nothing and leaves stock untouched

   Saving reserves stock for every item ([ARCHITECTURE §9.2](ARCHITECTURE.md#92-stock-movement)).

### O2 Apply Discount Policy
Part of O1 and O4. The menu lists the three strategies with their labels:
- Regular: no ID needed.
- Senior Citizen / PWD (20%): asks for the ID number (O6).
- Bulk Wholesale (10%): only accepted at 50 or more units (O7).

After choosing, the totals are recomputed and shown.

### O3 View Sales Orders
**Path:** Dispensing orders → 2 My orders
- Lists **only the logged-in pharmacist's** orders: ID, date, customer, item count, total, status. Newest first ([ADR-007](DECISIONS.md)).
- Can filter by status. Selecting an ID shows full details: items, quantities, unit prices, subtotal, discount, total, status, and Rx info.

### O4 Update Pending Order
**Path:** Dispensing orders → 3 Update order
- Only for your own PENDING or ON_HOLD orders (O8, O9). The pharmacist can:
  - add an item
  - change a quantity
  - remove an item
  - change the discount
  - edit the customer notes
  - put the order on hold, or resume it
- Each stock change goes through reserve/restore ([ARCHITECTURE §9.2](ARCHITECTURE.md#92-stock-movement)).

### O5 Complete Order
**Path:** Dispensing orders → 4 Complete order
- PENDING → COMPLETED after the expiry re-check (O11) and the Rx check (O5). Sets `completedAt`.

### O6 Cancel Dispensing Order
**Path:** Dispensing orders → 5 Cancel order
- Confirm first. PENDING/ON_HOLD → CANCELLED, and all reserved stock is restored (O12). The record is kept, not erased.

### O7 Update Transaction Status (Admin)
**Path:** Admin 4 → Orders & prescriptions → Update status
- Enter the order ID. Only the allowed next statuses are shown, from `canTransitionTo`. VOIDED is not offered here; it goes through O8.

### O8 Void Erroneous Orders (Admin)
**Path:** Admin 4 → Orders & prescriptions → Void order
- Only for COMPLETED orders. Confirm, then enter a reason. Sets VOIDED, restores stock, and writes `void_log.txt` (O12, O13). After that the order is locked.

### O9 Audit Prescriptions (Admin)
**Path:** Admin 4 → Orders & prescriptions → Prescription audit
- Lists every order that has a prescription ID: order ID, date, customer, Rx medicines, prescription ID, doctor license, pharmacist name (via Heather's `UserRepository.findById`), and status.
- Can filter by doctor license or date range.

### O10 Search and Filter Orders (Admin)
**Path:** Admin 4 → Orders & prescriptions → Search orders
- Search by order ID or customer name (partial, case-insensitive).
- Filter by status, pharmacist, or date range. Filters combine.

### O11 View Sales & Inventory Analytics (Admin)
**Path:** Admin 5 → Sales & analytics
- Choose a period: a day, a month, a year, or a custom range (O14).
- Only **COMPLETED** orders, by `completedAt`, count toward:
  - **Gross revenue**: the sum of order totals
  - **Transaction count**
  - **Total discounts given**
  - **Top 5 medicines** by units sold
- **Inventory shrinkage** for the period: the sum of negative `difference` values in stock audits, in units and in pesos at current prices (via Karlo's `StockAuditRepository`).
- Also shows the counts of cancelled and voided orders for the period.
