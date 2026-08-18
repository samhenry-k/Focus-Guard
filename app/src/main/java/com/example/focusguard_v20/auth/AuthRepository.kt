package com.example.focusguard_v20.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser).isSuccess }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser).isSuccess
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun signUp(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: IllegalStateException("Sign up failed")))
            }
    }

    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: IllegalStateException("Login failed")))
            }
    }

    fun logout() {
        auth.signOut()
    }
}

