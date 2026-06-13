package com.example.myscreenshot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfidenceChip(confidence: String, modifier: Modifier = Modifier) {
    val textColor = when (confidence) {
        "High confidence" -> MaterialTheme.colorScheme.primary
        "Review needed" -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    val backgroundColor = when (confidence) {
        "High confidence" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        "Review needed" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.errorContainer
    }
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(confidence, color = textColor, style = MaterialTheme.typography.labelMedium)
    }
}
