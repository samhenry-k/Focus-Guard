package com.example.focusguard_v20.focus.timer

import android.util.Log
import com.example.focusguard_v20.data.FirebaseUserSession
import com.google.firebase.firestore.FirebaseFirestore

class FocusSessionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val session: FirebaseUserSession = FirebaseUserSession(),
) {
    fun recordFocusSession(
        startAtMs: Long,
        endAtMs: Long,
        durationSec: Long,
    ) {
        val uid = session.uidOrNull() ?: return
        val doc =
            mapOf(
                "startAt" to startAtMs,
                "endAt" to endAtMs,
                "durationSec" to durationSec,
                "mode" to "focus",
            )
        firestore.collection("users")
            .document(uid)
            .collection("focusSessions")
            .add(doc)
            .addOnFailureListener { e ->
                Log.e("FocusSessionRepository", "Failed to write focus session", e)
            }
    }
}

