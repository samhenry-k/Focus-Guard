package com.example.focusguard_v20.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val email: String) : AuthState
}

class AuthViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repo = AuthRepository()

    private val _showWelcomeAfterSignup = MutableStateFlow(false)
    val showWelcomeAfterSignup: StateFlow<Boolean> = _showWelcomeAfterSignup.asStateFlow()

    val authState: StateFlow<AuthState> =
        repo.authState
            .map { user ->
                val email = user?.email
                if (email.isNullOrBlank()) AuthState.LoggedOut else AuthState.LoggedIn(email)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    fun logout() {
        _showWelcomeAfterSignup.value = false
        repo.logout()
    }

    fun signUp(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        repo.signUp(email.trim(), password) { result ->
            if (result.isSuccess) _showWelcomeAfterSignup.value = true
            onResult(result)
        }
    }

    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        _showWelcomeAfterSignup.value = false
        repo.login(email.trim(), password, onResult)
    }

    fun currentEmailOrNull(): String? = repo.currentUser?.email

    fun consumeWelcomeAfterSignup() {
        _showWelcomeAfterSignup.value = false
    }
}

