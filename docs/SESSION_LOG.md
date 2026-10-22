# Session Log (shared)

A dated, **append-only** log of changes that affect **more than one member**: changes to `common/`, contracts, data formats, ADRs, and integration milestones. Each member's own day-to-day work goes in their `<name>/docs/SESSION_LOG.md`.

**Rules**
- Add new entries at the **bottom**. Never rewrite or delete old ones.
- Write in prose: what changed, why, and who is affected. Link the PR, commit, or ADR instead of repeating its contents. This is not a copy of `git log`.
- One entry per change or work session. Keep entries short.

Format:
```
## yyyy-MM-dd — <member> — <short title>
What changed and why. Who needs to do something about it. Links.
```

---

## 2026-10-22 — Setup — Project documentation created
The repository was created with documentation only, and no Java code yet. Contents:
- [README](../README.md), [AGENTS.md](../AGENTS.md), [PROJECT-BRIEF](../PROJECT-BRIEF.md)
- The shared `docs/` folder: architecture and contracts, data formats, features, validation, decisions, conventions, gotchas
- One folder per member (`heather/`, `karlo/`, `cameron/`), plus `common/`, each with agent context docs

Scope decisions ADR-001 to ADR-015 are recorded in [DECISIONS.md](DECISIONS.md), including:
- no branches
- one lot per medicine
- pharmacists see only their own orders
- seeded `admin` account
- deactivate instead of delete

Next: Milestone 0 in the [README](../README.md#build-order).

## 2026-10-22 — Setup — Blueprint, git guide, member prompts
Added three things:
- [BLUEPRINT.md](BLUEPRINT.md): every file, with its owner and milestone. It replaces the folder trees that used to be in ARCHITECTURE §2 and each CONTEXT.md.
- [GIT-GUIDE.md](GIT-GUIDE.md) and a PR template in `.github/`.
- `<member>/docs/PROMPTS.md`: step-by-step agent prompts with the git commands for each step.

Two new files are planned: `karlo/.../ui/MedicineTable.java` and `cameron/.../model/SalesReport.java`. The merge order across members is in GIT-GUIDE §9.

## 2026-10-22 — Heather — Common foundation and accounts completed
Heather has built the full `common/` foundation and `heather/` accounts module:
- Built `common/`: all exceptions, `Storable`, `Role`, `AccountStatus`, `User`, `Admin`, `Pharmacist`, `DataStore` (auto-seeding `U-0001` default admin), `Session`, `InputValidator`, `ConsoleInput`, `Format`, `BaseMenu`, `AdminMenu`, `PharmacistMenu`, and `Main`.
- Built `heather/`: `UserRepository`, `AuthService`, `UserService`, `AuthMenu`, `ProfileMenu`, `UserManagementMenu`.
- All contracts in ARCHITECTURE §6.1 and §6.2 are implemented and stable. Karlo and Cameron can safely pull `common/` and begin their Milestone 0 and 1 implementations.

