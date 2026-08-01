package com.wildwatch.app.core.data.feed

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.wildwatch.app.core.model.Article
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val FEED_COLLECTION = "feed"

interface FeedRemoteDataSource {
    fun observeFeedChanges(): Flow<FeedRemoteChange>
}

data class FeedRemoteChange(
    val article: Article,
    val isRemoved: Boolean,
)

@Singleton
class FeedRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FeedRemoteDataSource {

    override fun observeFeedChanges(): Flow<FeedRemoteChange> = callbackFlow {
        val registration = firestore.collection(FEED_COLLECTION)
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Feed snapshot listener error")
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    val article = Article.fromFirestoreDocument(
                        documentId = change.document.id,
                        data = change.document.data,
                    )
                    trySend(
                        FeedRemoteChange(
                            article = article,
                            isRemoved = change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED,
                        ),
                    )
                }
            }
        awaitClose { registration.remove() }
    }
}
