# Git & GitHub Guide

Exact commands for this project, from first setup to merged pull request. The rules behind them are in [CONVENTIONS §4](CONVENTIONS.md#4-git-workflow). Each member's `docs/PROMPTS.md` repeats the commands for each of their milestones with the right branch names filled in.

Commands work in **macOS Terminal**, **Git Bash**, and **Windows PowerShell**. Replace anything in `<angle brackets>`.

## Contents
1. [One-time setup (everyone)](#1-one-time-setup-everyone)
2. [Create the GitHub repo (one person, once)](#2-create-the-github-repo-one-person-once)
3. [Get the project (the other two)](#3-get-the-project-the-other-two)
4. [The daily loop: branch → commit → push → PR](#4-the-daily-loop-branch--commit--push--pr)
5. [Reviewing a groupmate's PR](#5-reviewing-a-groupmates-pr)
6. [Merging and syncing](#6-merging-and-syncing)
7. [Merge conflicts](#7-merge-conflicts)
8. [Fixing mistakes](#8-fixing-mistakes)
9. [Cheat sheet](#9-cheat-sheet)

---

## 1. One-time setup (everyone)

Install **Git** (https://git-scm.com) and the **GitHub CLI** `gh` (https://cli.github.com). `gh` is optional: everything it does can also be done on github.com. Then:

```bash
git config --global user.name "<Your Name>"
```
```bash
git config --global user.email "<the email on your GitHub account>"
```
```bash
git config --global init.defaultBranch main
```
```bash
gh auth login
```
(`gh auth login`: choose GitHub.com → HTTPS → log in with a web browser.)

## 2. Create the GitHub repo (one person, once)

Whoever has the local project folder with the docs commit does this.

**Option A — with `gh`** (creates the repo and pushes in one step):
```bash
cd pharmacy-management-system
```
```bash
gh repo create pharmacy-management-system --private --source=. --remote=origin --push
```

**Option B — on github.com:** New repository → name `pharmacy-management-system` → **leave "Add a README" unchecked** → Create. Then:
```bash
git remote add origin https://github.com/<github-user>/pharmacy-management-system.git
```
```bash
git push -u origin main
```

**Add your groupmates:** repo → Settings → Collaborators → Add people → their GitHub usernames. They must accept the email invite.

**Protect `main` (recommended):** Settings → Branches → Add branch ruleset (or "Add rule") for `main` → turn on *Require a pull request before merging* with *1 required approval*.
- This works for **public** repos, or for private repos on **GitHub Pro**. Pro is free with the GitHub Student Developer Pack (education.github.com).
- Without it, follow the rule anyway: never push to `main` directly.

## 3. Get the project (the other two)

```bash
git clone https://github.com/<github-user>/pharmacy-management-system.git
```
```bash
cd pharmacy-management-system
```

## 4. The daily loop: branch → commit → push → PR

Do this for **every** feature. Example: Karlo starting the catalog.

**① Start from the latest `main`**
```bash
git switch main
```
```bash
git pull
```

**② Create your feature branch** (`<member>/<feature>`)
```bash
git switch -c karlo/catalog-crud
```

**③ Work.** Code with your agent using your `docs/PROMPTS.md`. Compile and test (see the [README → Run it](../README.md#run-it)).

**④ Look at what changed.** You should only see your own folder (and docs you meant to change).
```bash
git status
```
```bash
git diff
```

**⑤ Stage only your files.** Never use `git add .` blindly.
```bash
git add karlo/
```
If you also updated a shared doc:
```bash
git add docs/DATA.md
```

**⑥ Commit** with a small message in the format `type(area): summary` (see the [cheat sheet](#9-cheat-sheet)):
```bash
git commit -m "feat(inventory): add and list medicines"
```
Commit as often as you like. Each commit should be one idea.

**⑦ Before pushing, bring in the latest `main`** and re-test:
```bash
git fetch origin
```
```bash
git merge origin/main
```
If there are conflicts, see [§7](#7-merge-conflicts). Then compile and run again.

**⑧ Push your branch**
```bash
git push -u origin karlo/catalog-crud
```
(After the first push, plain `git push` is enough.)

**⑨ Open the pull request**
```bash
gh pr create --base main --title "feat(inventory): medicine catalog CRUD"
```
`gh` asks for the body and pre-fills it from [the PR template](../.github/pull_request_template.md). Fill it in, then submit.
Without `gh`: open the repo on github.com → **Compare & pull request** → fill in the template → **Create pull request**.

**⑩ Ask a groupmate to review** (group chat, or `gh pr edit --add-reviewer <github-user>`). Fix anything they ask: commit, then `git push`. The PR updates automatically.

## 5. Reviewing a groupmate's PR

```bash
gh pr list
```
```bash
gh pr checkout <number>
```
Compile **all** folders, run the program, and try their feature plus a few items from the [hostile-input checklist](VALIDATION.md#4-hostile-input-checklist). Check that they only touched their own folder:
```bash
gh pr diff <number> --name-only
```
Then approve or request changes:
```bash
gh pr review <number> --approve --body "Tested login/logout, works."
```
```bash
gh pr review <number> --request-changes --body "Entering letters in quantity crashes the menu."
```
Go back to your own work:
```bash
git switch <your-branch>
```

## 6. Merging and syncing

After approval, the **author** merges. We use **squash merge** so each PR becomes one commit on `main`:
```bash
gh pr merge <number> --squash --delete-branch
```
(On github.com: **Squash and merge** → **Delete branch**.)

**Everyone**, after any merge:
```bash
git switch main
```
```bash
git pull
```
If you have a branch in progress, update it as well:
```bash
git switch <your-branch>
```
```bash
git merge main
```

Delete your local branch once it is merged:
```bash
git branch -d karlo/catalog-crud
```
(If git refuses because of the squash merge, and you are sure it was merged, use `-D`.)

## 7. Merge conflicts

Conflicts should be rare because everyone edits only their own folder. They usually happen in shared `docs/` files.

1. `git merge origin/main` reports `CONFLICT in docs/DATA.md`.
2. Open the file and find the markers:
   ```
   <<<<<<< HEAD
   your version
   =======
   the version on main
   >>>>>>> origin/main
   ```
3. Edit the file so it contains the correct combined text, and delete the three marker lines.
4. Stage the file and finish the merge:
   ```bash
   git add docs/DATA.md
   ```
   ```bash
   git commit
   ```
5. Compile, run, then `git push`.

If you get lost, `git merge --abort` returns you to before the merge.

## 8. Fixing mistakes

| Problem | Fix |
|---|---|
| Staged a file by accident | `git restore --staged <file>` |
| Want to throw away uncommitted changes to a file (**cannot be undone**) | `git restore <file>` |
| Typo in the last commit message, **not pushed yet** | `git commit --amend -m "new message"` |
| Forgot a file in the last commit, **not pushed yet** | `git add <file>` then `git commit --amend --no-edit` |
| Committed `data/` or `out/` | `git rm -r --cached data out` → commit. `.gitignore` keeps them out afterwards. |
| Committed on `main` by mistake, **not pushed yet** | `git switch -c <member>/<feature>` (your work moves to a new branch), then `git switch main` and `git reset --hard origin/main`. **Do the first command first**, or the work is lost. |
| Already pushed something wrong | Don't rewrite history. Fix it in a new commit and push again. |
| Not sure what state you're in | `git status`, then `git log --oneline -5`, then ask in the group chat before trying risky commands. |

**Never** run `git push --force` on `main`, or on someone else's branch.

## 9. Cheat sheet

**Commit types:** `feat` (new feature) · `fix` (bug fix) · `refactor` (same behaviour, cleaner code) · `docs` (documentation only) · `chore` (setup, gitignore).
**Areas:** `common`, `auth`, `inventory`, `orders`, `docs`.

```
feat(common): add DataStore and Session singletons
feat(auth): add login with 3-attempt limit
feat(inventory): add medicine search and filters
feat(orders): add discount strategies
fix(orders): restore stock when an order is cancelled
docs: record bulk discount rule in DATA.md
```

**Branch names used in the member prompts**

| Heather | Karlo | Cameron |
|---|---|---|
| `heather/common-foundation` | `karlo/menu-skeleton` | `cameron/menu-skeleton` |
| `heather/wire-menus` | `karlo/medicine-model` | `cameron/discounts-status` |
| `heather/register` | `karlo/catalog-crud` | `cameron/order-data` |
| `heather/profile` | `karlo/stock-contract` | `cameron/process-order` |
| `heather/manage-users` | `karlo/browse-search` | `cameron/manage-orders` |
| | `karlo/stock-audit` | `cameron/admin-orders` |
| | `karlo/catalog-delete` | `cameron/analytics` |

**Overall merge order** (across members): Heather `common-foundation` → Karlo + Cameron `menu-skeleton` → Heather `wire-menus` → everything else in parallel, respecting the "Wait for" column in each member's `docs/PROMPTS.md`.
