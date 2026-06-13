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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.myscreenshot.ui.components.ActionCard
import com.example.myscreenshot.ui.components.SourceThumbnail
import com.example.myscreenshot.ui.theme.AppCoral
import com.example.myscreenshot.ui.theme.AppInk
import com.example.myscreenshot.ui.theme.AppOrange
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = AppInk.copy(alpha = 0.10f))
                    .background(AppInk, RoundedCornerShape(26.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SourceThumbnail(sourceType, modifier = Modifier.height(76.dp), sourceUri = sourceUri)
                Text("Local capture analysis", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(sourceNote, color = Color.White.copy(alpha = 0.72f))
                aiAssistNote?.let {
                    Text(it, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge)
                }
                if (loading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White)
                        Text("Reading shared content", color = Color.White)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(AppOrange, AppCoral)), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${drafts.size} actions found", color = AppInk, fontWeight = FontWeight.Bold)
                        Text("Ready to save", color = AppInk.copy(alpha = 0.74f))
                    }
                }
                errorMessage?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                    ) {
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        itemsIndexed(drafts) { index, draft ->
            ActionCard(
                action = draft.action,
                checked = draft.checked,
                title = draft.title,
                notes = draft.notes,
                sourceType = sourceType,
                sourceUri = sourceUri,
                onCheckedChange = { onDraftChanged(index, draft.copy(checked = it)) },
            )
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
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
            ) {
                Text(if (saving) "Saving..." else "Save actions")
            }
            TextButton(onClick = onToggleDetectedText, enabled = detectedText.isNotBlank()) {
                Text(if (showDetectedText) "Hide detected text" else "Review detected text")
            }
            if (showDetectedText) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Detected text", fontWeight = FontWeight.SemiBold)
                    Text(detectedText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
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
    var calendarSaved by remember(reminder.id) { mutableStateOf(false) }
    var alarmSaved by remember(reminder.id) { mutableStateOf(false) }
    val canAddCalendar = reminder.dateTime != null
    val zoneId = ZoneId.systemDefault()
    val alarmDateTime = alarmTime.toLocalDateTime(zoneId)
    val calendarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            calendarSaved = true
            statusText = "Calendar event saved"
        } else if (!calendarSaved) {
            statusText = "Finish the event in your calendar app to save it."
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Saved", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(reminder.title, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (canAddCalendar) {
                Button(
                    onClick = {
                        runCatching { calendarLauncher.launch(CalendarIntentBuilder.build(reminder)) }
                            .onFailure { statusText = "Calendar app not available on this device." }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (calendarSaved) AppSuccess else MaterialTheme.colorScheme.primary,
                        contentColor = if (calendarSaved) Color.White else MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(if (calendarSaved) "Saved to calendar" else "Add to calendar")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Phone alarm", fontWeight = FontWeight.SemiBold)
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
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(alarmDateTime.format(DateTimeFormatter.ofPattern("MMM d")))
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
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(alarmDateTime.format(DateTimeFormatter.ofPattern("h:mm a")))
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
                                statusText = "Phone alarm set"
                            }.onFailure {
                                statusText = "Could not set the phone reminder."
                            }
                            scheduling = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = alarmTime > System.currentTimeMillis() && alarmTitle.isNotBlank() && !scheduling,
                    shape = RoundedCornerShape(18.dp),
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

            statusText?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        suggestions.forEach { suggestion ->
            OutlinedButton(
                onClick = { onSelected(suggestion.time) },
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f),
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
                Text(suggestion.label)
            }
        }
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
