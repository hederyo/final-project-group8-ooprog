# Pharmacy Management System

A Java **console** application for a community or hospital pharmacy. It handles medicine dispensing, inventory and expiry tracking, prescription checks, automatic Senior/PWD and Bulk discounts, and sales analytics, with separate **Admin** and **Pharmacist** roles.

**Group #8 · OOPROG Final Project · Section C2B**
Valenzuela, Kerth Heather · Silang, Karlo · Roxas, Cameron

> **Status:** documentation and design complete. Coding has not started; see [Build order](#build-order).
> **AI agents:** read [AGENTS.md](AGENTS.md) first.

---

## Contents
- [Course requirements](#course-requirements)
- [Roles](#roles)
- [Features and owners](#features-and-owners)
- [How the code is organized](#how-the-code-is-organized)
- [Run it](#run-it)
- [Build order](#build-order)
- [Working together on GitHub](#working-together-on-github)
- [Documentation map](#documentation-map)

## Course requirements

| Requirement | How this project meets it |
|---|---|
| Login, CRUD, different roles | Login for Admin and Pharmacist. Full CRUD on users, medicines, and orders. |
| Encapsulation | Private fields with guarded setters on every model |
| Abstraction | Abstract `User` and `BaseMenu`; the `Menu`, `Storable`, and `DiscountStrategy` interfaces |
| Inheritance | `Admin`/`Pharmacist extends User`; every menu `extends BaseMenu`; the exception hierarchy |
| Polymorphism | `user.createMenu()`, `discount.computeDiscount(...)`, overridden `toFileLine()` and `handle()` |
| Exception handling | Custom checked exceptions under `PharmacyException`, caught centrally in `BaseMenu` |
| **Singleton** | `DataStore` (file access) and `Session` (logged-in user) |
| **Strategy** | `RegularDiscount`, `SeniorPwdDiscount` (20%), `BulkDiscount` (10% at 50+ units) |
| All types of validation | Every rule is listed in [docs/VALIDATION.md](docs/VALIDATION.md) |

The group's fixed choices: console UI, text-file storage, plaintext passwords (a school-project shortcut, [ADR-003](docs/DECISIONS.md)).

## Roles

| Role | Can do |
|---|---|
| **Pharmacist** (Regular User) | Register · Log in/out · Manage own profile · Browse, search, and view medicines · Create dispensing orders with discounts and prescriptions · View, update, complete, and cancel **their own** orders |
| **Admin** | Log in/out · Manage own profile · Medicine catalog CRUD · Stock audits, low-stock and expiry reports · Manage staff accounts · Update order status · Void orders · Audit prescriptions · Search records · Sales and inventory analytics |

**Default admin** (created on the first run): username `admin`, password `Admin123`. Change it after logging in ([ADR-009](docs/DECISIONS.md)).

## Features and owners

| Member | Area | Features |
|---|---|---|
| **Heather** | Accounts, plus the shared foundation (`common/`) | Register · Login/Logout · Manage Profile · Manage Users (Admin) |
| **Karlo** | Medicines & inventory | Browse · View Details · Search & Filter · Manage Catalog (Admin) · Stock & Inventory Audit (Admin) |
| **Cameron** | Orders & transactions | Process Order · Apply Discount (Strategy) · View/Update/Complete/Cancel Orders · Update Status · Void · Audit Prescriptions · Search Orders · Analytics (Admin) |

The full spec for every feature, with menu paths and rules, is in [docs/FEATURES.md](docs/FEATURES.md).

## How the code is organized

```
pharmacy-management-system/
├── README.md · AGENTS.md · PROJECT-BRIEF.md
├── docs/                 shared reference for everyone
├── common/src/pharmacy/  Main + shared foundation          (owner: Heather)
├── heather/src/pharmacy/auth/        accounts                (owner: Heather)
├── karlo/src/pharmacy/inventory/     medicines & stock       (owner: Karlo)
├── cameron/src/pharmacy/orders/      orders & analytics      (owner: Cameron)
├── <member>/docs/        each member's CONTEXT, PROMPTS, GOTCHAS, SESSION_LOG
└── data/                 created at runtime, git-ignored
```

**The full blueprint of all 62 Java files, with each one's purpose, owner and milestone, is in [docs/BLUEPRINT.md](docs/BLUEPRINT.md).** All four `src/` folders compile together into **one** program. The layers (Menu → Service → Repository → DataStore), class diagram, and the exact classes each member exposes to the others are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Run it

**Requirements:** JDK 17 or newer (`java -version`). No other libraries.

Always run from the **repository root**, so that `data/` is created in the right place.

macOS / Linux:
```bash
javac -d out $(find common heather karlo cameron -name "*.java")
```
```bash
java -cp out pharmacy.Main
```

Windows (PowerShell):
```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java common,heather,karlo,cameron).FullName
```
```powershell
java -cp out pharmacy.Main
```

**IDE setup.** Open the repository root folder, then:
- **IntelliJ IDEA:** File → Project Structure → Modules → Sources. Mark `common/src`, `heather/src`, `karlo/src`, and `cameron/src` as **Sources**. Run configuration: main class `pharmacy.Main`, working directory = the repo root.
- **VS Code** (Extension Pack for Java): add the four folders to `"java.project.sourcePaths"` in your workspace settings.
- **NetBeans:** New Project → Java with Existing Sources → add the four `src` folders.

To reset all data, delete the `data/` folder. The next run recreates it with only the default admin.

## Build order

`common/` must exist before anything else compiles, and `AdminMenu`/`PharmacistMenu` need every member's menu class. Work in this order.

**Each member follows their own step-by-step prompts, with the git commands for each step, in `<member>/docs/PROMPTS.md`:** [Heather](heather/docs/PROMPTS.md) · [Karlo](karlo/docs/PROMPTS.md) · [Cameron](cameron/docs/PROMPTS.md).

| Milestone | Who | What | Done when |
|---|---|---|---|
| **M0a** | Heather | `common/` foundation ([common/docs/CONTEXT.md](common/docs/CONTEXT.md)), plus `AuthMenu` and `ProfileMenu`. `AdminMenu`/`PharmacistMenu` list only *My profile* for now. | You can log in as `admin` and log out. |
| **M0b** | Karlo, Cameron | Each creates their menu classes (`extends BaseMenu`) with their titles. | All four folders compile. |
| **M0c** | Heather | Wire every member's menu into `AdminMenu`/`PharmacistMenu`. | Every menu opens. |
| **M1** | Everyone, in parallel | Features, in the order listed in your own `docs/CONTEXT.md`. Karlo: add medicines first, so Cameron has data to test with. Cameron: build `hasOrdersFor*` early for Heather and Karlo. | Each feature meets its definition of done. |
| **M2** | Everyone | Integration: a full walkthrough with both roles, the [hostile-input checklist](docs/VALIDATION.md#4-hostile-input-checklist), and a fresh start with no `data/`. | No crashes; data files match [DATA.md](docs/DATA.md). |
| **M3** | Everyone | Defense prep: each member shows all 4 OOP pillars, the patterns, and the exception handling in **their own** code (see the table in [ARCHITECTURE §10](docs/ARCHITECTURE.md#10-where-each-oop-pillar-is-demonstrated)). | Everyone can explain their code without notes. |

## Working together on GitHub

**Exact commands** for setup, branching, committing, pushing, pull requests, reviews, merging, and fixing mistakes are in **[docs/GIT-GUIDE.md](docs/GIT-GUIDE.md)**. The rules are in [docs/CONVENTIONS.md §4](docs/CONVENTIONS.md#4-git-workflow). In short:

1. Never commit straight to `main`. `main` must always compile.
2. Branch per feature: `heather/login`, `karlo/catalog-crud`, `cameron/discounts`.
3. Edit only your own folder. `common/` belongs to Heather. To change it, or a shared contract, ask in the group chat first ([CONVENTIONS §5](docs/CONVENTIONS.md#5-changing-common-or-a-cross-member-contract)).
4. Before opening a PR: merge the latest `main`, compile **all** folders, and run the hostile-input checklist on your screens.
5. A groupmate reviews every PR before it is merged.
6. If your change makes a doc wrong, fix the doc in the same PR.

## Documentation map

| File | Read it when you need… |
|---|---|
| [AGENTS.md](AGENTS.md) | Rules for AI coding agents (start here if you are one) |
| [PROJECT-BRIEF.md](PROJECT-BRIEF.md) | Why the project exists, its objectives, and scope |
| [docs/BLUEPRINT.md](docs/BLUEPRINT.md) | **Every file** in the app: path, purpose, owner, milestone; who-calls-whom diagram |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, packages, class diagram, patterns, **cross-member contracts**, order status and stock rules |
| [docs/DATA.md](docs/DATA.md) | Every data file, its column order, ID and date formats, enums, constants |
| [docs/FEATURES.md](docs/FEATURES.md) | What every feature does, its owner and menu path |
| [docs/VALIDATION.md](docs/VALIDATION.md) | Every validation rule and error message, plus the hostile-input checklist |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Why things are the way they are (ADR-001…015) |
| [docs/CONVENTIONS.md](docs/CONVENTIONS.md) | Code style, OOP rules, git workflow, how to verify |
| [docs/GIT-GUIDE.md](docs/GIT-GUIDE.md) | Copy-paste git/GitHub commands: branch, commit, push, PR, review, merge |
| [docs/GOTCHAS.md](docs/GOTCHAS.md) | Java pitfalls that look like bugs |
| [docs/SESSION_LOG.md](docs/SESSION_LOG.md) | Changes that affect everyone |
| `heather/`, `karlo/`, `cameron/` `docs/` | Each member's checklist (CONTEXT), step-by-step agent prompts and git commands (PROMPTS), own gotchas, own session log |
