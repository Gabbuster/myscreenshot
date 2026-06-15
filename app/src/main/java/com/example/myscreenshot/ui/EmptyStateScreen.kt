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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.ui.components.AppLogo
import com.example.myscreenshot.ui.theme.AppInk
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
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(AppInk, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AppLogo(Modifier.size(82.dp))
        }
        Text("No reminders yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Share a screenshot, bill, booking, appointment, or PDF. Snappy will find the useful part.",
            modifier = Modifier.padding(vertical = 14.dp),
        )
        Button(
            onClick = onTrySample,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("Try sample screenshot")
        }
        Text("Everything is processed locally on your phone.", modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
fun EmptyStateCard(onTrySample: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = AppInk.copy(alpha = 0.06f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .background(AppInk, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AppLogo(Modifier.size(74.dp))
        }
        Text("Snappy is ready", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(
            "Add a screenshot and Screen4U will turn dates, bookings, bills, and tasks into reminders.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onTrySample,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppInk,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("Try sample screenshot")
        }
        Text("Everything stays local on your phone.", style = MaterialTheme.typography.bodySmall, color = AppOrange)
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPreview() {
    MyScreenshotTheme { EmptyStateScreen({}) }
}
