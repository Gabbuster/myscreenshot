package com.example.myscreenshot.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myscreenshot.calendar.CalendarIntentBuilder
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.ui.components.ReminderTypeIcon
import com.example.myscreenshot.ui.components.SourceThumbnail
import com.example.myscreenshot.ui.detail.ActionKind
import com.example.myscreenshot.ui.detail.DetailAction
import com.example.myscreenshot.ui.detail.ReminderCategory
import com.example.myscreenshot.ui.detail.category
import com.example.myscreenshot.ui.detail.getCategoryLabel
import com.example.myscreenshot.ui.detail.getPrimaryActions
import com.example.myscreenshot.ui.detail.getSmartReminderSuggestions
import com.example.myscreenshot.ui.detail.isAvailable
import com.example.myscreenshot.ui.detail.isVisibleAction
import com.example.myscreenshot.ui.theme.AppInk
import com.example.myscreenshot.ui.theme.AppPaper
import com.example.myscreenshot.ui.theme.AppSuccess
import com.example.myscreenshot.ui.theme.MyScreenshotTheme
import java.text.DateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
            onUpdate = { updated ->
                scope.launch {
                    repository.updateReminder(updated)
                }
            },
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
    onUpdate: (Reminder) -> Unit = {},
) {
    val context = LocalContext.current
    val category = reminder.category()
    var showScreenshot by remember { mutableStateOf(false) }
    var showScreenshotViewer by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Text("<", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("Reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            TopReminderCard(
                reminder = reminder,
                category = category,
                onClick = { showEditDialog = true },
            )
        }
        item {
            CompactSmartRemindersCard(category)
        }
        item {
            SmartActionsRow(
                actions = getPrimaryActions(category),
                reminder = reminder,
                onAction = { action ->
                    when (action.kind) {
                        ActionKind.Calendar -> if (reminder.dateTime != null) {
                            context.startActivity(CalendarIntentBuilder.build(reminder))
                            onUpdate(reminder.copy(calendarSavedAt = System.currentTimeMillis()))
                        }
                        ActionKind.Maps -> reminder.location?.takeIf { it.isNotBlank() }?.let {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(it)}")))
                        }
                        ActionKind.Share -> {
                            val shareIntent = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, reminder.title)
                            context.startActivity(Intent.createChooser(shareIntent, "Share reminder"))
                        }
                        ActionKind.Delete -> showDeleteDialog = true
                        else -> Unit
                    }
                },
            )
        }
        item {
            CollapsedSection(
                title = "Original screenshot",
                expanded = showScreenshot,
                onToggle = { showScreenshot = !showScreenshot },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceThumbnail(
                        sourceType = reminder.sourceType,
                        sourceUri = reminder.sourceImageUri,
                        contentScale = ContentScale.Fit,
                        thumbnailSize = 720,
                        showSheen = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(154.dp)
                            .clickable { showScreenshotViewer = true },
                    )
                    Text(
                        "Tap to view full screenshot",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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

    if (showEditDialog) {
        EditReminderDialog(
            reminder = reminder,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                onUpdate(updated)
                showEditDialog = false
            },
        )
    }

    if (showScreenshotViewer) {
        Dialog(
            onDismissRequest = { showScreenshotViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                SourceThumbnail(
                    sourceType = reminder.sourceType,
                    sourceUri = reminder.sourceImageUri,
                    contentScale = ContentScale.Fit,
                    thumbnailSize = 1400,
                    showSheen = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.62f),
                )
                TextButton(
                    onClick = { showScreenshotViewer = false },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EditReminderDialog(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
) {
    var title by remember(reminder.id) { mutableStateOf(reminder.title) }
    var type by remember(reminder.id) { mutableStateOf(reminder.type) }
    var startText by remember(reminder.id) { mutableStateOf(reminder.dateTime.toEditableDateTime()) }
    var endText by remember(reminder.id) { mutableStateOf(reminder.endDateTime.toEditableDateTime()) }
    var location by remember(reminder.id) { mutableStateOf(reminder.location.orEmpty()) }
    var amount by remember(reminder.id) { mutableStateOf(reminder.amount?.let { String.format("%.2f", it) }.orEmpty()) }
    var currency by remember(reminder.id) { mutableStateOf(reminder.currency.orEmpty()) }
    var notes by remember(reminder.id) { mutableStateOf(reminder.notes) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(value = title, onValueChange = { title = it }, singleLine = true, label = { Text("Title") })
                TextField(value = type, onValueChange = { type = it }, singleLine = true, label = { Text("Category") })
                TextField(
                    value = startText,
                    onValueChange = { startText = it },
                    singleLine = true,
                    label = { Text("Starts") },
                    supportingText = { Text("YYYY-MM-DD HH:mm") },
                )
                TextField(
                    value = endText,
                    onValueChange = { endText = it },
                    singleLine = true,
                    label = { Text("Ends") },
                    supportingText = { Text("Optional") },
                )
                TextField(value = location, onValueChange = { location = it }, singleLine = true, label = { Text("Place or details") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = amount, onValueChange = { amount = it }, singleLine = true, label = { Text("Amount") }, modifier = Modifier.weight(1f))
                    TextField(value = currency, onValueChange = { currency = it }, singleLine = true, label = { Text("Currency") }, modifier = Modifier.weight(1f))
                }
                TextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, minLines = 2)
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedStart = startText.parseEditableDateTimeOrNull()
                    val parsedEnd = endText.parseEditableDateTimeOrNull()
                    val parsedAmount = amount.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
                    when {
                        title.isBlank() -> errorText = "Title is required."
                        startText.isNotBlank() && parsedStart == null -> errorText = "Use YYYY-MM-DD HH:mm for Starts."
                        endText.isNotBlank() && parsedEnd == null -> errorText = "Use YYYY-MM-DD HH:mm for Ends."
                        amount.isNotBlank() && parsedAmount == null -> errorText = "Amount must be a number."
                        else -> onSave(
                            reminder.copy(
                                title = title.trim(),
                                type = type.trim().ifBlank { reminder.type },
                                dateTime = parsedStart,
                                endDateTime = parsedEnd,
                                location = location.trim().ifBlank { null },
                                amount = parsedAmount,
                                currency = currency.trim().uppercase().ifBlank { null },
                                notes = notes.trim(),
                            ),
                        )
                    }
                },
            ) {
                Text("Save changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun TopReminderCard(
    reminder: Reminder,
    category: ReminderCategory,
    onClick: () -> Unit,
) {
    val secondaryInfo = reminder.secondaryInformation(category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = AppInk.copy(alpha = 0.08f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ReminderTypeIcon(reminder.type)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        getCategoryLabel(category),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text("Screen4U reminder", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text(
            reminder.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            reminder.mainDateText(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        if (secondaryInfo.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                secondaryInfo.take(4).forEach { DetailLine(it) }
            }
        }
        Text("Tap to edit details", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DetailLine(text: String) {
    val parts = text.split(":", limit = 2)
    if (parts.size == 1) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            parts.first().trim(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(0.40f),
        )
        Text(
            parts[1].trim(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.60f),
        )
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp), ambientColor = AppInk.copy(alpha = 0.05f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun SmartActionsRow(
    actions: List<DetailAction>,
    reminder: Reminder,
    onAction: (DetailAction) -> Unit,
) {
    val visibleActions = actions.filter { it.isVisibleAction(reminder) }
    val savedLabels = listOfNotNull(
        reminder.calendarSavedAt?.let { "Saved in calendar" },
        reminder.alarmSavedAt?.let { "Saved into Alarms" },
    )
    if (visibleActions.isEmpty() && savedLabels.isEmpty()) return

    InfoCard(title = "Actions") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (savedLabels.isNotEmpty()) {
                savedLabels.forEach { SavedStatusChip(it) }
            }
            visibleActions.chunked(2).forEach { rowActions ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    rowActions.forEach { action ->
                        Button(
                            onClick = { onAction(action) },
                            enabled = action.isAvailable(reminder),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Text(action.label)
                        }
                    }
                    if (rowActions.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSmartRemindersCard(category: ReminderCategory) {
    val reminders = getSmartReminderSuggestions(category).take(3)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Smart reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        reminders.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { label ->
                    Text(
                        label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        maxLines = 2,
                    )
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SavedStatusChip(text: String) {
    Text(
        text,
        color = AppSuccess,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSuccess.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun CollapsedSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper, RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(if (expanded) "Hide" else "Show")
            }
        }
        if (expanded) {
            content()
        }
    }
}

private fun Reminder.secondaryInformation(category: ReminderCategory): List<String> {
    val text = listOf(title, notes, ocrText).joinToString("\n")
    return when (category) {
        ReminderCategory.Travel -> listOfNotNull(
            extractRoute(text)?.let { "Route: $it" },
            extractFlightNumber(text)?.let { "Flight: $it" },
            extractLabeledValue(text, listOf("terminal", "gate", "seat"))?.let { "Details: $it" },
            location?.takeIf { it.isNotBlank() }?.let { "Airport: $it" },
        )
        ReminderCategory.Hotel -> listOfNotNull(
            "Hotel: $title",
            dateTime?.let { "Check-in: ${formatDate(it)}" },
            endDateTime?.let { "Check-out: ${formatDate(it)}" },
            location?.takeIf { it.isNotBlank() }?.let { "Address: $it" },
        )
        ReminderCategory.Appointment -> listOfNotNull(
            extractLabeledValue(text, listOf("doctor", "company", "person", "with"))?.let { "With: $it" },
            location?.takeIf { it.isNotBlank() }?.let { "Location: $it" },
        )
        ReminderCategory.Bill -> listOfNotNull(
            amount?.let { "Amount: ${currency.orEmpty()} ${String.format("%.2f", it)}" },
            dateTime?.let { "Due: ${formatDate(it)}" },
            extractLabeledValue(text, listOf("provider", "merchant", "company"))?.let { "Provider: $it" },
        )
        ReminderCategory.Document -> listOfNotNull(
            "Document: $title",
            dateTime?.let { "Expiry: ${formatDate(it)}" },
            extractLabeledValue(text, listOf("owner", "name"))?.let { "Owner: $it" },
            extractReference(text)?.let { "Reference: $it" },
        )
        ReminderCategory.Delivery -> listOfNotNull(
            "Package: $title",
            dateTime?.let { "Expected: ${formatDate(it)}" },
            extractTrackingNumber(text)?.let { "Tracking: $it" },
            location?.takeIf { it.isNotBlank() }?.let { "Address: $it" },
        )
        ReminderCategory.Unknown -> listOfNotNull(
            amount?.let { "Amount: ${currency.orEmpty()} ${String.format("%.2f", it)}" },
            location?.takeIf { it.isNotBlank() },
            notes.takeIf { it.isNotBlank() },
        )
    }.ifEmpty {
        listOf("No extra details detected.")
    }
}

private fun Reminder.mainDateText(): String =
    dateTime?.let { formatDate(it) } ?: "No date or time detected"

private fun formatDate(value: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(Date(value))

private val editableDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun Long?.toEditableDateTime(): String =
    this?.let {
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault())
            .format(editableDateTimeFormatter)
    }.orEmpty()

private fun String.parseEditableDateTimeOrNull(): Long? {
    val clean = trim()
    if (clean.isBlank()) return null
    return runCatching {
        LocalDateTime.parse(clean, editableDateTimeFormatter)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private fun extractRoute(text: String): String? =
    Regex("""\b([A-Z]{3})\s*(?:→|->|-|to)\s*([A-Z]{3})\b""").find(text)?.let {
        "${it.groupValues[1]} → ${it.groupValues[2]}"
    }

private fun extractFlightNumber(text: String): String? =
    Regex("""\b[A-Z]{2}\s?\d{2,4}\b""").find(text)?.value

private fun extractTrackingNumber(text: String): String? =
    Regex("""\b[A-Z0-9]{10,22}\b""").find(text)?.value

private fun extractReference(text: String): String? =
    Regex("""(?i)\b(?:ref|reference|document no|number)[:\s#-]+([A-Z0-9-]{4,24})""")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)

private fun extractLabeledValue(text: String, labels: List<String>): String? {
    labels.forEach { label ->
        Regex("""(?im)\b$label[:\s-]+([^\n,]+)""")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it.take(48) }
    }
    return null
}

@Preview(showBackground = true)
@Composable
private fun DetailPreview() {
    MyScreenshotTheme {
        ReminderDetailContent(Samples.reminders().first(), {}, {})
    }
}
