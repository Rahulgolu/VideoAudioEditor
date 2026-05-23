package com.editor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editor.app.ui.screens.CloudBackupSheet
import com.editor.app.ui.screens.EditorScreen
import com.editor.app.ui.screens.HomeScreen
import com.editor.app.ui.screens.PermissionScreen
import com.editor.app.ui.screens.TemplatesSheet
import com.editor.app.ui.screens.SettingsSheet
import com.editor.app.ui.screens.NotificationsSheet
import com.editor.app.ui.theme.VideoAudioEditorTheme
import com.editor.app.viewmodel.EditorViewModel
import com.editor.app.viewmodel.HomeViewModel
import com.editor.app.utils.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val themePrefs = ThemePreferences(this)
        
        setContent {
            var themeMode by remember { mutableStateOf(themePrefs.themeMode) }
            
            VideoAudioEditorTheme(themeMode = themeMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    AppNavigation(
                        themePrefs = themePrefs,
                        onThemeChange = { themeMode = it }
                    )
                }
            }
        }
    }
}

enum class Screen { PERMISSION, HOME, EDITOR }

@Composable
fun AppNavigation(
    themePrefs: ThemePreferences,
    onThemeChange: (com.editor.app.ui.theme.ThemeMode) -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.PERMISSION) }
    var showTemplates by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showCloudBackup by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(context))
    val editorViewModel: EditorViewModel = viewModel(factory = EditorViewModel.factory(context))

    when (currentScreen) {

        Screen.PERMISSION -> PermissionScreen(
            onPermissionsGranted = { currentScreen = Screen.HOME }
        )

        Screen.HOME -> {
            HomeScreen(
                homeViewModel = homeViewModel,
                onNewProject = {
                    editorViewModel.newProject()
                    currentScreen = Screen.EDITOR
                },
                onOpenProject = { projectId ->
                    editorViewModel.loadProject(projectId)
                    currentScreen = Screen.EDITOR
                },
                onTemplatesClick = { showTemplates = true },
                onSettingsClick = { showSettings = true },
                onNotificationsClick = { showNotifications = true },
                onCloudBackupClick = { showCloudBackup = true }
            )
            if (showTemplates) TemplatesSheet(
                onDismiss = { showTemplates = false },
                onSelectTemplate = { template ->
                    editorViewModel.newProjectFromTemplate(template)
                    currentScreen = Screen.EDITOR
                    showTemplates = false
                }
            )
            if (showSettings) SettingsSheet(
                onDismiss = { showSettings = false }
            )
            if (showNotifications) NotificationsSheet(onDismiss = { showNotifications = false })
            if (showCloudBackup) CloudBackupSheet(
                onDismiss = { showCloudBackup = false }
            )
        }

        Screen.EDITOR -> EditorScreen(
            viewModel = editorViewModel,
            onBack = {
                currentScreen = Screen.HOME
                homeViewModel.refresh()
            }
        )
    }
}