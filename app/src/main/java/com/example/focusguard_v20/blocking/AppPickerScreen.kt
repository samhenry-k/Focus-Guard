package com.example.focusguard_v20.blocking

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val iconBitmap: android.graphics.Bitmap?,
)

@Composable
fun AppPickerScreen(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val repo = remember(context) { BlockingRepository(context) }
    val blockedRules by repo.observeBlockedApps().collectAsStateWithLifecycle(initialValue = emptyList())
    val blockedSet = remember(blockedRules) { blockedRules.associateBy { it.packageName } }

    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps =
            withContext(Dispatchers.Default) {
                // Android 11+ package visibility: use LAUNCHER apps list so it works without QUERY_ALL_PACKAGES.
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

                resolves
                    .asSequence()
                    .mapNotNull { ri ->
                        val ai = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
                        val packageName = ai.packageName ?: return@mapNotNull null
                        if (packageName == context.packageName) return@mapNotNull null
                        val label = ri.loadLabel(pm)?.toString() ?: packageName
                        val icon = runCatching { ri.loadIcon(pm).toBitmap(width = 96, height = 96) }.getOrNull()
                        InstalledApp(packageName, label, icon)
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
                    .toList()
            }
    }

    val filtered =
        remember(apps, query) {
            val q = query.trim().lowercase()
            if (q.isBlank()) apps
            else apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Block apps", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Select an app to block it immediately. Unselected apps are not blocked.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        var hasUsage by remember { mutableStateOf(PermissionChecks.hasUsageAccess(context)) }
        var hasA11y by remember { mutableStateOf(PermissionChecks.isAccessibilityServiceEnabled(context, BlockingAccessibilityService::class.java)) }
        LaunchedEffect(Unit) {
            // Refresh once when screen opens.
            hasUsage = PermissionChecks.hasUsageAccess(context)
            hasA11y = PermissionChecks.isAccessibilityServiceEnabled(context, BlockingAccessibilityService::class.java)
        }
        if (!hasUsage || !hasA11y) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Required permissions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Usage Access: ${if (hasUsage) "Enabled" else "Disabled"}\nAccessibility: ${if (hasA11y) "Enabled" else "Disabled"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                            Text("Enable usage")
                        }
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                            Text("Enable accessibility")
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it },
            label = { Text("Search apps") },
            singleLine = true,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filtered, key = { it.packageName }) { app ->
                val existing = blockedSet[app.packageName]
                AppRow(
                    app = app,
                    existingRule = existing,
                    onSave = { minutes ->
                        repo.upsertRule(app.packageName, app.label, minutes)
                    },
                    onUnblock = {
                        repo.setBlocked(app.packageName, false)
                    },
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDone,
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    existingRule: BlockedAppRule?,
    onSave: (Int) -> Unit,
    onUnblock: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.height(40.dp),
                    )
                } else {
                    Spacer(Modifier.height(40.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (existingRule?.isBlocked == true) {
                            onUnblock()
                        } else {
                            onSave(0) // 0 means blocked permanently
                        }
                    },
                ) {
                    Text(if (existingRule?.isBlocked == true) "Unblock" else "Block")
                }
            }
        }
    }
}

