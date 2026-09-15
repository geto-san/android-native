package com.silversentry.sentry.core.data.wipe

import com.silversentry.sentry.core.data.user.UserDataRepository
import com.silversentry.sentry.core.database.AppDatabase
import com.silversentry.sentry.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wipes every piece of on-device application state: the Room database (incidents, alerts,
 * articles, notifications, patrol logs - the offline-first source of truth) and the DataStore
 * preferences (theme choice, pending email-link address, pending anonymous-auth flag).
 *
 * Used by:
 *  - sign-out, so a different user starting a fresh session does not inherit the previous
 *    account's cached reports and notifications (wipe-on-signout);
 *  - the Profile "Reset local data" action, which clears the same state while keeping the
 *    current Firebase sign-in session.
 *
 * Firestore itself is never touched from here - remote data stays authoritative on the server
 * and is simply re-pulled by the realtime listeners once local state is gone.
 */
@Singleton
class LocalDataClearer @Inject constructor(
    private val database: AppDatabase,
    private val userDataRepository: UserDataRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun clearAllLocalData() = withContext(ioDispatcher) {
        database.clearAllTables()
        userDataRepository.setDarkThemeConfig(null)
        userDataRepository.setPendingEmailLinkAddress(null)
        userDataRepository.setPendingAnonymousAuth(false)
    }
}
