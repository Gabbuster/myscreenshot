package com.example.myscreenshot.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.ocr.ScreenshotBackfillScanner
import com.example.myscreenshot.ocr.SharedInput
import com.example.myscreenshot.ui.EmptyStateScreen
import com.example.myscreenshot.ui.HomeScreen
import com.example.myscreenshot.ui.ReminderDetailScreen
import com.example.myscreenshot.ui.ReviewSaveScreen
import com.example.myscreenshot.ui.SettingsScreen
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    repository: AppRepository,
    initialSharedInput: SharedInput?,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val startDestination = if (initialSharedInput == null) Routes.HOME else Routes.REVIEW
    var sharedInput by remember { mutableStateOf(initialSharedInput) }
    var showImportSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBatchInputs by remember { mutableStateOf<List<SharedInput>>(emptyList()) }

    fun openNextBatchInput(): Boolean {
        val next = pendingBatchInputs.firstOrNull() ?: return false
        sharedInput = next
        pendingBatchInputs = pendingBatchInputs.drop(1)
        navController.navigate(Routes.REVIEW) {
            launchSingleTop = true
        }
        return true
    }

    fun openHome() {
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun startScreenshotBackfill() {
        scope.launch {
            val inputs = ScreenshotBackfillScanner(context).findRecentScreenshots(daysBack = 30)
            if (inputs.isEmpty()) return@launch
            sharedInput = inputs.first()
            pendingBatchInputs = inputs.drop(1)
            navController.navigate(Routes.REVIEW) {
                launchSingleTop = true
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        sharedInput = SharedInput(
            uri = uri,
            text = null,
            mimeType = context.contentResolver.getType(uri) ?: "image/*",
        )
        navController.navigate(Routes.REVIEW) {
            launchSingleTop = true
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            sharedInput = SharedInput(uri = uri, text = null, mimeType = "image/jpeg")
            navController.navigate(Routes.REVIEW) {
                launchSingleTop = true
            }
        }
        pendingCameraUri = null
    }
    val screenshotPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startScreenshotBackfill()
        }
    }
    val goBackOrHome = {
        if (!navController.popBackStack()) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            navController.navigate(Routes.HOME) {
                if (currentRoute != null) {
                    popUpTo(currentRoute) {
                        inclusive = true
                    }
                }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(initialSharedInput) {
        if (initialSharedInput != null) {
            sharedInput = initialSharedInput
            navController.navigate(Routes.REVIEW) {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                onOpenReminder = { navController.navigate(Routes.detail(it)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onTrySample = { navController.navigate(Routes.REVIEW_SAMPLE) },
                onAddManual = { showImportSheet = true },
            )
        }
        composable(Routes.REVIEW) {
            ReviewSaveScreen(
                repository = repository,
                sharedInput = sharedInput,
                useSample = false,
                onBack = goBackOrHome,
                onSaved = {
                    if (!openNextBatchInput()) openHome()
                },
            )
        }
        composable(Routes.REVIEW_SAMPLE) {
            ReviewSaveScreen(
                repository = repository,
                sharedInput = null,
                useSample = true,
                onBack = goBackOrHome,
                onSaved = {
                    openHome()
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.EMPTY) {
            EmptyStateScreen(onTrySample = { navController.navigate(Routes.REVIEW) })
        }
        composable(
            route = "${Routes.DETAIL}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            ReminderDetailScreen(
                repository = repository,
                reminderId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }

    if (showImportSheet) {
        ImportImageSheet(
            onDismiss = { showImportSheet = false },
            onCamera = {
                showImportSheet = false
                val uri = context.createCapturedImageUri()
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            },
            onPhotoLibrary = {
                showImportSheet = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onScanRecentScreenshots = {
                showImportSheet = false
                val permission = if (Build.VERSION.SDK_INT >= 33) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    startScreenshotBackfill()
                } else {
                    screenshotPermissionLauncher.launch(permission)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportImageSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onPhotoLibrary: () -> Unit,
    onScanRecentScreenshots: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Import screenshot", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Take a new photo or choose an existing image to scan for reminders.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onCamera,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Open camera")
            }
            OutlinedButton(
                onClick = onPhotoLibrary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
            ) {
                Text("Choose from photos")
            }
            OutlinedButton(
                onClick = onScanRecentScreenshots,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
            ) {
                Text("Scan recent screenshots")
            }
        }
    }
}

private fun Context.createCapturedImageUri(): Uri {
    val imageDir = File(cacheDir, "captured_images").apply { mkdirs() }
    val imageFile = File.createTempFile("capture_", ".jpg", imageDir)
    return FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        imageFile,
    )
}
