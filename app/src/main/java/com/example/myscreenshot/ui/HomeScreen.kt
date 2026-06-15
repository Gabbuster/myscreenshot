package com.example.myscreenshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.ui.components.AppLogo
import com.example.myscreenshot.ui.components.FilterChipRow
import com.example.myscreenshot.ui.components.ReminderCard
import com.example.myscreenshot.ui.tags.EmptyTagsCard
import com.example.myscreenshot.ui.tags.TagCategoryRow
import com.example.myscreenshot.ui.tags.TagEditorDialog
import com.example.myscreenshot.ui.tags.TagsSectionHeader
import com.example.myscreenshot.ui.tags.tagSummaries
import com.example.myscreenshot.ui.theme.AppInk
import com.example.myscreenshot.ui.theme.AppOrange
import com.example.myscreenshot.ui.theme.AppProof
import com.example.myscreenshot.ui.theme.AppScan
import com.example.myscreenshot.ui.theme.MyScreenshotTheme
import java.time.LocalTime
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: AppRepository,
    onOpenReminder: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onTrySample: () -> Unit,
    onAddManual: () -> Unit,
) {
    val reminders by repository.observeReminders().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    HomeContent(
        reminders = reminders,
        onOpenReminder = onOpenReminder,
        onOpenSettings = onOpenSettings,
        onTrySample = onTrySample,
        onAddManual = onAddManual,
        onAssignTag = { reminder, tagName, tagColor ->
            scope.launch {
                repository.assignTag(reminder, tagName, tagColor)
            }
        },
    )
}

@Composable
fun HomeContent(
    reminders: List<Reminder>,
    onOpenReminder: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onTrySample: () -> Unit,
    onAddManual: () -> Unit,
    onAssignTag: (Reminder, String?, String?) -> Unit = { _, _, _ -> },
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("Home") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var tagEditorReminder by remember { mutableStateOf<Reminder?>(null) }
    val tagSummaries = reminders.tagSummaries()
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = NavigationBarDefaults.Elevation,
            ) {
                val navItems = listOf(
                    "Home",
                    "Tags",
                    "Settings",
                )
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedSection == item,
                        onClick = {
                            when (item) {
                                "Settings" -> onOpenSettings()
                                else -> selectedSection = item
                            }
                        },
                        icon = { NavDot(selected = selectedSection == item) },
                        label = { Text(item) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AttentionHero(
                    reminders = reminders,
                    onAddScreenshot = onAddManual,
                    onToggleSearch = { searchVisible = !searchVisible },
                    onOpenSettings = onOpenSettings,
                )
            }
            if (searchVisible) {
                item {
                    SearchField(searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it })
                }
            }
            if (selectedSection == "Tags") {
                item {
                    TagsSectionHeader(tagSummaries = tagSummaries)
                }
                if (tagSummaries.isEmpty()) {
                    item {
                        EmptyTagsCard()
                    }
                } else {
                    item {
                        TagCategoryRow(
                            tags = tagSummaries,
                            selectedTag = selectedTag,
                            onSelected = { selectedTag = it.name },
                        )
                    }
                    val tagReminders = selectedTag
                        ?.let { tag -> reminders.filter { it.tagName == tag } }
                        ?: reminders.filter { it.tagName != null }
                    item {
                        Text(
                            selectedTag ?: "All tagged reminders",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    items(tagReminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onClick = { onOpenReminder(reminder.id) },
                            onTagClick = { tagEditorReminder = it },
                        )
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Focus by type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${reminders.count { it.dateTime == null || it.dateTime >= System.currentTimeMillis() }} active", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
                item {
                    FilterChipRow(selected = selectedFilter, onSelected = { selectedFilter = it })
                }
                val visibleReminders = reminders.filterFor(selectedFilter).search(searchQuery)
                if (visibleReminders.isEmpty()) {
                    item {
                        Spacer(Modifier.height(18.dp))
                        EmptyStateCard(onTrySample = onTrySample)
                    }
                } else {
                    item {
                        Text("Recent captures", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                    items(visibleReminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onClick = { onOpenReminder(reminder.id) },
                            onTagClick = { tagEditorReminder = it },
                        )
                    }
                }
            }
        }
    }

    tagEditorReminder?.let { reminder ->
        TagEditorDialog(
            reminder = reminder,
            existingTags = tagSummaries,
            onDismiss = { tagEditorReminder = null },
            onSave = { name, color ->
                onAssignTag(reminder, name, color)
                tagEditorReminder = null
            },
        )
    }
}

@Composable
private fun AttentionHero(
    reminders: List<Reminder>,
    onAddScreenshot: () -> Unit,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val active = reminders.count { it.dateTime == null || it.dateTime >= now }
    val urgent = reminders.count { reminder ->
        reminder.dateTime?.let { it in now..(now + 86_400_000L) } == true
    }
    val awaitingReview = reminders.count { it.confidence.equals("Review needed", ignoreCase = true) }
    val headline = when {
        urgent > 0 -> "$urgent things need attention."
        active > 0 -> "Your screenshots are under control."
        else -> "Nothing important is being forgotten."
    }
    val status = when {
        urgent > 0 -> "Due today"
        active > 0 -> greeting()
        else -> "Nothing urgent today"
    }
    val supportText = if (reminders.isEmpty()) {
        "Snappy reads the screenshot and turns the useful bit into a reminder."
    } else {
        "Snappy found ${reminders.size} reminders in your captures."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(8.dp), ambientColor = AppInk.copy(alpha = 0.08f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BoxWithConstraints {
            val compact = maxWidth < 340.dp
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(AppInk, RoundedCornerShape(8.dp))
                                .border(1.dp, AppOrange.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppLogo(Modifier.size(50.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Screen4U", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text("Snappy is on it", color = AppOrange, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        ) {
                            SearchGlyph(color = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        ) {
                            SettingsGlyph(color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(status.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(
                        headline,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        supportText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Button(
            onClick = onAddScreenshot,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppInk,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 13.dp),
        ) {
            Text("+ Add Screenshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeroChip("Active", active.toString(), Modifier.weight(1f))
            HeroChip("Due today", urgent.toString(), Modifier.weight(1f))
            HeroChip("Review", awaitingReview.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroChip(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(value, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun SearchField(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("FIND", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                Box(Modifier.weight(1f)) {
                    if (searchQuery.isBlank()) {
                        Text(
                            "bill, flight, appointment...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun NavDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(if (selected) 22.dp else 10.dp)
            .background(
                if (selected) Brush.linearGradient(listOf(AppScan, AppProof)) else Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f), MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)),
                ),
                RoundedCornerShape(if (selected) 7.dp else 3.dp),
            )
    )
}

@Composable
private fun SearchGlyph(color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawCircle(
            color = color,
            radius = size.minDimension * 0.32f,
            center = center.copy(x = center.x - size.minDimension * 0.08f, y = center.y - size.minDimension * 0.08f),
            style = Stroke(width = 2.4.dp.toPx()),
        )
        drawLine(
            color = color,
            start = center.copy(x = center.x + size.minDimension * 0.18f, y = center.y + size.minDimension * 0.18f),
            end = center.copy(x = center.x + size.minDimension * 0.42f, y = center.y + size.minDimension * 0.42f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SettingsGlyph(color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val radius = 2.dp.toPx()
        listOf(0.26f, 0.5f, 0.74f).forEach { fraction ->
            drawCircle(color = color, radius = radius, center = center.copy(x = size.width * fraction))
        }
    }
}

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..22 -> "Good evening"
        else -> "Quiet hours"
    }
}

private fun List<Reminder>.filterFor(filter: String): List<Reminder> =
    if (filter == "All") this else filter { it.type.equals(filter.removeSuffix("s"), true) || it.type.equals(filter, true) }

private fun List<Reminder>.search(query: String): List<Reminder> {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return this
    return filter {
        it.title.contains(cleanQuery, ignoreCase = true) ||
            it.type.contains(cleanQuery, ignoreCase = true) ||
            it.notes.contains(cleanQuery, ignoreCase = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    MyScreenshotTheme {
        HomeContent(Samples.reminders(), {}, {}, {}, {})
    }
}
