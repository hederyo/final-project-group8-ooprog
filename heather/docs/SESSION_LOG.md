# Heather — Session Log

Append-only log of Heather's work sessions, written by Heather or Heather's agent. This is how the next agent session knows where work stopped. Changes that affect other members also go in the shared [docs/SESSION_LOG.md](../../docs/SESSION_LOG.md).

**Rules**
- New entries go at the **bottom**. Never edit or delete old entries.
- Write prose, not a copy of `git log`: what was done, what is unfinished, what to do next, and any decision made. Link commits/PRs.
- If a session ends mid-feature, say exactly where it stopped (file, method, what is missing).

Format:
```
## yyyy-MM-dd — <short title>
**Done:** …
**Not done / next:** …
**Notes:** decisions, problems, links
```

---

## 2026-10-22 — Workspace created
**Done:** Documentation only; no code yet. Features and checklist are in [CONTEXT.md](CONTEXT.md).
**Not done / next:** Milestone 0. See the [README build order](../../README.md#build-order).

## 2026-10-22 — Common foundation and accounts complete (Solo Mode)
**Done:**
- Built entire `common/` foundation: custom checked exception hierarchy under `PharmacyException`, models (`Storable`, `Role`, `AccountStatus`, `User`, `Admin`, `Pharmacist`), `DataFiles`, `DataStore` Singleton with auto-seeding, `Session` Singleton, `InputValidator` with all 17 format rules, `ConsoleInput` robust reader, `Format` strict date/time and peso formatters, and `BaseMenu` abstract menu template.
- Built application entry point `Main` with safety net and polymorphic role menu dispatching.
- Wired `AdminMenu` and `PharmacistMenu` using solo-mode stand-in stubs in `stubs/` for Karlo's and Cameron's menu classes and `OrderService`.
- Built `heather/`: `UserRepository` stateless persistence, `AuthService` with 3-attempt lockout and pharmacist self-registration, `UserService` for profile and admin user CRUD, and interactive menus `AuthMenu`, `ProfileMenu`, and `UserManagementMenu`.
- Successfully verified: deletion of `data/` recreates default admin, 3 wrong passwords trigger return to welcome, profile viewing/updates and password changes work, admin user CRUD enforces all security rules (A11 self-deactivation/deletion guard, last-admin guard, A12 pharmacist order history check).
**Not done / next:** Ready for integration with Karlo and Cameron once their parts are completed.
**Notes:** Stand-in stubs placed in `stubs/` and git-ignored.

