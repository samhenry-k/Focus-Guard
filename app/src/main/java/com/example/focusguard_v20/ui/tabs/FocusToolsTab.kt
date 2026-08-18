package com.example.focusguard_v20.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.focusguard_v20.ui.focus.FocusToolsNavigator

@Composable
fun FocusToolsTab(
    contentPadding: PaddingValues,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
    ) {
        // Internal navigation for Focus/Clock.
        FocusToolsNavigator(
            contentPadding = PaddingValues(0.dp),
        )

        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = onLogout,
        ) {
            Text("Log out")
        }
    }
}

