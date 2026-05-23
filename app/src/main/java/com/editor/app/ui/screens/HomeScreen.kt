package com.editor.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.editor.app.repository.ProjectSummary
import com.editor.app.ui.theme.*
import com.editor.app.viewmodel.HomeViewModel

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun formatRelativeDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L        -> "Just now"
        diff < 3_600_000L     -> "${diff / 60_000}m ago"
        diff < 86_400_000L    -> "${diff / 3_600_000}h ago"
        diff < 172_800_000L   -> "Yesterday"
        diff < 604_800_000L   -> "${diff / 86_400_000} days ago"
        else                  -> "${diff / 604_800_000} weeks ago"
    }
}

/** Deterministic colour per project — cycles through palette using id hash. */
private val palette = listOf(
    Color(0xFF0066DD), Color(0xFF9900DD), Color(0xFF00AA66),
    Color(0xFFAA5500), Color(0xFFDD0066), Color(0xFF006688), Color(0xFF880066)
)
private fun colorFor(id: String): Color =
    palette[id.hashCode().and(0x7FFF_FFFF) % palette.size]

// ─── Home Screen ──────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNewProject: () -> Unit,
    onOpenProject: (String) -> Unit,
    onTemplatesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onCloudBackupClick: () -> Unit = {}
) {
    val projects  by homeViewModel.projects.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { homeViewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        HomeTopBar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onSettingsClick = onSettingsClick,
            onNotificationsClick = onNotificationsClick,
            onCloudBackupClick = onCloudBackupClick
        )
        QuickActionsRow(onNewProject = onNewProject, onTemplatesClick = onTemplatesClick)
        RecentProjectsSection(
            projects  = projects.filter { it.name.contains(searchQuery, ignoreCase = true) },
            isLoading = isLoading,
            onOpenProject  = onOpenProject,
            onDeleteProject = { homeViewModel.deleteProject(it) },
            onRenameProject = { id, name -> homeViewModel.renameProject(id, name) }
        )
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
fun HomeTopBar(searchQuery: String, onSearchChange: (String) -> Unit, onSettingsClick: () -> Unit, onNotificationsClick: () -> Unit, onCloudBackupClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "VIBE EDIT",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    "Video & Audio Editor",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCloudBackupClick) {
                    Icon(Icons.Default.CloudUpload, "Cloud Backup", tint = TextSecondary)
                }
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Default.Notifications, "Notifications", tint = TextSecondary)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, "Settings", tint = TextSecondary)
                }
            }
        }

        OutlinedTextField(
            value          = searchQuery,
            onValueChange  = onSearchChange,
            placeholder    = { Text("Search projects...", color = TextSecondary) },
            leadingIcon    = { Icon(Icons.Default.Search, "Search", tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon   = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = NeonCyan,
                unfocusedBorderColor = Color(0xFF2A2A3A),
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
                cursorColor          = NeonCyan,
                focusedContainerColor   = CardDark,
                unfocusedContainerColor = CardDark
            ),
            shape      = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

// ─── Quick Actions ────────────────────────────────────────────────────────────

private data class QuickAction(
    val label: String, val icon: ImageVector, val color: Color, val description: String
)

private val quickActions = listOf(
    QuickAction("New Video",  Icons.Default.VideoCall,   NeonCyan,     "Start a video project"),
    QuickAction("New Audio",  Icons.Default.AudioFile,   NeonPurple,   "Audio-only project"),
    QuickAction("Import",     Icons.Default.FileUpload,  NeonPink,     "Import from gallery"),
    QuickAction("Templates",  Icons.Default.Dashboard,   AccentOrange, "Start from template")
)

@Composable
fun QuickActionsRow(onNewProject: () -> Unit, onTemplatesClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Quick Start", style = MaterialTheme.typography.titleLarge, color = TextPrimary)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(quickActions) { action ->
                Column(
                    modifier = Modifier.width(90.dp).clickable(onClick = when (action.label) {
                        "Templates" -> onTemplatesClick
                        else -> onNewProject
                    }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(action.color.copy(0.1f), RoundedCornerShape(18.dp))
                            .border(1.dp, action.color.copy(0.3f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(action.icon, action.label, tint = action.color, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        action.label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ─── Recent Projects ──────────────────────────────────────────────────────────

@Composable
fun RecentProjectsSection(
    projects: List<ProjectSummary>,
    isLoading: Boolean,
    onOpenProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Projects", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
                if (isLoading) "Loading…"
                else "${projects.size} project${if (projects.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(32.dp))
                }
            }

            projects.isEmpty() -> EmptyProjectsPlaceholder()

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project  = project,
                            onClick  = { onOpenProject(project.id) },
                            onDelete = { onDeleteProject(project.id) },
                            onRename = { newName -> onRenameProject(project.id, newName) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Project Card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    val color = colorFor(project.id)
    var showMenu         by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText       by remember { mutableStateOf(project.name) }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = CardDark,
            title = { Text("Delete Project?", color = TextPrimary) },
            text  = {
                Text(
                    "\"${project.name}\" will be permanently deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = NeonPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── Rename dialog ─────────────────────────────────────────────────────────
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor   = CardDark,
            title = { Text("Rename Project", color = TextPrimary) },
            text  = {
                OutlinedTextField(
                    value         = renameText,
                    onValueChange = { renameText = it },
                    singleLine    = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = NeonCyan,
                        unfocusedBorderColor = Color(0xFF2A2A3A),
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = NeonCyan,
                        focusedContainerColor   = CardDark,
                        unfocusedContainerColor = CardDark
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        if (renameText.isNotBlank()) {
                            onRename(renameText.trim())
                        }
                        showRenameDialog = false
                    },
                    enabled = renameText.isNotBlank()
                ) {
                    Text("Rename", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false; renameText = project.name }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── Card ──────────────────────────────────────────────────────────────────
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onClick,
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF22223A))
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!project.thumbnailUri.isNullOrBlank()) {
                    AsyncImage(
                        model = project.thumbnailUri,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(color.copy(0.3f), color.copy(0.1f))))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                Brush.linearGradient(listOf(color.copy(0.4f), color.copy(0.1f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint   = Color.White.copy(0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                if (project.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            formatDuration(project.durationMs),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Info row
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    project.name,
                    style      = MaterialTheme.typography.bodySmall,
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        formatRelativeDate(project.updatedAt),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextSecondary,
                        fontSize = 9.sp
                    )
                    Box(Modifier.size(6.dp).background(color, CircleShape))
                }
            }
        }

        // Long-press context menu
        DropdownMenu(
            expanded          = showMenu,
            onDismissRequest  = { showMenu = false },
            modifier          = Modifier.background(CardDark)
        ) {
            DropdownMenuItem(
                text = { Text("Open", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.OpenInNew, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                onClick = { showMenu = false; onClick() }
            )
            DropdownMenuItem(
                text = { Text("Rename", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                onClick = {
                    showMenu = false
                    renameText = project.name
                    showRenameDialog = true
                }
            )
            HorizontalDivider(color = Color(0xFF22223A))
            DropdownMenuItem(
                text = { Text("Delete", color = NeonPink, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = NeonPink, modifier = Modifier.size(16.dp)) },
                onClick = { showMenu = false; showDeleteDialog = true }
            )
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyProjectsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                tint     = TextSecondary,
                modifier = Modifier.size(52.dp)
            )
            Text(
                "No projects yet",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Tap Quick Start to create your first project",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(0.6f)
            )
        }
    }
}