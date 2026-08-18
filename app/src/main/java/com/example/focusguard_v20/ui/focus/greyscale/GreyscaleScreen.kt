package com.example.focusguard_v20.ui.focus.greyscale

import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.focusguard_v20.blocking.BlockingAccessibilityService
import com.example.focusguard_v20.blocking.PermissionChecks
import com.example.focusguard_v20.ui.focus.mode.FocusModeBlocking

private data class GreyscaleApp(
    val packageName: String,
    val label: String,
)

@Composable
fun GreyscaleScreen(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }

    var apps by remember { mutableStateOf<List<GreyscaleApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(GreyscalePrefs.getSelectedPackages(context)) }
    var currentMode by remember { mutableStateOf(GreyscalePrefs.getMode(context)) }
    var hasSecureSettings by remember { mutableStateOf(GreyscaleController.hasSecureSettingsPermission(context)) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(PermissionChecks.isAccessibilityServiceEnabled(context, BlockingAccessibilityService::class.java))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSecureSettings = GreyscaleController.hasSecureSettingsPermission(context)
                isAccessibilityEnabled = PermissionChecks.isAccessibilityServiceEnabled(context, BlockingAccessibilityService::class.java)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val pkgs = FocusModeBlocking.launcherPackages(context)
        apps = pkgs.map { pkg ->
            val label = runCatching {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            }.getOrDefault(pkg)
            GreyscaleApp(pkg, label)
        }.sortedBy { it.label.lowercase() }
    }

    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) apps
        else apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Select apps") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Automation Setup") })
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                0 -> {
                    GreyscaleSelectAppsTab(
                        query = query,
                        onQueryChange = { query = it },
                        apps = filtered,
                        selected = selectedPackages,
                        onToggle = { pkg ->
                            selectedPackages = if (selectedPackages.contains(pkg)) {
                                selectedPackages - pkg
                            } else {
                                selectedPackages + pkg
                            }
                        }
                    )
                }
                1 -> {
                    GreyscaleAutomationTab(
                        context = context,
                        mode = currentMode,
                        hasSecureSettings = hasSecureSettings,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onModeChange = { currentMode = it },
                        onSave = {
                            GreyscalePrefs.setMode(context, currentMode)
                            GreyscalePrefs.setSelectedPackages(context, selectedPackages)
                            Toast.makeText(context, "Greyscale settings saved", Toast.LENGTH_SHORT).show()
                            onDone()
                        },
                        onCancel = onDone,
                        onRequestAccessibility = {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GreyscaleSelectAppsTab(
    query: String,
    onQueryChange: (String) -> Unit,
    apps: List<GreyscaleApp>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search apps") },
            singleLine = true,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                val isChecked = selected.contains(app.packageName)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    onClick = { onToggle(app.packageName) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(checked = isChecked, onCheckedChange = { onToggle(app.packageName) })
                        Column {
                            Text(app.label, style = MaterialTheme.typography.titleSmall)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GreyscaleAutomationTab(
    context: android.content.Context,
    mode: GreyscaleMode,
    hasSecureSettings: Boolean,
    isAccessibilityEnabled: Boolean,
    onModeChange: (GreyscaleMode) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRequestAccessibility: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val adbCommand = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Automation Strategy", style = MaterialTheme.typography.titleMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasSecureSettings) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (hasSecureSettings) "✓ ADB Permission Granted" else "⚠ ADB Permission Missing",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (hasSecureSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (hasSecureSettings)
                        "Greyscale will be automatically toggled based on your rules."
                    else "To enable automatic greyscale, you must grant the WRITE_SECURE_SETTINGS permission via ADB.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!hasSecureSettings) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Enable via ADB:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = adbCommand,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(4.dp)
                    )
                    TextButton(onClick = { 
                        clipboardManager.setText(AnnotatedString(adbCommand))
                        Toast.makeText(context, "Command copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy Command")
                    }
                }
            }
        }

        if (!isAccessibilityEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Accessibility Required", style = MaterialTheme.typography.titleSmall)
                    Text("Needed to detect app switches and apply greyscale rules.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onRequestAccessibility) {
                        Text("Enable Accessibility")
                    }
                }
            }
        }

        Text("Automation Rules", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                GreyscaleOption(
                    text = "Apply to selected apps",
                    selected = mode == GreyscaleMode.OnlySelected,
                    onClick = { onModeChange(GreyscaleMode.OnlySelected) }
                )
                GreyscaleOption(
                    text = "Apply everywhere except selected apps",
                    selected = mode == GreyscaleMode.EverywhereExceptSelected,
                    onClick = { onModeChange(GreyscaleMode.EverywhereExceptSelected) }
                )
                GreyscaleOption(
                    text = "Always on",
                    selected = mode == GreyscaleMode.Everywhere,
                    onClick = { onModeChange(GreyscaleMode.Everywhere) }
                )
                GreyscaleOption(
                    text = "Always off",
                    selected = mode == GreyscaleMode.Off,
                    onClick = { onModeChange(GreyscaleMode.Off) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(modifier = Modifier.weight(1f), onClick = onSave) {
                Text("Save Settings")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onCancel,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun GreyscaleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, modifier = Modifier.padding(start = 8.dp))
    }
}
