package com.wildwatch.app.core.database

// Which gradient/category-pill treatment the Feed card uses - kept as a
// presentation-only enum rather than storing raw color values in the
// database, so theming stays a UI concern.
enum class ArticleTheme {
    FOREST,
    SUNSET,
    SKY,
    WILDLIFE,
    SECURITY
}
