# Implementation Plan: App Icon & Logo Redesign (Strava Inspiration)

This plan covers scrapping the current branding assets and designing a new logo and app icon for "WildWatch" from scratch, using the Strava app icon's bold, geometric aesthetic as inspiration.

## Goal
Create a modern, minimalist, and bold logo based on the "W" in WildWatch, using geometric shapes (chevrons/triangles) similar to the Strava logo.

## Design Concept
- **The "W" Mark**: Composed of two bold, geometric chevrons (triangles without bases) of different sizes.
- **Large Chevron**: Represents the "Wild" (Mountain peaks/Nature).
- **Small Chevron**: Represents the "Watch" (Focus/Lens/Precision).
- **Style**: High contrast, bold geometric lines, no complex gradients or borders.

## Proposed Changes

### [Component] Core UI Design System

#### [MODIFY] [Surfaces.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/core/ui/component/Surfaces.kt)
- Redefine `WildWatchLogoMark` to draw the new geometric "W" using a custom `Canvas` or `Path`.
- Remove the dependency on `Icons.Filled.Eco`.

#### [MODIFY] [Color.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/core/ui/theme/Color.kt)
- Define a primary brand color for the new logo if `SunsetAmber` or `ForestGreen` needs adjustment for high contrast (e.g., a vibrant "Wild" Orange or a deep "Watch" Green).

---

### [Component] Android Resources (Icons)

#### [MODIFY] [ic_launcher_foreground.xml](file:///home/geto/Projects/Github/android-native/app/src/main/res/drawable/ic_launcher_foreground.xml)
- Replace the current path data with the new geometric "W" design.

#### [MODIFY] [ic_launcher_background.xml](file:///home/geto/Projects/Github/android-native/app/src/main/res/drawable/ic_launcher_background.xml)
- Simplify the background to a solid color (e.g., `ForestGreen` or `PureBlack`) to make the foreground "W" pop.

#### [DELETE] [Old Icon Assets]
- Remove `drawable-xxxhdpi/ic_launcher_foreground.png` and any other legacy `.png` launcher icons to ensure the vector version is used.

---

### [Component] UI Screens

#### [MODIFY] [WelcomeScreen.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/feature/welcome/WelcomeScreen.kt)
- Update the layout to accommodate the new bold logo mark.

## Verification Plan

### Automated Tests
- Render Compose previews for the new `WildWatchLogoMark` with different sizes.
- Verify the build with `./gradlew :app:assembleDebug`.

### Manual Verification
- Deploy to the device and check the home screen icon.
- Verify the logo appearance on the Welcome and Auth screens.
- Check the icon's legibility at small sizes (e.g., in the app switcher).
