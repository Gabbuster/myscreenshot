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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val drafts = remember { mutableStateListOf<ActionDraft>() }
    var extractedText by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(sharedInput?.sourceType ?: "image") }
    var sourceNote by remember { mutableStateOf("Processing locally") }
    var sourceUri by remember { mutableStateOf(sharedInput?.uri?.toString()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDetectedText by remember { mutableStateOf(false) }

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
            detected
                .sortedBy { it.confidence.priority }
                .map { ActionDraft(it, true, it.title, defaultNotes(it)) },
        )
        loading = false
    }

    ReviewSaveContent(
        loading = loading,
        sourceType = sourceType,
        sourceNote = sourceNote,
        errorMessage = errorMessage,
        saving = saving,
        detectedText = extractedText,
        showDetectedText = showDetectedText,
        drafts = drafts,
        onBack = onBack,
        onSave = {
            scope.launch {
                saving = true
                runCatching {
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
                    lastId?.let(onSaved)
                }.onFailure {
                    saving = false
                    errorMessage = "Could not save this reminder. Please try again."
                }
            }
        },
        onDraftChanged = { index, draft ->
            drafts[index] = draft
        },
        onToggleDetectedText = { showDetectedText = !showDetectedText },
    )
}

@Composable
fun ReviewSaveContent(
    loading: Boolean,
    sourceType: String,
    sourceNote: String,
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
                SourceThumbnail(sourceType, modifier = Modifier.height(76.dp))
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
                onCheckedChange = { onDraftChanged(index, draft.copy(checked = it)) },
                onTitleChange = { onDraftChanged(index, draft.copy(title = it)) },
                onNotesChange = { onDraftChanged(index, draft.copy(notes = it)) },
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

data class ActionDraft(
    val action: DetectedAction,
    val checked: Boolean,
    val title: String,
    val notes: String,
)

private fun defaultNotes(action: DetectedAction): String =
    action.reminderSuggestions.joinToString("\n") { it.label }

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
