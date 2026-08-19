## Firebase setup (per developer)

`google-services.json` is gitignored and must never be committed. Each developer
downloads their own copy from the Firebase console (Project Settings → your Android
app → google-services.json) and places it at `MessLedger/app/google-services.json`
before building.

**Note for whoever has console access:** the API key that was previously committed
(`AIzaSyAKvSnnnp2VO0PHs6Lt6XizQ2u6DH0HUvE`, project `mess--ledger`) is public in this
repo's git history and needs to be restricted in Google Cloud Console → APIs &
Credentials (restrict to package `com.messledger.app` + your SHA-1 fingerprints), or
the Android app registration should be deleted and recreated in Firebase for a clean
key. This is a manual console action — it can't be done by editing files in this repo.
