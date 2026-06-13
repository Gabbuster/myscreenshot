package com.example.myscreenshot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myscreenshot.data.AppRepository
import com.example.myscreenshot.ocr.SharedInput
import com.example.myscreenshot.ui.EmptyStateScreen
import com.example.myscreenshot.ui.HomeScreen
import com.example.myscreenshot.ui.ReminderDetailScreen
import com.example.myscreenshot.ui.ReviewSaveScreen
import com.example.myscreenshot.ui.SettingsScreen

@Composable
fun AppNavHost(
    repository: AppRepository,
    initialSharedInput: SharedInput?,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val startDestination = if (initialSharedInput == null) Routes.HOME else Routes.REVIEW
    val sharedInput = initialSharedInput
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
                onAddManual = { navController.navigate(Routes.REVIEW_SAMPLE) },
            )
        }
        composable(Routes.REVIEW) {
            ReviewSaveScreen(
                repository = repository,
                sharedInput = sharedInput,
                useSample = false,
                onBack = goBackOrHome,
                onSaved = { navController.navigate(Routes.detail(it)) },
            )
        }
        composable(Routes.REVIEW_SAMPLE) {
            ReviewSaveScreen(
                repository = repository,
                sharedInput = null,
                useSample = true,
                onBack = goBackOrHome,
                onSaved = { navController.navigate(Routes.detail(it)) },
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
}
