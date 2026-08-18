package com.example.focusguard_v20.ui.focus.greyscale

import android.content.Context

enum class GreyscaleMode {
    Everywhere,
    OnlySelected,
    EverywhereExceptSelected,
    Off
}

internal object GreyscalePrefs {
    private const val Name = "greyscale_prefs"
    private const val KeyMode = "greyscale_mode"
    private const val KeySelectedPackages = "selected_packages"

    fun getMode(context: Context): GreyscaleMode {
        val raw = context.getSharedPreferences(Name, Context.MODE_PRIVATE).getString(KeyMode, GreyscaleMode.Off.name)
        return runCatching { GreyscaleMode.valueOf(raw!!) }.getOrDefault(GreyscaleMode.Off)
    }

    fun setMode(context: Context, mode: GreyscaleMode) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit().putString(KeyMode, mode.name).apply()
    }

    fun getSelectedPackages(context: Context): Set<String> {
        return context.getSharedPreferences(Name, Context.MODE_PRIVATE).getStringSet(KeySelectedPackages, emptySet()) ?: emptySet()
    }

    fun setSelectedPackages(context: Context, packages: Set<String>) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit().putStringSet(KeySelectedPackages, packages).apply()
    }
}
