package com.example.myscreenshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.ui.theme.MyScreenshotTheme

@Composable
fun EmptyStateScreen(onTrySample: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No reminders yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Share a screenshot, bill, booking, appointment, or PDF to create your first reminder.",
            modifier = Modifier.padding(vertical = 14.dp),
        )
        Button(onClick = onTrySample) { Text("Try sample screenshot") }
        Text("Everything is processed locally on your phone.", modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
fun EmptyStateCard(onTrySample: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("No reminders yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Share a screenshot, bill, booking, appointment, or PDF to create your first reminder.")
        Button(onClick = onTrySample) { Text("Try sample screenshot") }
        Text("Everything is processed locally on your phone.", style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPreview() {
    MyScreenshotTheme { EmptyStateScreen({}) }
}
