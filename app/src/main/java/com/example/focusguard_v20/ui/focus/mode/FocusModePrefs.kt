package com.example.focusguard_v20.ui.focus.mode

import android.content.Context

internal object FocusModePrefs {
    private const val Name = "focus_mode_prefs"

    private const val KeySelectedPackages = "selected_packages"
    private const val KeyBlockMode = "block_mode"

    private const val KeyAutoEnabled = "auto_enabled"
    private const val KeyAutoTitle = "auto_title"
    private const val KeyAutoFrom = "auto_from_min_day"
    private const val KeyAutoTo = "auto_to_min_day"

    private const val KeySessionBlocked = "session_blocked_packages"
    private const val KeySessionEndTime = "session_end_time"

    fun selectedPackages(context: Context): Set<String> =
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).getStringSet(KeySelectedPackages, emptySet()).orEmpty()

    fun setSelectedPackages(context: Context, pkgs: Set<String>) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit().putStringSet(KeySelectedPackages, pkgs).apply()
    }

    fun blockMode(context: Context): FocusBlockMode {
        val raw =
            context.getSharedPreferences(Name, Context.MODE_PRIVATE).getString(KeyBlockMode, null)
                ?: FocusBlockMode.BlockAllExceptSelected.name
        return runCatching { FocusBlockMode.valueOf(raw) }.getOrDefault(FocusBlockMode.BlockAllExceptSelected)
    }

    fun setBlockMode(context: Context, mode: FocusBlockMode) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit().putString(KeyBlockMode, mode.name).apply()
    }

    fun autoEnabled(context: Context): Boolean =
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).getBoolean(KeyAutoEnabled, false)

    fun setAutoWindow(context: Context, window: FocusAutoWindow) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit()
            .putBoolean(KeyAutoEnabled, window.enabled)
            .putString(KeyAutoTitle, window.title)
            .putInt(KeyAutoFrom, window.fromMinutesOfDay)
            .putInt(KeyAutoTo, window.toMinutesOfDay)
            .apply()
    }

    fun autoWindowOrNull(context: Context): FocusAutoWindow? {
        val sp = context.getSharedPreferences(Name, Context.MODE_PRIVATE)
        val enabled = sp.getBoolean(KeyAutoEnabled, false)
        if (!enabled) return null
        val title = sp.getString(KeyAutoTitle, "")?.trim().orEmpty()
        val from = sp.getInt(KeyAutoFrom, 9 * 60)
        val to = sp.getInt(KeyAutoTo, 17 * 60)
        return FocusAutoWindow(title = title, fromMinutesOfDay = from, toMinutesOfDay = to, enabled = true)
    }

    fun setOneTimeSessionBlockedPackages(context: Context, pkgs: Set<String>) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit().putStringSet(KeySessionBlocked, pkgs).apply()
    }

    fun sessionBlockedPackages(context: Context): Set<String> =
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).getStringSet(KeySessionBlocked, emptySet()).orEmpty()

    fun clearOneTimeSession(context: Context) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit()
            .remove(KeySessionBlocked)
            .remove(KeySessionEndTime)
            .apply()
    }

    fun setSessionEndTime(context: Context, endTimeMillis: Long) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit().putLong(KeySessionEndTime, endTimeMillis).apply()
    }

    fun sessionEndTime(context: Context): Long =
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).getLong(KeySessionEndTime, 0L)
}

