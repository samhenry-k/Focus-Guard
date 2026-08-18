package com.example.focusguard_v20.ui.focus.mode

import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class FocusInstalledApp(
    val packageName: String,
    val label: String,
)

@Composable
fun FocusModeScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var tab by remember { mutableIntStateOf(0) }

    var apps by remember { mutableStateOf<List<FocusInstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(FocusModePrefs.selectedPackages(context)) }

    var sessionMinutesText by remember { mutableStateOf("") }
    var blockMode by remember { mutableStateOf(FocusModePrefs.blockMode(context)) }

    // Session tracking
    var sessionEndTime by remember { mutableLongStateOf(FocusModePrefs.sessionEndTime(context)) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isSessionActive = remember(sessionEndTime, currentTime) { sessionEndTime > currentTime }

    LaunchedEffect(isSessionActive) {
        if (isSessionActive) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                currentTime = System.currentTimeMillis()
                // Refresh sessionEndTime from Prefs in case it was stopped/changed externally
                val savedEndTime = FocusModePrefs.sessionEndTime(context)
                if (savedEndTime != sessionEndTime) {
                    sessionEndTime = savedEndTime
                }
                if (sessionEndTime <= currentTime) break
            }
        }
    }

    // Auto focus UI state
    var autoTitle by remember { mutableStateOf(FocusModePrefs.autoWindowOrNull(context)?.title.orEmpty()) }
    var autoFrom by remember { mutableIntStateOf(FocusModePrefs.autoWindowOrNull(context)?.fromMinutesOfDay ?: 9 * 60) }
    var autoTo by remember { mutableIntStateOf(FocusModePrefs.autoWindowOrNull(context)?.toMinutesOfDay ?: 17 * 60) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val pkgs = FocusModeBlocking.launcherPackages(context)
        apps =
            pkgs.map { pkg ->
                val label =
                    runCatching {
                        val ai = pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                        pm.getApplicationLabel(ai)?.toString()
                    }.getOrNull().orEmpty().ifBlank { pkg }
                FocusInstalledApp(pkg, label)
            }.sortedBy { it.label.lowercase() }
    }

    val filtered =
        remember(apps, query) {
            val q = query.trim().lowercase()
            if (q.isBlank()) apps
            else apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Select apps") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Start focus time") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Setup auto focus") })
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .then(
                    if (tab != 0) Modifier.verticalScroll(scrollState)
                    else Modifier
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                0 -> {
                    SelectAppsTab(
                        query = query,
                        onQueryChange = { query = it },
                        apps = filtered,
                        selected = selected,
                        onToggle = { pkg ->
                            selected =
                                if (selected.contains(pkg)) selected - pkg
                                else selected + pkg
                            FocusModePrefs.setSelectedPackages(context, selected)
                        },
                    )
                }

                1 -> {
                    if (isSessionActive) {
                        ActiveSessionTab(
                            endTime = sessionEndTime,
                            currentTime = currentTime,
                            onStop = {
                                val pkgs = FocusModePrefs.sessionBlockedPackages(context)
                                FocusModeBlocking.clearBlocking(context, pkgs)
                                FocusModePrefs.clearOneTimeSession(context)
                                sessionEndTime = 0
                                currentTime = System.currentTimeMillis()
                            }
                        )
                    } else {
                        StartFocusTimeTab(
                            selectedCount = selected.size,
                            sessionMinutesText = sessionMinutesText,
                            onMinutesChange = { sessionMinutesText = it.filter(Char::isDigit).take(4) },
                            blockMode = blockMode,
                            onBlockModeChange = {
                                blockMode = it
                                FocusModePrefs.setBlockMode(context, it)
                            },
                            onStart = {
                                if (!com.example.focusguard_v20.blocking.PermissionChecks.hasUsageAccess(context)) {
                                    Toast.makeText(context, "Please enable Usage Access in Settings", Toast.LENGTH_LONG).show()
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    return@StartFocusTimeTab
                                }
                                if (!com.example.focusguard_v20.blocking.PermissionChecks.isAccessibilityServiceEnabled(context, com.example.focusguard_v20.blocking.BlockingAccessibilityService::class.java)) {
                                    Toast.makeText(context, "Please enable Accessibility Service in Settings", Toast.LENGTH_LONG).show()
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    return@StartFocusTimeTab
                                }

                                val minutes = sessionMinutesText.toIntOrNull()?.coerceAtLeast(1) ?: 0
                                if (minutes <= 0) {
                                    Toast.makeText(context, "Please enter minutes", Toast.LENGTH_SHORT).show()
                                    return@StartFocusTimeTab
                                }

                                // Apply now
                                FocusModeBlocking.applyBlocking(context, selected, blockMode)

                                val endTime = System.currentTimeMillis() + (minutes * 60_000L)
                                FocusModePrefs.setSessionEndTime(context, endTime)
                                sessionEndTime = endTime

                                FocusModeScheduler.scheduleEndOfOneTimeSession(context, minutes)

                                Toast.makeText(context, "Focus session started for $minutes minutes", Toast.LENGTH_SHORT).show()
                                sessionMinutesText = ""
                            },
                        )
                    }
                }

                else -> {
                    AutoFocusTab(
                        selectedCount = selected.size,
                        title = autoTitle,
                        onTitleChange = { autoTitle = it },
                        fromMinutesOfDay = autoFrom,
                        toMinutesOfDay = autoTo,
                        onFromChange = { autoFrom = it },
                        onToChange = { autoTo = it },
                        onStart = {
                            if (!com.example.focusguard_v20.blocking.PermissionChecks.hasUsageAccess(context)) {
                                Toast.makeText(context, "Please enable Usage Access in Settings", Toast.LENGTH_LONG).show()
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                return@AutoFocusTab
                            }
                            if (!com.example.focusguard_v20.blocking.PermissionChecks.isAccessibilityServiceEnabled(context, com.example.focusguard_v20.blocking.BlockingAccessibilityService::class.java)) {
                                Toast.makeText(context, "Please enable Accessibility Service in Settings", Toast.LENGTH_LONG).show()
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                return@AutoFocusTab
                            }

                            val window =
                                FocusAutoWindow(
                                    title = autoTitle.trim(),
                                    fromMinutesOfDay = autoFrom,
                                    toMinutesOfDay = autoTo,
                                    enabled = true,
                                )
                            FocusModePrefs.setAutoWindow(context, window)
                            FocusModeScheduler.scheduleDailyAutoWindow(context, autoFrom, autoTo)
                            Toast.makeText(context, "Auto focus scheduled", Toast.LENGTH_SHORT).show()
                            tab = 0
                        },
                    )
                }
            }
            if (tab != 0) {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ActiveSessionTab(
    endTime: Long,
    currentTime: Long,
    onStop: () -> Unit,
) {
    val remainingMs = (endTime - currentTime).coerceAtLeast(0)
    val minutes = remainingMs / 60_000
    val seconds = (remainingMs % 60_000) / 1000

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Focus Session Active", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("remaining", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStop,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Stop Focus Mode")
        }
    }
}

@Composable
private fun SelectAppsTab(
    query: String,
    onQueryChange: (String) -> Unit,
    apps: List<FocusInstalledApp>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Select apps",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Choose apps for Focus Mode.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search apps") },
            singleLine = true,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Selected: ${selected.size}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "These selections are used in the next tabs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                val isChecked = selected.contains(app.packageName)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    onClick = { onToggle(app.packageName) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggle(app.packageName) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartFocusTimeTab(
    selectedCount: Int,
    sessionMinutesText: String,
    onMinutesChange: (String) -> Unit,
    blockMode: FocusBlockMode,
    onBlockModeChange: (FocusBlockMode) -> Unit,
    onStart: () -> Unit,
) {
    Text("Start focus time", style = MaterialTheme.typography.titleMedium)
    Text(
        "How long do you want the session to last?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = sessionMinutesText,
        onValueChange = onMinutesChange,
        label = { Text("Minutes") },
        singleLine = true,
    )

    Spacer(Modifier.height(4.dp))
    Text("Blocking option", style = MaterialTheme.typography.titleSmall)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = blockMode == FocusBlockMode.BlockAllExceptSelected,
                    onClick = { onBlockModeChange(FocusBlockMode.BlockAllExceptSelected) },
                )
                Text("Block all apps except selected")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = blockMode == FocusBlockMode.BlockOnlySelected,
                    onClick = { onBlockModeChange(FocusBlockMode.BlockOnlySelected) },
                )
                Text("Block only selected apps")
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Selected apps: $selectedCount", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Session will end automatically and unblock apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onStart,
        enabled = selectedCount > 0,
    ) {
        Text("Start apps to block")
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AutoFocusTab(
    selectedCount: Int,
    title: String,
    onTitleChange: (String) -> Unit,
    fromMinutesOfDay: Int,
    toMinutesOfDay: Int,
    onFromChange: (Int) -> Unit,
    onToChange: (Int) -> Unit,
    onStart: () -> Unit,
) {
    Text("Setup auto focus", style = MaterialTheme.typography.titleMedium)
    Text(
        "Specific auto focus hours",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Title") },
        singleLine = true,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("From", style = MaterialTheme.typography.titleSmall)
            TimeOfDayPickerField(
                label = "From time",
                minutesOfDay = fromMinutesOfDay,
                onChange = onFromChange,
            )
            Text("To", style = MaterialTheme.typography.titleSmall)
            TimeOfDayPickerField(
                label = "To time",
                minutesOfDay = toMinutesOfDay,
                onChange = onToChange,
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Selected apps: $selectedCount", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Auto focus will block apps every day in this window.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onStart,
        enabled = selectedCount > 0,
    ) {
        Text("Start apps to block")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = { onTitleChange("") },
        ) {
            Text("Clear title")
        }
    }
}

@Composable
private fun TimeOfDayPickerField(
    label: String,
    minutesOfDay: Int,
    onChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val hour24 = (minutesOfDay / 60).coerceIn(0, 23)
    val minute = (minutesOfDay % 60).coerceIn(0, 59)

    val display = remember(minutesOfDay) { formatMinutesOfDay(minutesOfDay) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = display,
            onValueChange = {},
            readOnly = true,
            enabled = false, // prevents TextField from consuming taps
            label = { Text(label) },
            singleLine = true,
            supportingText = { Text("Tap to select time") },
            trailingIcon = {
                Text(
                    if (hour24 < 12) "AM" else "PM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        // Full-size click target on top of the disabled field.
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clickable {
                        TimePickerDialog(
                            context,
                            { _, h, m -> onChange(h * 60 + m) },
                            hour24,
                            minute,
                            false,
                        ).show()
                    },
        )
    }
}

private fun formatMinutesOfDay(minutesOfDay: Int): String {
    val h24 = (minutesOfDay / 60).coerceIn(0, 23)
    val m = (minutesOfDay % 60).coerceIn(0, 59)
    val am = h24 < 12
    val h12 = when (val h = h24 % 12) {
        0 -> 12
        else -> h
    }
    val mm = m.toString().padStart(2, '0')
    return "$h12:$mm ${if (am) "AM" else "PM"}"
}

