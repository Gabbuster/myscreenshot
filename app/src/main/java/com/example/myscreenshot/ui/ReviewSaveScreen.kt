package com.example.myscreenshot.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.calendar.CalendarIntentBuilder
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.extraction.DetectedAction
import com.example.myscreenshot.extraction.OnDeviceAiExtractor
import com.example.myscreenshot.ocr.OcrProcessor
import com.example.myscreenshot.ocr.SharedInput
import com.example.myscreenshot.ui.components.ReminderTypeIcon
import com.example.myscreenshot.ui.components.SourceThumbnail
import com.example.myscreenshot.ui.theme.AppInk
import com.example.myscreenshot.ui.theme.AppPaper
import com.example.myscreenshot.ui.theme.AppProof
import com.example.myscreenshot.ui.theme.AppSuccess
import com.example.myscreenshot.ui.theme.MyScreenshotTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReviewSaveScreen(
    repository: AppRepository,
    sharedInput: SharedInput?,
    useSample: Boolean,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeReminders by repository.observeReminders().collectAsState(initial = emptyList())
    val drafts = remember { mutableStateListOf<ActionDraft>() }
    var extractedText by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(sharedInput?.sourceType ?: "image") }
    var sourceNote by remember { mutableStateOf("Processing locally") }
    var sourceUri by remember { mutableStateOf(sharedInput?.uri?.toString()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var aiAssistNote by remember { mutableStateOf<String?>(null) }
    var showDetectedText by remember { mutableStateOf(false) }
    var duplicateMatches by remember { mutableStateOf<List<DuplicateMatch>>(emptyList()) }
    var savedReminder by remember { mutableStateOf<Reminder?>(null) }

    LaunchedEffect(sharedInput, useSample) {
        loading = true
        errorMessage = null
        aiAssistNote = null
        drafts.clear()
        val input = when {
            sharedInput != null -> sharedInput
            useSample -> SharedInput(null, Samples.flightText, "text/plain")
            else -> SharedInput(null, "", "text/plain")
        }
        val ocr = runCatching { OcrProcessor(context).process(input) }.getOrElse {
            errorMessage = "Could not read this file. Try sharing a clearer image, PDF, or plain text."
            loading = false
            return@LaunchedEffect
        }
        drafts.clear()
        extractedText = ocr.text
        sourceType = ocr.sourceType
        sourceNote = if (useSample) "Sample screenshot loaded" else ocr.note
        if (ocr.text.isBlank()) {
            errorMessage = "No readable text found. Try another screenshot or share plain text."
            loading = false
            return@LaunchedEffect
        }
        val analysis = OnDeviceAiExtractor(context).analyze(
            text = ocr.text,
            now = LocalDateTime.now(),
            zoneId = ZoneId.systemDefault(),
        )
        val detected = analysis.actions
        aiAssistNote = analysis.note
        if (detected.isEmpty()) {
            errorMessage = "No clear reminder action found. You can still try a clearer screenshot or text with a date, amount, flight, bill, or appointment."
        }
        drafts.addAll(
            detected.onlyPlanePropositionForClearTicket(ocr.text)
                .sortedBy { it.confidence.priority }
                .map { ActionDraft(it, true, it.title, defaultNotes(it)) },
        )
        loading = false
    }

    fun saveSelectedDrafts(duplicateIdsToDelete: Set<String> = emptySet()) {
        scope.launch {
            saving = true
            runCatching {
                duplicateIdsToDelete.forEach { repository.deleteReminder(it) }
                var lastReminder: Reminder? = null
                drafts.filter { it.checked }.forEach { draft ->
                    val reminder = repository.saveAction(
                        action = draft.action,
                        title = draft.title,
                        notes = draft.notes,
                        sourceType = sourceType,
                        sourceUri = sourceUri,
                        ocrText = extractedText,
                    )
                    lastReminder = reminder
                }
                lastReminder
            }.onSuccess { reminder ->
                saving = false
                duplicateMatches = emptyList()
                savedReminder = reminder
            }.onFailure {
                saving = false
                errorMessage = "Could not save this reminder. Please try again."
            }
        }
    }

    ReviewSaveContent(
        loading = loading,
        sourceType = sourceType,
        sourceNote = sourceNote,
        sourceUri = sourceUri,
        aiAssistNote = aiAssistNote,
        errorMessage = errorMessage,
        saving = saving,
        detectedText = extractedText,
        showDetectedText = showDetectedText,
        drafts = drafts,
        onBack = onBack,
        onSave = {
            val matches = findDuplicateMatches(drafts.filter { it.checked }, activeReminders)
            if (matches.isNotEmpty()) {
                duplicateMatches = matches
            } else {
                saveSelectedDrafts()
            }
        },
        onDraftChanged = { index, draft ->
            drafts[index] = draft
        },
        onToggleDetectedText = { showDetectedText = !showDetectedText },
    )

    if (duplicateMatches.isNotEmpty()) {
        DuplicateReminderDialog(
            matches = duplicateMatches,
            onDismiss = { duplicateMatches = emptyList() },
            onKeepBoth = { saveSelectedDrafts() },
            onEraseDuplicate = {
                saveSelectedDrafts(duplicateMatches.map { it.existing.id }.toSet())
            },
        )
    }

    savedReminder?.let { reminder ->
        SavedReminderSheet(
            reminder = reminder,
            repository = repository,
            onDismiss = {
                savedReminder = null
                onSaved(reminder.id)
            },
        )
    }
}

@Composable
fun ReviewSaveContent(
    loading: Boolean,
    sourceType: String,
    sourceNote: String,
    sourceUri: String?,
    aiAssistNote: String?,
    errorMessage: String?,
    saving: Boolean,
    detectedText: String,
    showDetectedText: Boolean,
    drafts: List<ActionDraft>,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDraftChanged: (Int, ActionDraft) -> Unit,
    onToggleDetectedText: () -> Unit,
) {
    val selectedDrafts = drafts.filter { it.checked }
    val primaryDraft = selectedDrafts.firstOrNull() ?: drafts.firstOrNull()
    val bestGuessDraft = drafts.firstOrNull()
    val actionCount = drafts.size
    val ambiguousOptions = drafts.distinctCategoryOptions()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack) { Text("Back") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Review detected reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (ambiguousOptions.size > 1) {
                                "AI found ${ambiguousOptions.size} possible matches"
                            } else if (actionCount == 1) {
                                "AI found 1 action"
                            } else {
                                "AI found $actionCount actions"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (loading) {
            item {
                LoadingResultCard()
            }
        } else {
            errorMessage?.let { message ->
                item {
                    ErrorCard(message)
                }
            }
            if (drafts.isEmpty()) {
                item {
                    EmptyResultCard()
                }
            }
            if (ambiguousOptions.size > 1 && bestGuessDraft != null) {
                item {
                    AmbiguousResultCard(
                        options = ambiguousOptions,
                        bestGuessDraft = bestGuessDraft,
                    )
                }
            }
            drafts.forEachIndexed { index, draft ->
                item {
                    PrimaryResultCard(
                        draft = draft,
                        onCheckedChange = { onDraftChanged(index, draft.copy(checked = it)) },
                    )
                }
            }
            primaryDraft?.let { draft ->
                item {
                    SmartRemindersCard(action = draft.action)
                }
                item {
                    QuickActionsCard(action = draft.action)
                }
            }
        }
        item {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = drafts.any { it.checked } && !loading && !saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
            ) {
                Text(
                    when {
                        saving -> "Saving..."
                        selectedDrafts.size == 1 -> "Save Reminder"
                        else -> "Save ${selectedDrafts.size} Reminders"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        item {
            SourceDetailsSection(
                expanded = showDetectedText,
                onToggle = onToggleDetectedText,
                sourceType = sourceType,
                sourceNote = sourceNote,
                sourceUri = sourceUri,
                aiAssistNote = aiAssistNote,
                detectedText = detectedText,
            )
        }
    }
}

@Composable
private fun AmbiguousResultCard(
    options: List<String>,
    bestGuessDraft: ActionDraft,
) {
    ResultSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("A few things look possible", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "I found ${options.joinToString(" or ")}. Best guess: ${bestGuessDraft.action.type.cleanCategoryName()} because it has the strongest reminder signals.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.take(3).forEach { option ->
                OptionChip(option)
            }
        }
    }
}

@Composable
private fun OptionChip(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun LoadingResultCard() {
    ResultSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Finding the reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("AI is looking for the action and date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Needs another look", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun EmptyResultCard() {
    ResultSurface {
        Text("No reminder found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Try a screenshot with a date, amount, booking, delivery, or appointment.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrimaryResultCard(
    draft: ActionDraft,
    onCheckedChange: (Boolean) -> Unit,
) {
    val action = draft.action
    ResultSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReminderTypeIcon(action.type)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(action.type.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text(draft.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            Switch(checked = draft.checked, onCheckedChange = onCheckedChange)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            action.primaryDateLabel()?.let { FieldBlock(label = it.first, value = it.second) }
            action.secondaryDateLabel()?.let { FieldBlock(label = it.first, value = it.second) }
            action.amountLabel()?.let { FieldBlock(label = "Amount", value = it) }
            action.location?.takeIf { it.isNotBlank() }?.let { FieldBlock(label = action.locationLabel(), value = it) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("AI Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(action.aiSummary(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FieldBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SmartRemindersCard(action: DetectedAction) {
    ResultSurface {
        Text("Suggested reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            action.smartReminderLabels().forEach { label ->
                SmartReminderToggle(label)
            }
        }
    }
}

@Composable
private fun SmartReminderToggle(label: String) {
    var enabled by remember(label) { mutableStateOf(true) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓ $label", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { enabled = it })
    }
}

@Composable
private fun QuickActionsCard(action: DetectedAction) {
    val actions = action.quickActions()
    if (actions.isEmpty()) return
    ResultSurface {
        Text("Quick actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            actions.forEach { label ->
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun SourceDetailsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    sourceType: String,
    sourceNote: String,
    sourceUri: String?,
    aiAssistNote: String?,
    detectedText: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPaper, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Source details", fontWeight = FontWeight.SemiBold)
                Text(if (expanded) "Hide" else "Show")
            }
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Original screenshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SourceThumbnail(
                    sourceType = sourceType,
                    sourceUri = sourceUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
                Text("OCR text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    detectedText.ifBlank { "No OCR text saved." },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Technical information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(
                        "Source type: $sourceType",
                        sourceNote.takeIf { it.isNotBlank() },
                        aiAssistNote?.takeIf { it.isNotBlank() },
                    ).joinToString("\n"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ResultSurface(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(28.dp), ambientColor = AppInk.copy(alpha = 0.07f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

private fun DetectedAction.aiSummary(): String = when (type.lowercase()) {
    "travel" -> "Flight booking detected."
    "hotel" -> "Hotel reservation detected."
    "appointment" -> "Appointment detected."
    "bill" -> "Bill payment detected."
    "documents", "document" -> "Document expiry detected."
    "delivery" -> "Delivery notification detected."
    else -> "Reminder detected."
}

private fun DetectedAction.primaryDateLabel(): Pair<String, String>? {
    val value = dateTime ?: return null
    val label = when (type.lowercase()) {
        "hotel" -> "Check-in"
        "bill" -> "Due date"
        "documents", "document" -> "Expiry date"
        "delivery" -> "Expected delivery"
        "travel" -> "Departure"
        else -> "Date"
    }
    return label to value.formatActionDate()
}

private fun DetectedAction.secondaryDateLabel(): Pair<String, String>? {
    val value = endDateTime ?: return null
    val label = when (type.lowercase()) {
        "hotel" -> "Check-out"
        else -> "Ends"
    }
    return label to value.formatActionDate()
}

private fun DetectedAction.amountLabel(): String? =
    amount?.let { "${currency.orEmpty()} ${String.format("%.2f", it)}".trim() }

private fun DetectedAction.locationLabel(): String = when (type.lowercase()) {
    "hotel" -> "Address"
    "appointment" -> "Location"
    "delivery" -> "Delivery address"
    "travel" -> "Airport"
    else -> "Location"
}

private fun DetectedAction.smartReminderLabels(): List<String> {
    val detectedLabels = reminderSuggestions.map { it.label }.filter { it.isNotBlank() }
    if (detectedLabels.isNotEmpty()) return detectedLabels.take(3)
    return when (type.lowercase()) {
        "hotel" -> listOf("Hotel preparation 7 days before", "Check-in reminder", "Check-out reminder")
        "travel" -> listOf("Check-in opens", "Leave for airport", "Flight departure")
        "appointment" -> listOf("Reminder 1 day before", "Reminder 2 hours before", "Leave on time")
        "bill" -> listOf("Due soon", "Due tomorrow", "Due today")
        "documents", "document" -> listOf("Renew 6 months before", "Renew 3 months before", "Expiry warning")
        "delivery" -> listOf("Delivery expected", "Package arriving today", "Follow up if delayed")
        else -> listOf("Reminder 1 day before", "Reminder 2 hours before")
    }
}

private fun DetectedAction.quickActions(): List<String> = when (type.lowercase()) {
    "travel" -> listOf("Calendar", "Airport")
    "hotel" -> listOf("Calendar", "Maps", "Call")
    "appointment" -> listOf("Calendar", "Maps", "Call")
    "bill" -> listOf("Pay", "Reminder")
    "documents", "document" -> listOf("Reminder", "Share")
    "delivery" -> listOf("Track", "Address")
    else -> listOf("Reminder")
}

private fun List<ActionDraft>.distinctCategoryOptions(): List<String> =
    map { it.action.type.cleanCategoryName() }
        .distinct()
        .take(4)

private fun String.cleanCategoryName(): String = when (lowercase()) {
    "documents" -> "Document"
    else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun Long.formatActionDate(): String {
    val zoneId = ZoneId.systemDefault()
    return DateTimeFormatter.ofPattern("d MMM yyyy • HH:mm")
        .format(Instant.ofEpochMilli(this).atZone(zoneId))
}

@Composable
private fun DuplicateReminderDialog(
    matches: List<DuplicateMatch>,
    onDismiss: () -> Unit,
    onKeepBoth: () -> Unit,
    onEraseDuplicate: () -> Unit,
) {
    val duplicateNames = matches
        .map { it.existing.title }
        .distinct()
        .take(3)
        .joinToString("\n") { "Already saved: $it" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Possible duplicate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This looks already saved. Do you want to erase the older duplicate and keep this version?")
                Text(duplicateNames, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = onEraseDuplicate) {
                Text("Erase duplicate")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                OutlinedButton(onClick = onKeepBoth) { Text("Keep both") }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedReminderSheet(
    reminder: Reminder,
    repository: AppRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var alarmTitle by remember(reminder.id) { mutableStateOf(suggestedAlarmTitle(reminder)) }
    var alarmTime by remember(reminder.id) { mutableStateOf(suggestedAlarmTime(reminder)) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var scheduling by remember { mutableStateOf(false) }
    var calendarSelected by remember(reminder.id) { mutableStateOf(false) }
    var alarmSelected by remember(reminder.id) { mutableStateOf(false) }
    var alarmSaved by remember(reminder.id) { mutableStateOf(false) }
    val canAddCalendar = reminder.dateTime != null
    val zoneId = ZoneId.systemDefault()
    val alarmDateTime = alarmTime.toLocalDateTime(zoneId)
    val calendarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            calendarSelected = true
            statusText = "Calendar event saved"
        } else if (!calendarSelected) {
            statusText = "Finish the event in your calendar app to save it."
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Saved", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                reminder.title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
            )

            if (calendarSelected) {
                SuccessMention(
                    text = "Reminder saved in calendar",
                    accent = MaterialTheme.colorScheme.primaryContainer,
                )
            } else {
                QuestionToggleRow(
                    question = "Save in calendar",
                    selected = false,
                    hint = "Adds a calendar event for this reminder.",
                    onYes = {
                        if (!canAddCalendar) {
                            statusText = "No date found to save in calendar."
                            return@QuestionToggleRow
                        }
                        runCatching { calendarLauncher.launch(CalendarIntentBuilder.build(reminder)) }
                            .onFailure { statusText = "Calendar app not available on this device." }
                    },
                    onNo = {
                        calendarSelected = false
                        statusText = null
                    },
                )
            }

            if (alarmSaved) {
                SuccessMention(
                    text = "Reminder saved into Alarms",
                    accent = AppSuccess.copy(alpha = 0.18f),
                )
            } else {
                QuestionToggleRow(
                    question = "Set an alarm on your phone",
                    selected = alarmSelected,
                    hint = "You can tune the alarm after this step.",
                    onYes = {
                        alarmSelected = true
                        statusText = null
                    },
                    onNo = {
                        alarmSelected = false
                        statusText = null
                    },
                )
            }

            if (alarmSelected && !alarmSaved) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Adjust the alarm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextField(
                        value = alarmTitle,
                        onValueChange = {
                            alarmTitle = it
                            alarmSaved = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Alarm title") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selectedDate = LocalDate.of(year, month + 1, day)
                                        alarmTime = LocalDateTime.of(selectedDate, alarmDateTime.toLocalTime())
                                            .atZone(zoneId)
                                            .toInstant()
                                            .toEpochMilli()
                                        alarmSaved = false
                                    },
                                    alarmDateTime.year,
                                    alarmDateTime.monthValue - 1,
                                    alarmDateTime.dayOfMonth,
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                alarmDateTime.format(DateTimeFormatter.ofPattern("MMM d")),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        alarmTime = LocalDateTime.of(alarmDateTime.toLocalDate(), LocalTime.of(hour, minute))
                                            .atZone(zoneId)
                                            .toInstant()
                                            .toEpochMilli()
                                        alarmSaved = false
                                    },
                                    alarmDateTime.hour,
                                    alarmDateTime.minute,
                                    false,
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                alarmDateTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                        }
                    }
                    AlarmSuggestionRow(
                        reminder = reminder,
                        selectedTime = alarmTime,
                        onSelected = {
                            alarmTime = it
                            alarmSaved = false
                        },
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                scheduling = true
                                runCatching {
                                    repository.scheduleCustomAlert(
                                        reminder = reminder,
                                        alertDateTime = alarmTime,
                                        alertLabel = alarmTitle,
                                    )
                                }.onSuccess {
                                    alarmSaved = true
                                    alarmSelected = false
                                    statusText = "Phone alarm set"
                                }.onFailure {
                                    statusText = "Could not set the phone reminder."
                                }
                                scheduling = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = alarmTime > System.currentTimeMillis() && alarmTitle.isNotBlank() && !scheduling,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (alarmSaved) AppSuccess else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            when {
                                scheduling -> "Setting..."
                                alarmSaved -> "Phone alarm saved"
                                else -> "Set phone alarm"
                            },
                        )
                    }
                }
            }

            statusText?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun AlarmSuggestionRow(
    reminder: Reminder,
    selectedTime: Long,
    onSelected: (Long) -> Unit,
) {
    val suggestions = reminder.alarmSuggestions()
    if (suggestions.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quick picks", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            suggestions.forEach { suggestion ->
                OutlinedButton(
                    onClick = { onSelected(suggestion.time) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedTime == suggestion.time) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (selectedTime == suggestion.time) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                ) {
                    Text(
                        suggestion.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionToggleRow(
    question: String,
    selected: Boolean,
    hint: String,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            question,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
        )
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onYes,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) AppSuccess else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("V", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Yes", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
            OutlinedButton(
                onClick = onNo,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("X", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("No", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SuccessMention(
    text: String,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Saved", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
        Text(
            text,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
        )
    }
}

private data class AlarmSuggestionOption(
    val label: String,
    val time: Long,
)

private fun suggestedAlarmTitle(reminder: Reminder): String = when (reminder.type.lowercase()) {
    "travel" -> "Airport reminder"
    "hotel" -> "Hotel check-in reminder"
    "bill" -> "Payment reminder"
    "appointment" -> "Appointment reminder"
    "delivery" -> "Delivery reminder"
    "documents" -> "Document reminder"
    else -> "Reminder"
} + ": " + reminder.title

private fun suggestedAlarmTime(reminder: Reminder): Long =
    reminder.alarmSuggestions().firstOrNull()?.time ?: (System.currentTimeMillis() + 60 * 60_000L)

private fun Reminder.alarmSuggestions(): List<AlarmSuggestionOption> {
    val eventTime = dateTime ?: return listOf(
        AlarmSuggestionOption("1 hour", System.currentTimeMillis() + 60 * 60_000L),
        AlarmSuggestionOption("Tomorrow", tomorrowAtNine()),
    )
    val minute = 60_000L
    val day = 24 * 60 * minute
    val options = when (type.lowercase()) {
        "travel" -> listOf(
            AlarmSuggestionOption("24h before", eventTime - day),
            AlarmSuggestionOption("3h before", eventTime - 3 * 60 * minute),
            AlarmSuggestionOption("At time", eventTime),
        )
        "hotel" -> listOf(
            AlarmSuggestionOption("7d before", eventTime - 7 * day),
            AlarmSuggestionOption("1d before", eventTime - day),
            AlarmSuggestionOption("At time", eventTime),
        )
        "bill" -> listOf(
            AlarmSuggestionOption("3d before", eventTime - 3 * day),
            AlarmSuggestionOption("1d before", eventTime - day),
            AlarmSuggestionOption("Due time", eventTime),
        )
        "appointment" -> listOf(
            AlarmSuggestionOption("1d before", eventTime - day),
            AlarmSuggestionOption("2h before", eventTime - 2 * 60 * minute),
            AlarmSuggestionOption("At time", eventTime),
        )
        "delivery" -> listOf(
            AlarmSuggestionOption("Morning", morningOf(eventTime)),
            AlarmSuggestionOption("2h before", eventTime - 2 * 60 * minute),
            AlarmSuggestionOption("At time", eventTime),
        )
        else -> listOf(
            AlarmSuggestionOption("7d before", eventTime - 7 * day),
            AlarmSuggestionOption("1d before", eventTime - day),
            AlarmSuggestionOption("At time", eventTime),
        )
    }
    val now = System.currentTimeMillis()
    return options.filter { it.time > now }.distinctBy { it.time }.take(3)
        .ifEmpty { listOf(AlarmSuggestionOption("1 hour", now + 60 * minute)) }
}

private fun Long.toLocalDateTime(zoneId: ZoneId): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()

private fun tomorrowAtNine(): Long {
    val zoneId = ZoneId.systemDefault()
    return LocalDateTime.now(zoneId)
        .toLocalDate()
        .plusDays(1)
        .atTime(9, 0)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

private fun morningOf(epochMillis: Long): Long {
    val zoneId = ZoneId.systemDefault()
    return epochMillis.toLocalDateTime(zoneId)
        .toLocalDate()
        .atTime(9, 0)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

data class ActionDraft(
    val action: DetectedAction,
    val checked: Boolean,
    val title: String,
    val notes: String,
)

private data class DuplicateMatch(
    val draft: ActionDraft,
    val existing: Reminder,
)

private fun findDuplicateMatches(drafts: List<ActionDraft>, reminders: List<Reminder>): List<DuplicateMatch> =
    drafts.mapNotNull { draft ->
        reminders.firstOrNull { reminder -> reminder.looksLikeDuplicateOf(draft) }
            ?.let { DuplicateMatch(draft, it) }
    }

private fun Reminder.looksLikeDuplicateOf(draft: ActionDraft): Boolean {
    if (!type.equals(draft.action.type, ignoreCase = true)) return false
    val sameTitle = title.normalizedReminderTitle() == draft.title.normalizedReminderTitle()
    val sameDate = dateTime?.let { existing ->
        draft.action.dateTime?.let { incoming -> existing.sameLocalDayAs(incoming) }
    } == true
    val sameRouteOrPlace = location?.normalizedReminderTitle()
        ?.takeIf { it.isNotBlank() }
        ?.let { existingLocation ->
            val incomingLocation = draft.action.location?.normalizedReminderTitle().orEmpty()
            incomingLocation.isNotBlank() &&
                (incomingLocation.contains(existingLocation) || existingLocation.contains(incomingLocation))
        } == true
    return sameTitle || (sameDate && sameRouteOrPlace) || (sameDate && title.importantWordsOverlap(draft.title))
}

private fun String.normalizedReminderTitle(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

private fun String.importantWordsOverlap(other: String): Boolean {
    val left = normalizedReminderTitle().split(" ").filter { it.length >= 3 }.toSet()
    val right = other.normalizedReminderTitle().split(" ").filter { it.length >= 3 }.toSet()
    return left.intersect(right).size >= 2
}

private fun Long.sameLocalDayAs(other: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val left = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    val right = Instant.ofEpochMilli(other).atZone(zone).toLocalDate()
    return left == right
}

private fun defaultNotes(action: DetectedAction): String =
    action.reminderSuggestions.joinToString("\n") { it.label }

private fun List<DetectedAction>.onlyPlanePropositionForClearTicket(text: String): List<DetectedAction> {
    val hasTravelAction = any { it.type.equals("Travel", ignoreCase = true) }
    val clearTicketSignal = text.containsAnyTicketSignal() ||
        Regex("\\b[A-Z]{2}\\s?\\d{2,4}\\b").containsMatchIn(text) &&
        Regex("\\b(PNR|booking reference|e-?ticket|ticket number|boarding pass)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
    if (!hasTravelAction || !clearTicketSignal) return this
    return filter { it.type.equals("Travel", ignoreCase = true) }
}

private fun String.containsAnyTicketSignal(): Boolean =
    Regex("\\b(ticket number|e-?ticket|boarding pass|PNR|booking reference|flight number)\\b", RegexOption.IGNORE_CASE)
        .containsMatchIn(this)

private val String.priority: Int
    get() = when (this) {
        "High confidence" -> 0
        "Review needed" -> 1
        else -> 2
    }

@Preview(showBackground = true)
@Composable
private fun ReviewPreview() {
    MyScreenshotTheme {
        ReviewSaveContent(
            loading = false,
            sourceType = "image",
            sourceNote = "Image processed locally",
            sourceUri = null,
            aiAssistNote = "On-device AI Assist used OCR and local language detection",
            errorMessage = null,
            saving = false,
            detectedText = Samples.flightText,
            showDetectedText = false,
            drafts = Samples.actions().map { ActionDraft(it, true, it.title, defaultNotes(it)) },
            onBack = {},
            onSave = {},
            onDraftChanged = { _, _ -> },
            onToggleDetectedText = {},
        )
    }
}
