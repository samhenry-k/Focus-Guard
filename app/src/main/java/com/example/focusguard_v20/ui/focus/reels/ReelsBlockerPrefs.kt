package com.example.focusguard_v20.ui.focus.reels

import android.content.Context
import android.content.SharedPreferences

object ReelsBlockerPrefs {
    private const val PREFS_NAME = "reels_blocker_prefs"
    private const val KEY_ENABLED = "reels_blocker_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
