package com.example.focusguard_v20.ui.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.focusguard_v20.ui.tabs.DashboardTab
import com.example.focusguard_v20.ui.tabs.FocusToolsTab

@Composable
fun AppShell(
    onLogout: () -> Unit,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Filled.Timer, contentDescription = "Focus tools") },
                    label = { Text("Focus") },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> DashboardTab(contentPadding = innerPadding)
            else -> FocusToolsTab(contentPadding = innerPadding, onLogout = onLogout)
        }
    }
}

