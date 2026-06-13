package com.example.myscreenshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.ui.theme.AppCoral
import com.example.myscreenshot.ui.theme.AppOrange
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
            .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Brush.linearGradient(listOf(AppOrange, AppCoral)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
        Text("No reminders yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Share a screenshot, bill, booking, appointment, or PDF to create your first reminder.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onTrySample,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Try sample screenshot")
        }
        Text("Everything stays local on your phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPreview() {
    MyScreenshotTheme { EmptyStateScreen({}) }
}
