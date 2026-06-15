package com.example.myscreenshot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.ui.tags.toTagColor
import java.text.DateFormat
import java.util.Date

@Composable
fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onTagClick: (Reminder) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isPast = reminder.dateTime?.let { it < System.currentTimeMillis() } == true
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(7.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.68f), RoundedCornerShape(8.dp))
            .heightIn(min = 96.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReminderThumbnailWithBookmark(
                reminder = reminder,
                onTagClick = onTagClick,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    reminder.title,
                    color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                reminder.usefulPreview()?.let { preview ->
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                reminder.dateTime?.let { start ->
                    Text(
                        reminder.dateLabel(start),
                        color = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isPast) {
                PassedStamp()
            }
        }
    }
}

private fun Reminder.usefulPreview(): String? =
    notes
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?: ocrText
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length > 8 }

private fun Reminder.dateLabel(start: Long): String {
    val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    val startText = df.format(Date(start))
    return endDateTime?.let { "$startText - ${df.format(Date(it))}" } ?: startText
}

@Composable
private fun ReminderThumbnailWithBookmark(
    reminder: Reminder,
    onTagClick: (Reminder) -> Unit,
) {
    Box(modifier = Modifier.size(72.dp)) {
        SourceThumbnail(
            sourceType = reminder.sourceType,
            sourceUri = reminder.sourceImageUri,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(64.dp),
        )
        BookmarkButton(
            tagColor = reminder.tagColor,
            onClick = { onTagClick(reminder) },
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun BookmarkButton(
    tagColor: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = tagColor?.toTagColor() ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .size(width = 20.dp, height = 42.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp),
                ),
        )
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 42.dp)
                .background(
                    color = if (tagColor == null) MaterialTheme.colorScheme.surface else color,
                    shape = RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp),
                )
                .border(
                    width = 2.dp,
                    color = color,
                    shape = RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp),
                ),
        )
    }
}
