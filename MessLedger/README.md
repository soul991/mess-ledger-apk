# Mess Ledger v2 — Native Android App

A modern, native Android rebuild of Mess Ledger built with Kotlin, Jetpack Compose, Material 3, Room, MVVM Architecture, and Firebase.

---

## 🌟 Key Architecture & Features

### 1. Real User Accounts (Feature 1)
- Platform-wide user accounts separate from mess membership.
- Live username availability checking with debouncing.
- Live password criteria checklist (8+ chars, letter, number).
- Under-the-hood synthetic email mapping (`{username}@messledger.internal`) for Firebase Auth.
- Account persistence across app launches with real logout.

### 2. Manager Role (Feature 2)
- Mess creator becomes the initial **Manager**.
- Managers have exclusive permissions:
  - Approve / Reject Join Requests
  - Approve / Reject Leave Requests
  - Edit Mess Name & Expense Categories
  - Soft-delete members
  - Two-step Manager Role Transfer to another member
- Manager badge displayed alongside member names.

### 3. Invitations & Request Flows (Feature 3)
- Safe, hard-to-guess Mess IDs (8+ chars from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789#*-_`).
- Deep linking support (`messledger://invite/{messId}`).
- Join request flow: members request to join, manager approves/declines.
- Leave request flow: members request to leave, manager approves/declines.
- Native Android share sheet integration for sharing invite codes and links.

### 4. Full Activity Log (Feature 4)
- Server-side append-only activity feed via Firebase Cloud Functions.
- Chronological timeline tracking member joins/leaves, manager transfers, expenses, contributions, guest meals, and settings changes.
- Paginated activity feed with day-grouped items and action icons.

### 5. Data & Local Caching
- **Room Database** for offline-capable fast caching of messes and members.
- **Firebase Firestore** as realtime source of truth.
- **Firebase Cloud Messaging (FCM)** for manager approval and role transfer push notifications.
- **Firestore Security Rules** fully rewritten to enforce strict membership and manager role checks.

---

## 🚀 Getting Started

### 1. Firebase Android Configuration
1. Open the [Firebase Console](https://console.firebase.google.com/) for project `mess--ledger`.
2. Add an Android App with package name `com.messledger.app`.
3. Download the generated `google-services.json` and place it in the `MessLedger/app/` folder (replacing the placeholder).
4. In the Firebase Authentication tab, ensure **Email/Password** provider is enabled.

### 2. Opening in Android Studio
1. Open Android Studio (Hedgehog 2023.1.1 or newer).
2. Select **Open** and choose the `/Users/istakahamed06/Desktop/Mess Ledger/MessLedger` directory.
3. Allow Gradle to sync dependencies.
4. Run the app on an Android device or emulator running API 26+ (Android 8.0+).

### 3. Deploying Cloud Functions & Rules
```bash
# Navigate to functions folder
cd MessLedger/functions
npm install
npm run build

# Deploy Cloud Functions
firebase deploy --only functions

# Deploy Firestore Security Rules
cd ..
firebase deploy --only firestore:rules
```

### 4. Running the Mess ID Migration Script (One-Time)
To migrate existing 6-character mess IDs to 8+ character IDs:
```bash
cd MessLedger/functions
# Dry-run mode
npx ts-node src/migration.ts
```
