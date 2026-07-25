# Implementation Plan: UI Polish & Redesign

This plan covers committing the recent work, fixing the `HomeScreen` header alignment, and redesigning the `WelcomeScreen` for a nature-inspired minimalist aesthetic using the "Magfio" font.

## User Review Required

> [!IMPORTANT]
> **Magfio Font**: I could not find a "Magfio" font file (`.ttf`/`.otf`) in the project. Please ensure the font file is added to `app/src/main/res/font/`. I will prepare the code to use it once available.

## Proposed Changes

### [Component] Version Control
#### [ACTION] Commit & Push
- Stage all changes (UI enrichment, project cleanup, build fixes).
- Commit with message: "Enrich UI screens, remove claims feature, and fix build/routing logic".
- Push to `origin/master`.

---

### [Component] Home Feature
#### [MODIFY] [HomeScreen.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/feature/dashboard/HomeScreen.kt)
- Fix the header (TopAppBar) being "too low" by adjusting the `Scaffold`'s `contentWindowInsets`.
- Ensure the inner `Scaffold` does not double-pad the top if the `MainTabShell` or system is already handling it.

---

### [Component] Welcome Feature
#### [MODIFY] [WelcomeScreen.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/feature/welcome/WelcomeScreen.kt)
- **Nature Feel**: Use a subtle green/earth-tone gradient or a nature-inspired background (e.g., using `NiaGradientBackground` pattern).
- **Minimalist Design**: Simplify the layout, removing the redundant logo/text inside the button.
- **Typography**: Apply the "Magfio" font to the "WildWatch" logo/word. I will define a `FontFamily` in `Type.kt` for this.

---

### [Component] Core Design System
#### [MODIFY] [Type.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/core/ui/theme/Type.kt)
- Define `MagfioFontFamily` (linked to `res/font/magfio.ttf`).
- Create a specific `TextStyle` for the logo/branding.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the build.
- Render Compose previews for `WelcomeScreen` and `HomeScreen`.

### Manual Verification
- Deploy to device and verify the `WelcomeScreen` aesthetics.
- Check the `HomeScreen` header position relative to the status bar.
- Confirm that the app starts at `Welcome` on first run and `Login` thereafter.
