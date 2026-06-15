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
            .heightIn(min = 112.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReminderThumbnailWithBookmark(
                reminder = reminder,
                onTagClick = onTagClick,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    reminder.title,
                    color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(reminder.type.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                reminder.dateTime?.let { start ->
                    val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    val startStr = df.format(Date(start))
                    val dateText = if (reminder.endDateTime != null) {
                        "$startStr - ${df.format(Date(reminder.endDateTime))}"
                    } else {
                        startStr
                    }
                    Text(
                        dateText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (reminder.notes.isNotBlank()) {
                    Text(
                        reminder.notes.lineSequence().first(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (isPast) {
                PassedStamp()
            }
        }
    }
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
            modifier = Modifier.align(Alignment.TopStart),
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
