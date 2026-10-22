# Validation

"Include **all** types of validation" is a graded requirement. Every input in the program goes through a rule on this page. If you add an input and it is not covered here, add a rule here first.

## 1. Two layers — both are required

| Layer | Class | Behaviour on bad input |
|---|---|---|
| **Input layer** (UI) | `ConsoleInput` (+ `InputValidator` rules) | Prints the reason and **asks again**. The user never loses their place, the program never crashes. |
| **Rule layer** (service) | `InputValidator` + service checks | **Throws** a `PharmacyException`. `BaseMenu` prints `Error: <message>` and returns to the menu. |

The service layer always re-checks, even if the menu already validated. The service is the authority. The menu check only improves the experience.

## 2. Input-format rules — `InputValidator` (common)

All methods are `public static`, throw `ValidationException`, and **return the cleaned value** (trimmed, lowercased where noted) so callers store the cleaned form.

| Method | Rule | Error message (example) |
|---|---|---|
| `requireNonBlank(field, value)` | Not null, not empty, not only spaces. Returns trimmed. | `Full name is required.` |
| `requireNoDelimiter(field, value)` | Must not contain `|` (it would corrupt the data file). | `Notes cannot contain the "|" character.` |
| `requireText(field, value, maxLength)` | Optional free text: may be empty, ≤ maxLength, no `|`. | `Side effects must be at most 200 characters.` |
| `requireName(field, value)` | 2–60 chars; letters, spaces, `.` `-` `'` only; at least one letter. | `Full name may contain only letters, spaces, . - '` |
| `requireUsername(value)` | 4–20 chars, `[A-Za-z0-9_]`, starts with a letter. | `Username must be 4-20 letters, digits or _ and start with a letter.` |
| `requirePassword(value)` | 8–30 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit, no spaces, no `|`. | `Password needs at least one uppercase letter.` (say which rule failed) |
| `requireEmail(value)` | ≤ 100 chars, matches `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`. Returns lowercase. | `Enter a valid email, e.g. name@example.com.` |
| `requirePhone(value)` | PH mobile: `^09\d{9}$` (11 digits). | `Contact number must be 11 digits starting with 09.` |
| `requireLicenseNumber(value)` | PRC license: exactly 7 digits `^\d{7}$`. Used for pharmacists and doctors. | `License number must be exactly 7 digits.` |
| `requirePrescriptionId(value)` | `^[A-Za-z0-9-]{4,20}$`. | `Prescription ID must be 4-20 letters, digits or dashes.` |
| `requireSeniorPwdId(value)` | `^[A-Za-z0-9-]{4,20}$`. | `Senior/PWD ID must be 4-20 letters, digits or dashes.` |
| `requireLotNumber(value)` | `^[A-Za-z0-9-]{3,20}$`. | `Lot number must be 3-20 letters, digits or dashes.` |
| `requireDosageStrength(value)` | Number + unit, optional `/` + number + unit; units `mg g mcg mL IU %` (case-insensitive). `500mg`, `250mg/5mL`, `1 g`. | `Dosage must look like 500mg or 250mg/5mL.` |
| `requirePrice(value)` | > 0, ≤ 100,000, at most 2 decimal places. | `Price must be greater than 0.` |
| `requireQuantity(field, value, min, max)` | Whole number in [min, max]. | `Quantity must be between 1 and 1000.` |
| `requireDate(value)` | Strict `yyyy-MM-dd`, real calendar date (rejects `2026-02-30`, `2026-13-01`). | `Enter a real date as yyyy-MM-dd.` |
| `requireFutureDate(field, date)` | After today. | `Expiry date must be after today.` |
| `requireDateRange(from, to)` | `from` ≤ `to`. | `Start date must be on or before end date.` |

`ConsoleInput.readInt` / `readDouble` / `readDate` / `readYesNo` add the input-layer checks: not blank, is a number (catches `NumberFormatException`), within range, a real date, answer is y/yes/n/no.

## 3. Business rules (service layer)

### 3.1 Accounts — Heather

| # | Rule | Exception |
|---|---|---|
| A1 | Login fields are not blank. | `ValidationException` |
| A2 | Unknown username/email **or** wrong password → the same message: `Invalid username/email or password.` (never reveal which one was wrong). | `AuthenticationException` |
| A3 | A `DEACTIVATED` account cannot log in: `This account is deactivated. Please contact an admin.` | `AuthenticationException` |
| A4 | After **3** failed attempts in one login screen, return to the welcome menu. | — (`AuthMenu` counts) |
| A5 | Username, email, and license number are unique (case-insensitive). On update, ignore the user's own record. | `DuplicateRecordException` |
| A6 | Registration always creates a `PHARMACIST` with status `ACTIVE`. A user can never choose their own role. | — |
| A7 | Password entry is followed by "Confirm password"; they must match. | `ValidationException` |
| A8 | Changing your password requires your current password, and the new one must differ from it. | `AuthenticationException` / `ValidationException` |
| A9 | A user can view/update only **their own** profile (from `Session`), never by typing an ID. | — |
| A10 | Admin-only actions call `Session.requireRole(Role.ADMIN)` first. | `AccessDeniedException` |
| A11 | An admin cannot deactivate or delete **themselves**, and the **last active admin** can never be deactivated or deleted. | `ValidationException` |
| A12 | Only a pharmacist with **no** orders (`OrderService.hasOrdersForPharmacist`) can be deleted. Pharmacists with orders, and all admin accounts (they appear in audits and void logs), can only be deactivated ([ADR-012](DECISIONS.md)). | `ValidationException` |
| A13 | Delete and deactivate ask "Are you sure? (y/n)" first. | — |

### 3.2 Medicines and stock — Karlo

| # | Rule | Exception |
|---|---|---|
| M1 | Brand, generic name, category, dosage, price, stock, lot, expiry are required on create. | `ValidationException` |
| M2 | Price follows `requirePrice`; stock 0–100,000; reorder level 0–10,000. No negatives anywhere. | `ValidationException` |
| M3 | Expiry date on create/update must be after today. | `ValidationException` |
| M4 | brand + generic + dosage strength is unique (case-insensitive). | `DuplicateRecordException` |
| M5 | Unknown medicine ID → `Medicine MED-0099 not found.` | `NotFoundException` |
| M6 | Browse/search for pharmacists shows only `ACTIVE` and **not expired** medicines. Admin catalog shows everything, with `[EXPIRED]`, `[LOW STOCK]`, `[DISCONTINUED]` flags. | — |
| M7 | `findSellable` rejects discontinued and expired medicines: `Biogesic 500mg is expired and cannot be sold.` | `ValidationException` |
| M8 | `reserveStock` rejects quantity ≤ 0 and quantity > stock: `Only 4 units of Biogesic 500mg left.` | `ValidationException` / `InsufficientStockException` |
| M9 | Price filter: min ≥ 0, max ≥ min. Category must be a listed number. Filters can be combined. | `ValidationException` |
| M10 | Stock audit: counted quantity 0–100,000; a reason (required text) when counted ≠ system. Writes one `stock_audits.txt` row. | `ValidationException` |
| M11 | A medicine with order history (`OrderService.hasOrdersForMedicine`) cannot be deleted — discontinue instead. | `ValidationException` |
| M12 | Delete and discontinue ask for confirmation. | — |

### 3.3 Orders, discounts, prescriptions, analytics — Cameron

| # | Rule | Exception |
|---|---|---|
| O1 | Customer name follows `requireName`; contact is optional but, if given, follows `requirePhone`. | `ValidationException` |
| O2 | An order must have at least one item before it can be saved or completed. | `ValidationException` |
| O3 | Item quantity 1–1000 and ≤ available stock (via Karlo's `reserveStock`). | `ValidationException` / `InsufficientStockException` |
| O4 | Only sellable medicines can be added (via `findSellable`). | `ValidationException` |
| O5 | If **any** item is prescription-only, the order needs a prescription ID **and** a 7-digit doctor license. | `ValidationException` |
| O6 | `SENIOR_PWD` discount requires a Senior/PWD ID number. | `ValidationException` |
| O7 | `BULK` discount requires 50+ total units. Re-checked after every quantity change; if no longer eligible the change is rejected with a message telling the pharmacist to change the discount first. | `ValidationException` |
| O8 | A pharmacist can see and change **only their own** orders. Typing another pharmacist's order ID → `AccessDeniedException`. | `AccessDeniedException` |
| O9 | Only `PENDING` / `ON_HOLD` orders can be edited. | `InvalidStatusTransitionException` |
| O10 | Every status change goes through `OrderStatus.canTransitionTo` ([ARCHITECTURE §9.1](ARCHITECTURE.md#91-status-machine)). | `InvalidStatusTransitionException` |
| O11 | Completing re-checks that no item has expired since it was added. | `ValidationException` |
| O12 | Cancel and void ask for confirmation. Void also requires a reason (5–200 chars). | `ValidationException` |
| O13 | Void is Admin-only and only for `COMPLETED` orders. | `AccessDeniedException` / `InvalidStatusTransitionException` |
| O14 | Analytics: a day is a valid date; a month is 1–12; a year is 2000 to the current year; a custom range needs from ≤ to. | `ValidationException` |
| O15 | Unknown order ID → `Order ORD-000099 not found.` | `NotFoundException` |

## 4. Hostile-input checklist

Run these by hand on every screen you own before calling a feature done. Professors test exactly these.

- [ ] Press **Enter** on an empty prompt
- [ ] Type only **spaces**
- [ ] Type **letters** where a number is expected (`abc`, `1O`, `5.5` for an int)
- [ ] Type a number **outside the menu range** (`-1`, `99`, `0` where 0 is not an option)
- [ ] Type a **huge number** (`99999999999`): it must not crash with an overflow
- [ ] Type **negative** and **zero** price/quantity
- [ ] Type an **impossible date** (`2026-02-30`, `2026-13-01`, `22-10-2026`, `tomorrow`)
- [ ] Type a **past expiry** date
- [ ] Type a value containing **`|`**
- [ ] Type a **very long** string (200+ characters)
- [ ] Register a **duplicate** username, email, license (also with different capitalization)
- [ ] Log in with a wrong password 3 times
- [ ] Log in with a deactivated account
- [ ] Order more units than in stock; order an expired medicine; order an Rx drug without a prescription ID
- [ ] Try to edit a COMPLETED / CANCELLED order; try to void a PENDING order
- [ ] Pharmacist types **another pharmacist's** order ID
- [ ] Delete a medicine / user that has orders
- [ ] Delete the `data/` folder and start the program: it must recreate it with the default admin
