package com.example.focusguard_v20.blocking

data class BlockedAppRule(
    val packageName: String,
    val appName: String,
    val allowedMinutesPerDay: Int,
    val isBlocked: Boolean,
)

