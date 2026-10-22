# Data

Every entity, its fields, and the exact text-file format. The repository classes (`*Repository`) are the only code that turns these lines into objects and back. The column order below is the contract: if you add or reorder a column, update this file in the same commit and tell the group (it breaks everyone's existing `data/` files).

## 1. Storage rules

| Rule | Value |
|---|---|
| Folder | `data/` next to where the program runs. Created by `DataStore.seedDefaults()`. **Git-ignored**: each member has their own local data. |
| One file per entity | See §3. File names are constants in `DataFiles` — never type a file name anywhere else. |
| Encoding | UTF-8. |
| Delimiter | `|` (pipe). Split with `line.split("\\|", -1)` — the `-1` keeps empty trailing columns. |
| Header row | None. Every non-blank line is one record. |
| Empty / optional value | Empty string between pipes: `a||c`. |
| Pipe inside a value | Not allowed. `InputValidator.requireNoDelimiter` rejects `|` in every text input. |
| Saving | `DataStore.writeAll` rewrites the whole file from the in-memory list (simple and safe for small files). |
| Corrupted line | A repository that finds a line with the wrong column count **throws `DataAccessException`** naming the file and line number. It must never skip it — the next `writeAll` would silently delete that record. |

## 2. Value formats

| Kind | Stored as | Example |
|---|---|---|
| Date | `yyyy-MM-dd` (`Format.DATE`) | `2027-03-31` |
| Date-time | `yyyy-MM-dd HH:mm:ss` (`Format.DATE_TIME`) | `2026-10-22 14:30:05` |
| Money | `double`, 2 decimals, dot, no symbol, no commas | `1250.50` |
| Money on screen | `Format.peso` → `PHP 1,250.50` (ASCII, works in every console — see [GOTCHAS](GOTCHAS.md)) | |
| Boolean | `true` / `false` | |
| Enum | `name()` of the constant | `PHARMACIST`, `ON_HOLD` |
| Phone | `09XXXXXXXXX` (11 digits) | `09171234567` |

## 3. Files and columns

### 3.1 `users.txt` — owner: Heather

`userId|role|username|password|fullName|email|contactNumber|licenseNumber|status`

| # | Field | Type | Notes |
|---|---|---|---|
| 0 | userId | `U-0001` | `nextId(USERS, "U-", 4)` |
| 1 | role | `Role` | `ADMIN` or `PHARMACIST` — decides which subclass is created when loading |
| 2 | username | String | Unique, case-insensitive |
| 3 | password | String | **Plaintext** ([ADR-003](DECISIONS.md)) |
| 4 | fullName | String | |
| 5 | email | String | Unique, case-insensitive, stored lowercase |
| 6 | contactNumber | String | `09XXXXXXXXX` |
| 7 | licenseNumber | String | Pharmacist: 7-digit PRC license, unique. Admin: empty |
| 8 | status | `AccountStatus` | `ACTIVE` / `DEACTIVATED` |

```
U-0001|ADMIN|admin|Admin123|System Administrator|admin@pharmacy.local|09000000000||ACTIVE
U-0002|PHARMACIST|jdelacruz|Pharma2026|Juan Dela Cruz|juan@gmail.com|09171234567|0123456|ACTIVE
```

**Default admin (seeded).** When `users.txt` does not exist, `DataStore.seedDefaults()` creates it with the first line above: username `admin`, password `Admin123`. Change the password after the first login. ([ADR-009](DECISIONS.md))

### 3.2 `medicines.txt` — owner: Karlo

`medicineId|brandName|genericName|category|dosageStrength|unitPrice|stockQuantity|reorderLevel|lotNumber|expiryDate|prescriptionRequired|sideEffects|storageInstructions|status`

| # | Field | Type | Notes |
|---|---|---|---|
| 0 | medicineId | `MED-0001` | `nextId(MEDICINES, "MED-", 4)` |
| 1 | brandName | String | e.g. `Biogesic` |
| 2 | genericName | String | e.g. `Paracetamol` |
| 3 | category | `MedicineCategory` | see §4 |
| 4 | dosageStrength | String | e.g. `500mg`, `250mg/5mL` |
| 5 | unitPrice | money | > 0 |
| 6 | stockQuantity | int | ≥ 0 (reserved units are already subtracted) |
| 7 | reorderLevel | int | ≥ 0. Low-stock alert when `stockQuantity <= reorderLevel`. Default 10 |
| 8 | lotNumber | String | One lot per medicine ([ADR-006](DECISIONS.md)) |
| 9 | expiryDate | date | |
| 10 | prescriptionRequired | boolean | `true` = Prescription-Only (Rx), `false` = Over-the-Counter (OTC) |
| 11 | sideEffects | String | Side effects / contraindications; may be empty |
| 12 | storageInstructions | String | may be empty |
| 13 | status | `MedicineStatus` | `ACTIVE` / `DISCONTINUED` |

Uniqueness: brandName + genericName + dosageStrength (case-insensitive) must be unique.

```
MED-0001|Biogesic|Paracetamol|ANALGESIC|500mg|5.50|200|20|LOT-2026A|2027-12-31|false|Rare skin rash|Store below 30C|ACTIVE
MED-0002|Amoxil|Amoxicillin|ANTIBIOTIC|500mg|18.00|80|15|AMX-1123|2027-06-30|true|Diarrhea; allergy to penicillin|Keep dry|ACTIVE
```

### 3.3 `stock_audits.txt` — owner: Karlo

`auditId|medicineId|systemQuantity|countedQuantity|difference|reason|auditedBy|auditedAt`

| # | Field | Notes |
|---|---|---|
| 0 | auditId | `AUD-000001` |
| 1 | medicineId | |
| 2 | systemQuantity | stock in the system before the audit |
| 3 | countedQuantity | physical count entered by the admin (becomes the new `stockQuantity`) |
| 4 | difference | `counted − system` (negative = shrinkage) |
| 5 | reason | required when difference ≠ 0 |
| 6 | auditedBy | admin's userId |
| 7 | auditedAt | date-time |

Append-only: audits are never edited or deleted.

### 3.4 `orders.txt` — owner: Cameron

`orderId|pharmacistId|customerName|customerContact|discountType|discountIdNumber|prescriptionId|doctorLicense|notes|status|createdAt|updatedAt|completedAt`

| # | Field | Notes |
|---|---|---|
| 0 | orderId | `ORD-000001` |
| 1 | pharmacistId | userId of the pharmacist who created it |
| 2 | customerName | required |
| 3 | customerContact | optional `09XXXXXXXXX` |
| 4 | discountType | `DiscountType` |
| 5 | discountIdNumber | Senior/PWD ID; required only for `SENIOR_PWD`, else empty |
| 6 | prescriptionId | required if any item is prescription-only, else empty |
| 7 | doctorLicense | 7-digit PRC license; required with `prescriptionId`, else empty |
| 8 | notes | optional |
| 9 | status | `OrderStatus` |
| 10 | createdAt | date-time |
| 11 | updatedAt | date-time |
| 12 | completedAt | date-time; empty until COMPLETED. Analytics count revenue by this date |

Totals are **not stored** — they are computed from the items and the discount strategy (one source of truth).

### 3.5 `order_items.txt` — owner: Cameron

`orderId|medicineId|quantity|unitPriceAtSale`

One line per medicine per order. The same medicine is never listed twice in one order — adding it again increases the quantity.

### 3.6 `void_log.txt` — owner: Cameron

`voidId|orderId|adminId|reason|voidedAt`

`VOID-0001`. Append-only.

## 4. Enums

| Enum | Owner | Constants |
|---|---|---|
| `Role` | common | `ADMIN`, `PHARMACIST` |
| `AccountStatus` | common | `ACTIVE`, `DEACTIVATED` |
| `MedicineCategory` | Karlo | `ANALGESIC`, `ANTIBIOTIC`, `ANTIHISTAMINE`, `ANTIPYRETIC`, `ANTIHYPERTENSIVE`, `ANTIDIABETIC`, `ANTACID`, `COUGH_AND_COLD`, `VITAMINS_SUPPLEMENTS`, `OTHER` |
| `MedicineStatus` | Karlo | `ACTIVE`, `DISCONTINUED` |
| `OrderStatus` | Cameron | `PENDING`, `ON_HOLD`, `COMPLETED`, `CANCELLED`, `VOIDED` |
| `DiscountType` | Cameron | `REGULAR`, `SENIOR_PWD`, `BULK` |

Enums with a display name should have a `getLabel()` (e.g. `COUGH_AND_COLD` → "Cough & Cold") so menus never print raw constant names.

## 5. Named constants

| Constant | Value | Lives in |
|---|---|---|
| Max failed logins | 3 | `AuthService.MAX_LOGIN_ATTEMPTS` |
| Senior/PWD discount | 20% | `SeniorPwdDiscount.RATE` |
| Bulk discount | 10% | `BulkDiscount.RATE` |
| Bulk minimum | 50 units | `BulkDiscount.MIN_UNITS` |
| Near-expiry window | 30 days | `InventoryService.NEAR_EXPIRY_DAYS` |
| Default reorder level | 10 | `Medicine.DEFAULT_REORDER_LEVEL` |
| Max quantity per item | 1000 | `OrderService.MAX_ITEM_QUANTITY` |
