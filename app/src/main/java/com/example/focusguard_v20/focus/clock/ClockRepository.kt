package com.example.focusguard_v20.focus.clock

import com.example.focusguard_v20.data.FirebaseUserSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ClockSettings(
    val use24Hour: Boolean = true,
    val timezoneId: String? = null, // null means local device timezone
)

class ClockRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val session: FirebaseUserSession = FirebaseUserSession(),
) {
    fun observeSettings(): Flow<ClockSettings> = callbackFlow {
        val uid = session.uidOrNull()
        if (uid.isNullOrBlank()) {
            trySend(ClockSettings()).isSuccess
            close()
            return@callbackFlow
        }

        val reg =
            firestore.collection("users")
                .document(uid)
                .addSnapshotListener { snap, _ ->
                    val use24 = snap?.getBoolean("timeFormat24") ?: true
                    val tz = snap?.getString("timezoneId")
                    trySend(ClockSettings(use24Hour = use24, timezoneId = tz)).isSuccess
                }
        awaitClose { reg.remove() }
    }

    fun updateSettings(settings: ClockSettings) {
        val uid = session.uidOrNull() ?: return
        firestore.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "timeFormat24" to settings.use24Hour,
                    "timezoneId" to settings.timezoneId,
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )
    }
}

