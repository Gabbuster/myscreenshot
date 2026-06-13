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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onCheckedChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when (action.confidence) {
        "High confidence" -> MaterialTheme.colorScheme.primary
        "Review needed" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val surfaceColor = when (action.confidence) {
        "High confidence" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        "Review needed" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surface
    }
    val titleColor = if (action.confidence == "High confidence") {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(18.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ReminderTypeIcon(action.type)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(action.title, color = titleColor, fontWeight = FontWeight.Bold)
                Text(action.type, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
        ConfidenceChip(action.confidence)
        action.reminderSuggestions.forEach { Text(it.label, style = MaterialTheme.typography.bodySmall) }
        OutlinedTextField(value = title, onValueChange = onTitleChange, label = { Text("Title") }, singleLine = true)
        OutlinedTextField(value = notes, onValueChange = onNotesChange, label = { Text("Notes") })
    }
}
