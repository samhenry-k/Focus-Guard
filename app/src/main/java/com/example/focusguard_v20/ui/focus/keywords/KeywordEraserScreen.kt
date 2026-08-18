package com.example.focusguard_v20.ui.focus.keywords

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun KeywordEraserScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var keywordInput by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf(KeywordEraserPrefs.getKeywords(context).toList().sorted()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Keyword Eraser",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Any text you type matching these keywords will be automatically deleted.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = keywordInput,
                onValueChange = { keywordInput = it },
                label = { Text("Enter keyword") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (keywordInput.isNotBlank()) {
                        KeywordEraserPrefs.addKeyword(context, keywordInput.trim())
                        keywords = KeywordEraserPrefs.getKeywords(context).toList().sorted()
                        keywordInput = ""
                    }
                }
            ) {
                Text("Add")
            }
        }

        Divider()

        Text("Active Keywords", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(keywords) { keyword ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(keyword, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            KeywordEraserPrefs.removeKeyword(context, keyword)
                            keywords = KeywordEraserPrefs.getKeywords(context).toList().sorted()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
