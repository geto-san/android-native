# Implementation Plan: Navigation Refinement, Feed Enrichment, and Map Enhancements

This plan outlines the next steps for visual and functional enrichment, focusing on a cleaner navigation experience, a high-quality community feed, and advanced map features for rangers.

## Proposed Changes

### [Component] Navigation
Refine the bottom navigation to match modern industry standards (Now in Android style).

#### [MODIFY] [MainTabShell.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/ui/nav/MainTabShell.kt)
- **Remove Labels**: Update the bottom bar to show **only icons**, removing all text labels for a minimalist look.
- **Update Tabs**:
    - **Community**: Dashboard, Feed, You (Profile).
    - **Ranger**: Dashboard, Tracking, You (Profile).
- **Cleanup**: Remove `onOpenCommunityMap` and other unused callbacks related to the Community Map.

---

### [Component] Community Feed
Enrich the Feed feature with high-quality components and detailed views.

#### [MODIFY] [FeedScreen.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/feature/feed/FeedScreen.kt)
- **Design Update**: Use a modern, parameter-rich `ArticleCard` component (inspired by NIA's `NewsResourceCard`) with images, tags, and formatted metadata.
- **Organization**: Group articles by theme or date.

#### [NEW] [ArticleDetailScreen.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/feature/feed/ArticleDetailScreen.kt)
- Create a detailed view for articles, showing the full content, high-res images, and related actions (Like, Share).

#### [MODIFY] [WildWatchNavHost.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/ui/nav/WildWatchNavHost.kt)
- Add a new route `ArticleDetail(id: String)` to handle navigation to the detailed article view.
- Remove the `CommunityMap` route and screen.

---

### [Component] Ranger Map Experience
Enhance the `RangerMapScreen` with more advanced features and optimized performance.

#### [MODIFY] [RangerMapScreen.kt](file:///home/geto/Projects/Github/android-native/app/src/main/java/com/wildwatch/app/feature/tracking/RangerMapScreen.kt)
- **Advanced Overlays**: Add more specific park features (gates, water sources, ranger stations) using different icon sets.
- **Performance**: Use `remember` and `derivedStateOf` to ensure the map interactions remain fluid during high location update frequencies.

---

### [Component] Optimization
#### [ACTION] Performance Audit
- Review the entire codebase for performance bottlenecks, specifically focusing on:
    - Unnecessary recompositions in high-frequency UI (Maps, Feed).
    - Efficient use of `StateFlow` and `collectAsStateWithLifecycle`.
    - Optimizing image loading with Coil.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the build.
- Check navigation graph integrity.

### Manual Verification
- **Navigation**: Confirm bottom bar shows only icons and correctly switches between Dashboard, Feed/Tracking, and You.
- **Feed**: Click an article and verify it opens the detailed view.
- **Ranger Map**: Verify sidebar buttons and search bar functionality.
- **Performance**: Ensure smooth scrolling in the Feed and Map.
