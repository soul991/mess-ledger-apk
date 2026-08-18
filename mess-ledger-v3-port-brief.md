# Mess Ledger — Native Port Brief (v3, supersedes v2)

**Read this before touching any code.** The previous brief (`mess-ledger-v2-spec.md`) told you to build this "as a real native Android app... this is a rewrite of the client, not a patch." That instruction was wrong and caused real damage: the `MessLedger/` Compose project that resulted is a from-scratch redesign — different navigation, different screens, generic Material 3 instead of the app's actual visual identity — sitting disconnected next to the original app instead of replacing it, plus it shipped with a live Firebase credential committed to a public repo and two bugs severe enough to make mess creation and join requests both non-functional.

**This brief replaces that instruction with a much narrower one: port `android/assets/index.html` — the actual, working app — screen for screen, flow for flow, into Kotlin/Compose. Do not redesign anything. Do not invent new screens, new navigation patterns, or new visual language. The four features in sections 2–5 are the only new functionality in scope; everything else in the app should look and behave the same as it does today, just running natively instead of in a WebView.**

---

## 0. What you're starting from, and what to keep vs. throw away

### Keep, with the fixes already applied in this pass
The `MessLedger/` project's **data layer is largely fine and should be reused, not rewritten**:
- `data/model/*.kt` — data classes, correct
- `data/repository/*.kt` — repository pattern is sound
- `data/remote/FirestoreService.kt` — mostly correct Firestore access layer
- `data/local/*` (Room entities/DAOs) — fine as offline cache
- `MessLedger/firestore.rules` — now fixed (see the two blocking bugs below); use this version, not an earlier one
- `.gitignore` — now excludes `google-services.json` and `*.zip`; keep it that way

**Already fixed in this revision, verify these land in your working copy before building further:**
- `firestore.rules`: member-doc `create` no longer deadlocks mess creation (was `allow create: if isManager(messId)`, which is impossible to satisfy on your very first write since no member doc exists yet — fixed to also allow the mess's own `createdBy` to self-assign the manager role once).
- `firestore.rules`: member-doc `update` no longer lets any member write `role: "manager"` to their own row (added a `request.resource.data.role == resource.data.role` guard on the self-edit branch).
- `firestore.rules`: `joinRequests` `create` now requires the parent mess to actually `exists()` — otherwise Firestore silently creates the request under a nonexistent mess and nobody ever sees it.
- `Constants.MESS_ID_CHARS` — dropped `#` (it's a URI fragment delimiter; a mess ID containing it truncates when embedded in an invite deep link, which was reported and reproduced).
- `MessRepository.createMess()` — was generating a raw 36-char `UUID` and never actually calling the already-written `MessIdGenerator`; now uses the real short-ID generator with a collision check, and sets the new `createdBy` field the rules fix depends on.
- `RequestRepository.submitJoinRequest()` — was writing a join request with **no check that the target mess exists at all**, so both a genuinely invalid mess ID and a case/whitespace-mismatched valid one silently reported "Request Sent!" while creating an orphan document nobody was listening to. Now normalizes the ID (trim + uppercase) and validates existence first, surfacing "Mess ID not found" instead of a false success.
- `JoinMessScreen.kt` — the mess ID field now uppercases as you type, matching the original app's input behavior, so what the user sees matches what gets submitted.

### Throw away / redo
Every file under `MessLedger/app/src/main/java/com/messledger/app/ui/` — screens, navigation, theming — was designed from scratch rather than ported. Section 1 below tells you exactly what to build instead, screen by screen, against the real source of truth.

### Absolute must-do before any more commits
1. **`google-services.json` must never be committed.** It already was once, in a public repo — treat that key as burned. Go to Google Cloud Console → APIs & Credentials, find that key, and restrict it to the `com.messledger.app` package name + your release/debug SHA-1 fingerprints. Consider regenerating the Android app registration in Firebase entirely if you want a clean key. Distribute `google-services.json` to each developer out-of-band (Firebase console download), never via git.
2. Delete `files.zip` from the repo history if you care about repo cleanliness (it's already untracked going forward, but it's still in prior commits).

---

## 1. Source of truth: `android/assets/index.html`

This is the real spec. Every screen you build should be traceable to a specific function in this file. The canonical list of screens/render functions, in the order a new user encounters them:

| HTML function | What it renders | Compose equivalent to build |
|---|---|---|
| `showLandingScreen()` | "Create a mess" / "Join a mess" buttons, mess-ID input | `HomeScreen` landing state (pre-auth) |
| *(new, Feature 1)* | Login / Register tabs | `AuthScreen` — reuse existing `LoginScreen.kt`/`RegisterScreen.kt`, they're already close to right |
| `showSetupScreen()` | Mess name + member rows (name+PIN) to create a mess | `CreateMessScreen` — **PIN fields for pre-adding members are being replaced by real accounts; see decision needed in section 6** |
| `showMessCreatedScreen()` | Shows the new Mess ID with a copy button | Fold into `CreateMessScreen`'s post-creation state |
| `showLoginScreen()` / `renderLoginMembers()` | Per-mess member grid + PIN entry | **Retired.** Feature 1 replaces per-mess PIN login with the platform account you're already logged into. Don't port this screen; it's the thing being removed. |
| `renderDashboard()` | Fund balance, month expenses, meal rate, today's meals, recent expenses list | Port `SummaryScreen.kt` to match this layout/copy/card order exactly, using the app's actual color tokens (`Color.kt` already has `LedgerGreen` etc. — use them, don't default to Material 3 baseline colors) |
| `renderMeals()` / `renderMealRows()` | Date picker + per-member lunch/dinner present/absent pills | Port `MealsScreen.kt` — pill styling (present=green, absent=red, exact copy) should match, not just function |
| `renderGuests()` / `renderGuestList()` | Guest meal form + log | Port `GuestMealsScreen.kt` |
| `renderFund()` / `renderContribList()` | Contribution form + log, fund balance | Port `ContributionsScreen.kt` |
| `renderExpenses()` / `renderExpenseList()` | Expense form (category/split/paid-by) + category totals + log | Port `ExpensesScreen.kt` |
| `renderSettlement()` / `renderChit(r,s)` | Month picker + per-member settlement "chit" cards with the pay/receive stamp | Port as-is — this is the app's signature visual element (the circular stamp), don't simplify it away |
| `renderMembersPane()` / `renderCategoriesPane()` / `renderGeneralPane()` / `renderDangerPane()` | Settings modal, 4 tabs | Port `MessSettingsScreen.kt` — members pane now also needs the manager-only actions from Feature 2/3 (approve/reject/remove/transfer), added to this existing screen, not a new one |

**Tab navigation:** the original app uses a bottom tab bar on mobile / left rail on wide screens (Dashboard, Meals, Guests, Fund, Expenses, Settlement) — six tabs, this exact order, this exact icon set (SVGs are inline in the HTML `ICON` object, recreate them as vector assets or `ImageVector`s, don't substitute different Material icons). `MessDetailScreen.kt` currently has five tabs in a different order (Summary, Meals, Expenses, Funds, Activity) with Activity folded into the tab bar — the new Activity screen (Feature 4) should be reached from the settings/topbar area, not by displacing one of the original six tabs.

**Color/type tokens:** `Theme.kt`/`Color.kt` already define `LedgerGreen`, `LedgerGreenLight`, etc. — extend this file with the *rest* of the CSS custom properties from `index.html`'s `:root` block (`--paper`, `--paper-white`, `--paper-line`, `--ink`, `--ink-soft`, `--debit-red`, `--credit-green`, `--brass`) rather than falling back to Material 3 defaults anywhere. Same for typography — the original uses Zilla Slab (display), IBM Plex Sans (body), IBM Plex Mono (numbers/amounts) — bundle these as Compose custom fonts instead of the system default.

---

## 2. Feature 1 — Real user accounts (unchanged from v2 spec)

See `mess-ledger-v2-spec.md` sections 1–2 for the full functional spec (username/password rules, `users/{uid}`/`usernames/{username}` collections, synthetic-email mapping) — that part of the v2 brief was fine and doesn't need to change. `AuthScreen.kt`/`LoginScreen.kt`/`RegisterScreen.kt` in the current Compose project are close to this spec already and are reusable with light cleanup, not a rebuild.

**One addition based on what shipped:** `RegisterScreen.kt`'s live password checklist and debounced username availability check are good UX and match the spec — keep them exactly as built.

---

## 3. Feature 2 — Manager role, including transfer (unchanged from v2 spec)

See `mess-ledger-v2-spec.md` section 3 in full, including the manager-transfer flow. The current `MembersScreen.kt`/`MembersViewModel.kt` already implement transfer with a confirmation dialog — that part is genuinely good and should be kept, just needs to live inside the ported `renderMembersPane()` layout instead of its own separate screen with different visual styling.

---

## 4. Feature 3 — Invitations, join requests, manager-gated leaving (unchanged from v2 spec, bug-fixed)

See `mess-ledger-v2-spec.md` section 4 in full. The join-request bug described at the top of this document is now fixed in `RequestRepository`/`JoinMessScreen`/`firestore.rules` — build the rest of this feature (invite link generation/deep link, leave requests, the pending-requests approval screen) on top of that fixed foundation, and apply the same "does the target actually exist" defensive check anywhere else a raw ID from user input or a deep link reaches Firestore.

**Deep link format reminder:** `messledger://invite/{messId}` — now that `#` is out of the ID alphabet, this is safe, but double check `*` doesn't cause problems in whatever URI-building code constructs the shareable string (Android's `Uri.parse`/`NavDeepLink` matching handles `*` fine in a path segment; just don't URL-encode it unnecessarily and don't add other symbols to the alphabet later without checking against RFC 3986 reserved characters first).

---

## 5. Feature 4 — Full activity log (unchanged from v2 spec)

See `mess-ledger-v2-spec.md` section 5 in full. `activityLog.ts` (Cloud Function triggers) and `ActivityScreen.kt`/`ActivityViewModel.kt` already exist and match the spec reasonably well — reuse them, but move the entry point to a topbar/settings action rather than a sixth bottom tab, per section 1's navigation note above.

---

## 6. Decision needed before building section 1's `CreateMessScreen` port

The original app's mess-setup screen lets the creator add members by name + optional PIN, with **no account required** — this supports members who never install the app (e.g. someone without a smartphone, tracked purely for bookkeeping). Feature 1 makes every member doc keyed by a real account `uid`. These two things conflict, and the v2 spec never actually resolved it — it's being surfaced again here because you're about to port the exact screen where it matters.

Pick one before building `CreateMessScreen`/`renderMembersPane`'s add-member row:
- **(a) Drop phantom members entirely.** Every member must have a real account, added via the join-request flow only. Simpler, but a real capability loss for the target use case (shared housing, not everyone necessarily tech-savvy).
- **(b) Keep phantom members as an explicit second category.** A manager can still add a name-only placeholder row; it's flagged as "not a linked account" in the UI, participates fully in meals/settlement, but can't log in or approve anything itself. Someone can later "claim" it by registering and having the manager link their account to that row. More faithful to the original app, more implementation work (another state to handle throughout the member list, settlement, and activity log).

Don't guess — ask before building this screen, since it changes the `Member` data model (`uid: String?` nullable vs required) and several downstream rules.

---

## 7. What "done" looks like for this pass

- Every screen in section 1's table exists, visually and functionally traceable to its `index.html` counterpart — a side-by-side comparison should look like the same app, not a redesign.
- The six-tab bottom navigation matches the original exactly, in order.
- Colors/type come from the app's actual design tokens, not Material 3 defaults.
- Features 1–4 work end to end, including the join-request bug fix verified by an actual manual test: create a mess as user A, submit a join request as user B against that real mess ID, confirm it appears in user A's pending-requests list before approving.
- `google-services.json` is not in git history going forward; the leaked key has been restricted or rotated.
- Features 5 (fund add "by/to" verification) and the settlement-algorithm redesign remain out of scope.
