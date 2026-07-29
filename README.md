# WildWatch — Backend

Server-side pieces for the WildWatch platform: Firestore/Storage security rules,
Cloud Functions (custom-claims assignment, on-write triggers, notification
routing), and rule-emulator tests. Kept in its own history from the Android
app on `master`.

## Layout (stands corrected)
```
firestore.rules
storage.rules
functions/          # Cloud Functions source (Node/TS)
firebase.json
.firebaserc
```
