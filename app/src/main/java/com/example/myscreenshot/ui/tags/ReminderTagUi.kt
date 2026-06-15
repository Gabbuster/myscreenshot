package com.example.myscreenshot.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.data.Reminder

data class TagSummary(
    val name: String,
    val color: String,
    val count: Int,
)

val tagColorOptions = listOf(
    "#FF8A1F",
    "#E85D2A",
    "#1B7A4D",
    "#2F6FED",
    "#9A4DFF",
)

fun List<Reminder>.tagSummaries(): List<TagSummary> =
    filter { !it.tagName.isNullOrBlank() }
        .groupBy { it.tagName.orEmpty() }
        .map { (name, reminders) ->
            TagSummary(
                name = name,
                color = reminders.firstOrNull()?.tagColor ?: tagColorOptions.first(),
                count = reminders.size,
            )
        }
        .sortedBy { it.name.lowercase() }

fun String.toTagColor(): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrElse { Color(0xFFFF8A1F) }

@Composable
fun TagsSectionHeader(tagSummaries: List<TagSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Tags", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(
            if (tagSummaries.isEmpty()) {
                "Bookmark a capture to group trips, events, weddings, or projects."
            } else {
                "${tagSummaries.size} collections ready"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun EmptyTagsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .heightIn(min = 118.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No tags yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Tap the bookmark on a reminder thumbnail to mark a trip, event, or project.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun TagCategoryRow(
    tags: List<TagSummary>,
    selectedTag: String?,
    onSelected: (TagSummary) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        tags.forEach { tag ->
            val selected = selectedTag == tag.name
            Button(
                onClick = { onSelected(tag) },
                modifier = Modifier.width(156.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) tag.color.toTagColor() else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(width = 18.dp, height = 38.dp)
                                .background(Color.Black.copy(alpha = 0.16f), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)),
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 36.dp)
                                .background(tag.color.toTagColor(), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tag.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${tag.count} captures", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun TagEditorDialog(
    reminder: Reminder,
    existingTags: List<TagSummary>,
    onDismiss: () -> Unit,
    onSave: (String?, String?) -> Unit,
) {
    var tagName by remember(reminder.id) { mutableStateOf(reminder.tagName.orEmpty()) }
    var tagColor by remember(reminder.id) { mutableStateOf(reminder.tagColor ?: tagColorOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag this reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existingTags.isNotEmpty()) {
                    Text("Existing tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    existingTags.take(4).forEach { tag ->
                        TextButton(
                            onClick = {
                                tagName = tag.name
                                tagColor = tag.color
                            },
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(tag.color.toTagColor(), RoundedCornerShape(50)),
                                )
                                Text(tag.name)
                            }
                        }
                    }
                }
                TextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    singleLine = true,
                    label = { Text("Tag name") },
                    placeholder = { Text("Trip to Porto") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tagColorOptions.forEach { option ->
                        val selected = option == tagColor
                        Box(
                            modifier = Modifier
                                .size(if (selected) 38.dp else 32.dp)
                                .background(option.toTagColor(), RoundedCornerShape(50))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(50),
                                )
                                .clickable { tagColor = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tagName, tagColor) }) {
                Text("Save tag")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reminder.tagName != null) {
                    TextButton(onClick = { onSave(null, null) }) {
                        Text("Remove")
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}
