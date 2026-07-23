package com.silverback.sentry.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The Wildwatch wireframe pairs 'Plus Jakarta Sans' (headings) with 'Inter' (body)
// via Google Fonts. No font files are bundled in this project yet, so headings use
// the platform sans-serif at the wireframe's weights/sizes as a faithful stand-in —
// drop matching .ttf files into res/font and swap FontFamily.Default for a
// GoogleFont/downloadable FontFamily later without touching call sites.
val Typography = Typography(
    displaySmall = TextStyle( // splash / hero titles (text-4xl font-extrabold)
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle( // screen-level titles (text-2xl font-extrabold)
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle( // ScreenHeader title (text-xl font-bold)
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle( // card/section titles (text-sm font-bold)
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle( // subtitle/meta text (text-xs)
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelMedium = TextStyle( // pill/badge text (text-[10-11px] font-semibold)
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
    ),
)