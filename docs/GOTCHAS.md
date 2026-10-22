# Gotchas (shared)

These are problems that look like bugs, or bugs that look like they should work. Each one gives the symptom, the cause, and the fix. Area-specific gotchas live in each member's `docs/GOTCHAS.md`. This file is for problems that can hit **anyone**.

> The entries below are well-known Java pitfalls for this kind of project, written down **before** coding started. When you actually hit one, add `Confirmed:` and the date. If one turns out not to apply, delete it.

Format:
```
### Short title
**Symptom:** what you see
**Cause:** what is really happening
**Fix:** what to do
```

---

## Console input

### Input "skips" a prompt after reading a number
**Symptom:** After `Enter quantity:`, the next `Enter name:` is skipped.
**Cause:** `scanner.nextInt()` leaves the newline in the buffer, and the next `nextLine()` reads it as an empty line.
**Fix:** Never call `nextInt`/`nextDouble`. `ConsoleInput` reads every value with `nextLine()` and parses it. Always use `ConsoleInput` and never create your own `Scanner`.

### Program stops reading input after one screen
**Symptom:** `NoSuchElementException` on the next read.
**Cause:** Someone called `scanner.close()` or wrapped a `Scanner(System.in)` in try-with-resources. That closes `System.in` for the whole program.
**Fix:** One `Scanner` lives in `ConsoleInput` and is never closed.

### `Double.parseDouble` accepts weird input
**Symptom:** A price of `NaN`, `Infinity`, or `1e3` is accepted.
**Cause:** Java parses those as valid doubles.
**Fix:** `ConsoleInput.readDouble` rejects any value where `Double.isNaN` or `Double.isInfinite` is true, and only accepts the pattern `^\d+(\.\d{1,2})?$`.

### Huge numbers crash the menu
**Symptom:** Typing `99999999999` throws an exception.
**Cause:** It doesn't fit in an `int`, so `Integer.parseInt` throws `NumberFormatException`.
**Fix:** `ConsoleInput.readInt` catches it and re-prompts. Don't parse numbers anywhere else.

## Files

### Every character ends up in its own column
**Symptom:** `line.split("|")` returns single letters.
**Cause:** `split` takes a regex, and `|` means "or" in regex.
**Fix:** `line.split("\\|", -1)`. The `-1` keeps empty trailing fields such as an admin's empty license number.

### A record disappeared after saving
**Symptom:** A user or medicine vanished from the file.
**Cause:** A repository skipped a line it could not parse, then `writeAll` saved the list without it.
**Fix:** Never skip bad lines. Throw `DataAccessException` with the file name and line number ([DATA §1](DATA.md#1-storage-rules)).

### `data/` appears in a strange folder, or the program "forgets" everything
**Symptom:** Data from the last run is missing, or `data/` shows up inside `out/` or an IDE folder.
**Cause:** `data/` is relative to the **working directory**, and IDEs often run from a different folder.
**Fix:** Always run from the repository root. In the IDE, set the run configuration's working directory to the repo root.

## Dates and money

### `2026-02-30` is accepted as a date
**Symptom:** An impossible date is saved as `2026-02-28`.
**Cause:** `DateTimeFormatter.ofPattern("yyyy-MM-dd")` uses SMART resolving, which quietly "fixes" the date.
**Fix:** `Format.DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)`. Use `uuuu`, not `yyyy`: with STRICT, `yyyy` needs an era and rejects every date.

### Totals are off by a centavo, or `==` on money fails
**Symptom:** `0.1 + 0.2 != 0.3`, or totals like `PHP 123.4500000001`.
**Cause:** `double` is binary floating point.
**Fix:** Pass every computed amount through `Format.round2`. Never compare money with `==` ([ADR-015](DECISIONS.md)).

### Prices print with a comma as the decimal point
**Symptom:** `PHP 1.250,50` on some laptops.
**Cause:** `String.format` uses the computer's locale.
**Fix:** `Format.peso` uses `String.format(Locale.US, "PHP %,.2f", amount)`.

### The peso sign prints as `?`
**Symptom:** `₱` shows as `?` or garbage in the Windows console.
**Cause:** The console's encoding isn't UTF-8.
**Fix:** Print `PHP` instead of `₱` ([DATA §2](DATA.md#2-value-formats)).

## Building and collaboration

### My folder compiles alone but the program won't build
**Symptom:** `cannot find symbol: class CatalogMenu` when compiling `common/`.
**Cause:** `AdminMenu`/`PharmacistMenu` in `common/` reference every member's entry menu, so the program only compiles when all of them exist.
**Fix:** Always compile all four folders together. Milestone 0 in the [README](../README.md#build-order) has every member push their menu class names first.

### Strings that look equal aren't
**Symptom:** `if (username == input)` is never true.
**Cause:** `==` compares object references, not text.
**Fix:** Use `equals`, or `equalsIgnoreCase` for usernames, emails, and searches.

### `ConcurrentModificationException` when deleting
**Symptom:** The program crashes when removing an item inside a `for-each` loop.
**Cause:** You can't change a list while iterating it that way.
**Fix:** Use `list.removeIf(x -> x.getId().equals(id))`.

### Someone wrote `new DataStore()`
**Symptom:** It won't compile, or two different data views exist.
**Cause:** The Singleton constructor is private.
**Fix:** Always call `DataStore.getInstance()` and `Session.getInstance()`.
