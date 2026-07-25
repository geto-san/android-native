package com.wildwatch.app.core.data.claim

import com.google.firebase.firestore.FirebaseFirestore
import com.wildwatch.app.core.model.Claim
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val CLAIMS_COLLECTION = "claims"

@Singleton
class ClaimRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ClaimRemoteDataSource {

    override suspend fun upsert(claim: Claim): Result<Unit> = runCatching {
        firestore.collection(CLAIMS_COLLECTION)
            .document(claim.id)
            .set(claim.toFirestoreMap())
            .await()
    }
}
