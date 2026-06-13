package com.example.myscreenshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.ui.components.AppLogo
import com.example.myscreenshot.ui.components.FilterChipRow
import com.example.myscreenshot.ui.components.ReminderCard
import com.example.myscreenshot.ui.theme.MyScreenshotTheme

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
            FloatingActionButton(onClick = onAddManual) {
                Text("+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = {
            NavigationBar {
                val navItems = listOf(
                    "Home" to "H",
                    "Reminders" to "R",
                    "Folders" to "F",
                    "Settings" to "S",
                )
                navItems.forEach { (item, icon) ->
                    NavigationBarItem(
                        selected = item == "Home",
                        onClick = { if (item == "Settings") onOpenSettings() },
                        icon = { Text(icon, fontWeight = FontWeight.Bold) },
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
                        AppLogo(Modifier.size(42.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Screenshot Reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Upcoming",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = { searchVisible = !searchVisible }) {
                            Text("?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Text("...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                FilterChipRow(selected = selectedFilter, onSelected = { selectedFilter = it })
            }
            if (searchVisible) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search reminders") },
                        singleLine = true,
                    )
                }
            }
            val visibleReminders = reminders.filterFor(selectedFilter).search(searchQuery)
            if (visibleReminders.isEmpty()) {
                item {
                    Spacer(Modifier.height(36.dp))
                    EmptyStateCard(onTrySample = onTrySample)
                }
            } else {
                items(visibleReminders) { reminder ->
                    ReminderCard(reminder = reminder, onClick = { onOpenReminder(reminder.id) })
                }
            }
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
