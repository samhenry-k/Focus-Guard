package com.example.focusguard_v20.ui.focus.mode

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.example.focusguard_v20.blocking.BlockingRepository
import com.example.focusguard_v20.blocking.BlockingState

internal object FocusModeBlocking {
    fun launcherPackages(context: Context): List<String> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves: List<ResolveInfo> =
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

        return resolves
            .asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo?.packageName }
            .filter { it.isNotBlank() && it != context.packageName && !isDefaultLauncher(context, it) }
            .distinct()
            .sorted()
            .toList()
    }

    private fun isDefaultLauncher(context: Context, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultLauncher = res?.activityInfo?.packageName

        val launcherIntents = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val allLaunchers = context.packageManager.queryIntentActivities(launcherIntents, 0)
        val isAnyLauncher = allLaunchers.any { it.activityInfo.packageName == packageName }

        return packageName == defaultLauncher || isAnyLauncher
    }

    fun applyBlocking(
        context: Context,
        selectedPackages: Set<String>,
        mode: FocusBlockMode,
    ) {
        val repo = BlockingRepository()
        val toBlockPackages = mutableSetOf<String>()

        when (mode) {
            FocusBlockMode.BlockOnlySelected -> {
                toBlockPackages.addAll(selectedPackages)
                selectedPackages.forEach { pkg ->
                    repo.setBlocked(pkg, true)
                }
            }

            FocusBlockMode.BlockAllExceptSelected -> {
                val all = launcherPackages(context).toSet()
                // IMPORTANT: The selected packages should NOT be blocked.
                // We block EVERYTHING in 'all' that is NOT in 'selectedPackages'.
                val toBlock = (all - selectedPackages)
                val toUnblock = (all intersect selectedPackages)

                toBlockPackages.addAll(toBlock)

                // Update Firestore via repo
                toBlock.forEach { pkg -> repo.setBlocked(pkg, true) }
                toUnblock.forEach { pkg -> repo.setBlocked(pkg, false) }
            }
        }

        // Local fallback: Update BlockingState immediately
        FocusModePrefs.setOneTimeSessionBlockedPackages(context, toBlockPackages)
        BlockingState.setLocalBlockedPackages(toBlockPackages)
    }

    fun clearBlocking(
        context: Context,
        packagesToUnblock: Set<String>,
    ) {
        val repo = BlockingRepository()
        packagesToUnblock.forEach { pkg -> repo.setBlocked(pkg, false) }

        // Local fallback: Clear BlockingState immediately
        FocusModePrefs.clearOneTimeSession(context)
        BlockingState.setLocalBlockedPackages(emptySet())
    }
}

