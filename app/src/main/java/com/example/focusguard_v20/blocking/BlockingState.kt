package com.example.focusguard_v20.blocking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Process-wide blocking state cache.
 * Updated by UI and/or services; read by AccessibilityService.
 */
object BlockingState {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _firestoreBlockedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _permanentBlockedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _sessionBlockedPackages = MutableStateFlow<Set<String>>(emptySet())

    val blockedPackages: StateFlow<Set<String>> = combine(
        _firestoreBlockedPackages,
        _permanentBlockedPackages,
        _sessionBlockedPackages
    ) { firestore, permanent, session ->
        firestore + permanent + session
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptySet()
    )

    fun setFirestoreBlockedPackages(packages: Set<String>) {
        _firestoreBlockedPackages.value = packages
    }

    fun setPermanentBlockedPackages(packages: Set<String>) {
        _permanentBlockedPackages.value = packages
    }

    fun setSessionBlockedPackages(packages: Set<String>) {
        _sessionBlockedPackages.value = packages
    }
    
    // Legacy support to avoid breaking existing code immediately
    fun setLocalBlockedPackages(packages: Set<String>) {
        setSessionBlockedPackages(packages)
    }
}
