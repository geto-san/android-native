package com.silverback.sentry.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

// Confirms the Phase 1 Hilt graph (FirebaseModule, DispatchersModule, DataStoreModule)
// actually resolves end to end in an instrumented context, not just compiles. Real
// repository injection tests (with fakes standing in for these) land in Phase 4+.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltInjectionSmokeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var firebaseFirestore: FirebaseFirestore

    @Inject
    lateinit var firebaseStorage: FirebaseStorage

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Inject
    lateinit var preferencesDataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun allPhase1DependenciesInject() {
        assertNotNull(firebaseAuth)
        assertNotNull(firebaseFirestore)
        assertNotNull(firebaseStorage)
        assertNotNull(ioDispatcher)
        assertNotNull(defaultDispatcher)
        assertNotNull(preferencesDataStore)
    }
}
