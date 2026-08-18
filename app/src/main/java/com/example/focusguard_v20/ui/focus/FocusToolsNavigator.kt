package com.example.focusguard_v20.ui.focus

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.focusguard_v20.blocking.AppPickerScreen
import com.example.focusguard_v20.ui.focus.greyscale.GreyscaleScreen
import com.example.focusguard_v20.ui.focus.keywords.KeywordEraserScreen
import com.example.focusguard_v20.ui.focus.mode.FocusModeScreen
import com.example.focusguard_v20.ui.focus.reels.ReelsBlockerScreen
import androidx.compose.material3.HorizontalDivider as Divider

private object FocusRoutes {
    const val Menu = "menu"
    const val FocusMode = "focus_mode"
    const val BlockApps = "block_apps"
    const val Greyscale = "greyscale"
    const val ReelsBlocker = "reels_blocker"
    const val KeywordEraser = "keyword_eraser"
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FocusToolsNavigator(
    contentPadding: PaddingValues,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FocusRoutes.Menu,
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        composable(FocusRoutes.Menu) {
            FocusToolsMenu(
                onOpenFocusMode = { navController.navigate(FocusRoutes.FocusMode) },
                onOpenBlockApps = { navController.navigate(FocusRoutes.BlockApps) },
                onOpenGreyscale = { navController.navigate(FocusRoutes.Greyscale) },
                onOpenReelsBlocker = { navController.navigate(FocusRoutes.ReelsBlocker) },
                onOpenKeywordEraser = { navController.navigate(FocusRoutes.KeywordEraser) },
            )
        }

        composable(FocusRoutes.FocusMode) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Focus mode") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { inner ->
                FocusModeScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(16.dp),
                )
            }
        }

        composable(FocusRoutes.BlockApps) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Block apps") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { inner ->
                AppPickerScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    onDone = { navController.popBackStack() },
                )
            }
        }

        composable(FocusRoutes.Greyscale) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Greyscale filter") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { inner ->
                GreyscaleScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    onDone = { navController.popBackStack() },
                )
            }
        }

        composable(FocusRoutes.ReelsBlocker) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Reels blocker") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { inner ->
                ReelsBlockerScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                )
            }
        }

        composable(FocusRoutes.KeywordEraser) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Keyword eraser") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Back")
                            }
                        },
                    )
                },
            ) { inner ->
                KeywordEraserScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                )
            }
        }
    }
}

@Composable
private fun FocusToolsMenu(
    onOpenFocusMode: () -> Unit,
    onOpenBlockApps: () -> Unit,
    onOpenGreyscale: () -> Unit,
    onOpenReelsBlocker: () -> Unit,
    onOpenKeywordEraser: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Focus", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Choose a tool to open.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        MenuCard(
            title = "Focus mode",
            subtitle = "Select apps, start a session, or schedule auto focus",
            icon = { Icon(Icons.Filled.TimerOff, contentDescription = null) },
            onClick = onOpenFocusMode,
        )

        MenuCard(
            title = "Block apps",
            subtitle = "Set limits and block distractions",
            icon = { Icon(Icons.Filled.DoNotDisturbOn, contentDescription = null) },
            onClick = onOpenBlockApps,
        )

        MenuCard(
            title = "Greyscale filter",
            subtitle = "Reduce phone addiction with greyscale",
            icon = { Icon(Icons.Filled.FilterBAndW, contentDescription = null) },
            onClick = onOpenGreyscale,
        )

        MenuCard(
            title = "Reels blocker",
            subtitle = "Block mindless scrolling on Instagram",
            icon = { Icon(Icons.Filled.Block, contentDescription = null) },
            onClick = onOpenReelsBlocker,
        )

        MenuCard(
            title = "Keyword eraser",
            subtitle = "Auto-delete specific words as you type",
            icon = { Icon(Icons.Filled.Translate, contentDescription = null) },
            onClick = onOpenKeywordEraser,
        )
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        onClick = onClick,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun ForceLandscapeWhileVisible() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val previous = activity.requestedOrientation

    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity.requestedOrientation = previous
        }
    }
}
