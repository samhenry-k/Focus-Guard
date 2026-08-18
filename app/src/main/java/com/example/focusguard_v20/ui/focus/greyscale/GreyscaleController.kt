package com.example.focusguard_v20.ui.focus.greyscale

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Controller for Greyscale mode using ADB-level permissions (WRITE_SECURE_SETTINGS).
 * This method is the most reliable for automation but requires a one-time setup via ADB.
 */
object GreyscaleController {
    private const val TAG = "GreyscaleController"

    /**
     * Toggles greyscale mode.
     * Requires: adb shell pm grant com.example.focusguard_v20 android.permission.WRITE_SECURE_SETTINGS
     */
    fun setGreyscaleActive(context: Context, active: Boolean): Boolean {
        val current = isGreyscaleActive(context)
        if (current == active) return true

        return try {
            val contentResolver = context.contentResolver
            val enabled = if (active) 1 else 0
            
            // accessibility_display_daltonizer_enabled: 1 to enable, 0 to disable
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", enabled)
            
            // accessibility_display_daltonizer: 0 is the mode for Grayscale/Monochromacy
            Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer", 0)
            
            Log.d(TAG, "Greyscale set to $active via Secure Settings (ADB)")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied. Please run: adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set greyscale: ${e.message}")
            false
        }
    }

    fun isGreyscaleActive(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "accessibility_display_daltonizer_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the app has the necessary permission to toggle greyscale.
     */
    fun hasSecureSettingsPermission(context: Context): Boolean {
        return try {
            // Test if we can write a value (using the current value)
            val current = isGreyscaleActive(context)
            val enabled = if (current) 1 else 0
            Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer_enabled", enabled)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
