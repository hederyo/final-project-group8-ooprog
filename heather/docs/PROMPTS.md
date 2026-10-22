# Heather — Prompts & Git steps

Copy-paste prompts for your AI agent, in order, with the git commands for each step. You do the git commands yourself; the agent writes code only in `common/` and `heather/`. Background: [GIT-GUIDE](../../docs/GIT-GUIDE.md).

## How to use this file
1. Open your agent **at the repository root** (Claude Code: run `claude` inside `pharmacy-management-system/`; it loads `AGENTS.md` automatically).
2. For each step: run **Git: start** → paste **[Start of session](#start-of-session)** → paste the **step prompt** → review and test → paste **[End of session](#end-of-session)** → run **Git: finish**.
3. A step can take several sessions. Just start each new session with the *Start of session* prompt again.
4. Read what the agent writes. Ask it "explain this line" whenever you don't understand. You will defend this code.

## Your order and who waits for whom

| Step | Branch | Wait for | Others waiting for you |
|---|---|---|---|
| 1 | `heather/common-foundation` | — | **Karlo and Cameron cannot start until this is merged** |
| 2 | `heather/wire-menus` | Karlo's and Cameron's `menu-skeleton` merged | — |
| 3 | `heather/register` | — | — |
| 4 | `heather/profile` | — | — |
| 5 | `heather/manage-users` | Cameron's `order-data` merged (for delete) | — |

---

## Working rules
*(The step prompts tell the agent to follow this section.)*

1. **Plan first.** List the files and methods you will create or change, then wait for my OK.
2. Edit only `common/**` and `heather/**`. Shared `docs/` only if the change makes them wrong. Never touch `karlo/**` or `cameron/**`.
3. Follow docs/ARCHITECTURE.md (layers, §6 contracts), docs/CONVENTIONS.md, docs/VALIDATION.md, and docs/DATA.md exactly. If the spec is unclear or seems wrong, **ask me**; don't invent.
4. Write clear, simple Java 17 with no libraries. After each class, explain in 2–3 plain sentences what it does and which OOP pillar or pattern it shows.
5. Compile all four folders: `javac -d out $(find common heather karlo cameron -name "*.java")`. Fix every error.
6. Test by running `java -cp out pharmacy.Main`. You may script input, e.g. `printf '1\nadmin\nAdmin123\n0\n0\n' | java -cp out pharmacy.Main`. Then give me the manual test steps, including the hostile inputs from docs/VALIDATION.md §4.
7. **No git commands that change anything** (add, commit, push, merge, reset, switch). Read-only ones (`git status`, `git diff`, `git log`) are fine. I commit and push myself.

## Start of session
```text
I'm Heather (Group #8). Read AGENTS.md, heather/AGENTS.md, heather/docs/CONTEXT.md,
common/docs/CONTEXT.md, heather/docs/GOTCHAS.md, heather/docs/PROMPTS.md (Working rules),
and the last 3 entries of heather/docs/SESSION_LOG.md and docs/SESSION_LOG.md.
Then tell me in at most 6 lines: what is done, what is next, and anything blocking.
Don't write code yet.
```

## End of session
```text
Before we stop:
1. Tick finished items in heather/docs/CONTEXT.md (checklists and Progress table).
2. Append an entry to heather/docs/SESSION_LOG.md (Done / Not done-next / Notes).
3. Add anything surprising to heather/docs/GOTCHAS.md.
4. If you changed common/, a contract, or a data format: update docs/ARCHITECTURE.md /
   docs/DATA.md / docs/BLUEPRINT.md and append to docs/SESSION_LOG.md.
5. Give me: the list of changed files, a commit message "type(area): summary",
   and a PR description filled in from .github/pull_request_template.md.
Don't run any git commands.
```

---

## Step 1 — Shared foundation + login (M0a)

**Git: start**
```bash
git switch main
git pull
git switch -c heather/common-foundation
```

**Prompt 1A — models, exceptions, data, session**
```text
Step 1A, branch heather/common-foundation. Follow the Working rules in heather/docs/PROMPTS.md.
Build these parts of common/ exactly as in common/docs/CONTEXT.md and docs/BLUEPRINT.md §2:
- pharmacy.common.exception: PharmacyException + the 6 subclasses
- pharmacy.common.model: Storable, Role, AccountStatus, User, Admin, Pharmacist
  (createMenu() may only compile after Step 1C — if so, build User/Admin/Pharmacist in 1C)
- pharmacy.common.data: DataFiles, DataStore (Singleton, seedDefaults creates the default admin)
- pharmacy.common.session: Session (Singleton)
Plan first and wait for my OK.
```

**Prompt 1B — input and formatting utilities**
```text
Step 1B, same branch. Follow the Working rules in heather/docs/PROMPTS.md.
Build pharmacy.common.util: InputRule, InputValidator (every require* method in
docs/VALIDATION.md §2), ConsoleInput (one Scanner, every read* re-prompts until valid,
handles end of input), Format (STRICT uuuu-MM-dd, Locale.US peso, round2).
Read docs/GOTCHAS.md "Console input" and "Dates and money" first.
After building, show me how each hostile input in docs/VALIDATION.md §4 is handled by ConsoleInput.
```

**Prompt 1C — menus, Main, and login/logout**
```text
Step 1C, same branch. Follow the Working rules in heather/docs/PROMPTS.md.
Build:
- pharmacy.common.ui: Menu, BaseMenu, AdminMenu, PharmacistMenu. For now AdminMenu shows
  only "My profile" and "Manage users"; PharmacistMenu shows only "My profile".
  Karlo's and Cameron's menus get wired in Step 2.
- pharmacy.Main (welcome loop, safety net, logout after the role menu returns)
- heather/: UserRepository (read side + findByLogin), AuthService.login (rules A1-A4),
  AuthMenu.login() with the 3-attempt limit, AuthMenu.register() as an empty method
  (it is built in Step 3), ProfileMenu with only "View my profile",
  UserManagementMenu with no options yet.
Done when: a fresh start creates data/users.txt; I can log in as admin / Admin123, see the
Admin menu, view my profile, and log out; wrong password 3 times returns to the welcome menu.
```

**Git: finish** (commit after each part if you like; open the PR once 1C works)
```bash
git status
git add common/ heather/ docs/
git commit -m "feat(common): add shared foundation and login"
git fetch origin
git merge origin/main
git push -u origin heather/common-foundation
gh pr create --base main --title "feat(common): shared foundation and login (M0a)"
```
Then tell Karlo and Cameron: "common is merged, pull main and start your menu-skeleton".

---

## Step 2 — Wire everyone's menus (M0c)

**Git: start** (after Karlo's and Cameron's `menu-skeleton` PRs are merged)
```bash
git switch main
git pull
git switch -c heather/wire-menus
```

**Prompt**
```text
Step 2, branch heather/wire-menus. Follow the Working rules in heather/docs/PROMPTS.md.
Karlo's and Cameron's menu classes now exist. Update AdminMenu and PharmacistMenu so their
options match the menu tree in docs/ARCHITECTURE.md §8 exactly, opening:
CatalogMenu, StockAuditMenu, UserManagementMenu, OrderAdminMenu, AnalyticsMenu, ProfileMenu (admin);
MedicineBrowserMenu, OrderMenu, ProfileMenu (pharmacist).
Done when every option opens its menu and "0" returns.
```

**Git: finish**
```bash
git add common/
git commit -m "feat(common): wire all member menus into role menus"
git fetch origin
git merge origin/main
git push -u origin heather/wire-menus
gh pr create --base main --title "feat(common): wire all menus (M0c)"
```

---

## Step 3 — Register (A1)

**Git: start**
```bash
git switch main
git pull
git switch -c heather/register
```

**Prompt**
```text
Step 3, branch heather/register. Follow the Working rules in heather/docs/PROMPTS.md.
Implement feature A1 Register from docs/FEATURES.md with rules A5, A6, A7 from
docs/VALIDATION.md §3.1:
- UserRepository: usernameTaken/emailTaken/licenseTaken (case-insensitive, excludeUserId),
  save, nextId
- AuthService.register(...) re-validates every field and creates an ACTIVE Pharmacist
- AuthMenu.register(): each field read with ConsoleInput.readValidated + InputValidator,
  password + confirm password
Test duplicates with different capitalization, and every hostile input on every field.
```

**Git: finish**
```bash
git add heather/
git commit -m "feat(auth): add pharmacist registration"
git fetch origin
git merge origin/main
git push -u origin heather/register
gh pr create --base main --title "feat(auth): register account (A1)"
```

---

## Step 4 — Manage Profile (A3)

**Git: start**
```bash
git switch main
git pull
git switch -c heather/profile
```

**Prompt**
```text
Step 4, branch heather/profile. Follow the Working rules in heather/docs/PROMPTS.md.
Implement A3 Manage Profile from docs/FEATURES.md with rules A5, A8, A9:
- UserService.updateMyProfile(fullName, email, contact) — blank keeps the current value,
  email uniqueness excludes my own record
- UserService.changeMyPassword(current, new, confirm)
- ProfileMenu: View · Update details · Change password
The user always comes from Session, never from a typed ID. Never print the password.
```

**Git: finish**
```bash
git add heather/
git commit -m "feat(auth): add profile view, update and password change"
git fetch origin
git merge origin/main
git push -u origin heather/profile
gh pr create --base main --title "feat(auth): manage profile (A3)"
```

---

## Step 5 — Manage Users, Admin (A4)

**Git: start** (after Cameron's `order-data` is merged)
```bash
git switch main
git pull
git switch -c heather/manage-users
```

**Prompt**
```text
Step 5, branch heather/manage-users. Follow the Working rules in heather/docs/PROMPTS.md.
Implement A4 Manage Users from docs/FEATURES.md with rules A5, A10-A13:
- UserService admin methods (each starts with Session.requireRole(Role.ADMIN)):
  listUsers, searchUsers(keyword, role, status), createUser(role, ...), updateUser,
  setStatus (cannot deactivate self or the last active admin), deleteUser (only pharmacists
  with no orders — use Cameron's OrderService.hasOrdersForPharmacist; admins can only be
  deactivated)
- UserManagementMenu: List · Search/filter · View · Create · Update · Deactivate/Reactivate · Delete
Confirm before deactivate/delete. Show an empty-state message when no users match.
```

**Git: finish**
```bash
git add heather/
git commit -m "feat(auth): add admin user management"
git fetch origin
git merge origin/main
git push -u origin heather/manage-users
gh pr create --base main --title "feat(auth): manage users (A4)"
```

---

## Any time

**Fix a bug**
```text
Follow the Working rules in heather/docs/PROMPTS.md. Bug: <what I did> → <what happened>
→ <what should happen>. Find the root cause before changing anything, explain it to me,
then fix it in the shared place (not a workaround in one menu). If it's a trap others
could hit, add it to the right GOTCHAS.md.
```

**Prepare for the defense**
```text
Quiz me on my code. For each of: encapsulation, abstraction, inheritance, polymorphism,
exception handling, Singleton, and validation — show me the exact file and lines in common/
or heather/, explain it simply, then ask me one question about it and correct my answer.
Use docs/ARCHITECTURE.md §10 as the list.
```

**Review my branch before the PR**
```text
Review everything changed on this branch compared to main (git diff main --stat, then read
the files). Check against: docs/VALIDATION.md rules for my features, docs/CONVENTIONS.md
§2-3, the contracts in docs/ARCHITECTURE.md §6, and files only in common/ or heather/.
List problems by severity. Don't fix anything until I say so.
```
