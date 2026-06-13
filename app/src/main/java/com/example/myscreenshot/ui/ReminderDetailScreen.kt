package com.example.myscreenshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.calendar.CalendarIntentBuilder
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.ui.components.ConfidenceChip
import com.example.myscreenshot.ui.components.ReminderTypeIcon
import com.example.myscreenshot.ui.components.SourceThumbnail
import com.example.myscreenshot.ui.theme.MyScreenshotTheme
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun ReminderDetailScreen(
    repository: AppRepository,
    reminderId: String,
    onBack: () -> Unit,
) {
    val reminder by repository.observeReminder(reminderId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    reminder?.let {
        ReminderDetailContent(
            reminder = it,
            onBack = onBack,
            onDelete = {
                scope.launch {
                    repository.deleteReminder(it.id)
                    onBack()
                }
            },
        )
    } ?: EmptyStateScreen(onTrySample = onBack)
}

@Composable
fun ReminderDetailContent(
    reminder: Reminder,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack) {
                    Text("<", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("Reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReminderTypeIcon(reminder.type)
                    SourceThumbnail(reminder.sourceType, sourceUri = reminder.sourceImageUri)
                }
                Text(reminder.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(reminder.type, color = MaterialTheme.colorScheme.primary)
                reminder.dateTime?.let {
                    Text(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(Date(it)))
                }
                reminder.amount?.let { Text("${reminder.currency.orEmpty()} ${String.format("%.2f", it)}") }
                reminder.location?.let { Text(it) }
                ConfidenceChip(reminder.confidence)
                Text("Notes", fontWeight = FontWeight.SemiBold)
                Text(reminder.notes.ifBlank { "No notes" })
                Text("Detected text", fontWeight = FontWeight.SemiBold)
                Text(reminder.ocrText.ifBlank { "No detected text saved" }, maxLines = 6)
                if (reminder.type in listOf("Travel", "Appointment")) {
                    Button(
                        onClick = { context.startActivity(CalendarIntentBuilder.build(reminder)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("Add to calendar")
                    }
                }
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete reminder?") },
            text = { Text("This removes the reminder and keeps the app local. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailPreview() {
    MyScreenshotTheme {
        ReminderDetailContent(Samples.reminders().first(), {}, {})
    }
}
