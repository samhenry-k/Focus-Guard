package com.example.focusguard_v20.ui.focus.mode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FocusModeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Actions.EndOneTimeSession -> {
                val pkgs = FocusModePrefs.sessionBlockedPackages(context)
                FocusModeBlocking.clearBlocking(context, pkgs)
            }

            Actions.AutoStart -> {
                val enabled = FocusModePrefs.autoEnabled(context)
                if (!enabled) return
                val selected = FocusModePrefs.selectedPackages(context)
                val mode = FocusModePrefs.blockMode(context)
                FocusModeBlocking.applyBlocking(context, selected, mode)
            }

            Actions.AutoEnd -> {
                val enabled = FocusModePrefs.autoEnabled(context)
                if (!enabled) return
                // Conservative: only undo what the user selected (avoids unexpectedly unblocking other “always blocked” rules).
                val selected = FocusModePrefs.selectedPackages(context)
                when (FocusModePrefs.blockMode(context)) {
                    FocusBlockMode.BlockOnlySelected -> FocusModeBlocking.clearBlocking(context, selected)
                    FocusBlockMode.BlockAllExceptSelected -> {
                        // We blocked "all except selected". Undo by unblocking everything we can see.
                        val all = FocusModeBlocking.launcherPackages(context).toSet()
                        FocusModeBlocking.clearBlocking(context, all)
                    }
                }
            }
        }
    }

    object Actions {
        const val EndOneTimeSession = "com.example.focusguard_v20.focusmode.END_ONE_TIME"
        const val AutoStart = "com.example.focusguard_v20.focusmode.AUTO_START"
        const val AutoEnd = "com.example.focusguard_v20.focusmode.AUTO_END"
    }
}

