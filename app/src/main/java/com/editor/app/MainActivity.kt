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
import com.editor.app.ui.screens.EditorScreen
import com.editor.app.ui.screens.HomeScreen
import com.editor.app.ui.screens.PermissionScreen
import com.editor.app.ui.theme.VideoAudioEditorTheme
import com.editor.app.viewmodel.EditorViewModel
import com.editor.app.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoAudioEditorTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

enum class Screen { PERMISSION, HOME, EDITOR }

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.PERMISSION) }
    val context = LocalContext.current

    // Both ViewModels live at navigation scope — survive tab switches and screen rotation.
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(context))
    val editorViewModel: EditorViewModel = viewModel(factory = EditorViewModel.factory(context))

    when (currentScreen) {

        // ── 1. Permission gate ─────────────────────────────────────────────────
        Screen.PERMISSION -> PermissionScreen(
            onPermissionsGranted = { currentScreen = Screen.HOME }
        )

        // ── 2. Home — project list ─────────────────────────────────────────────
        Screen.HOME -> HomeScreen(
            homeViewModel = homeViewModel,

            // "New Video / Audio / Import / Templates" — always starts blank editor
            onNewProject = {
                editorViewModel.newProject()
                currentScreen = Screen.EDITOR
            },

            // Tap an existing project card → load it and open editor
            onOpenProject = { projectId ->
                editorViewModel.loadProject(projectId)
                currentScreen = Screen.EDITOR
            }
        )

        // ── 3. Editor ─────────────────────────────────────────────────────────
        Screen.EDITOR -> EditorScreen(
            viewModel = editorViewModel,
            onBack = {
                currentScreen = Screen.HOME
                // Refresh project list so any saves made in editor appear immediately
                homeViewModel.refresh()
            }
        )
    }
}