package com.example.myscreenshot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.extraction.DetectedAction
import java.text.DateFormat
import java.util.Date

@Composable
fun ActionCard(
    action: DetectedAction,
    checked: Boolean,
    title: String,
    notes: String,
    sourceType: String,
    sourceUri: String?,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SourceThumbnail(sourceType = sourceType, sourceUri = sourceUri)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
            ReminderTypeIcon(action.type)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(action.type, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                action.dateTime?.let { start ->
                    val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    val startStr = df.format(Date(start))
                    if (action.endDateTime != null) {
                        Text("$startStr - ${df.format(Date(action.endDateTime))}")
                    } else {
                        Text(startStr)
                    }
                }
                action.location?.let { Text(it) }
                action.amount?.let { Text("${action.currency.orEmpty()} ${String.format("%.2f", it)}") }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
        ConfidenceChip(action.confidence)
        action.reminderSuggestions.forEach {
            Text(it.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (notes.isNotBlank()) {
            Text(notes.lineSequence().first(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
    }
}
