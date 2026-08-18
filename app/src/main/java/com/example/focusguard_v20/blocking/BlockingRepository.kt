package com.example.focusguard_v20.blocking

import android.content.Context
import android.util.Log
import com.example.focusguard_v20.data.FirebaseUserSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

class BlockingRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val session: FirebaseUserSession = FirebaseUserSession(),
) {
    private val prefs by lazy {
        context?.getSharedPreferences("blocking_prefs", Context.MODE_PRIVATE)
    }

    private fun observeLocalPermanentBlocked(): Flow<Set<String>> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "permanent_blocked") {
                val set = p.getStringSet(key, emptySet()) ?: emptySet()
                trySend(set).isSuccess
            }
        }
        prefs?.registerOnSharedPreferenceChangeListener(listener)
        // Initial value
        val initial = prefs?.getStringSet("permanent_blocked", emptySet()) ?: emptySet()
        trySend(initial).isSuccess
        
        awaitClose { 
            prefs?.unregisterOnSharedPreferenceChangeListener(listener) 
        }
    }.onStart {
        val initial = prefs?.getStringSet("permanent_blocked", emptySet()) ?: emptySet()
        emit(initial)
    }

    fun observeBlockedApps(): Flow<List<BlockedAppRule>> {
        val uid = session.uidOrNull()
        
        val firestoreFlow = if (!uid.isNullOrBlank()) {
            callbackFlow {
                val reg = firestore.collection("users")
                    .document(uid)
                    .collection("blockedApps")
                    .addSnapshotListener { snap, e ->
                        if (e != null) {
                            Log.e("BlockingRepository", "Firestore error", e)
                            trySend(emptyList<BlockedAppRule>()).isSuccess
                            return@addSnapshotListener
                        }
                        val items = snap?.documents.orEmpty().map { d ->
                            BlockedAppRule(
                                packageName = d.id,
                                appName = d.getString("appName") ?: d.id,
                                allowedMinutesPerDay = (d.getLong("allowedMinutesPerDay") ?: 0L).toInt(),
                                isBlocked = d.getBoolean("isBlocked") ?: false,
                            )
                        }
                        trySend(items).isSuccess
                    }
                awaitClose { reg.remove() }
            }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }

        return combine(observeLocalPermanentBlocked(), firestoreFlow) { local, remote ->
            val localRules = local.map { BlockedAppRule(it, it, 0, true) }
            (localRules + remote).distinctBy { it.packageName }
        }
    }

    fun upsertRule(packageName: String, appName: String, allowedMinutesPerDay: Int, isBlocked: Boolean = true) {
        // Save locally for persistence and offline support
        if (isBlocked && allowedMinutesPerDay == 0) {
            val current = prefs?.getStringSet("permanent_blocked", emptySet()) ?: emptySet()
            prefs?.edit()?.putStringSet("permanent_blocked", current + packageName)?.apply()
            BlockingState.setLocalBlockedPackages(current + packageName)
        }

        val uid = session.uidOrNull() ?: return
        firestore.collection("users")
            .document(uid)
            .collection("blockedApps")
            .document(packageName)
            .set(
                mapOf(
                    "appName" to appName,
                    "allowedMinutesPerDay" to allowedMinutesPerDay,
                    "isBlocked" to isBlocked,
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            )
    }

    fun setBlocked(packageName: String, blocked: Boolean, appName: String? = null) {
        val current = prefs?.getStringSet("permanent_blocked", emptySet()) ?: emptySet()
        if (blocked) {
            prefs?.edit()?.putStringSet("permanent_blocked", current + packageName)?.apply()
            BlockingState.setLocalBlockedPackages(current + packageName)
        } else {
            prefs?.edit()?.putStringSet("permanent_blocked", current - packageName)?.apply()
            BlockingState.setLocalBlockedPackages(current - packageName)
        }

        val uid = session.uidOrNull() ?: return
        val data = mutableMapOf<String, Any>(
            "isBlocked" to blocked,
            "updatedAt" to System.currentTimeMillis()
        )
        if (appName != null) {
            data["appName"] = appName
        }
        firestore.collection("users")
            .document(uid)
            .collection("blockedApps")
            .document(packageName)
            .set(data, SetOptions.merge())
    }
}
