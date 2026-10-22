# AGENTS.md — read this first

This is a Java 17 **console** Pharmacy Management System, a school OOP final project for Group #8. It has Admin and Pharmacist roles, text-file storage, and must demonstrate the four OOP pillars, exception handling, Singleton and Strategy patterns, and thorough input validation. Product intent: [PROJECT-BRIEF.md](PROJECT-BRIEF.md). Overview for humans: [README.md](README.md).

## 1. Who are you working for?

Three students each code their own part with their own agent. Find out which member you are helping, then read their file next:

| Member | Read next | You may edit |
|---|---|---|
| Heather | [heather/AGENTS.md](heather/AGENTS.md) | `heather/**`, `common/**` |
| Karlo | [karlo/AGENTS.md](karlo/AGENTS.md) | `karlo/**` |
| Cameron | [cameron/AGENTS.md](cameron/AGENTS.md) | `cameron/**` |

If you don't know which member you are helping, **ask before editing anything.**

## 2. Where to look

| Question | File |
|---|---|
| Which file goes where? What is each file for? | [docs/BLUEPRINT.md](docs/BLUEPRINT.md) |
| Which layer does what, what can I call from another member? | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (§6 = contracts) |
| What is my member's next step? | `<member>/docs/PROMPTS.md` + `<member>/docs/CONTEXT.md` |
| Git commands | [docs/GIT-GUIDE.md](docs/GIT-GUIDE.md) (the member runs them, not you) |
| File format, columns, IDs, enums, constants | [docs/DATA.md](docs/DATA.md) |
| What exactly should feature X do? | [docs/FEATURES.md](docs/FEATURES.md) |
| How must input X be validated? | [docs/VALIDATION.md](docs/VALIDATION.md) |
| Why is it built this way? May I change it? | [docs/DECISIONS.md](docs/DECISIONS.md) |
| Code style, git workflow, verification | [docs/CONVENTIONS.md](docs/CONVENTIONS.md) |
| Something "should work" but doesn't | [docs/GOTCHAS.md](docs/GOTCHAS.md), then `<member>/docs/GOTCHAS.md` |
| What happened recently? | [docs/SESSION_LOG.md](docs/SESSION_LOG.md) + `<member>/docs/SESSION_LOG.md` (latest entries) |

## 3. Hard rules

1. **Stay in your member's folder.** Never edit another member's folder. Only Heather's agent edits `common/`.
2. **Don't break contracts.** The signatures in [ARCHITECTURE §6](docs/ARCHITECTURE.md#6-cross-member-contracts) are used by other members. Adding is fine. Renaming or removing follows [CONVENTIONS §5](docs/CONVENTIONS.md#5-changing-common-or-a-cross-member-contract).
3. **Don't redesign silently.** If a spec is unclear or seems wrong, stop and ask the member. Don't invent features or change ADR decisions.
4. **Keep the graded requirements intact:** the four OOP pillars, custom exceptions, `DataStore`/`Session` Singletons, the `DiscountStrategy` Strategy, and every rule in VALIDATION.md.
5. **Standard Java 17 only**: no libraries, no build tools, no `record` models ([ADR-013](docs/DECISIONS.md)).
6. **Layers:** Menu → Service → Repository → DataStore. Menus don't touch files; services don't print.
7. **Teach, don't just write.** The member must explain every line in the defense. Prefer simple, readable code and explain the non-obvious parts.

## 4. Verifying your work

There are no automated tests. Before saying something is done:
1. Compile **all four** folders together ([README → Run it](README.md#run-it)).
2. Run `pharmacy.Main` from the repo root and walk through the feature with the right role.
3. Run the [hostile-input checklist](docs/VALIDATION.md#4-hostile-input-checklist) on every screen you touched.
4. Check that the `data/*.txt` lines match [DATA.md](docs/DATA.md).

Report honestly what you did **not** verify.

## 5. Keeping these docs true

The code wins: if a doc disagrees with the code, fix the doc in the same change.

| You changed… | Update |
|---|---|
| Added, renamed or removed a file | `docs/BLUEPRINT.md` |
| A class, package, or cross-member method | `docs/ARCHITECTURE.md` + shared `docs/SESSION_LOG.md` |
| A data file column, ID format, enum, or constant | `docs/DATA.md` + shared `docs/SESSION_LOG.md` |
| Feature behaviour or a menu option | `docs/FEATURES.md` |
| A validation rule or error message | `docs/VALIDATION.md` |
| A design decision | Append an ADR to `docs/DECISIONS.md` (never renumber) |
| Anything, at the end of a session | `<member>/docs/SESSION_LOG.md` (append) + tick `<member>/docs/CONTEXT.md` |
| You hit a confusing bug | `<member>/docs/GOTCHAS.md` (or the shared one if it can hit anyone) |
