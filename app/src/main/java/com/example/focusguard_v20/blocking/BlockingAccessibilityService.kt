package com.example.focusguard_v20.blocking

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.util.Log
import com.example.focusguard_v20.ui.focus.greyscale.GreyscaleController
import com.example.focusguard_v20.ui.focus.greyscale.GreyscaleMode
import com.example.focusguard_v20.ui.focus.greyscale.GreyscalePrefs
import com.example.focusguard_v20.ui.focus.keywords.KeywordEraserPrefs
import com.example.focusguard_v20.ui.focus.reels.ReelsBlockerPrefs
import com.example.focusguard_v20.ui.focus.mode.FocusModePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BlockingAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var syncJob: Job? = null
    private var enforceJob: Job? = null
    private val repo by lazy { BlockingRepository(this) }

    private var overlayView: FrameLayout? = null
    private val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    private var rules: List<BlockedAppRule> = emptyList()
    private var currentPackage: String? = null
    private var trackedPackage: String? = null
    private var trackedBaseUsedMsAtEnter: Long = 0L
    private var trackedEnterElapsedRealtimeMs: Long = 0L
    private var lastReelsBlockTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Initialize session blocked packages (Focus Mode)
        val sessionPkgs = FocusModePrefs.sessionBlockedPackages(this)
        BlockingState.setLocalBlockedPackages(sessionPkgs)

        startSync()
        startEnforcementLoop()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        
        // Allowed event types for our features
        val allowedTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                           AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or 
                           AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                           
        if ((type and allowedTypes) == 0) return
            
        val pkg = event.packageName?.toString() ?: return
        currentPackage = pkg
        
        if (pkg == packageName) {
            hideOverlay()
            return
        }

        // Handle Reels Blocking immediately on any Instagram change
        if (pkg == "com.instagram.android") {
            checkAndBlockReels()
        }

        // Apply Greyscale immediately on app switch
        applyGreyscaleIfNeeded()

        // Handle Keyword Eraser
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            checkAndEraseKeywords(event)
        }

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (!PermissionChecks.hasUsageAccess(this)) {
                Log.w("BlockingService", "Usage access not granted; cannot enforce limits")
                showOverlay("Enable Usage Access for FocusGuard")
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }

            val blockedSet = BlockingState.blockedPackages.value
            if (blockedSet.contains(pkg)) {
                showOverlay("Blocked app")
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }
        }
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        syncJob = null
        stopEnforcementLoop()
        hideOverlay()
    }

    private fun startSync() {
        if (syncJob?.isActive == true) return
        syncJob =
            scope.launch {
                repo.observeBlockedApps().collect { list ->
                    rules = list
                    BlockingState.setFirestoreBlockedPackages(list.filter { it.isBlocked }.map { it.packageName }.toSet())
                }
            }
    }

    private fun startEnforcementLoop() {
        if (enforceJob?.isActive == true) return
        enforceJob =
            scope.launch {
                while (isActive) {
                    if (!PermissionChecks.hasUsageAccess(this@BlockingAccessibilityService)) {
                        delay(2_000L)
                        continue
                    }

                    val foreground = resolveForegroundPackage() ?: run {
                        delay(1_000L)
                        continue
                    }
                    currentPackage = foreground

                    if (foreground == packageName) {
                        hideOverlay()
                        resetTrackedPackage()
                        delay(1_000L)
                        continue
                    }

                    val blockedSet = BlockingState.blockedPackages.value
                    if (blockedSet.isNotEmpty() && blockedSet.contains(foreground)) {
                        Log.d("BlockingService", "Blocking $foreground because it is in blockedSet")
                        showOverlay("Focus Mode Active")
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        resetTrackedPackage()
                        delay(1_000L)
                        continue
                    }

                    val rule = rules.firstOrNull { it.packageName == foreground }
                    if (rule == null || rule.allowedMinutesPerDay <= 0) {
                        hideOverlay()
                        resetTrackedPackage()
                        delay(1_000L)
                        continue
                    }

                    // Track when user first opened the app, so we can block without waiting for UsageStats refresh.
                    if (trackedPackage != foreground) {
                        trackedPackage = foreground
                        trackedBaseUsedMsAtEnter = usedTodayMs(foreground)
                        trackedEnterElapsedRealtimeMs = SystemClock.elapsedRealtime()
                    }

                    val allowedMs = rule.allowedMinutesPerDay * 60_000L
                    val liveForegroundMs = (SystemClock.elapsedRealtime() - trackedEnterElapsedRealtimeMs).coerceAtLeast(0L)
                    val usedMs = (trackedBaseUsedMsAtEnter + liveForegroundMs).coerceAtLeast(0L)

                    if (usedMs >= allowedMs) {
                        repo.setBlocked(foreground, true)
                        showOverlay("Time limit exceeded")
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        resetTrackedPackage()
                        delay(1_000L)
                        continue
                    } else {
                        hideOverlay()
                    }

                    // Handle Greyscale
                    applyGreyscaleIfNeeded()

                    // Handle Reels Blocking
                    checkAndBlockReels()

                    delay(1_000L)
                }
            }
    }

    private fun applyGreyscaleIfNeeded() {
        val mode = GreyscalePrefs.getMode(this)
        val foreground = currentPackage ?: return
        val selected = GreyscalePrefs.getSelectedPackages(this)

        val shouldBeGreyscale = when (mode) {
            GreyscaleMode.Off -> false
            GreyscaleMode.Everywhere -> true
            GreyscaleMode.OnlySelected -> selected.contains(foreground)
            GreyscaleMode.EverywhereExceptSelected -> {
                // Don't greyscale our own app or launcher if not selected, or just generic check
                !selected.contains(foreground) && foreground != packageName
            }
        }

        val isActive = GreyscaleController.isGreyscaleActive(this)
        if (shouldBeGreyscale != isActive) {
            GreyscaleController.setGreyscaleActive(this, shouldBeGreyscale)
        }
    }

    private fun checkAndBlockReels() {
        if (!ReelsBlockerPrefs.isEnabled(this)) return
        
        val pkg = currentPackage ?: return
        if (pkg != "com.instagram.android") return

        val now = SystemClock.elapsedRealtime()
        if (now - lastReelsBlockTime < 2000) return 

        val rootNode = rootInActiveWindow ?: return
        // Double check we are still in Instagram
        if (rootNode.packageName != "com.instagram.android") return

        // 1. Identify the state of the bottom navigation tabs.
        // We look for 'isSelected' and 'isVisibleToUser' to ensure we only block when the Reels tab is ACTIVE.
        val reelsTabActive = isTabActive(rootNode, "com.instagram.android:id/reels_tab") || 
                             isTabActive(rootNode, "com.instagram.android:id/clips_tab")

        // 2. Identify the Full-Screen Immersive Viewer (Pager).
        // This is the component used for swiping through Reels vertically.
        // It's present in the dedicated Reels tab AND when opening a Reel from Home/Search.
        val immersiveViewerIds = listOf(
            "com.instagram.android:id/reels_viewer_pager",
            "com.instagram.android:id/clips_viewer_pager",
            "com.instagram.android:id/reels_viewer_container"
        )
        val hasImmersivePager = immersiveViewerIds.any { id ->
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            val active = nodes.any { it.isVisibleToUser }
            nodes.forEach { it.recycle() }
            active
        }

        // 3. Identify "Safe" areas where we should NOT block.
        // If the user is clearly on the Home, Search, or Profile tab, we stay quiet.
        val onHomeTab = isTabActive(rootNode, "com.instagram.android:id/home_tab")
        val onSearchTab = isTabActive(rootNode, "com.instagram.android:id/search_tab")
        val onProfileTab = isTabActive(rootNode, "com.instagram.android:id/profile_tab")
        val onExploreTab = isTabActive(rootNode, "com.instagram.android:id/explore_tab")
        
        val isExplicitlyOnSafeTab = onHomeTab || onSearchTab || onProfileTab || onExploreTab

        // DECISION LOGIC:
        // - BLOCK if the Reels tab is selected.
        // - BLOCK if the immersive full-screen viewer is visible (even if we think we're on Home).
        // - DO NOT BLOCK if we are on a safe tab AND the immersive pager is not visible.
        // This ensures the blocker doesn't trigger on Home feed previews or the Explore grid.
        
        val shouldBlock = (reelsTabActive || hasImmersivePager) && !(isExplicitlyOnSafeTab && !hasImmersivePager)

        if (shouldBlock) {
            lastReelsBlockTime = now
            Log.d("BlockingService", "Instagram Reels Blocked: Tab=$reelsTabActive, Immersive=$hasImmersivePager, SafeTab=$isExplicitlyOnSafeTab")
            showOverlay("Reels are restricted")
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun checkAndEraseKeywords(event: AccessibilityEvent) {
        val keywords = KeywordEraserPrefs.getKeywords(this)
        if (keywords.isEmpty()) return

        // 1. Check the event source node
        event.source?.let { source ->
            try {
                processNodeForKeywords(source, keywords)
            } finally {
                source.recycle()
            }
        }

        // 2. Also check the currently focused input field for redundancy (helps in apps like Google/Chrome)
        rootInActiveWindow?.let { root ->
            try {
                root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)?.let { focusedNode ->
                    try {
                        processNodeForKeywords(focusedNode, keywords)
                    } finally {
                        focusedNode.recycle()
                    }
                }
            } finally {
                root.recycle()
            }
        }
    }

    private fun processNodeForKeywords(node: android.view.accessibility.AccessibilityNodeInfo, keywords: Set<String>) {
        if (!node.isEditable) return
        
        val text = node.text?.toString() ?: ""
        if (text.isEmpty()) return

        for (keyword in keywords) {
            if (text.contains(keyword, ignoreCase = true)) {
                val newText = text.replace(keyword, "", ignoreCase = true)
                val arguments = android.os.Bundle()
                arguments.putCharSequence(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                    newText
                )
                node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                Log.d("BlockingService", "Erased keyword '$keyword' from node. New text length: ${newText.length}")
            }
        }
    }

    private fun isTabActive(root: android.view.accessibility.AccessibilityNodeInfo, viewId: String): Boolean {
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        val active = nodes.any { it.isSelected && it.isVisibleToUser }
        nodes.forEach { it.recycle() }
        return active
    }

    private fun stopEnforcementLoop() {
        enforceJob?.cancel()
        enforceJob = null
        resetTrackedPackage()
    }

    private fun resetTrackedPackage() {
        trackedPackage = null
        trackedBaseUsedMsAtEnter = 0L
        trackedEnterElapsedRealtimeMs = 0L
    }

    private fun usedTodayMs(packageName: String): Long {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000L)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
        val item = stats.firstOrNull { it.packageName == packageName } ?: return 0L
        return if (Build.VERSION.SDK_INT >= 29) item.totalTimeVisible else item.totalTimeInForeground
    }

    private fun resolveForegroundPackage(): String? {
        // Prefer the last package we saw via Accessibility events.
        val fromA11y = currentPackage
        if (!fromA11y.isNullOrBlank()) return fromA11y

        // Fallback: infer from UsageEvents.
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 10_000L, now)
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND || (Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.ACTIVITY_RESUMED)) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun showOverlay(message: String) {
        if (overlayView != null) return
        val root = FrameLayout(this)
        root.setBackgroundColor(0xCC000000.toInt())
        val tv = TextView(this)
        tv.text = message
        tv.textSize = 18f
        tv.setTextColor(0xFFFFFFFF.toInt())
        tv.setPadding(40, 40, 40, 40)
        val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.CENTER
        root.addView(tv, lp)

        val wlp =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT,
            )
        wlp.gravity = Gravity.TOP or Gravity.START
        wm.addView(root, wlp)
        overlayView = root

        scope.launch {
            delay(1_000L)
            hideOverlay()
        }
    }

    private fun hideOverlay() {
        val v = overlayView ?: return
        overlayView = null
        runCatching { wm.removeView(v) }
    }
}

