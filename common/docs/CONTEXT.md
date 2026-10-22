# common/ — Build spec

Owner: **Heather**. Signatures are in [ARCHITECTURE §6.1](../../docs/ARCHITECTURE.md#61-provided-by-common-heather--used-by-everyone). This file describes how each class must **behave**.

Build `common/` **first** (Milestone 0–1 in the [README](../../README.md#build-order)). Karlo and Cameron cannot compile anything until it exists.

## Checklist

### `pharmacy` (root)
- [x] `Main` — program entry ([ARCHITECTURE §8](../../docs/ARCHITECTURE.md#8-program-flow)):
  1. `DataStore.getInstance().seedDefaults()`. If this throws `DataAccessException`, print the message and exit with code 1.
  2. Welcome loop with `1 Login`, `2 Register`, `0 Exit`, read with `ConsoleInput.readInt`.
  3. After a successful login: `Session.getInstance().getCurrentUser().createMenu().show()`. When it returns, call `Session.getInstance().logout()` and print `You have been logged out.`
  4. Wrap one loop pass in `try { … } catch (RuntimeException e)` as the last-resort net: print `Unexpected error: …`, log out, continue.

### `pharmacy.common.exception`
- [x] `PharmacyException` — `abstract`, `extends Exception`, constructors `(String message)` and `(String message, Throwable cause)`.
- [x] `ValidationException`, `AuthenticationException`, `AccessDeniedException`, `NotFoundException`, `DuplicateRecordException`, `DataAccessException`. Each is a thin subclass with the same two constructors.

### `pharmacy.common.model`
- [x] `Storable` (interface), `Role`, `AccountStatus` (enums).
- [x] `User` — `abstract`, `implements Storable`.
  - Private fields; a protected constructor with every field.
  - Getters for all fields; setters for everything except `userId` and `role`.
  - `getId()` returns `userId`.
  - `checkPassword(attempt)` is `password.equals(attempt)`.
  - `isActive()` is `status == ACTIVE`.
  - Abstract: `getRole()`, `createMenu()`, `toFileLine()`.
  - **Never** put the password in `toString()`.
- [x] `Admin extends User`:
  - `getRole()` returns `ADMIN`.
  - `createMenu()` returns `new AdminMenu()`.
  - `toFileLine()` writes an empty license column ([DATA §3.1](../../docs/DATA.md#31-userstxt--owner-heather)).
- [x] `Pharmacist extends User`:
  - Adds `licenseNumber`.
  - `getRole()` returns `PHARMACIST`.
  - `createMenu()` returns `new PharmacistMenu()`.

### `pharmacy.common.data`
- [x] `DataFiles` — the six file-name constants. Private constructor.
- [x] `DataStore` — **Singleton** (private constructor, `private static final INSTANCE`, `getInstance()`).
  - `readAll(file)`:
    - Missing file → empty list.
    - Otherwise read UTF-8 lines, skip blank ones, and return `line.split("\\|", -1)` for each.
  - `writeAll(file, records)`: create `data/` if needed, then write `record.toFileLine()` one per line (UTF-8), replacing the file.
  - `append(file, record)`: add one line at the end (for audit and void logs).
  - `nextId(file, prefix, digits)`:
    - Take the highest number found after `prefix` in column 0, add 1, and zero-pad: `String.format("%s%0" + digits + "d", prefix, n)`.
    - An empty file gives `prefix + 0…01`.
  - `seedDefaults()`: create `data/`. If `users.txt` doesn't exist, write the default admin ([ADR-009](../../docs/DECISIONS.md)).
  - Every `IOException` → `DataAccessException("Could not read/save <file>: <reason>", e)`.

### `pharmacy.common.session`
- [x] `Session` — **Singleton**. Holds `currentUser`.
  - `login(user)` sets it.
  - `logout()` sets it to `null`.
  - `requireUser()` throws `AuthenticationException("Please log in first.")` when there is no user.
  - `requireRole(role)` calls `requireUser()`, then throws `AccessDeniedException("This action is for <role> accounts only.")` if the role doesn't match.

### `pharmacy.common.util`
- [x] `InputRule` — functional interface.
- [x] `InputValidator` — every `require*` method in [VALIDATION §2](../../docs/VALIDATION.md#2-input-format-rules--inputvalidator-common). Private constructor. Precompile regexes as `private static final Pattern`.
- [x] `ConsoleInput` — one `private static final Scanner` on `System.in`, never closed. Every method:
  - prints the prompt
  - reads with `nextLine()`, then trims
  - checks the input; on failure prints `  x <reason>` and **asks again**
  - returns the clean value

  `readValidated(prompt, rule)` calls `rule.check(value)` and catches `ValidationException` to re-prompt.

  Handle end of input (`NoSuchElementException` when stdin closes): print `Input closed. Exiting.` and `System.exit(0)`. This is the one allowed exit outside `Main`, so document it in the Javadoc.
- [x] `Format` — `DATE` (`uuuu-MM-dd`, STRICT), `DATE_TIME` (`uuuu-MM-dd HH:mm:ss`, STRICT), `peso(double)` with `Locale.US`, `round2(double)` = `Math.round(x * 100.0) / 100.0`. See [GOTCHAS](../../docs/GOTCHAS.md#dates-and-money).

### `pharmacy.common.ui`
- [x] `Menu` — interface with `show()`.
- [x] `BaseMenu` — `abstract`, `implements Menu`. `show()` is `final`:
  ```
  loop:
    print blank line, "=== " + getTitle() + " ===", options as "1. ..", then "0. " + getExitLabel()
    choice = ConsoleInput.readInt("Choose an option: ", 0, getOptions().length)
    if choice == 0: return
    try { handle(choice) } catch (PharmacyException e) { print "Error: " + e.getMessage() }
  ```
- [x] `PharmacistMenu extends BaseMenu`: title `Pharmacist Menu — <name>`, exit label `Logout`. Options open `MedicineBrowserMenu`, `OrderMenu`, `ProfileMenu` ([menu tree](../../docs/ARCHITECTURE.md#menu-tree)).
- [x] `AdminMenu extends BaseMenu`: title `Admin Menu — <name>`, exit label `Logout`. Options open `CatalogMenu`, `StockAuditMenu`, `UserManagementMenu`, `OrderAdminMenu`, `AnalyticsMenu`, `ProfileMenu`.

## Done when
- [x] Every class above compiles together with the other members' Milestone 0 menu classes.
- [x] Fresh start (no `data/`) creates `data/users.txt` containing the admin line.
- [x] Every `ConsoleInput` method survives the [hostile-input checklist](../../docs/VALIDATION.md#4-hostile-input-checklist) input cases.
- [x] Logging in as `admin` opens the Admin menu, and a pharmacist account opens the Pharmacist menu (polymorphic `createMenu()`).
