package com.example.myscreenshot.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.extraction.ActionDetector
import com.example.myscreenshot.extraction.DetectedAction
import com.example.myscreenshot.extraction.EntityExtractor
import com.example.myscreenshot.ocr.OcrProcessor
import com.example.myscreenshot.ocr.SharedInput
import com.example.myscreenshot.ui.components.ActionCard
import com.example.myscreenshot.ui.components.SourceThumbnail
import com.example.myscreenshot.ui.theme.AppCoral
import com.example.myscreenshot.ui.theme.AppOrange
import com.example.myscreenshot.ui.theme.MyScreenshotTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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
    var showDetectedText by remember { mutableStateOf(false) }
    var duplicateMatches by remember { mutableStateOf<List<DuplicateMatch>>(emptyList()) }

    LaunchedEffect(sharedInput, useSample) {
        loading = true
        errorMessage = null
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
        val entities = EntityExtractor(context).extract(ocr.text)
        val detected = ActionDetector().detect(
            text = ocr.text,
            entities = entities,
            now = LocalDateTime.now(),
            zoneId = ZoneId.systemDefault(),
        )
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
                var lastId: String? = null
                drafts.filter { it.checked }.forEach { draft ->
                    val reminder = repository.saveAction(
                        action = draft.action,
                        title = draft.title,
                        notes = draft.notes,
                        sourceType = sourceType,
                        sourceUri = sourceUri,
                        ocrText = extractedText,
                    )
                    lastId = reminder.id
                }
                lastId
            }.onSuccess { lastId ->
                saving = false
                duplicateMatches = emptyList()
                lastId?.let(onSaved)
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
}

@Composable
fun ReviewSaveContent(
    loading: Boolean,
    sourceType: String,
    sourceNote: String,
    sourceUri: String?,
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
                    .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(26.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SourceThumbnail(sourceType, modifier = Modifier.height(76.dp), sourceUri = sourceUri)
                Text("Local capture analysis", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(sourceNote, color = Color.White.copy(alpha = 0.72f))
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
                        Text("${drafts.size} actions found", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Ready to save", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f))
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
