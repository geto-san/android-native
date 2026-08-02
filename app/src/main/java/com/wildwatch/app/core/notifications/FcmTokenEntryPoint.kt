package com.wildwatch.app.core.notifications

import android.content.Context
import com.wildwatch.app.WildWatchApplication
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FcmTokenEntryPoint {
    fun fcmTokenRepository(): FcmTokenRepository
}

fun Context.fcmTokenRepository(): FcmTokenRepository =
    EntryPointAccessors.fromApplication(
        applicationContext as WildWatchApplication,
        FcmTokenEntryPoint::class.java,
    ).fcmTokenRepository()
