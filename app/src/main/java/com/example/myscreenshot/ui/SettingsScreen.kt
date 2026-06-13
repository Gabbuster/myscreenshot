package com.example.myscreenshot.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myscreenshot.ui.theme.MyScreenshotTheme

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var localOnly by remember { mutableStateOf(true) }
    val notificationsGranted = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

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
                Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Dark mode")
                    Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
                }
                Text("Uses a darker version of the new soft workspace palette.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Local processing only")
                    Switch(checked = localOnly, onCheckedChange = { localOnly = it }, enabled = false)
                }
                Text("Your screenshots stay on this phone. No account. No cloud upload.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsCard {
                Text("Notification permission status", fontWeight = FontWeight.SemiBold)
                Text(if (notificationsGranted) "Allowed" else "Not allowed")
                Text("Exact reminders use alarms when available. WorkManager is used if exact alarms are not available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsCard {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) { Text("Calendar integration") }
                Text("Calendar events open in your calendar app for confirmation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsCard {
                Text("Local AI Assist coming soon.", fontWeight = FontWeight.SemiBold)
                Text("It will improve messy screenshots and still run privately on your phone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsCard {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) { Text("Export local backup") }
                TextButton(onClick = {}) { Text("Clear all data") }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    MyScreenshotTheme { SettingsScreen(false, {}, {}) }
}
