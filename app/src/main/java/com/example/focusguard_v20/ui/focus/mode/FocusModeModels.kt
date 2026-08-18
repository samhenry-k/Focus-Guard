package com.example.focusguard_v20.ui.focus.mode

enum class FocusBlockMode {
    BlockAllExceptSelected,
    BlockOnlySelected,
}

data class FocusAutoWindow(
    val title: String,
    val fromMinutesOfDay: Int,
    val toMinutesOfDay: Int,
    val enabled: Boolean,
)

