package com.example.myscreenshot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.navigation.AppNavHost
import com.example.myscreenshot.ocr.SharedInput
import com.example.myscreenshot.reminders.NotificationHelper
import com.example.myscreenshot.ui.theme.MyScreenshotTheme

class MainActivity : ComponentActivity() {
    private var sharedInputState = mutableStateOf<SharedInput?>(null)
    private val preferences by lazy { getSharedPreferences("screenshot_reminder", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper(this).ensureChannel()
        sharedInputState.value = parseSharedInput(intent)

        setContent {
            val repository = remember { AppRepository(applicationContext) }
            var sharedInput by remember { sharedInputState }
            var darkMode by remember { mutableStateOf(preferences.getBoolean("dark_mode", false)) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) {}

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            MyScreenshotTheme(darkTheme = darkMode) {
                AppNavHost(
                    repository = repository,
                    initialSharedInput = sharedInput,
                    darkMode = darkMode,
                    onDarkModeChange = {
                        darkMode = it
                        preferences.edit().putBoolean("dark_mode", it).apply()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedInputState.value = parseSharedInput(intent)
    }

    private fun parseSharedInput(intent: Intent?): SharedInput? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val mimeType = intent.type
        val stream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        stream?.let {
            contentResolver.takePersistableUriPermissionIfPossible(intent, it)
            return SharedInput(uri = it, text = null, mimeType = mimeType)
        }
        return text?.let { SharedInput(uri = null, text = it, mimeType = mimeType ?: "text/plain") }
    }

    private fun android.content.ContentResolver.takePersistableUriPermissionIfPossible(intent: Intent, uri: Uri) {
        val flags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (flags and Intent.FLAG_GRANT_READ_URI_PERMISSION == 0) return
        runCatching { takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}
