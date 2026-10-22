# Decisions (ADRs)

Each decision is recorded with the reasoning behind it, including choices that look wrong until you know why they were made.

**Rules**
- Append new ADRs at the bottom with the next number.
- Never renumber or delete an ADR. To change a decision, add a new ADR and mark the old one `Superseded by ADR-0XX`.
- Before changing anything an ADR covers, ask the group first.

Format:
```
## ADR-0XX — Title
Status: Accepted | Superseded by ADR-0YY · Date: yyyy-MM-dd
Context: why a decision was needed
Decision: what we chose
Consequences: what this means for the code
```

---

## ADR-001 — Console application
Status: Accepted · Date: 2026-10-22
Context: The project is graded on OOP, patterns, exceptions and validation. The interface is not graded.
Decision: Text console only (`System.in` / `System.out`), menu-driven, numbered options.
Consequences: All input goes through `ConsoleInput`. No Swing, JavaFX, or web code.

## ADR-002 — Pipe-delimited text files, stateless repositories
Status: Accepted · Date: 2026-10-22
Context: The group chose file storage. Data is small, a few hundred lines at most.
Decision: One `.txt` file per entity in `data/`, with `|` as the delimiter, no header row. Repositories re-read the file on every call and rewrite the whole file after every change. `DataStore` is the only class that opens files.
Consequences: There is no cache, so data can never be stale between members' features. `|` is banned in all text input. A corrupted line throws instead of being skipped. Format details are in [DATA.md](DATA.md).

## ADR-003 — Passwords stored as plaintext
Status: Accepted · Date: 2026-10-22
Context: The group chose plaintext to keep the project simple. Hashing is not a course requirement.
Decision: `users.txt` stores the password as typed. `User.checkPassword` uses `equals`.
Consequences: This is a **school-project shortcut only**: never do this in real software. Passwords are still never printed on screen (not in profile views or user lists).

## ADR-004 — Code inside member folders, plus `common/`
Status: Accepted · Date: 2026-10-22
Context: The group wants clear ownership for GitHub collaboration: one folder per member.
Decision: `heather/src`, `karlo/src`, and `cameron/src` each hold that member's packages. The shared foundation (base classes, singletons, exceptions, input helpers, `Main`) lives in `common/src`, owned by Heather. All four folders compile together into one program.
Consequences: Members edit only their own folder. Changes to `common/` or to any contract in [ARCHITECTURE §6](ARCHITECTURE.md#6-cross-member-contracts) follow [CONVENTIONS §5](CONVENTIONS.md#5-changing-common-or-a-cross-member-contract). IDEs need four source roots ([README](../README.md#run-it)).

## ADR-005 — No multi-branch support
Status: Accepted · Date: 2026-10-22
Context: The proposal mentions "branch-level access control" once, but no other feature uses branches.
Decision: One pharmacy, one set of data. No `Branch` entity.
Consequences: Admins see all records. The branch sentence is dropped from the scope ([PROJECT-BRIEF](../PROJECT-BRIEF.md#scope)).

## ADR-006 — One lot number and one expiry date per medicine
Status: Accepted · Date: 2026-10-22
Context: Real pharmacies track several batches per medicine, but that needs a separate batch entity and first-expiry-first-out selling.
Decision: Each medicine record has a single `lotNumber` and `expiryDate`. A new delivery with a different expiry date is added as a separate medicine record, or the admin updates the lot and expiry during an audit.
Consequences: Stock, expiry, and low-stock logic stay per medicine.

## ADR-007 — Pharmacists see and manage only their own orders
Status: Accepted · Date: 2026-10-22
Context: The proposal says "only transactions completed during their active shift or account session". Tying it to a login session would wipe the history from view at every logout.
Decision: A pharmacist sees every order they created (`pharmacistId` = current user), across all sessions. The pharmacist who owns an order completes it, puts it on hold, or cancels it.
Consequences: Every pharmacist order action checks ownership (O8). Admins see all orders.

## ADR-008 — Single login screen; self-registration creates active pharmacists
Status: Accepted · Date: 2026-10-22
Context: The proposal lists "Login" and "Admin Login" separately and does not say whether registrations need approval.
Decision: One login screen for both roles. The account's stored role decides which menu opens (`user.createMenu()`). Self-registration always creates a `PHARMACIST` with status `ACTIVE`. There is no approval step. Admins can deactivate an account at any time.
Consequences: No role selection at registration. Admin accounts can only be created by another admin, or seeded (ADR-009).

## ADR-009 — Seeded default admin
Status: Accepted · Date: 2026-10-22
Context: Admins cannot self-register, so a fresh install would have no way in.
Decision: When `users.txt` is missing, `DataStore.seedDefaults()` creates `U-0001`, username `admin`, password `Admin123`.
Consequences: This is documented in the README for the demo. Change the password after the first login.

## ADR-010 — Discount rules
Status: Accepted · Date: 2026-10-22
Context: The proposal names three discounts but no eligibility rules.
Decision: One discount per order.
- Regular: 0%.
- Senior Citizen / PWD: 20% of the subtotal, and requires an ID number.
- Bulk Wholesale: 10% of the subtotal, only for 50 or more total units.

These are implemented with the Strategy pattern ([ARCHITECTURE §5.2](ARCHITECTURE.md#52-strategy--discounts)).
Consequences: Bulk eligibility is re-checked whenever quantities change (O7). The rates are named constants in the strategy classes. Senior/PWD VAT exemption is out of scope.

## ADR-011 — Reserve stock when ordering; audits record system vs. counted
Status: Accepted · Date: 2026-10-22
Context: Cancel and void must "restore reserved quantities". Analytics must show "inventory shrinkage".
Decision: Stock is subtracted when an item is added to an order. It is returned on removal, cancel, or void. Every physical count writes a `stock_audits.txt` row with the system quantity, the counted quantity, and the difference.
Consequences: Shrinkage is the sum of negative differences. Writes that touch two files follow [ARCHITECTURE §9.3](ARCHITECTURE.md#93-writing-safely-across-two-files).

## ADR-012 — Deactivate or discontinue instead of delete when history exists
Status: Accepted · Date: 2026-10-22
Context: Deleting a user or medicine that appears in past orders would break order history and prescription audits.
Decision:
- **Users:** a pharmacist with no orders can be deleted. Pharmacists with orders, and all admins, can only be deactivated.
- **Medicines:** a medicine with no order history can be deleted. Otherwise it can only be discontinued.
- **Orders:** orders are never erased. Cancel and void are status changes.

Consequences: Delete still exists, which satisfies the CRUD requirement. The "D" for orders is a soft delete.

## ADR-013 — Java 17+, no libraries, no build tool
Status: Accepted · Date: 2026-10-22
Context: Every member must be able to compile on their own laptop and on the lab PCs.
Decision: Standard Java 17 or newer, `javac` + `java` only, with no Maven, Gradle, or external JARs.
Consequences: Use only Java 17 language features. Models are classes, not `record`s, because they need private fields with guarded setters to show encapsulation.

## ADR-014 — Checked custom exceptions, caught in `BaseMenu`
Status: Accepted · Date: 2026-10-22
Context: Exception handling is graded and must be visible.
Decision: `PharmacyException extends Exception` (checked), with one subclass per kind of failure ([ARCHITECTURE §7](ARCHITECTURE.md#7-exception-handling)). Services throw. `BaseMenu.show()` catches and prints.
Consequences: Method signatures list their `throws`. `catch (Exception e)` is not allowed in services.

## ADR-015 — Money as `double`, rounded to 2 decimals
Status: Accepted · Date: 2026-10-22
Context: `BigDecimal` is exact but verbose. Amounts here are small.
Decision: Use `double`, and pass every computed amount through `Format.round2`. Display with `Format.peso`.
Consequences: Never compare money with `==`. Compare rounded values, or use `Math.abs(a - b) < 0.005`.
