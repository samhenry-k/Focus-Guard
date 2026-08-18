package com.example.focusguard_v20.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DashboardTotals(
    val totalFocusSeconds: Long,
    val blockedAppsCount: Int,
    val lastError: String? = null,
)

class DashboardRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val session: FirebaseUserSession = FirebaseUserSession(),
) {
    fun observeTotals(): Flow<DashboardTotals> = callbackFlow {
        val uid = session.uidOrNull()
        if (uid.isNullOrBlank()) {
            trySend(DashboardTotals(totalFocusSeconds = 0L, blockedAppsCount = 0, lastError = "Not signed in")).isSuccess
            close()
            return@callbackFlow
        }

        val userDoc = firestore.collection("users").document(uid)

        var totalFocusSeconds: Long = 0L
        var blockedAppsCount: Int = 0
        var lastError: String? = null

        fun emit() {
            trySend(
                DashboardTotals(
                    totalFocusSeconds = totalFocusSeconds,
                    blockedAppsCount = blockedAppsCount,
                    lastError = lastError,
                ),
            ).isSuccess
        }

        val sessionsListener =
            userDoc.collection("focusSessions")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        lastError = e.localizedMessage ?: e.javaClass.simpleName
                        Log.e("DashboardRepository", "focusSessions listener error", e)
                        emit()
                        return@addSnapshotListener
                    }
                    totalFocusSeconds =
                        snapshot?.documents
                            ?.sumOf { (it.getLong("durationSec") ?: 0L) }
                            ?: 0L
                    lastError = null
                    emit()
                }

        val blockedListener =
            userDoc.collection("blockedApps")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        lastError = e.localizedMessage ?: e.javaClass.simpleName
                        Log.e("DashboardRepository", "blockedApps listener error", e)
                        emit()
                        return@addSnapshotListener
                    }
                    blockedAppsCount = snapshot?.size() ?: 0
                    lastError = null
                    emit()
                }

        emit()

        awaitClose {
            sessionsListener.remove()
            blockedListener.remove()
        }
    }
}

