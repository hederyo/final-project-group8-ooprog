# Heather — Gotchas (accounts + common)

Area-specific traps for `pharmacy.auth` and `common/`. Shared Java pitfalls are in [docs/GOTCHAS.md](../../docs/GOTCHAS.md). The entries below were written before coding as warnings. Mark them `Confirmed: <date>` when you hit them, and add new ones as you find them.

Format: `### Title` → **Symptom** / **Cause** / **Fix**.

---

### The previous user is still "logged in" after logout
**Symptom:** After logout and a new login, a service still acts as the old user.
**Cause:** `Session.logout()` was never called, or it didn't set `currentUser` to `null`.
**Fix:** `Main` always calls `Session.getInstance().logout()` when the role menu returns, even after an error.

### Duplicate check rejects a user's own email on profile update
**Symptom:** `Email already in use` when a user saves their profile without changing the email.
**Cause:** The uniqueness check counted the user's own record.
**Fix:** Pass the current `userId` as `excludeUserId` to `emailTaken`/`usernameTaken`/`licenseTaken`.

### `Juan@Gmail.com` and `juan@gmail.com` both get registered
**Symptom:** Duplicate accounts that differ only by capital letters.
**Cause:** A case-sensitive comparison.
**Fix:** Store emails lowercase (`requireEmail` returns lowercase). Compare usernames and emails with `equalsIgnoreCase`.

### Admin locks everyone out
**Symptom:** No admin can log in any more.
**Cause:** The last active admin was deactivated, or an admin deactivated themselves.
**Fix:** Rule A11. Count active admins before any deactivate or delete.

### Admin line has the wrong number of columns
**Symptom:** `DataAccessException` on `users.txt` line 1 after a fresh start.
**Cause:** `Admin.toFileLine()` dropped the empty license column, giving 8 columns instead of 9.
**Fix:** Write `...|contactNumber||status`, keeping the empty column ([DATA §3.1](../../docs/DATA.md#31-userstxt--owner-heather)).
