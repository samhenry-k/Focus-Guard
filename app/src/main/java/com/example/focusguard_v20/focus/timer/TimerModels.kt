package com.example.focusguard_v20.focus.timer

enum class PomodoroPhase {
    Focus,
    Break,
}

data class TimerConfig(
    val focusSeconds: Long = 25 * 60L,
    val breakSeconds: Long = 5 * 60L,
    val autoRepeat: Boolean = false,
)

data class TimerState(
    val isRunning: Boolean,
    val phase: PomodoroPhase,
    val remainingSeconds: Long,
    val config: TimerConfig,
) {
    companion object {
        fun initial(config: TimerConfig = TimerConfig()): TimerState =
            TimerState(
                isRunning = false,
                phase = PomodoroPhase.Focus,
                remainingSeconds = config.focusSeconds,
                config = config,
            )
    }
}

