package com.example.focusguard_v20.focus.clock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ClockScreen(
    modifier: Modifier = Modifier,
) {
    val vm: ClockViewModel = viewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var timezoneText by remember(settings.timezoneId) { mutableStateOf(settings.timezoneId.orEmpty()) }

    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(settings.timezoneId) {
        while (true) {
            val zone = settings.timezoneId?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
            now = ZonedDateTime.now(zone)
            kotlinx.coroutines.delay(1_000L)
        }
    }

    val timeFmt =
        if (settings.use24Hour) DateTimeFormatter.ofPattern("HH:mm:ss") else DateTimeFormatter.ofPattern("hh:mm:ss a")
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Clock mode", style = MaterialTheme.typography.titleMedium)
        Text(now.format(timeFmt), style = MaterialTheme.typography.headlineSmall)
        Text(now.format(dateFmt), style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("24-hour format", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = settings.use24Hour, onCheckedChange = { vm.set24Hour(it) })
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = timezoneText,
            onValueChange = { timezoneText = it },
            label = { Text("Timezone (optional, e.g. Asia/Kolkata)") },
            singleLine = true,
        )

        LaunchedEffect(timezoneText) {
            // Commit changes shortly after user edits.
            kotlinx.coroutines.delay(600L)
            val trimmed = timezoneText.trim()
            if (trimmed.isBlank()) {
                vm.setTimezoneIdOrNull(null)
            } else {
                runCatching { ZoneId.of(trimmed) }.onSuccess { vm.setTimezoneIdOrNull(trimmed) }
            }
        }
    }
}

