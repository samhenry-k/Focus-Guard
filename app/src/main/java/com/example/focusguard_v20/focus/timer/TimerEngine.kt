package com.example.focusguard_v20.focus.timer

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide state holder updated by [TimerService].
 * Keeps UI simple (no binder), while the service owns the clock.
 */
object TimerEngine {
    private val _state = MutableStateFlow(TimerState.initial())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var endElapsedRealtimeMs: Long? = null
    private var phaseStartElapsedRealtimeMs: Long? = null

    fun setConfig(config: TimerConfig) {
        val current = _state.value
        val newRemaining =
            when (current.phase) {
                PomodoroPhase.Focus -> config.focusSeconds
                PomodoroPhase.Break -> config.breakSeconds
            }
        _state.value = current.copy(config = config, remainingSeconds = newRemaining, isRunning = false)
        endElapsedRealtimeMs = null
    }

    fun start() {
        val current = _state.value
        if (current.isRunning) return
        if (phaseStartElapsedRealtimeMs == null) {
            phaseStartElapsedRealtimeMs = SystemClock.elapsedRealtime()
        }
        endElapsedRealtimeMs = SystemClock.elapsedRealtime() + current.remainingSeconds * 1000L
        _state.value = current.copy(isRunning = true)
    }

    fun pause() {
        val current = _state.value
        if (!current.isRunning) return
        tick() // compute accurate remaining
        endElapsedRealtimeMs = null
        _state.value = _state.value.copy(isRunning = false)
    }

    fun reset() {
        val current = _state.value
        endElapsedRealtimeMs = null
        phaseStartElapsedRealtimeMs = null
        val remaining =
            when (current.phase) {
                PomodoroPhase.Focus -> current.config.focusSeconds
                PomodoroPhase.Break -> current.config.breakSeconds
            }
        _state.value = current.copy(isRunning = false, remainingSeconds = remaining)
    }

    fun currentPhaseElapsedSecondsOrZero(): Long {
        val current = _state.value
        if (!current.isRunning) return 0L
        val start = phaseStartElapsedRealtimeMs ?: return 0L
        val elapsed = (SystemClock.elapsedRealtime() - start) / 1000L
        return elapsed.coerceAtLeast(0L)
    }

    fun tick(): Boolean {
        val current = _state.value
        if (!current.isRunning) return false
        val endMs = endElapsedRealtimeMs ?: return false
        val remainingMs = endMs - SystemClock.elapsedRealtime()
        val remainingSec = (remainingMs / 1000L).coerceAtLeast(0L)
        if (remainingSec == current.remainingSeconds) return false

        if (remainingSec > 0L) {
            _state.value = current.copy(remainingSeconds = remainingSec)
            return true
        }

        // phase transition
        val nextPhase =
            when (current.phase) {
                PomodoroPhase.Focus -> PomodoroPhase.Break
                PomodoroPhase.Break -> PomodoroPhase.Focus
            }
        val nextRemaining =
            when (nextPhase) {
                PomodoroPhase.Focus -> current.config.focusSeconds
                PomodoroPhase.Break -> current.config.breakSeconds
            }

        val shouldContinue = current.config.autoRepeat || current.phase == PomodoroPhase.Focus
        // Default behavior: run focus->break automatically, stop after break unless autoRepeat.
        endElapsedRealtimeMs = if (shouldContinue) SystemClock.elapsedRealtime() + nextRemaining * 1000L else null
        phaseStartElapsedRealtimeMs = if (shouldContinue) SystemClock.elapsedRealtime() else null
        _state.value =
            current.copy(
                phase = nextPhase,
                remainingSeconds = nextRemaining,
                isRunning = shouldContinue,
            )
        return true
    }
}

