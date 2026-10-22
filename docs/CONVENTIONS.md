# Conventions

How to write code that fits the rest of this project, and how the three of us work together on GitHub.

> No code exists yet, so these are agreed rules rather than observed patterns. Once real code exists, update this file to match it ("code wins").

## 1. Java style

- **Java 17+, standard library only** ([ADR-013](DECISIONS.md)).
- One public class per file. The file path matches the package (`karlo/src/pharmacy/inventory/Medicine.java` is wrong; `.../inventory/model/Medicine.java` + `package pharmacy.inventory.model;` is right).
- Naming:
  - `PascalCase` for classes
  - `camelCase` for methods and fields
  - `UPPER_SNAKE` for constants
  - Descriptive names (`reserveStock`, not `rs`)
- 4-space indentation, braces on the same line, always use braces, even for one-line `if`s.
- **No magic numbers.** Every rate, limit and threshold is a named constant ([DATA §5](DATA.md#5-named-constants)).
- Comments explain *why*, not *what*. Javadoc on every public method in a cross-member contract.
- No `System.exit` anywhere except `Main`. No `printStackTrace()` shown to users.
- Use `LocalDate.now()` / `LocalDateTime.now()` only in services, never in models, so models stay testable.

## 2. OOP rules (graded — don't break these)

- **Every field is `private`.** Expose it with getters. Add setters only where a value may legally change, and have the setter guard invariants:
  ```java
  public void setStockQuantity(int stockQuantity) {
      if (stockQuantity < 0) {
          throw new IllegalArgumentException("Stock cannot be negative");  // programming-bug guard
      }
      this.stockQuantity = stockQuantity;
  }
  ```
  User-facing validation happens in the service with `ValidationException`. The setter guard is only a safety net.
- Don't give `id` or `role` fields a setter.
- Return collections as `Collections.unmodifiableList(...)` or as a copy, never the internal list.
- Models are classes, not `record`s ([ADR-013](DECISIONS.md)).
- Every menu `extends BaseMenu`. Every custom exception `extends PharmacyException`. Every stored model `implements Storable`.
- Prefer overriding a method over `if (x instanceof Y)` or `switch` on a type. Use polymorphism.

## 3. Layers and error handling

- Follow [ARCHITECTURE §3](ARCHITECTURE.md#3-layers): Menu → Service → Repository → DataStore. Menus never open files. Services never print.
- Services throw `PharmacyException` subclasses with a message the user can act on. `BaseMenu` catches them.
- Never write an empty `catch` block. Never use `catch (Exception e)` in services.
- Wrap `IOException` only in `DataStore`, as `DataAccessException`.
- Every destructive action (delete, cancel, void, deactivate, discontinue) asks `ConsoleInput.readYesNo("Are you sure? (y/n): ")` first.
- Print an empty-state line for every list (`No orders found.`). Never print a blank table.

## 4. Git workflow

The exact commands are in [GIT-GUIDE.md](GIT-GUIDE.md). These are the rules:

| Rule | Detail |
|---|---|
| `main` | Always compiles and runs. Never commit directly to `main`. |
| Branches | `<member>/<feature>`, e.g. `heather/login`, `karlo/catalog-crud`, `cameron/discounts`. |
| Commits | Small, one idea each. Format: `type(area): summary`, e.g. `feat(inventory): add medicine search`, `fix(orders): restore stock on cancel`, `docs: update DATA for orders`. Types: `feat`, `fix`, `refactor`, `docs`, `chore`. |
| Before you start | `git switch main && git pull`, then create your branch. |
| Before you open a PR | Merge the latest `main` into your branch, compile everything, and run the [hostile-input checklist](VALIDATION.md#4-hostile-input-checklist) for your screens. |
| Pull requests | One feature per PR. At least **one groupmate reviews** before merging. Describe what changed, how you tested it, and any doc updates. |
| Ownership | Edit only your own folder (and your own `docs/`). Need a change elsewhere? Ask the owner (see §5). |
| Never commit | `data/`, `out/`, `*.class`, IDE folders. `.gitignore` handles this. Don't force-add them. |
| Docs | If your change makes a doc wrong, fix the doc **in the same PR**. |

## 5. Changing `common/` or a cross-member contract

`common/` and the signatures in [ARCHITECTURE §6](ARCHITECTURE.md#6-cross-member-contracts) are used by everyone, so one careless change breaks three people's code.

1. Post in the group chat what you need and why, before coding it.
2. The owner (Heather for `common/`, or the member who provides the contract) makes the change in its own small PR: `refactor(common): ...`.
3. Update [ARCHITECTURE.md](ARCHITECTURE.md) and [DATA.md](DATA.md) in the same PR.
4. Add a line to [docs/SESSION_LOG.md](SESSION_LOG.md) so everyone's agent sees it.
5. Every member pulls `main` and recompiles before continuing.

**Adding** a method is safe. **Renaming or removing** one breaks callers: search all four folders for usages first.

## 6. Verifying your work

There is no automated test framework ([ADR-013](DECISIONS.md)). Verification is done by hand:

1. **Compile everything** (not just your folder). The build commands are in the [README](../README.md#run-it). Zero errors, zero warnings you introduced.
2. **Run the program** and walk through each feature you touched, using both roles.
3. Run the **[hostile-input checklist](VALIDATION.md#4-hostile-input-checklist)** on every screen you touched.
4. Open the `data/*.txt` files and check that the lines match [DATA.md](DATA.md).
5. Delete `data/` and run again. A fresh start must work.

## 7. Working with AI agents

- Every agent starts by reading the root [AGENTS.md](../AGENTS.md), then the member's own `AGENTS.md`.
- The agent writes code **only** in its member's folder. It asks before touching anything else.
- Before a session ends, the agent appends to the member's `docs/SESSION_LOG.md` and, if it hit a surprising problem, adds it to `docs/GOTCHAS.md`.
- Agents must not invent new features, rename contracts, or change ADR decisions on their own. They raise the question with the member instead.
- The member must understand every line the agent writes. You will be asked to explain your code in the defense.
