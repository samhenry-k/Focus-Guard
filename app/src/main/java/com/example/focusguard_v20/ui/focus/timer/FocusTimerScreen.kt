package com.example.focusguard_v20.ui.focus.timer

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.focusguard_v20.focus.timer.PomodoroPhase
import com.example.focusguard_v20.focus.timer.TimerEngine
import com.example.focusguard_v20.focus.timer.TimerService

@Composable
fun FocusTimerScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by TimerEngine.state.collectAsStateWithLifecycle()
    var autoRepeat by remember { mutableStateOf(state.config.autoRepeat) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pomodoro • ${if (state.phase == PomodoroPhase.Focus) "Focus" else "Break"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${state.remainingSeconds / 60}m ${(state.remainingSeconds % 60).toString().padStart(2, '0')}s",
                style = MaterialTheme.typography.headlineLarge,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val action = if (state.isRunning) TimerService.Actions.Pause else TimerService.Actions.Start
                        context.startService(Intent(context, com.example.focusguard_v20.focus.timer.TimerService::class.java).setAction(action))
                    },
                ) {
                    Text(if (state.isRunning) "Pause" else "Start")
                }

                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startService(
                            Intent(context, com.example.focusguard_v20.focus.timer.TimerService::class.java)
                                .setAction(TimerService.Actions.Reset),
                        )
                    },
                ) { Text("Reset") }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto-repeat", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = autoRepeat,
                    onCheckedChange = { checked ->
                        autoRepeat = checked
                        context.startService(
                            Intent(context, com.example.focusguard_v20.focus.timer.TimerService::class.java)
                                .setAction(TimerService.Actions.SetAutoRepeat)
                                .putExtra(TimerService.Extras.AutoRepeat, checked),
                        )
                    },
                )
            }
        }
    }
}

