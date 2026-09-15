package com.silversentry.sentry.core.database

enum class Park {
    BWINDI_IMPENETRABLE,
    MGAHINGA_GORILLA,
    MURCHISON_FALLS,
    QUEEN_ELIZABETH,
    KIBALE,
    KIDEPO_VALLEY,
    RWENZORI_MOUNTAINS,
    MOUNT_ELGON,
    LAKE_MBURO,
    SEMULIKI,
    ;

    // "BWINDI_IMPENETRABLE" -> "Bwindi Impenetrable", for UI rows that need a
    // human-readable park label without a server round-trip.
    val displayName: String
        get() = name.lowercase().split('_').joinToString(" ") { part ->
            part.replaceFirstChar { it.uppercase() }
        }

    companion object {
        // "bwindi-impenetrable" -> BWINDI_IMPENETRABLE, matching the Firestore slug the
        // portal writes into custom claims (park_id). Returns null for slugs outside the
        // known set rather than defaulting to a wrong park.
        fun fromFirestoreId(firestoreId: String?): Park? =
            firestoreId
                ?.replace('-', '_')
                ?.uppercase()
                ?.let { slug -> entries.find { it.name == slug } }
    }
}
