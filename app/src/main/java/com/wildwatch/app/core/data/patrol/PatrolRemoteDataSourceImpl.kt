package com.wildwatch.app.core.data.patrol

import com.google.firebase.firestore.FirebaseFirestore
import com.wildwatch.app.core.model.PatrolLog
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val PATROL_LOGS_COLLECTION = "patrol_logs"

@Singleton
class PatrolRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PatrolRemoteDataSource {

    override suspend fun upsert(patrolLog: PatrolLog): Result<Unit> = runCatching {
        firestore.collection(PATROL_LOGS_COLLECTION)
            .document(patrolLog.id)
            .set(patrolLog.toFirestoreMap())
            .await()
    }
}
