package com.wildwatch.app.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val MOTION_DURATION_MS = 300

/**
 * Material motion patterns for Navigation Compose.
 * Forward drill-down uses shared-axis horizontal slide; auth ↔ main uses fade-through.
 */
object NavMotion {
    fun forwardEnter(): EnterTransition =
        slideInHorizontally(tween(MOTION_DURATION_MS)) { fullWidth -> fullWidth / 4 } +
            fadeIn(tween(MOTION_DURATION_MS))

    fun forwardExit(): ExitTransition =
        slideOutHorizontally(tween(MOTION_DURATION_MS)) { fullWidth -> -fullWidth / 4 } +
            fadeOut(tween(MOTION_DURATION_MS))

    fun backwardEnter(): EnterTransition =
        slideInHorizontally(tween(MOTION_DURATION_MS)) { fullWidth -> -fullWidth / 4 } +
            fadeIn(tween(MOTION_DURATION_MS))

    fun backwardExit(): ExitTransition =
        slideOutHorizontally(tween(MOTION_DURATION_MS)) { fullWidth -> fullWidth / 4 } +
            fadeOut(tween(MOTION_DURATION_MS))

    fun fadeThroughEnter(): EnterTransition = fadeIn(tween(MOTION_DURATION_MS))

    fun fadeThroughExit(): ExitTransition = fadeOut(tween(MOTION_DURATION_MS))
}
