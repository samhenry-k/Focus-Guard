package com.example.focusguard_v20.focus.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class ClockViewModel(
    private val repo: ClockRepository = ClockRepository(),
) : ViewModel() {
    val settings: StateFlow<ClockSettings> =
        repo.observeSettings()
            .catch { emit(ClockSettings()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClockSettings())

    fun set24Hour(enabled: Boolean) {
        repo.updateSettings(settings.value.copy(use24Hour = enabled))
    }

    fun setTimezoneIdOrNull(tzId: String?) {
        repo.updateSettings(settings.value.copy(timezoneId = tzId?.takeIf { it.isNotBlank() }))
    }
}

