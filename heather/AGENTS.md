# heather/ — Heather's workspace

**Member:** Valenzuela, Kerth Heather
**Area:** Accounts: Register, Login/Logout, Manage Profile, Admin Manage Users. Also the **owner of [`common/`](../common/AGENTS.md)**, the shared foundation everyone builds on.
**Package:** `pharmacy.auth` (+ everything under `pharmacy.common` and `pharmacy.Main`)

## Read in this order
1. Root [AGENTS.md](../AGENTS.md): project rules and doc map.
2. This file.
3. [docs/CONTEXT.md](docs/CONTEXT.md): your features, classes, contracts, checklist.
   Your member runs the steps in [docs/PROMPTS.md](docs/PROMPTS.md). Follow its **Working rules**.
4. [../common/docs/CONTEXT.md](../common/docs/CONTEXT.md): the foundation you also own.
5. [docs/GOTCHAS.md](docs/GOTCHAS.md) and the last few entries of [docs/SESSION_LOG.md](docs/SESSION_LOG.md).
6. Shared docs as needed: [FEATURES §Accounts](../docs/FEATURES.md#accounts--heather), [VALIDATION §3.1](../docs/VALIDATION.md#31-accounts--heather), [DATA §3.1](../docs/DATA.md#31-userstxt--owner-heather).

## What you may edit
- `heather/**` and `common/**`.
- Shared `docs/*.md`, only when your change makes them wrong, in the same PR.
- **Never** `karlo/**` or `cameron/**`. If you need something from them, stop and tell Heather what to ask for.

## Rules
- Build `common/` before your own features. The others are blocked until it compiles.
- Keep the [contracts](../docs/ARCHITECTURE.md#6-cross-member-contracts) you provide stable (§6.1 and §6.2).
- Follow [CONVENTIONS](../docs/CONVENTIONS.md). Every input needs a validation rule from [VALIDATION](../docs/VALIDATION.md).
- Heather must be able to explain every line in the defense. Prefer clear code to clever code, and explain any non-obvious choice to her.

## Before ending a session
1. Tick finished items in [docs/CONTEXT.md](docs/CONTEXT.md).
2. Append an entry to [docs/SESSION_LOG.md](docs/SESSION_LOG.md).
3. Add anything surprising to [docs/GOTCHAS.md](docs/GOTCHAS.md).
4. If you changed `common/` or a contract, also log it in the shared [docs/SESSION_LOG.md](../docs/SESSION_LOG.md).
