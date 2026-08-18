package com.example.focusguard_v20.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusguard_v20.data.DashboardRepository
import com.example.focusguard_v20.data.DashboardTotals
import com.example.focusguard_v20.focus.timer.PomodoroPhase
import com.example.focusguard_v20.focus.timer.TimerEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val repo: DashboardRepository = DashboardRepository(),
) : ViewModel() {
    val totals: StateFlow<DashboardTotals> =
        repo.observeTotals()
            .catch { emit(DashboardTotals(totalFocusSeconds = 0L, blockedAppsCount = 0, lastError = it.localizedMessage)) }
            .combine(TimerEngine.state) { stored, timerState ->
                val liveExtra =
                    if (timerState.isRunning && timerState.phase == PomodoroPhase.Focus) {
                        val elapsed = TimerEngine.currentPhaseElapsedSecondsOrZero()
                        val configured = timerState.config.focusSeconds
                        elapsed.coerceIn(0L, configured)
                    } else {
                        0L
                    }

                stored.copy(totalFocusSeconds = stored.totalFocusSeconds + liveExtra)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardTotals(totalFocusSeconds = 0L, blockedAppsCount = 0),
            )
}

