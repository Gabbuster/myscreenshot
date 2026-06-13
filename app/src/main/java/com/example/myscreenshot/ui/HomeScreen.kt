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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.ui.components.FilterChipRow
import com.example.myscreenshot.ui.components.ReminderCard
import com.example.myscreenshot.ui.theme.AppCoral
import com.example.myscreenshot.ui.theme.AppInk
import com.example.myscreenshot.ui.theme.AppOrange
import com.example.myscreenshot.ui.theme.MyScreenshotTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    repository: AppRepository,
    onOpenReminder: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onTrySample: () -> Unit,
    onAddManual: () -> Unit,
) {
    val reminders by repository.observeReminders().collectAsState(initial = emptyList())
    HomeContent(
        reminders = reminders,
        onOpenReminder = onOpenReminder,
        onOpenSettings = onOpenSettings,
        onTrySample = onTrySample,
        onAddManual = onAddManual,
    )
}

@Composable
fun HomeContent(
    reminders: List<Reminder>,
    onOpenReminder: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onTrySample: () -> Unit,
    onAddManual: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddManual,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = NavigationBarDefaults.Elevation,
            ) {
                val navItems = listOf(
                    "Home",
                    "Review",
                    "Archive",
                    "Settings",
                )
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = item == "Home",
                        onClick = {
                            when (item) {
                                "Settings" -> onOpenSettings()
                                "Review" -> onAddManual()
                            }
                        },
                        icon = { NavDot(selected = item == "Home") },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Brush.linearGradient(listOf(AppOrange, AppCoral)),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("SR", color = AppInk, fontWeight = FontWeight.Bold)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Good morning", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                            Text("Your Reminders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { searchVisible = !searchVisible },
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        ) {
                            SearchGlyph()
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        ) {
                            SettingsGlyph()
                        }
                    }
                }
            }
            item {
                WorkspaceCard(reminders = reminders, onAddManual = onAddManual)
            }
            if (searchVisible) {
                item {
                    SearchField(searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Quick filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${reminders.count { it.dateTime == null || it.dateTime >= System.currentTimeMillis() }} active", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Recent activity", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
                items(visibleReminders) { reminder ->
                    ReminderCard(reminder = reminder, onClick = { onOpenReminder(reminder.id) })
                }
            }
        }
    }
}

@Composable
private fun WorkspaceCard(reminders: List<Reminder>, onAddManual: () -> Unit) {
    val now = System.currentTimeMillis()
    val active = reminders.count { it.dateTime == null || it.dateTime >= now }
    val urgent = reminders.count { reminder ->
        reminder.dateTime?.let { it in now..(now + 86_400_000L) } == true
    }
    val nextReminder = reminders
        .filter { it.dateTime != null && it.dateTime >= now }
        .minByOrNull { it.dateTime ?: Long.MAX_VALUE }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(26.dp), ambientColor = AppInk.copy(alpha = 0.18f))
            .background(AppInk, RoundedCornerShape(26.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Screenshot workspace", color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.labelLarge)
            Text("Capture what matters", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(134.dp)
                .background(Brush.linearGradient(listOf(AppOrange, AppCoral)), RoundedCornerShape(22.dp))
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatBlock("Active", active.toString(), "saved reminders", Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.26f))
            )
            StatBlock("Today", urgent.toString(), "need attention", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Next reminder", color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.labelMedium)
                Text(
                    nextReminder?.title ?: "No deadline yet",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                nextReminder?.dateTime?.let {
                    Text(
                        DateTimeFormatter.ofPattern("MMM d, h:mm a")
                            .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Button(
                onClick = onAddManual,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = AppInk,
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = AppInk.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = AppInk, style = MaterialTheme.typography.headlineMedium)
        Text(caption, color = AppInk.copy(alpha = 0.74f), style = MaterialTheme.typography.bodySmall)
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
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
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
                if (selected) Brush.linearGradient(listOf(AppOrange, AppCoral)) else Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f), MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)),
                ),
                CircleShape,
            )
    )
}

@Composable
private fun SearchGlyph() {
    val color = MaterialTheme.colorScheme.onSurface
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
private fun SettingsGlyph() {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.size(22.dp)) {
        val radius = 2.dp.toPx()
        listOf(0.26f, 0.5f, 0.74f).forEach { fraction ->
            drawCircle(color = color, radius = radius, center = center.copy(x = size.width * fraction))
        }
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
