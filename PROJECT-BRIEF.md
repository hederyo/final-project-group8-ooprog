# Project Brief — Pharmacy Management System

**Group #8 · OOPROG Final Project · Section C2B**
Members: Valenzuela, Kerth Heather · Silang, Karlo · Roxas, Cameron

This file is the *why*. For the *how*, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Course requirements (graded — never drop any of these)

The final project must be a system written in **Java** with:

| Requirement | Where it is satisfied |
|---|---|
| Login | Login/Logout feature — [FEATURES.md §A](docs/FEATURES.md) |
| CRUD (Create, Read, Update, Delete) | Users, Medicines, Orders — [FEATURES.md](docs/FEATURES.md) |
| Different roles | Admin and Pharmacist (Regular User) |
| Encapsulation | Private fields + getters/setters on every model |
| Abstraction | `User`, `BaseMenu` (abstract classes); `Menu`, `Storable`, `DiscountStrategy` (interfaces) |
| Inheritance | `Admin`/`Pharmacist extends User`; every menu `extends BaseMenu`; every exception `extends PharmacyException` |
| Polymorphism | `user.createMenu()`, `strategy.computeDiscount(...)`, `record.toFileLine()`, menu `handle(...)` overrides |
| Exception handling | Custom checked exception hierarchy, caught in `BaseMenu` — [ARCHITECTURE.md §7](docs/ARCHITECTURE.md#7-exception-handling) |
| Singleton (GoF) | `DataStore`, `Session` |
| Strategy (GoF) | `DiscountStrategy` → `RegularDiscount`, `SeniorPwdDiscount`, `BulkDiscount` |
| **All types of validation** | Every rule listed in [VALIDATION.md](docs/VALIDATION.md) |

Fixed project choices (from the group): **console** application, **text-file** storage, passwords stored as **plaintext** (school-project shortcut — see [ADR-003](docs/DECISIONS.md)).

## Overview

The Pharmacy Management System helps community and hospital pharmacies run daily medicine dispensing, stock replenishment, and transaction auditing. It gives pharmacy staff and administrators one organized place to:

- dispense medicines without errors,
- track stock levels and expiry dates,
- verify physician prescriptions for prescription-only drugs,
- compute bills with automatic discounts (Senior Citizen / PWD, Bulk).

## Objectives

1. Let pharmacists dispense medicines accurately while keeping inventory counts exact.
2. Let administrators monitor stock movement, lot numbers, low-stock and near-expiry medicines.
3. Promote patient safety: prescription-only drugs require a prescription ID and doctor license; expired medicines cannot be sold.
4. Remove manual calculation errors through automated discount computation.

## Users

| Role | Who | Summary |
|---|---|---|
| **Pharmacist** (Regular User) | Dispensary staff at the counter | Registers, logs in, manages own profile, browses/searches medicines, processes dispensing orders, applies discounts, updates/completes/cancels **their own** orders. |
| **Admin** | Pharmacy owner / manager | Manages the medicine catalog, stock audits, staff accounts, order statuses and voids, prescription audits, and analytics. |

## Scope

**In scope:** everything in [FEATURES.md](docs/FEATURES.md).

**Out of scope (decided — do not add without a group decision):**
- Multiple branches ([ADR-005](docs/DECISIONS.md))
- Multiple batches per medicine ([ADR-006](docs/DECISIONS.md))
- GUI, database, network, password hashing
- Real payment processing, receipts printing, supplier ordering
