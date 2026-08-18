package com.example.focusguard_v20.ui.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.focusguard_v20.ui.dashboard.DashboardScreen

@Composable
fun DashboardTab(
    contentPadding: PaddingValues,
) {
    DashboardScreen(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    )
}

