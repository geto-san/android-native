# Walkthrough: Build Fix & Navigation Refinement

I have resolved the compilation errors in `SyncScheduler.kt` and refined the app's onboarding and logout routing logic.

## Changes Made

### 1. Build Fix
- **[SyncScheduler.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/core/sync/SyncScheduler.kt)**: Removed all leftover references to `ClaimSyncWorker` following the removal of the Claims feature. This resolved the "Inapplicable candidate" errors for `PeriodicWorkRequestBuilder`.

### 2. Onboarding State Tracking
- **[NEW] [UserDataRepository.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/core/data/user/UserDataRepository.kt)**: Implemented a new repository using `DataStore` to track whether the user has seen the welcome screen.
- **[RepositoryModule.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/core/di/RepositoryModule.kt)**: Bound the new `UserDataRepository` for dependency injection.

### 3. Navigation & Routing
- **[WildWatchNavHost.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/ui/nav/WildWatchNavHost.kt)**:
    - Updated routing logic to check both `currentUser` (auth state) and `shouldShowWelcomeScreen` (onboarding state).
    - If a user is not logged in:
        - If they haven't seen the welcome screen, they are routed to `Welcome`.
        - If they *have* already seen it (e.g., after logout or app restart), they are routed directly to the **Login** screen.
    - Updated `WelcomeScreen` callbacks to dismiss the welcome screen state upon proceeding.

### 4. UI Fixes
- **[WelcomeScreen.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/feature/welcome/WelcomeScreen.kt)**: Added the missing `Cream` import to fix build errors in the custom logo/text layout.

## Verification Results

### Automated Tests
- Ran `./gradlew :app:compileDebugKotlin` - **SUCCESS**.

### Manual Verification Path
1. **First Open**: The `WelcomeScreen` appears.
2. **Proceed**: Click "Get Started" or "Log In".
3. **App Restart**: Close and reopen the app while signed out. You should now go straight to the **Login** screen.
4. **Logout**: Click Logout from within the app. You should be taken to the **Login** screen instead of the Welcome screen.
