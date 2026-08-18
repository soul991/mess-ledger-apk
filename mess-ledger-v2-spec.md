# Mess Ledger — Native Rebuild & Feature Spec (v2)

**Purpose of this document:** this is a implementation brief meant to be handed directly to a coding agent (e.g. Antigravity) to build against. It covers *why* the current app needs to stop being a WebView wrapper, the current architecture the agent is starting from, and full functional specs for the four features below. Features 5 (fund add "by/to" verification) and the settlement-algorithm redesign are explicitly **out of scope** for this pass — do not build them, but don't design anything in a way that would block adding them later.

---

## 0. Why this has to stop being an HTML app in a WebView

### What exists today
`Mess-Ledger-` (github.com/Incredible-Maruf/Mess-Ledger-) is a single ~1,800-line `index.html` — vanilla JS, Firebase Firestore + Anonymous Auth, no build step. `mess-ledger-apk` (github.com/soul991/mess-ledger-apk) wraps that same `index.html` unmodified inside a bare Android `WebView` (`MainActivity.kt` just calls `webView.loadUrl("file:///android_asset/index.html")`). There is no Gradle project, no Jetpack Compose, no Room, no real Android architecture — it's a static site loaded from local assets, given an app icon.

### Why that path was taken (context, not an excuse to keep it)
It let one person ship a working multi-device, realtime-synced app fast, with no Android build toolchain, no App Store review of anything but a thin shell, and free hosting. That's a reasonable way to prototype. It stops being reasonable the moment the app needs things a WebView fundamentally can't give it well:

- **Real user accounts** (feature 1) need proper credential storage, session tokens, and secure local persistence — doable in a WebView via Firestore, but every security boundary has to be re-implemented in rules/JS instead of using platform primitives (Android Keystore, FirebaseUI, biometric unlock).
- **Push notifications for approvals** (features 3 and 4 both need "manager gets notified" flows) require FCM wired into a native `Service`/`BroadcastReceiver` — a WebView can receive push through the JS layer only if the native shell forwards it, which adds a native layer anyway. Might as well be a real one.
- **Deep links for invitations** (feature 3) are a native OS mechanism (Android App Links / `intent-filter` with `autoVerify`) — a WebView has no meaningful way to register as a Play Store deferred-deep-link target.
- **Offline-first activity logging** (feature 4) wants durable local storage independent of whether Firestore's offline cache decides to evict something — Room gives you that; `localStorage`/IndexedDB inside a WebView does not, reliably, across Android OEM skins.
- Performance, memory footprint, and Play Store's WebView-wrapped-app policies are all worse for this shape of app as it grows.

**Directive: build this as a real native Android app.** Kotlin, Jetpack Compose for UI, a proper Gradle project (not the hand-rolled `aapt2`/`d8` shell script), MVVM with a repository layer, Room for local persistence/offline cache, Firebase Auth (real email/username+password, not anonymous) + Firestore for the backend, FCM for push. Retire `mess-ledger-apk`'s `WebView` approach entirely — this is a rewrite of the client, not a patch. The Firestore data the existing web app already wrote (messes, members, meals, guestMeals, contributions, expenses) is real data worth preserving — see the migration section below for how the new app should read/adapt it, not discard it.

---

## 1. Current data model (starting point — do not redesign this part yet)

Firestore, one document per mess under `messes/{messId}`, with subcollections:

```
messes/{messId}
  messName: string
  categories: string[]
  createdAt: timestamp
  messes/{messId}/members/{memberId}
    name: string
    pin: string            // 4-digit, plaintext, client-side-only check — being replaced by feature 1/2
    joinedAt: "YYYY-MM-DD"  // added in a prior fix pass
    deletedAt: timestamp?   // soft-delete, added in a prior fix pass
  messes/{messId}/meals/{dateStr}
    { [memberId]: { lunchAbsent: bool, dinnerAbsent: bool } }
  messes/{messId}/guestMeals/{id}
    hostId, date, meal, count, note?
  messes/{messId}/contributions/{id}
    memberId, amount, date, note?
  messes/{messId}/expenses/{id}
    paidBy, category, amount, date, splitType ('meals'|'equal'), note?
```

Auth today: `signInAnonymously()` on load, no real identity. Mess IDs are 6 characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (no look-alikes), generated in `generateUniqueMessId()`. Per-mess "login" is just tapping a member's name and entering their 4-digit PIN, checked client-side against the plaintext value already fetched.

**Known issue to fix as part of this work, not separately:** current Firestore rules only check `request.auth != null` with no per-mess membership check, which — combined with anonymous auth being free to obtain — lets anyone run a Firestore collection-group query and read/write every mess's data, including plaintext PINs. Feature 1 (real accounts) is the natural fix: once every request carries a real authenticated UID, rules can check actual membership (a `members` doc keyed by UID, or a custom claim) instead of just "is signed in at all." Rules must be rewritten as part of this work — see section 6.

---

## 2. Feature 1 — Real user accounts (register/login), separate from mess membership

### Concept
Today, "who you are" only exists *inside* a mess (a member row + PIN). That has to become two layers: a **platform account** (one identity per person, across all messes) and **mess membership** (which messes that account belongs to, and with what role). This mirrors how e.g. Slack or Discord separate "your account" from "which workspaces/servers you're in."

### Flow
1. **App launch, no active session:** landing screen offers **"Create a mess"** / **"Join a mess"** (same as today) — but tapping either now routes through account auth first if there's no logged-in account.
2. **No account yet →** show **Login** / **Register** tabs.
3. **Register:**
   - Fields: **Name** (display name, shown to other mess members), **Username** (unique, used to log in — not necessarily their real name), **Password**, **Confirm password**.
   - Username rules: 3–20 characters, lowercase letters/digits/underscore only, must be unique platform-wide. Check availability with a debounced lookup as they type (don't wait for submit to tell them it's taken).
   - Password rules — enforce and show these as a live checklist while typing, not just an error after submit:
     - Minimum 8 characters
     - At least one letter (a–z or A–Z)
     - At least one number
     - (Recommended addition, confirm with product owner before enforcing: also require it not be in a common-password blocklist — Firebase Auth doesn't check this by default, so it'd need a client-side check against a small bundled list. Optional for v1, flag as a fast-follow if skipped.)
   - On submit: create the account (Firebase Auth email/password under the hood works fine even though the user-facing field is "username" — map `username` → a synthetic email like `{username}@messledger.internal` for Firebase Auth's email/password provider, and store the real `username`/`name` in a `users/{uid}` Firestore doc). Do not expose the synthetic email anywhere in the UI.
   - On success: account is created and logged in immediately, no email verification gate for v1 (there's no real email to verify) — land them back on "Create a mess / Join a mess."
4. **Login:** username + password. Generic error message ("Incorrect username or password") on failure — never reveal whether the username specifically exists, to avoid username enumeration.
5. **Session persistence:** stay logged in across app restarts (Firebase Auth's built-in persistence handles this natively — this is one of the concrete wins of dropping the WebView, since `localStorage`-based session persistence in a WebView is exactly the kind of thing that gets silently cleared by OEM battery/storage optimizers). Add a **Log out** action reachable from settings.
6. **Data model addition:**
   ```
   users/{uid}
     name: string
     username: string        // also indexed for uniqueness lookups
     createdAt: timestamp
   usernames/{username}       // separate collection, doc ID = username, for fast uniqueness checks + reverse lookup
     uid: string
   ```
7. **Relationship to existing per-mess `members` documents:** a `members/{memberId}` doc becomes "this account's membership record in this specific mess," and `memberId` should now be the account's `uid` (not a random Firestore auto-ID) so a single account maps to exactly one membership row per mess. Add a `role` field here — see Feature 2.

### Explicit non-goals for this pass
No password reset flow yet (no email to send one to) — add a "forgot password" placeholder that says "contact your mess manager" for now, don't leave it silently missing. No social login. No profile photo.

---

## 3. Feature 2 — Manager role

### Concept
Whoever creates a mess is that mess's manager by default. The manager is the approval authority for feature 3 (join/leave requests) and feature 5 (out of scope, but the role needs to exist now so it's not retrofitted later).

### Flow & rules
1. On mess creation, the creator's membership doc is written with `role: "manager"`. Every other member who joins gets `role: "member"`.
2. **Exactly one manager per mess for this pass**, but managers must be able to **hand the role to another member** — this is not optional, since it's the only real way for a manager to step back without deleting the whole mess for everyone. (Multiple simultaneous managers / an "assistant-manager" tier is still future scope — don't build that — but single-manager-at-a-time-with-transfer is table stakes for v1, not an extra.) `role` should be a string field, not a boolean `isManager`, so adding more tiers later doesn't require a schema migration.
3. **Manager transfer flow:**
   - From the members list, the current manager gets an action on any other active member: **"Make manager."**
   - Requires confirmation (this is a meaningful, hard-to-casually-undo action): "Make {name} the manager of {mess name}? You'll become a regular member." Two-step confirm, not a single tap.
   - On confirm: update the outgoing manager's `role` to `"member"` and the incoming member's `role` to `"manager"` in a single Firestore transaction — never allow a state where the mess briefly has zero managers or two managers, since concurrent writes could otherwise race into that.
   - Write a `manager_assigned` activity log entry recording both the old and new manager (see Feature 4's log).
   - Notify the newly assigned manager via push ("You're now the manager of {mess name}") so it's not a silent change from their side.
   - No approval step needed from the incoming member for this pass — being handed the role takes effect immediately. (Requiring them to accept first is a reasonable future refinement; flag it, don't build it now, since it adds another pending-request type for a fairly low-risk action — worst case the outgoing manager hands it back.)
4. Manager-only actions (enforce both in UI — hide/disable the controls for non-managers — **and** in Firestore rules, since UI-only enforcement is not enforcement):
   - Approve/reject join requests (Feature 3)
   - Approve/reject leave requests (Feature 3)
   - Edit mess-level settings (mess name, categories) — this already existed as an implicit "whoever's in settings" action; scope it to manager-only now.
   - Remove a member directly (the existing soft-delete `deleteMember` action) — restrict to manager only; a regular member should not be able to remove another member.
   - Transfer the manager role (previous point)
5. **What happens if the manager wants to leave and there's no one to hand it to** (a mess with only one member, who is the manager): block leaving in that specific case — there's no one to transfer to — with the message "You're the only member of this mess. Remove the mess or add another member first." This is now a true edge case (a mess of one) rather than the default experience for every manager who wants to leave, since transfer (point 3) handles the normal case.
6. Show the manager's name with a visible badge (e.g. "Manager" tag next to their name) everywhere member lists are shown, so it's never ambiguous who to ask for approval — and so it's visible immediately after a transfer who holds the role now.

---

## 4. Feature 3 — Invitations, join requests, and manager-gated leaving

This is the biggest behavior change. Break it into three parts: longer mess IDs, invite links, and the request/approval flow.

### 4a. Longer, harder-to-guess Mess IDs

- New format: **minimum 8 characters**, alphanumeric plus a small safe set of symbols (e.g. `#`, `*`, `!`, `-`, `_` — avoid symbols that are awkward to type on mobile keyboards or that URL-encode badly if the ID ever appears in a link; recommend restricting to `#`, `*`, `-`, `_` and skip characters like `!`/`?` that could confuse users about whether it's part of the ID or punctuation in a sentence).
- Keep excluding look-alike characters the current app already avoids (`O`/`0`, `I`/`1`, `l`).
- **Migrate all existing mess IDs**, don't just apply this to new messes going forward:
  - Write a one-time migration (Cloud Function or an admin script run once, not client-side) that, for every existing `messes/{oldId}` document: generates a new ≥8-character ID in the new format, copies the mess document and all subcollections to `messes/{newId}`, deletes the old document tree, and — critically — **notifies existing members of the new ID**, since anyone with the mess open will have the old ID saved/shared. The cleanest way: keep a `messes/{oldId}` tombstone doc containing only `{ redirectTo: newId }` for some transition window (e.g. 30 days) so old links/saved IDs still resolve, then delete the tombstone after.
  - This migration is a one-time operation — build it as a standalone script, run it once against production data, and confirm it before deleting old documents. Don't make it something that runs automatically on every app launch.

### 4b. Invitation links

- A manager or existing member can generate a **shareable link** for their mess (e.g. `https://messledger.app/invite/{messId}` or a custom scheme if there's no real domain yet — use Android App Links with `autoVerify="true"` if a domain exists, otherwise a custom URI scheme like `messledger://invite/{messId}` as a fallback that still works for anyone who already has the app installed).
- Tapping the link on a device **without the app installed**: Android App Links naturally fall through to the browser if no app claims the link, so point the fallback web page at the GitHub repo/releases page so the download starts from there. (Do not hardcode any specific store or "how this gets distributed" language into the app itself — just make the fallback URL configurable, since distribution is still an open question.)
- Tapping the link **with the app installed, first launch or not**: deep link routes straight into "Join this mess" pre-filled with the mess ID, landing on login/register if there's no active session yet (per Feature 1's flow), then straight to the join-request screen (4c) once authenticated — the user should not have to manually type or paste the mess ID at all in this path.
- Regenerate note: an invite link should be revocable (e.g. the manager can regenerate the mess ID, which invalidates old links) — this falls out naturally from the ID-based link scheme, no extra field needed.

### 4c. Join requests (manager approval required)

- The existing "enter mess ID to join" flow no longer joins immediately. Instead:
  1. User enters/deep-links to a mess ID, and (once logged in) submits a **join request**.
  2. Write to `messes/{messId}/joinRequests/{uid}`: `{ name, requestedAt, status: "pending" }`.
  3. Manager sees a **pending requests** list (badge/notification count in the app; also send an FCM push to the manager: "X wants to join {mess name}").
  4. Manager taps **Approve** → create the `members/{uid}` doc with `role: "member"`, delete the join request doc, notify the requester (push: "You've joined {mess name}").
  5. Manager taps **Reject** → delete the join request doc, notify the requester (push: "Your request to join {mess name} was declined").
  6. Requester sees their own pending request with a status indicator ("Waiting for manager approval") and can withdraw it before it's actioned.
- A user should not be able to submit a duplicate pending request to the same mess (check for an existing `joinRequests/{uid}` doc client-side before allowing submission, and enforce it in rules too — same `uid` as doc ID naturally prevents duplicates).

### 4d. Leaving also requires manager approval

- Add a **leave request** flow mirroring join requests: `messes/{messId}/leaveRequests/{uid}`: `{ requestedAt, status: "pending" }`.
- A member cannot simply remove themselves from a mess anymore — tapping "Leave mess" submits a leave request, notifies the manager, and the member's status shows "Leave request pending" until the manager approves.
- On approval: soft-delete the member's `members/{uid}` doc (same `deletedAt` mechanism already built for the manager-initiated removal), delete the leave request doc, notify the ex-member.
- On rejection: delete the leave request doc, notify the member, they remain a member. (Give the manager an optional reason field when rejecting — not required, but worth including since "why was I rejected" is a natural question.)
- **Exception carried over from Feature 2:** a manager who's still the mess's only member is blocked from submitting a leave request (there's no one to approve it or hand the role to). Any manager with at least one other member should transfer the role first (section 3, point 3) — once they're a regular member, leaving works through the normal leave-request flow like anyone else.

---

## 5. Feature 4 — Full activity log

### Concept
A single, append-only, chronological log per mess, visible to every member, recording every meaningful change: who did what, when. This is distinct from the existing per-item lists (expenses list, contributions list, etc.) — those show *current state*; this shows *history of changes*, including things that don't have their own list view (member joins/leaves, role changes, mess settings edits, join/leave-request approvals).

### Data model
```
messes/{messId}/activityLog/{autoId}
  actorUid: string           // who did it
  actorName: string          // denormalized at write time, so the log still reads correctly
                              // even if that member is later soft-deleted or renamed
  action: string              // enum, see below
  summary: string             // short human-readable line, generated at write time
  targetId?: string           // id of the affected expense/contribution/member/etc, if applicable
  amount?: number              // for money-related actions, denormalized for quick display
  timestamp: server timestamp
```

`action` enum (extend as needed, don't treat this as exhaustive — anything that changes shared state should log something):
`member_joined`, `member_left`, `member_removed`, `join_request_submitted`, `join_request_approved`, `join_request_rejected`, `leave_request_submitted`, `leave_request_approved`, `leave_request_rejected`, `manager_assigned`, `expense_added`, `expense_edited`, `expense_deleted`, `contribution_added`, `contribution_edited`, `contribution_deleted`, `guest_meal_added`, `guest_meal_edited`, `guest_meal_deleted`, `meal_toggled`, `mess_settings_changed`.

### Behavior
- Every write path that changes mess state (expense/contribution/guest-meal CRUD, meal toggles, member join/leave/removal, request approvals/rejections, settings edits) must also write an activity log entry, in the same logical operation. Prefer doing this via a Cloud Function trigger (`onWrite` on the relevant collections) rather than requiring every client call site to remember to write both documents — that guarantees the log can't drift out of sync with the actual data, and it can't be bypassed by a client that skips the log write (accidentally or maliciously).
- UI: a dedicated **Activity** tab/section, reverse-chronological, grouped by day (e.g. "Today," "Yesterday," "Aug 14"). Each entry shows actor name, a short description ("Aditi added ₹450 expense — Groceries," "Rahul approved Priya's join request"), and a relative timestamp.
- Should support basic filtering (by action type or by member) once there's enough volume to need it — not required for v1, but the `action` and `actorUid` fields should already support building that filter later without a schema change.
- Retention: keep indefinitely for v1 — don't build expiry/pruning logic now, but note it as a future cost concern (a very active, long-running mess will accumulate a lot of log rows — Firestore read costs for the activity view should be paginated, not "load everything," from day one).
- Meal toggles specifically are high-frequency (every lunch/dinner tap) — logging every single one could flood the feed. Recommendation: log meal-absence toggles, but batch/collapse them if reasonable (e.g. one log line per member per day summarizing that day's final state, rather than one line per tap) — flag this as a judgment call for whoever implements it rather than a hard requirement, since it trades off completeness against feed readability.

---

## 6. Firestore security rules — must be rewritten alongside this work, not after

The current rules (`request.auth != null` with no membership check) are inadequate even before these features — see section 1's callout. Once every user has a real Firebase Auth UID (Feature 1) and membership is keyed by UID (`members/{uid}`), rules can and must check actual membership:

```
match /messes/{messId} {
  allow get: if isMember(messId) || hasPendingRequest(messId);
  allow list: if false;
  allow create: if request.auth != null; // creating a new mess is fine for any signed-in user
  allow update: if isManager(messId);
  allow delete: if isManager(messId);

  match /members/{memberId} {
    allow read: if isMember(messId);
    allow write: if isManager(messId) || (request.auth.uid == memberId); // members can edit their own row (e.g. display name), not others'
  }
  match /joinRequests/{uid} {
    allow create: if request.auth.uid == uid;
    allow read: if isManager(messId) || request.auth.uid == uid;
    allow update, delete: if isManager(messId) || request.auth.uid == uid; // manager actions it, requester can withdraw it
  }
  match /leaveRequests/{uid} {
    allow create: if request.auth.uid == uid && isMember(messId);
    allow read: if isManager(messId) || request.auth.uid == uid;
    allow update, delete: if isManager(messId) || request.auth.uid == uid;
  }
  match /activityLog/{entryId} {
    allow read: if isMember(messId);
    allow write: if false; // written only by the Cloud Function trigger, using the Admin SDK, which bypasses rules
  }
  match /{document=**} {
    allow read, write: if isMember(messId);
  }
}

function isMember(messId) {
  return request.auth != null &&
         exists(/databases/$(database)/documents/messes/$(messId)/members/$(request.auth.uid));
}
function isManager(messId) {
  return isMember(messId) &&
         get(/databases/$(database)/documents/messes/$(messId)/members/$(request.auth.uid)).data.role == 'manager';
}
```

This is a sketch, not final — whoever implements it should test it against the Firestore emulator with the actual query patterns the app uses (particularly: does the app ever need a collection-group query for anything legitimate? If not, keep the `match /{document=**}` block scoped as above and it naturally can't leak across messes, since `isMember(messId)` is evaluated per-path). This closes the collection-group leak from the earlier review as a side effect of doing Feature 1 properly — call that out to whoever reviews this as a security win, not just a feature add.

---

## 7. What "done" looks like for this pass

- New Android project: Kotlin, Jetpack Compose, MVVM, Room (local cache) + Firestore (source of truth) + Firebase Auth (real email/password under a username-mapped scheme) + FCM (push for approvals).
- `mess-ledger-apk`'s WebView-based `MainActivity` is retired, not extended.
- Existing Firestore data (messes, members, meals, guestMeals, contributions, expenses) is preserved and readable by the new app — no data loss during the transition. Existing members will need a one-time step to create a real account and get linked to their existing membership row (design this explicitly: e.g. on first login after the update, prompt "Is this you?" against existing member names in a mess they were invited into, or handle it via the invite-link/join-request flow treating existing members as needing to "claim" their spot — pick one and document it, don't leave it implicit).
- Mess IDs migrated to the new 8+ character format per section 4a.
- Firestore rules rewritten per section 6 and tested against the emulator.
- Features 1–4 fully working end to end, features 5 and settlement-redesign untouched.
