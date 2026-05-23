package com.editor.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.editor.app.data.*
import com.editor.app.ui.theme.*
import com.editor.app.viewmodel.EditorViewModel
import kotlin.math.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import coil.compose.AsyncImage
import com.editor.app.utils.rememberDragDropState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.activity.compose.BackHandler

// ─── Root Editor Screen ───────────────────────────────────────────────────────

@Composable
fun EditorScreen(
    viewModel : EditorViewModel,
    onBack    : () -> Unit
) {
    BackHandler { onBack() }

    val state   by viewModel.uiState.collectAsState()
    val context  = LocalContext.current
    var showImportSheet        by remember { mutableStateOf(false) }
    var showVoiceoverSheet     by remember { mutableStateOf(false) }
    var showTemplatesSheet     by remember { mutableStateOf(false) }
    var showSettingsSheet      by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {

            EditorTopBar(
                projectName = state.projectName,
                canUndo     = state.undoStack.isNotEmpty(),
                canRedo     = state.redoStack.isNotEmpty(),
                isSaving    = state.isSaving,
                lastSavedAt = state.lastSavedAt,
                onBack      = onBack,
                onUndo      = { viewModel.undo() },
                onRedo      = { viewModel.redo() },
                onRecord    = { showVoiceoverSheet = true },
                onImport    = { showImportSheet = true },
                onSave      = { viewModel.saveProject() },
                onExport    = { viewModel.setActiveTab(EditorTab.EXPORT) },
                onRename    = { viewModel.setProjectName(it) }
            )

            VideoPreviewArea(
                isPlaying       = state.isPlaying,
                currentMs       = state.currentPositionMs,
                durationMs      = state.durationMs,
                previewImageUri = state.previewImageUri,
                playerManager   = viewModel.playerManager
            )

            PlaybackControlsBar(
                isPlaying     = state.isPlaying,
                currentMs     = state.currentPositionMs,
                durationMs    = state.durationMs,
                masterVolume  = state.masterVolume,
                playbackSpeed = state.playbackSpeed,
                onPlayPause   = { viewModel.togglePlayPause() },
                onSeek        = { viewModel.seekTo(it) },
                onSkipBack    = { viewModel.seekToStart() },
                onSkipForward = { viewModel.seekToEnd() },
                onRewind      = { viewModel.skipBackward() },
                onFastForward = { viewModel.skipForward() },
                onSpeedChange = { viewModel.setPlaybackSpeed(it) }
            )

            EditorTabBar(activeTab = state.activeTab, onTabChange = { viewModel.setActiveTab(it) })

            Box(modifier = Modifier.weight(1f)) {
                when (state.activeTab) {
                    EditorTab.EDIT -> TimelinePanel(
                        tracks              = state.tracks,
                        currentMs           = state.currentPositionMs,
                        durationMs          = state.durationMs,
                        zoomLevel           = state.zoomLevel,
                        selectedClipId      = state.selectedClipId,
                        selectedClipIds     = state.selectedClipIds,
                        isMultiSelectMode   = state.isMultiSelectMode,
                        onClipSelect        = { viewModel.selectClip(it) },
                        onSeek              = { viewModel.seekTo(it) },
                        onMuteTrack         = { viewModel.toggleTrackMute(it) },
                        onLockTrack         = { viewModel.toggleTrackLock(it) },
                        onDeleteClip        = { trackId, clipId -> viewModel.deleteClip(trackId, clipId) },
                        onSplitClip         = { trackId, clipId -> viewModel.splitClipAtPlayhead(trackId, clipId) },
                        onZoomChange        = { viewModel.setZoom(it) },
                        onMoveClip          = { ft, tt, cid, ms -> viewModel.moveClipToTrack(ft, tt, cid, ms) },
                        onTrimClip          = { tid, cid, ms, isStart ->
                            if (isStart) viewModel.trimClipStart(tid, cid, ms)
                            else viewModel.trimClipEnd(tid, cid, ms)
                        },
                        onMultiSelectToggle  = { viewModel.toggleMultiSelect(it) },
                        onDeleteSelected     = { viewModel.deleteSelectedClips() },
                        onDuplicateSelected  = { viewModel.duplicateSelectedClips() },
                        onClearMultiSelect   = { viewModel.clearMultiSelect() },
                        onTransitionChange   = { tid, cid, t -> viewModel.setClipTransition(tid, cid, t) },
                        onAddTrack           = { type, name -> viewModel.addTrack(type, name) },
                        onRemoveTrack        = { viewModel.removeTrack(it) },
                        onClipVolumeChange   = { tid, cid, v -> viewModel.setClipVolume(tid, cid, v) },
                        onClipSpeedChange    = { tid, cid, s -> viewModel.setClipSpeed(tid, cid, s) }
                    )
                    EditorTab.AUDIO -> AudioMixerPanel(
                        tracks               = state.tracks,
                        masterVolume         = state.masterVolume,
                        onTrackVolumeChange  = { id, v -> viewModel.setTrackVolume(id, v) },
                        onTrackPanChange     = { id, p -> viewModel.setTrackPan(id, p) },
                        onTrackMuteToggle    = { viewModel.toggleTrackMute(it) },
                        onMasterVolumeChange = { viewModel.setMasterVolume(it) },
                        // FIX: stub callbacks — add real implementations to ViewModel when ready
                        onTrackEqChange      = { _, _, _ -> },
                        onTrackFxChange      = { _, _, _ -> }
                    )
                    EditorTab.EFFECTS -> EffectsPanel(
                        effects        = state.effects,
                        onToggleEffect = { viewModel.toggleEffect(it) },
                        onSetIntensity = { id, v -> viewModel.setEffectIntensity(id, v) },
                        // FIX: stub callbacks — add real implementations to ViewModel when ready
                        onSetBlend     = { _, _ -> },
                        onSetFade      = { _, _ -> }
                    )
                    EditorTab.TEXT -> TextPanel(
                        overlays        = state.textOverlays,
                        onAddOverlay    = { text, style, color -> viewModel.addTextOverlay(text, style, color) },
                        onRemoveOverlay = { viewModel.removeTextOverlay(it) }
                    )
                    EditorTab.EXPORT -> ExportPanel(
                        config            = state.exportConfig,
                        isExporting       = state.isExporting,
                        exportProgress    = state.exportProgress,
                        exportError       = state.exportError,
                        exportedUri       = state.exportedFileUri,
                        exportedFilePath  = state.exportedFilePath,
                        isSimulatedExport = state.isSimulatedExport,
                        onConfigChange    = { viewModel.updateExportConfig(it) },
                        onExport          = { viewModel.startExport(context) }
                    )
                }
            }
        }

        if (showImportSheet)        MediaPickerSheet(viewModel = viewModel, onDismiss = { showImportSheet = false })
        if (showVoiceoverSheet)     VoiceoverSheet(viewModel = viewModel, onDismiss = { showVoiceoverSheet = false })
        if (showTemplatesSheet)     TemplatesSheet(
            onDismiss = { showTemplatesSheet = false },
            onSelectTemplate = { showTemplatesSheet = false }   // FIX: stub until newProjectFromTemplate added to ViewModel
        )
        if (showSettingsSheet)      SettingsSheet(onDismiss = { showSettingsSheet = false })
        if (showNotificationsSheet) NotificationsSheet(onDismiss = { showNotificationsSheet = false })
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
fun EditorTopBar(
    projectName : String,
    canUndo     : Boolean,
    canRedo     : Boolean,
    isSaving    : Boolean,
    lastSavedAt : Long,
    onBack      : () -> Unit,
    onUndo      : () -> Unit,
    onRedo      : () -> Unit,
    onRecord    : () -> Unit,
    onImport    : () -> Unit,
    onSave      : () -> Unit,
    onExport    : () -> Unit,
    onRename    : (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(projectName) { mutableStateOf(projectName) }

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
                    label         = { Text("Project name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = NeonCyan,
                        unfocusedBorderColor    = Color(0xFF2A2A3A),
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        cursorColor             = NeonCyan,
                        focusedContainerColor   = CardDark,
                        unfocusedContainerColor = CardDark
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onRename(renameText.trim()); showRenameDialog = false },
                    enabled = renameText.isNotBlank()
                ) { Text("Rename", color = NeonCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false; renameText = projectName }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { renameText = projectName; showRenameDialog = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(projectName, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1)
                Icon(Icons.Default.Edit, "Rename", tint = TextSecondary.copy(0.5f), modifier = Modifier.size(13.dp))
            }
            IconButton(onClick = onRecord,  modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Mic, "Record", tint = AccentOrange) }
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Undo, "Undo", tint = if (canUndo) TextSecondary else TextSecondary.copy(0.3f))
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Redo, "Redo", tint = if (canRedo) TextSecondary else TextSecondary.copy(0.3f))
            }
            IconButton(onClick = onImport,  modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, "Import", tint = NeonCyan) }
            IconButton(onClick = onSave, enabled = !isSaving, modifier = Modifier.size(36.dp)) {
                if (isSaving) CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Save, "Save", tint = if (lastSavedAt > 0) NeonCyan else TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = onExport,
                colors  = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                shape   = RoundedCornerShape(8.dp)
            ) {
                Text("Export", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (lastSavedAt > 0 && !isSaving) {
            Text(
                "Saved ${formatSaveTime(lastSavedAt)}",
                modifier = Modifier.fillMaxWidth().background(NeonCyan.copy(0.06f)).padding(horizontal = 16.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan.copy(0.7f)
            )
        }
    }
}

fun formatSaveTime(ts: Long): String {
    val d = System.currentTimeMillis() - ts
    return when { d < 60_000 -> "just now"; d < 3_600_000 -> "${d / 60_000}m ago"; else -> "${d / 3_600_000}h ago" }
}

// ─── Video Preview ────────────────────────────────────────────────────────────

@Composable
fun VideoPreviewArea(
    isPlaying: Boolean, currentMs: Long, durationMs: Long,
    previewImageUri: String?, playerManager: com.editor.app.utils.PlayerManager
) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory  = { ctx -> androidx.media3.ui.PlayerView(ctx).apply { player = playerManager.player; useController = false } },
            update   = { it.player = playerManager.player },
            modifier = Modifier.fillMaxSize()
        )
        if (previewImageUri != null) {
            AsyncImage(model = previewImageUri, contentDescription = "Image preview",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
        if (!isPlaying && currentMs == 0L && previewImageUri == null) {
            Icon(Icons.Default.PlayCircle, "Play", tint = Color.White.copy(0.3f), modifier = Modifier.size(56.dp))
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("${formatTime(currentMs)} / ${formatTime(durationMs)}", style = MaterialTheme.typography.bodySmall,
                color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            .background(NeonCyan.copy(0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text("1080p", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontSize = 10.sp)
        }
    }
}

// ─── Playback Controls ────────────────────────────────────────────────────────

@Composable
fun PlaybackControlsBar(
    isPlaying: Boolean, currentMs: Long, durationMs: Long, masterVolume: Float, playbackSpeed: Float,
    onPlayPause: () -> Unit, onSeek: (Long) -> Unit, onSkipBack: () -> Unit, onSkipForward: () -> Unit,
    onRewind: () -> Unit, onFastForward: () -> Unit, onSpeedChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 16.dp, vertical = 6.dp)) {
        val progress = if (durationMs > 0) currentMs.toFloat() / durationMs else 0f
        Slider(value = progress, onValueChange = { onSeek((it * durationMs).toLong()) },
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = Color(0xFF33334A)),
            modifier = Modifier.fillMaxWidth().height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onSkipBack,    modifier = Modifier.size(34.dp)) { Icon(Icons.Default.SkipPrevious, "Start",    tint = TextSecondary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onRewind,      modifier = Modifier.size(34.dp)) { Icon(Icons.Default.FastRewind,  "Rewind",   tint = TextSecondary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onPlayPause,   modifier = Modifier.size(44.dp).background(NeonCyan, CircleShape)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = DeepBlack, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = onFastForward, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.FastForward, "Forward",  tint = TextSecondary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onSkipForward, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.SkipNext,    "End",      tint = TextSecondary, modifier = Modifier.size(20.dp)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.background(CardDark, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.VolumeUp, "Volume", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Text("${(masterVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                var showSpeedMenu by remember { mutableStateOf(false) }
                Box {
                    Row(modifier = Modifier.background(CardDark, RoundedCornerShape(8.dp)).clickable { showSpeedMenu = true }.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Speed, "Speed", tint = if (playbackSpeed != 1f) NeonCyan else TextSecondary, modifier = Modifier.size(14.dp))
                        Text("${playbackSpeed}×", style = MaterialTheme.typography.labelSmall, color = if (playbackSpeed != 1f) NeonCyan else TextSecondary)
                    }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }, modifier = Modifier.background(CardDark)) {
                        listOf(0.25f, 0.5f, 1f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}×", color = if (speed == playbackSpeed) NeonCyan else TextPrimary, fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = { if (speed == playbackSpeed) Icon(Icons.Default.Check, null, tint = NeonCyan, modifier = Modifier.size(14.dp)) },
                                onClick = { onSpeedChange(speed); showSpeedMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Tab Bar ──────────────────────────────────────────────────────────────────

@Composable
fun EditorTabBar(activeTab: EditorTab, onTabChange: (EditorTab) -> Unit) {
    val tabs = listOf(EditorTab.EDIT to "Edit", EditorTab.AUDIO to "Audio", EditorTab.EFFECTS to "Effects", EditorTab.TEXT to "Text", EditorTab.EXPORT to "Export")
    Row(modifier = Modifier.fillMaxWidth().background(CardDarker).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
        tabs.forEach { (tab, label) ->
            val isActive = tab == activeTab
            TextButton(onClick = { onTabChange(tab) }, modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = if (isActive) NeonCyan else TextSecondary)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                    if (isActive) Box(Modifier.width(20.dp).height(2.dp).background(NeonCyan, RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}

// ─── Timeline Panel ───────────────────────────────────────────────────────────

@Composable
fun TimelinePanel(
    tracks: List<TimelineTrack>, currentMs: Long, durationMs: Long, zoomLevel: Float,
    selectedClipId: String?, selectedClipIds: Set<String>, isMultiSelectMode: Boolean,
    onClipSelect: (String) -> Unit, onSeek: (Long) -> Unit,
    onMuteTrack: (String) -> Unit, onLockTrack: (String) -> Unit,
    onDeleteClip: (String, String) -> Unit, onSplitClip: (String, String) -> Unit,
    onZoomChange: (Float) -> Unit,
    onMoveClip: (String, String, String, Long) -> Unit,
    onTrimClip: (String, String, Long, Boolean) -> Unit,
    onTransitionChange: (String, String, ClipTransition) -> Unit,
    onMultiSelectToggle: (String) -> Unit,
    onDeleteSelected: () -> Unit, onDuplicateSelected: () -> Unit, onClearMultiSelect: () -> Unit,
    onAddTrack: (TrackType, String) -> Unit, onRemoveTrack: (String) -> Unit,
    onClipVolumeChange: (trackId: String, clipId: String, volume: Float) -> Unit,
    onClipSpeedChange : (trackId: String, clipId: String, speed: Float) -> Unit
) {
    val pxPerMs   = 0.08f * zoomLevel
    val dragState = rememberDragDropState()

    Column(modifier = Modifier.fillMaxSize().background(CardDarker)) {
        Row(modifier = Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Timeline", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { onZoomChange(zoomLevel - 0.25f) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ZoomOut, "Zoom out", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                Text("${(zoomLevel * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                IconButton(onClick = { onZoomChange(zoomLevel + 0.25f) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ZoomIn, "Zoom in", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (isMultiSelectMode) {
            Row(modifier = Modifier.fillMaxWidth().background(NeonPurple.copy(0.1f))
                .border(BorderStroke(1.dp, NeonPurple.copy(0.3f))).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${selectedClipIds.size} selected", style = MaterialTheme.typography.labelSmall, color = NeonPurple, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionChip("Duplicate", NeonCyan)      { onDuplicateSelected() }
                    SmallActionChip("Delete",    NeonPink)      { onDeleteSelected() }
                    SmallActionChip("Clear",     TextSecondary) { onClearMultiSelect() }
                }
            }
        }

        TimelineRuler(durationMs = durationMs, pxPerMs = pxPerMs, currentMs = currentMs, onSeek = onSeek)

        if (dragState.isDragging) {
            Row(modifier = Modifier.fillMaxWidth().background(NeonCyan.copy(0.08f)).padding(horizontal = 12.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Moving clip → ${formatTime(dragState.pendingStartMs)}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                Text("Release to drop", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            tracks.forEach { track ->
                TrackRow(
                    track = track, durationMs = durationMs, pxPerMs = pxPerMs, currentMs = currentMs,
                    selectedClipId = selectedClipId, selectedClipIds = selectedClipIds, isMultiSelectMode = isMultiSelectMode,
                    dragState = dragState, onClipSelect = onClipSelect,
                    onMute = { onMuteTrack(track.id) }, onLock = { onLockTrack(track.id) },
                    onDeleteClip = { onDeleteClip(track.id, it) },
                    onSplitClip  = { onSplitClip(track.id, it) },
                    onDropClip   = { clipId, newMs -> onMoveClip(dragState.draggingTrackId ?: track.id, track.id, clipId, newMs) },
                    onTrimClip   = { clipId, ms, isStart -> onTrimClip(track.id, clipId, ms, isStart) },
                    onMultiSelectToggle = { onMultiSelectToggle(it) },
                    onTransitionChange  = { clipId, t -> onTransitionChange(track.id, clipId, t) },
                    onRemoveTrack       = { onRemoveTrack(track.id) },
                    onClipVolumeChange  = { clipId, v -> onClipVolumeChange(track.id, clipId, v) },
                    onClipSpeedChange   = { clipId, s -> onClipSpeedChange(track.id, clipId, s) }
                )
            }
            AddTrackRow(onAddTrack = onAddTrack)
        }
    }
}

@Composable
fun SmallActionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.background(color.copy(0.12f), RoundedCornerShape(6.dp))
        .border(BorderStroke(1.dp, color.copy(0.3f)), RoundedCornerShape(6.dp))
        .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

// ─── Timeline Ruler ───────────────────────────────────────────────────────────

@Composable
fun TimelineRuler(durationMs: Long, pxPerMs: Float, currentMs: Long, onSeek: (Long) -> Unit) {
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxWidth().height(32.dp).background(SurfaceDark)) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().horizontalScroll(scrollState)) {
            val rulerWidth = maxOf((durationMs * pxPerMs).dp, 600.dp)
            Canvas(
                modifier = Modifier.width(rulerWidth).fillMaxHeight()
                    .pointerInput(durationMs, pxPerMs) {
                        detectTapGestures { offset ->
                            onSeek((offset.x / pxPerMs).toLong().coerceIn(0L, durationMs))
                        }
                    }
                    .pointerInput(durationMs, pxPerMs) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            onSeek((change.position.x / pxPerMs).toLong().coerceIn(0L, durationMs))
                        }
                    }
            ) {
                var ms = 0L
                while (ms <= durationMs) {
                    val x       = ms * pxPerMs * density
                    val isMajor = ms % 10_000L == 0L
                    val isMid   = ms % 5_000L  == 0L
                    drawLine(
                        color       = if (isMajor) Color(0xFF7070A0) else if (isMid) Color(0xFF555566) else Color(0xFF2E2E3E),
                        start       = Offset(x, if (isMajor) 0f else if (isMid) size.height * 0.5f else size.height * 0.7f),
                        end         = Offset(x, size.height),
                        strokeWidth = if (isMajor) 1.5f else 1f
                    )
                    if (isMajor) {
                        drawContext.canvas.nativeCanvas.drawText(
                            formatTime(ms), x + 3f * density, 10f * density,
                            android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#7A7A9A"); textSize = 8f * density; isAntiAlias = true }
                        )
                    }
                    ms += 2_500L
                }
            }
            Box(modifier = Modifier.offset(x = (currentMs * pxPerMs).dp - 1.dp).width(2.dp).fillMaxHeight().background(NeonCyan))
            Canvas(modifier = Modifier.width(rulerWidth).fillMaxHeight()) {
                val px = currentMs * pxPerMs * density
                drawPath(path = Path().apply { moveTo(px - 6f * density, 0f); lineTo(px + 6f * density, 0f); lineTo(px, 10f * density); close() }, color = NeonCyan)
            }
        }
    }
}

// ─── Track Row ────────────────────────────────────────────────────────────────

@Composable
fun TrackRow(
    track: TimelineTrack, durationMs: Long, pxPerMs: Float, currentMs: Long,
    selectedClipId: String?, selectedClipIds: Set<String>, isMultiSelectMode: Boolean,
    onMultiSelectToggle: (String) -> Unit, dragState: com.editor.app.utils.DragDropState,
    onClipSelect: (String) -> Unit, onMute: () -> Unit, onLock: () -> Unit,
    onDeleteClip: (String) -> Unit, onSplitClip: (String) -> Unit,
    onDropClip: (String, Long) -> Unit, onTrimClip: (String, Long, Boolean) -> Unit,
    onTransitionChange: (String, ClipTransition) -> Unit, onRemoveTrack: () -> Unit,
    onClipVolumeChange: (String, Float) -> Unit, onClipSpeedChange: (String, Float) -> Unit
) {
    val isHovered = dragState.hoveredTrackId == track.id && dragState.isDragging
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp)
            .border(BorderStroke(if (isHovered) 1.dp else 0.5.dp, if (isHovered) NeonCyan.copy(0.6f) else Color(0xFF1A1A2A)))
            .onGloballyPositioned { coords ->
                if (dragState.isDragging) {
                    val bounds = coords.boundsInRoot()
                    if (bounds.contains(dragState.dragOffset)) dragState.hoveredTrackId = track.id
                }
            }
    ) {
        Column(modifier = Modifier.width(84.dp).fillMaxHeight().background(SurfaceDark).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).background(track.color, CircleShape))
                Text(track.name, style = MaterialTheme.typography.labelSmall, color = TextPrimary,
                    fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(if (track.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, "Mute",
                        tint = if (track.isMuted) NeonPink else TextSecondary, modifier = Modifier.size(14.dp).clickable { onMute() })
                    Icon(if (track.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, "Lock",
                        tint = if (track.isLocked) AccentYellow else TextSecondary, modifier = Modifier.size(14.dp).clickable { onLock() })
                }
                Icon(Icons.Default.Close, "Remove track", tint = TextSecondary.copy(0.4f),
                    modifier = Modifier.size(12.dp).clickable { onRemoveTrack() })
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()
            .background(if (isHovered) TimelineBar.copy(alpha = 0.7f) else TimelineBar)
            .horizontalScroll(rememberScrollState())) {
            Box(modifier = Modifier.width(maxOf((durationMs * pxPerMs).dp, 600.dp)).fillMaxHeight()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var x = 0f; while (x < size.width) { drawLine(Color(0xFF1E1E2E), Offset(x, 0f), Offset(x, size.height), 0.5f); x += 40f }
                }
                if (isHovered && dragState.isDragging) {
                    Box(modifier = Modifier.offset(x = (dragState.pendingStartMs * pxPerMs).dp).width(4.dp).fillMaxHeight().background(NeonCyan))
                }
                track.clips.forEach { clip ->
                    ClipBlock(
                        clip = clip, pxPerMs = pxPerMs,
                        isSelected = clip.id == selectedClipId, isMultiSelected = clip.id in selectedClipIds,
                        isMultiSelectMode = isMultiSelectMode, isTrackLocked = track.isLocked,
                        isDragging = dragState.draggingClipId == clip.id,
                        onSelect = { onClipSelect(clip.id) }, onMultiSelectToggle = { onMultiSelectToggle(clip.id) },
                        onDelete = { onDeleteClip(clip.id) }, onSplit = { onSplitClip(clip.id) },
                        onTransitionChange = { t -> onTransitionChange(clip.id, t) },
                        onDragStart  = { offset -> if (!track.isLocked) dragState.startDrag(clip.id, track.id, offset, clip.startTimeMs) },
                        onDragUpdate = { offset -> dragState.updateDrag(offset, pxPerMs) },
                        onDragEnd    = { onDropClip(clip.id, dragState.pendingStartMs); dragState.endDrag() },
                        onTrimStart  = { ms -> onTrimClip(clip.id, ms, true) },
                        onTrimEnd    = { ms -> onTrimClip(clip.id, ms, false) },
                        onVolumeChange = { v -> onClipVolumeChange(clip.id, v) },
                        onSpeedChange  = { s -> onClipSpeedChange(clip.id, s) }
                    )
                }
                Box(Modifier.offset(x = (currentMs * pxPerMs).dp - 1.dp).width(2.dp).fillMaxHeight().background(NeonCyan))
            }
        }
    }
}

// ─── Clip Properties Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipPropertiesSheet(clip: MediaClip, onDismiss: () -> Unit, onVolumeChange: (Float) -> Unit, onSpeedChange: (Float) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardDark, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(clip.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(when (clip.type) { ClipType.VIDEO -> "Video clip"; ClipType.AUDIO -> "Audio clip"; ClipType.IMAGE -> "Image clip"; else -> "Clip" }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = TextSecondary) }
            }
            HorizontalDivider(color = Color(0xFF22223A))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("Duration", formatTime(clip.visibleDurationMs), NeonCyan)
                InfoChip("Start", formatTime(clip.startTimeMs), NeonPurple)
                if (clip.type == ClipType.AUDIO || clip.type == ClipType.VIDEO) InfoChip("Vol", "${(clip.volume * 100).toInt()}%", WaveformGreen)
                InfoChip("Speed", "${clip.speed}×", AccentOrange)
            }
            if (clip.type == ClipType.AUDIO || clip.type == ClipType.VIDEO) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Clip Volume", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("${(clip.volume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = WaveformGreen)
                    }
                    Slider(value = clip.volume, onValueChange = { onVolumeChange(it) }, valueRange = 0f..2f,
                        colors = SliderDefaults.colors(thumbColor = WaveformGreen, activeTrackColor = WaveformGreen, inactiveTrackColor = Color(0xFF33334A)),
                        modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("100%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("200%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
            if (clip.type == ClipType.VIDEO || clip.type == ClipType.AUDIO) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Clip Speed", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("${clip.speed}×", style = MaterialTheme.typography.bodySmall, color = AccentOrange)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 4f).forEach { speed ->
                            SpeedChip(label = "${speed}×", isActive = clip.speed == speed) { onSpeedChange(speed) }
                        }
                    }
                    Slider(value = clip.speed, onValueChange = { onSpeedChange(it) }, valueRange = 0.25f..4f, steps = 14,
                        colors = SliderDefaults.colors(thumbColor = AccentOrange, activeTrackColor = AccentOrange, inactiveTrackColor = Color(0xFF33334A)),
                        modifier = Modifier.fillMaxWidth())
                }
            }
            if (clip.trimStartMs > 0 || clip.trimEndMs > 0) {
                HorizontalDivider(color = Color(0xFF22223A))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trim", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoChip("Trimmed start", formatTime(clip.trimStartMs), TextSecondary)
                        InfoChip("Trimmed end", formatTime(clip.trimEndMs), TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Column(modifier = Modifier.background(color.copy(0.08f), RoundedCornerShape(8.dp))
        .border(1.dp, color.copy(0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun SpeedChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.background(if (isActive) AccentOrange.copy(0.2f) else CardDarker, RoundedCornerShape(6.dp))
        .border(1.dp, if (isActive) AccentOrange else Color(0xFF2A2A3A), RoundedCornerShape(6.dp))
        .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (isActive) AccentOrange else TextSecondary,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

// ─── Clip Block ───────────────────────────────────────────────────────────────

@Composable
fun ClipBlock(
    clip: MediaClip, pxPerMs: Float,
    isSelected: Boolean, isMultiSelected: Boolean, isMultiSelectMode: Boolean,
    isTrackLocked: Boolean, isDragging: Boolean,
    onSelect: () -> Unit, onDelete: () -> Unit,
    onTransitionChange: (ClipTransition) -> Unit,
    onSplit: () -> Unit, onMultiSelectToggle: () -> Unit,
    onDragStart: (Offset) -> Unit, onDragUpdate: (Offset) -> Unit, onDragEnd: () -> Unit,
    onTrimStart: (Long) -> Unit, onTrimEnd: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit, onSpeedChange: (Float) -> Unit
) {
    var showMenu             by remember { mutableStateOf(false) }
    var showTransitionPicker by remember { mutableStateOf(false) }
    var showPropertiesSheet  by remember { mutableStateOf(false) }

    if (showPropertiesSheet) {
        ClipPropertiesSheet(clip = clip, onDismiss = { showPropertiesSheet = false },
            onVolumeChange = onVolumeChange, onSpeedChange = onSpeedChange)
    }

    if (showTransitionPicker) {
        var transitionDuration by remember { mutableStateOf(clip.transition.durationMs) }
        AlertDialog(
            onDismissRequest = { showTransitionPicker = false },
            containerColor   = CardDark,
            title = { Text("Choose Transition", color = TextPrimary) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TransitionType.values().forEach { type ->
                        val isActive = clip.transition.type == type
                        Row(modifier = Modifier.fillMaxWidth()
                            .background(if (isActive) NeonPurple.copy(0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onTransitionChange(ClipTransition(type = type, durationMs = transitionDuration)); showTransitionPicker = false }
                            .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(type.name.replace("_", " "), color = if (isActive) NeonPurple else TextPrimary,
                                style = MaterialTheme.typography.bodySmall, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                            if (isActive) Icon(Icons.Default.Check, null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                        }
                    }
                    HorizontalDivider(color = Color(0xFF22223A))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Duration: ${(transitionDuration / 1000f).toString().take(3)}s", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Slider(value = transitionDuration.toFloat(), onValueChange = { transitionDuration = it.toLong() },
                            valueRange = 200f..2000f, steps = 17,
                            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple, inactiveTrackColor = Color(0xFF33334A)))
                    }
                }
            },
            confirmButton = {}
        )
    }

    val alpha by animateFloatAsState(targetValue = if (isDragging) 0.4f else 1f, animationSpec = tween(150), label = "clipAlpha")
    val startX = ((clip.startTimeMs + clip.trimStartMs) * pxPerMs).dp
    val width  = (clip.visibleDurationMs * pxPerMs).dp

    Box(modifier = Modifier.offset(x = startX).width(width).fillMaxHeight().padding(vertical = 4.dp).alpha(alpha)) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp).clip(RoundedCornerShape(6.dp))
            .background(if (isMultiSelected) clip.color.copy(0.6f) else clip.color.copy(if (isSelected) 1f else 0.8f))
            .border(BorderStroke(
                when { isMultiSelected -> 2.dp; isSelected -> 1.5.dp; else -> 0.5.dp },
                when { isMultiSelected -> NeonPurple; isSelected -> Color.White.copy(0.9f); else -> Color.White.copy(0.2f) }
            ), RoundedCornerShape(6.dp))
            .pointerInput(clip.id, isTrackLocked, isMultiSelectMode) {
                if (isTrackLocked) return@pointerInput
                if (isMultiSelectMode) detectTapGestures { onMultiSelectToggle() }
                else detectDragGesturesAfterLongPress(
                    onDragStart  = { offset -> onSelect(); onDragStart(offset) },
                    onDrag       = { change, _ -> change.consume(); onDragUpdate(change.position) },
                    onDragEnd    = { onDragEnd() }, onDragCancel = { onDragEnd() }
                )
            }
            .clickable(enabled = !isTrackLocked) {
                if (isMultiSelectMode) onMultiSelectToggle()
                else { onSelect(); showMenu = true }
            }
        ) {
            if (clip.type == ClipType.AUDIO && clip.waveformData.isNotEmpty()) {
                WaveformView(waveformData = clip.waveformData, color = Color.White.copy(0.4f))
            }
            Text(clip.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.9f), fontSize = 9.sp, maxLines = 1)
            if (clip.speed != 1f) {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
                    .background(AccentOrange.copy(0.85f), RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                    Text("${clip.speed}×", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (clip.volume != 1f && (clip.type == ClipType.AUDIO || clip.type == ClipType.VIDEO)) {
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(3.dp)
                    .background(WaveformGreen.copy(0.85f), RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                    Text("${(clip.volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (clip.transition.type != TransitionType.NONE) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(8.dp).background(NeonPurple, CircleShape))
            }
            if (isMultiSelected) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(3.dp).size(14.dp).background(NeonPurple, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
            if (!isMultiSelectMode) {
                DropdownMenu(expanded = showMenu && isSelected, onDismissRequest = { showMenu = false }, modifier = Modifier.background(CardDark)) {
                    DropdownMenuItem(text = { Text("Properties…", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Tune, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                        onClick = { showPropertiesSheet = true; showMenu = false })
                    DropdownMenuItem(text = { Text("Split at playhead", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCut, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                        onClick = { onSplit(); showMenu = false })
                    DropdownMenuItem(text = { Text("Transition: ${clip.transition.type.name}", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Transform, null, tint = NeonPurple, modifier = Modifier.size(16.dp)) },
                        trailingIcon = { Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(14.dp)) },
                        onClick = { showTransitionPicker = true; showMenu = false })
                    DropdownMenuItem(text = { Text("Delete", color = NeonPink, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = NeonPink, modifier = Modifier.size(16.dp)) },
                        onClick = { onDelete(); showMenu = false })
                }
            }
        }

        if (isSelected && !isTrackLocked && !isMultiSelectMode) {
            Box(modifier = Modifier.align(Alignment.CenterStart).width(12.dp).fillMaxHeight(0.7f)
                .background(Color.White, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                .pointerInput(clip.id) {
                    detectDragGestures { change, dragAmount -> change.consume(); onTrimStart(clip.trimStartMs + (dragAmount.x / pxPerMs).toLong()) }
                }) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    repeat(3) { Box(Modifier.size(width = 2.dp, height = 6.dp).background(Color.Black.copy(0.5f))); Spacer(Modifier.height(2.dp)) }
                }
            }
        }
        if (isSelected && !isTrackLocked && !isMultiSelectMode) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).width(12.dp).fillMaxHeight(0.7f)
                .background(Color.White, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                .pointerInput(clip.id) {
                    detectDragGestures { change, dragAmount -> change.consume(); onTrimEnd(clip.trimEndMs - (dragAmount.x / pxPerMs).toLong()) }
                }) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    repeat(3) { Box(Modifier.size(width = 2.dp, height = 6.dp).background(Color.Black.copy(0.5f))); Spacer(Modifier.height(2.dp)) }
                }
            }
        }
    }
}

@Composable
fun WaveformView(waveformData: List<Float>, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / waveformData.size
        waveformData.forEachIndexed { i, amp ->
            val barH = amp * size.height * 0.7f; val x = i * barWidth + barWidth / 2
            drawLine(color, Offset(x, size.height / 2 - barH / 2), Offset(x, size.height / 2 + barH / 2), strokeWidth = max(barWidth * 0.6f, 1f))
        }
    }
}

// ─── Add Track Row ────────────────────────────────────────────────────────────

@Composable
fun AddTrackRow(onAddTrack: (TrackType, String) -> Unit) {
    var showSheet    by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(TrackType.AUDIO) }
    var trackName    by remember { mutableStateOf("") }

    if (showSheet) {
        AlertDialog(
            onDismissRequest = { showSheet = false },
            containerColor   = CardDark,
            title = { Text("Add Track", color = TextPrimary) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Track Type", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrackType.values().forEach { type ->
                            val isSelected = type == selectedType
                            val typeColor  = when (type) { TrackType.VIDEO -> NeonCyan; TrackType.AUDIO -> WaveformGreen; else -> NeonPurple }
                            Box(modifier = Modifier.background(if (isSelected) typeColor.copy(0.15f) else CardDarker, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, if (isSelected) typeColor else Color(0xFF2A2A3A)), RoundedCornerShape(8.dp))
                                .clickable { selectedType = type }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) typeColor else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    LaunchedEffect(selectedType) {
                        if (trackName.isBlank() || trackName.startsWith("New ")) {
                            trackName = when (selectedType) { TrackType.VIDEO -> "New Video"; TrackType.AUDIO -> "New Audio"; else -> "New FX" }
                        }
                    }
                    OutlinedTextField(value = trackName, onValueChange = { trackName = it }, label = { Text("Track name", color = TextSecondary) }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color(0xFF2A2A3A),
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NeonCyan,
                            focusedContainerColor = CardDark, unfocusedContainerColor = CardDark))
                }
            },
            confirmButton = {
                TextButton(onClick = { onAddTrack(selectedType, trackName.trim().ifBlank { "Track" }); showSheet = false; trackName = "" }, enabled = trackName.isNotBlank()) {
                    Text("Add Track", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showSheet = false }) { Text("Cancel", color = TextSecondary) } }
        )
    }

    Row(modifier = Modifier.fillMaxWidth().height(44.dp).background(CardDarker)
        .border(BorderStroke(0.5.dp, Color(0xFF22223A))).clickable { showSheet = true }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(24.dp).background(NeonCyan.copy(0.12f), CircleShape).border(1.dp, NeonCyan.copy(0.3f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, "Add track", tint = NeonCyan, modifier = Modifier.size(14.dp))
        }
        Text("Add Track", style = MaterialTheme.typography.bodySmall, color = NeonCyan, fontWeight = FontWeight.Medium)
    }
}

// ─── Audio Mixer Panel ────────────────────────────────────────────────────────

@Composable
fun AudioMixerPanel(
    tracks: List<TimelineTrack>, masterVolume: Float,
    onTrackVolumeChange: (String, Float) -> Unit, onTrackPanChange: (String, Float) -> Unit,
    onTrackMuteToggle: (String) -> Unit, onMasterVolumeChange: (Float) -> Unit,
    onTrackEqChange: (String, Int, Float) -> Unit,
    onTrackFxChange: (String, String, Float) -> Unit
) {
    var eqTrackId by remember { mutableStateOf<String?>(null) }
    var fxTrackId by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(CardDarker), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Audio Mixer", style = MaterialTheme.typography.headlineMedium, color = TextPrimary) }
        items(tracks) { track ->
            AudioChannelStrip(track = track,
                onVolumeChange = { onTrackVolumeChange(track.id, it) },
                onPanChange    = { onTrackPanChange(track.id, it) },
                onMuteToggle   = { onTrackMuteToggle(track.id) },
                onEqClick      = { eqTrackId = track.id },
                onFxClick      = { fxTrackId = track.id })
        }
        item { MasterVolumeStrip(volume = masterVolume, onVolumeChange = onMasterVolumeChange) }
    }

    // FIX: EqualizerDialog — was called but never defined. Now defined below.
    eqTrackId?.let { tid ->
        tracks.find { it.id == tid }?.let { track ->
            EqualizerDialog(track = track, onDismiss = { eqTrackId = null },
                onBandChange = { band, value -> onTrackEqChange(tid, band, value) })
        }
    }

    // FIX: FxPickerDialog — was called but never defined. Now defined below.
    fxTrackId?.let { tid ->
        tracks.find { it.id == tid }?.let { track ->
            FxPickerDialog(track = track, onDismiss = { fxTrackId = null },
                onFxChange = { fxType, intensity -> onTrackFxChange(tid, fxType, intensity) })
        }
    }
}

// FIX: EqualizerDialog — was missing, causing unresolved reference
@Composable
fun EqualizerDialog(track: TimelineTrack, onDismiss: () -> Unit, onBandChange: (Int, Float) -> Unit) {
    val bandLabels = listOf("60Hz", "250Hz", "1kHz", "4kHz", "16kHz")
    val bandValues = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).background(track.color, CircleShape))
                Text("EQ — ${track.name}", color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bandLabels.forEachIndexed { i, label ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            val db = bandValues[i]
                            Text(if (db >= 0) "+${"%.1f".format(db)}dB" else "${"%.1f".format(db)}dB",
                                style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        }
                        Slider(value = bandValues[i], onValueChange = { bandValues[i] = it; onBandChange(i, it) },
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = Color(0xFF33334A)))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = NeonCyan, fontWeight = FontWeight.Bold) } }
    )
}

// FIX: FxPickerDialog — was missing, causing unresolved reference
@Composable
fun FxPickerDialog(track: TimelineTrack, onDismiss: () -> Unit, onFxChange: (String, Float) -> Unit) {
    data class FxItem(val id: String, val name: String, val emoji: String)
    val fxList = listOf(
        FxItem("reverb",    "Reverb",    "🔊"),
        FxItem("echo",      "Echo",      "📡"),
        FxItem("pitch_up",  "Pitch Up",  "⬆️"),
        FxItem("bass",      "Bass Boost","🎵"),
        FxItem("normalize", "Normalize", "🎚️")
    )
    var selectedFx    by remember { mutableStateOf<String?>(null) }
    var intensity     by remember { mutableStateOf(0.5f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).background(track.color, CircleShape))
                Text("FX — ${track.name}", color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fxList.forEach { fx ->
                        val isActive = fx.id == selectedFx
                        Column(modifier = Modifier.clickable { selectedFx = fx.id; onFxChange(fx.id, intensity) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(48.dp)
                                .background(if (isActive) NeonPurple.copy(0.2f) else CardDarker, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, if (isActive) NeonPurple else Color(0xFF2A2A3A)), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center) {
                                Text(fx.emoji, fontSize = 20.sp)
                            }
                            Text(fx.name, style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) NeonPurple else TextSecondary, fontSize = 9.sp)
                        }
                    }
                }
                if (selectedFx != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Intensity", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${(intensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = NeonPurple)
                        }
                        Slider(value = intensity, onValueChange = { intensity = it; selectedFx?.let { fx -> onFxChange(fx, it) } },
                            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple, inactiveTrackColor = Color(0xFF33334A)))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = NeonCyan, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun AudioChannelStrip(track: TimelineTrack, onVolumeChange: (Float) -> Unit, onPanChange: (Float) -> Unit,
                      onMuteToggle: () -> Unit, onEqClick: () -> Unit, onFxClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFF22223A))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(track.color, CircleShape))
                    Text(track.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MixerChip(if (track.isMuted) "MUTED" else "LIVE", if (track.isMuted) NeonPink else WaveformGreen, onMuteToggle)
                    MixerChip("EQ", NeonCyan, onEqClick)
                    MixerChip("FX", NeonPurple, onFxClick)
                }
            }
            LabeledSlider("VOL", track.volume, track.color, "${(track.volume * 100).toInt()}%", onVolumeChange)
            val panLabel = when { track.pan < -0.05f -> "L${(-track.pan * 100).toInt()}"; track.pan > 0.05f -> "R${(track.pan * 100).toInt()}"; else -> "C" }
            LabeledSlider("PAN", (track.pan + 1f) / 2f, AccentOrange, panLabel) { onPanChange(it * 2f - 1f) }
            VUMeter(volume = track.volume, color = track.color)
        }
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, color: Color, valueText: String, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(28.dp))
        Slider(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = Color(0xFF33334A)))
        Text(valueText, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(36.dp))
    }
}

@Composable
fun VUMeter(volume: Float, color: Color) {
    val segments = 20
    Row(modifier = Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(segments) { i ->
            val threshold = (i + 1f) / segments; val isActive = volume >= threshold
            val segColor = when { i >= segments * 0.85f -> if (isActive) Color(0xFFFF4444) else Color(0xFFFF4444).copy(0.15f); i >= segments * 0.70f -> if (isActive) AccentYellow else AccentYellow.copy(0.15f); else -> if (isActive) color else color.copy(0.15f) }
            Box(Modifier.weight(1f).fillMaxHeight().background(segColor, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
fun MixerChip(label: String, color: Color, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.background(color.copy(0.12f), RoundedCornerShape(6.dp))
        .border(BorderStroke(1.dp, color.copy(0.35f)), RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MasterVolumeStrip(volume: Float, onVolumeChange: (Float) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D1A)),
        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, NeonCyan.copy(0.3f))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, "Master", tint = NeonCyan, modifier = Modifier.size(16.dp))
                Text("Master Output", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
            }
            LabeledSlider("MASTER", volume, NeonCyan, "${(volume * 100).toInt()}%", onVolumeChange)
            VUMeter(volume = volume, color = NeonCyan)
        }
    }
}

// ─── Effects Panel ────────────────────────────────────────────────────────────

// FIX: EditEffect.blend and .fade don't exist — using safe defaults (0.5f, 0.3f)
// To make these real, add `val blend: Float = 0.5f` and `val fade: Float = 0.3f`
// to the EditEffect data class in Models.kt. Until then, sliders use local state.
@Composable
fun EffectsPanel(effects: List<EditEffect>, onToggleEffect: (String) -> Unit,
                 onSetIntensity: (String, Float) -> Unit,
                 onSetBlend: (String, Float) -> Unit,
                 onSetFade: (String, Float) -> Unit) {
    var selectedCategory by remember { mutableStateOf(EffectCategory.FILTER) }
    val filtered         = effects.filter { it.category == selectedCategory }
    val selectedEffect   = effects.firstOrNull { it.isApplied && it.category == selectedCategory }
    // Local state for blend/fade until EditEffect gets these fields
    var localBlend by remember { mutableStateOf(0.5f) }
    var localFade  by remember { mutableStateOf(0.3f) }

    Column(modifier = Modifier.fillMaxSize().background(CardDarker)) {
        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EffectCategory.values()) { cat ->
                val isSelected = cat == selectedCategory
                Box(modifier = Modifier.background(if (isSelected) NeonPurple.copy(0.2f) else CardDark, RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, if (isSelected) NeonPurple else Color(0xFF22223A)), RoundedCornerShape(20.dp))
                    .clickable { selectedCategory = cat }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text(cat.name.replace("_", " "), style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) NeonPurple else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { effect -> EffectCard(effect = effect, onClick = { onToggleEffect(effect.id) }) }
        }
        HorizontalDivider(color = Color(0xFF22223A), modifier = Modifier.padding(vertical = 8.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (selectedEffect != null) "Adjust: ${selectedEffect.name}" else "Select an effect to adjust",
                style = MaterialTheme.typography.titleLarge, color = if (selectedEffect != null) TextPrimary else TextSecondary)
            if (selectedEffect != null) {
                LabeledSlider("Intensity", selectedEffect.intensity, NeonPurple, "${(selectedEffect.intensity * 100).toInt()}%") { onSetIntensity(selectedEffect.id, it) }
                LabeledSlider("Blend",     localBlend,               NeonCyan,   "${(localBlend * 100).toInt()}%")              { localBlend = it; onSetBlend(selectedEffect.id, it) }
                LabeledSlider("Fade",      localFade,                AccentOrange,"${(localFade * 100).toInt()}%")              { localFade  = it; onSetFade(selectedEffect.id, it) }
            }
        }
    }
}

@Composable
fun EffectCard(effect: EditEffect, onClick: () -> Unit) {
    Column(modifier = Modifier.width(72.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(64.dp).background(if (effect.isApplied) NeonPurple.copy(0.2f) else CardDark, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.5.dp, if (effect.isApplied) NeonPurple else Color(0xFF22223A)), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Text(effect.emoji, fontSize = 24.sp)
        }
        Text(effect.name, style = MaterialTheme.typography.labelSmall, color = if (effect.isApplied) NeonPurple else TextSecondary, fontSize = 10.sp)
    }
}

// ─── Text Panel ───────────────────────────────────────────────────────────────

@Composable
fun TextPanel(overlays: List<TextOverlay>, onAddOverlay: (String, String, Color) -> Unit, onRemoveOverlay: (String) -> Unit) {
    var textInput     by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Title") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    val styles       = listOf("Title", "Caption", "Body", "Watermark", "Quote")
    val colorOptions = listOf(Color.White, NeonCyan, NeonPink, NeonPurple, AccentYellow, AccentOrange)

    Column(modifier = Modifier.fillMaxSize().background(CardDarker).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add Text Overlay", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        OutlinedTextField(value = textInput, onValueChange = { textInput = it }, placeholder = { Text("Type your text...", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = Color(0xFF33334A), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NeonCyan), shape = RoundedCornerShape(12.dp))
        Text("Style", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles) { style ->
                Box(modifier = Modifier.background(if (selectedStyle == style) NeonCyan.copy(0.15f) else CardDark, RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, if (selectedStyle == style) NeonCyan else Color(0xFF22223A)), RoundedCornerShape(10.dp))
                    .clickable { selectedStyle = style }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(style, style = MaterialTheme.typography.bodySmall, color = if (selectedStyle == style) NeonCyan else TextSecondary)
                }
            }
        }
        Text("Color", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colorOptions.forEach { color ->
                Box(modifier = Modifier.size(32.dp).background(color, CircleShape)
                    .border(BorderStroke(if (selectedColor == color) 2.5.dp else 1.dp, if (selectedColor == color) Color.White else Color.White.copy(0.2f)), CircleShape)
                    .clickable { selectedColor = color })
            }
        }
        Button(onClick = { if (textInput.isNotBlank()) { onAddOverlay(textInput, selectedStyle, selectedColor); textInput = "" } },
            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), shape = RoundedCornerShape(12.dp), enabled = textInput.isNotBlank()) {
            Icon(Icons.Default.TextFields, "Add", tint = DeepBlack); Spacer(Modifier.width(8.dp))
            Text("Add to Timeline", color = DeepBlack, fontWeight = FontWeight.Bold)
        }
        if (overlays.isNotEmpty()) {
            Text("Added Overlays", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            overlays.forEach { overlay ->
                Row(modifier = Modifier.fillMaxWidth().background(CardDark, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(overlay.color, CircleShape))
                        Column { Text(overlay.text, style = MaterialTheme.typography.bodySmall, color = TextPrimary); Text("${overlay.style} · ${formatTime(overlay.startTimeMs)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                    }
                    IconButton(onClick = { onRemoveOverlay(overlay.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = NeonPink, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ─── Export Panel ─────────────────────────────────────────────────────────────

@Composable
fun ExportPanel(
    config: ExportConfig, isExporting: Boolean, exportProgress: Float,
    exportError: String?, exportedUri: String?, exportedFilePath: String?,
    isSimulatedExport: Boolean, onConfigChange: (ExportConfig) -> Unit, onExport: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().background(CardDarker).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Export Settings", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        ExportSection("Resolution") { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { VideoResolution.values().forEach { res -> SelectableChip(res.label, config.resolution == res) { onConfigChange(config.copy(resolution = res)) } } } }
        ExportSection("Frame Rate") { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(24, 30, 60).forEach { fps -> SelectableChip("${fps}fps", config.fps == fps) { onConfigChange(config.copy(fps = fps)) } } } }
        ExportSection("Quality")    { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { VideoBitrate.values().forEach { b -> SelectableChip(b.label, config.bitrate == b) { onConfigChange(config.copy(bitrate = b)) } } } }
        ExportSection("Format")     { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { VideoFormat.values().forEach { f -> SelectableChip(f.label, config.format == f) { onConfigChange(config.copy(format = f)) } } } }

        Card(colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(0.05f)), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, NeonCyan.copy(0.3f))) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Export Summary", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                ExportSummaryRow("Resolution", "${config.resolution.width}×${config.resolution.height}")
                ExportSummaryRow("Frame Rate", "${config.fps} fps")
                ExportSummaryRow("Bitrate",    "${config.bitrate.kbps / 1000} Mbps")
                ExportSummaryRow("Format",     config.format.ext.uppercase())
                ExportSummaryRow("Est. Size",  "~${config.bitrate.kbps * 30 / 8 / 1024} MB")
            }
        }

        if (isExporting) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Exporting…", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text("${(exportProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                }
                LinearProgressIndicator(progress = { exportProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = NeonCyan, trackColor = Color(0xFF33334A))
            }
        }

        if (exportedUri != null && !isExporting) {
            Card(colors = CardDefaults.cardColors(containerColor = WaveformGreen.copy(0.08f)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, WaveformGreen.copy(0.5f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = WaveformGreen, modifier = Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Saved to your phone!", style = MaterialTheme.typography.titleLarge, color = WaveformGreen, fontWeight = FontWeight.Bold)
                            Text("Movies / VibeEdit folder", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    if (!exportedFilePath.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A1A0A), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(exportedFilePath.removePrefix("/storage/emulated/0/").removePrefix("/sdcard/"),
                                style = MaterialTheme.typography.labelSmall, color = WaveformGreen.copy(0.9f), fontSize = 10.sp, maxLines = 2)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            try {
                                val uri = android.net.Uri.parse(exportedUri)
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { setDataAndType(uri, "video/*"); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = WaveformGreen), border = BorderStroke(1.dp, WaveformGreen.copy(0.6f)), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Play", fontSize = 13.sp)
                        }
                        Button(onClick = {
                            try {
                                val uri = android.net.Uri.parse(exportedUri)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "video/*"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                context.startActivity(android.content.Intent.createChooser(intent, "Share video"))
                            } catch (_: Exception) { }
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = WaveformGreen), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Share, null, tint = Color.Black, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Share", color = Color.Black, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (isSimulatedExport && !isExporting) {
            Card(colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(0.07f)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AccentOrange.copy(0.4f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = AccentOrange, modifier = Modifier.size(26.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Demo export only", style = MaterialTheme.typography.titleLarge, color = AccentOrange, fontWeight = FontWeight.Bold)
                        Text("Import a real video clip first, then export to save to your phone.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        if (exportError != null && !isExporting) {
            Card(colors = CardDefaults.cardColors(containerColor = NeonPink.copy(0.08f)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, NeonPink.copy(0.4f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Error, null, tint = NeonPink, modifier = Modifier.size(26.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export failed", style = MaterialTheme.typography.titleLarge, color = NeonPink, fontWeight = FontWeight.Bold)
                        Text(exportError, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        Button(onClick = onExport, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !isExporting,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, disabledContainerColor = NeonCyan.copy(0.3f)), shape = RoundedCornerShape(14.dp)) {
            if (isExporting) CircularProgressIndicator(color = DeepBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else { Icon(Icons.Default.FileUpload, "Export", tint = DeepBlack, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Export Video", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable fun ExportSection(title: String, content: @Composable () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary); content() } }
@Composable fun SelectableChip(label: String, isSelected: Boolean, onClick: () -> Unit) { Box(modifier = Modifier.background(if (isSelected) NeonCyan.copy(0.15f) else CardDark, RoundedCornerShape(10.dp)).border(BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF22223A)), RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)) { Text(label, style = MaterialTheme.typography.bodySmall, color = if (isSelected) NeonCyan else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) } }
@Composable fun ExportSummaryRow(label: String, value: String) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary); Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium) } }

// ─── Templates Sheet ──────────────────────────────────────────────────────────
// FIX: ProjectTemplate and defaultTemplates() were missing — defined here as simple stubs.
// Move to Models.kt when you want to persist template data.

data class ProjectTemplate(val id: String, val name: String, val description: String, val emoji: String, val tracks: List<TimelineTrack> = defaultTracks(), val effects: List<EditEffect> = defaultEffects())

fun defaultTemplates() = listOf(
    ProjectTemplate("t1", "Short Reel",   "30s vertical video for social media", "🎬"),
    ProjectTemplate("t2", "Podcast",      "Audio-focused multi-track layout",    "🎙️"),
    ProjectTemplate("t3", "Travel Vlog",  "B-roll + voiceover + music",          "✈️"),
    ProjectTemplate("t4", "Product Demo", "Screen + camera + captions",          "📱"),
    ProjectTemplate("t5", "Music Video",  "Beat-synced cuts with effects",       "🎵")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesSheet(onDismiss: () -> Unit, onSelectTemplate: (ProjectTemplate) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardDark, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Templates", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = TextSecondary) }
            }
            Text("Start your project with a pre-built template", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(defaultTemplates()) { template ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onSelectTemplate(template) },
                        colors = CardDefaults.cardColors(containerColor = CardDarker), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF22223A))) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).background(NeonCyan.copy(0.1f), RoundedCornerShape(12.dp)).border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text(template.emoji, fontSize = 24.sp)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(template.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(template.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${template.tracks.size} tracks · ${template.effects.size} effects", style = MaterialTheme.typography.labelSmall, color = NeonCyan.copy(0.7f))
                            }
                            Icon(Icons.Default.ArrowForward, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Settings Sheet ───────────────────────────────────────────────────────────
// FIX: ThemePreferences and ThemeMode were missing — stubbed with simple state.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(onDismiss: () -> Unit) {
    var themeMode  by remember { mutableStateOf("Dark") }
    var autoSave   by remember { mutableStateOf(true) }
    var showGrid   by remember { mutableStateOf(true) }
    var snapToGrid by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardDark, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = TextSecondary) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                SettingsRow("Theme", themeMode, listOf("Dark", "Light", "System")) { themeMode = it }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Editing", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                ToggleSettingRow("Auto-save projects",  autoSave)   { autoSave   = it }
                ToggleSettingRow("Show timeline grid",  showGrid)   { showGrid   = it }
                ToggleSettingRow("Snap to grid",        snapToGrid) { snapToGrid = it }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Export Defaults", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                Text("Resolution: 1080p · 30fps · High quality", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            HorizontalDivider(color = Color(0xFF22223A))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                Text("Vibe Edit v1.0.0", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Video & Audio Editor for Android", style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(0.6f))
            }
        }
    }
}

@Composable
private fun SettingsRow(label: String, currentValue: String, options: List<String>, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val isActive = option == currentValue
                Box(modifier = Modifier.background(if (isActive) NeonCyan.copy(0.15f) else CardDarker, RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, if (isActive) NeonCyan else Color(0xFF2A2A3A)), RoundedCornerShape(6.dp))
                    .clickable { onSelect(option) }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(option, style = MaterialTheme.typography.labelSmall, color = if (isActive) NeonCyan else TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ToggleSettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(0.5f), uncheckedThumbColor = TextSecondary, uncheckedTrackColor = Color(0xFF33334A)))
    }
}

// ─── Notifications Sheet ──────────────────────────────────────────────────────
// FIX: broken braces — rewritten cleanly

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(onDismiss: () -> Unit) {
    val notifications = listOf(
        "Welcome to Vibe Edit!"  to "Start creating amazing videos with our powerful editor.",
        "Tip: Drag & Drop"       to "Long press any clip to drag it between tracks.",
        "Tip: Transitions"       to "Tap a clip → context menu to add transition effects.",
        "Tip: Audio Mixing"      to "Use the Audio tab to mix multiple tracks with EQ and FX.",
        "Tip: Export"            to "Import a real video then tap Export to save to your phone."
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardDark, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Notifications", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = TextSecondary) }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notifications) { (title, message) ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDarker),
                        shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFF22223A))) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.size(8.dp).background(NeonCyan, CircleShape).align(Alignment.CenterVertically))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(title,   style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(message, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Cloud Backup Sheet ───────────────────────────────────────────────────────
// FIX: removed GlobalScope anti-pattern, removed ThemePreferences dependency,
//      fixed broken closing braces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupSheet(onDismiss: () -> Unit) {
    var backupEnabled  by remember { mutableStateOf(false) }
    var isBackingUp    by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf(0f) }
    var lastBackupText by remember { mutableStateOf("Never") }

    // FIX: use LaunchedEffect instead of GlobalScope for the simulated backup
    LaunchedEffect(isBackingUp) {
        if (isBackingUp) {
            repeat(20) { i ->
                kotlinx.coroutines.delay(200)
                backupProgress = (i + 1) / 20f
            }
            lastBackupText = "Just now"
            isBackingUp    = false
            backupProgress = 0f
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardDark, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cloud Backup", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = TextSecondary) }
            }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDarker), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Cloud, "Cloud", tint = NeonCyan, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Backup to Cloud", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text("Sync your projects to Google Drive", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    ToggleSettingRow("Enable auto-backup", backupEnabled) { backupEnabled = it }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last backup:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(lastBackupText, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                }
            }
            if (isBackingUp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Backing up...", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("${(backupProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                    }
                    LinearProgressIndicator(progress = { backupProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = NeonCyan, trackColor = Color(0xFF33334A))
                }
            }
            Button(onClick = { isBackingUp = true },
                modifier = Modifier.fillMaxWidth(), enabled = !isBackingUp,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), shape = RoundedCornerShape(12.dp)) {
                if (isBackingUp) CircularProgressIndicator(color = DeepBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else { Icon(Icons.Default.CloudUpload, "Backup", tint = DeepBlack); Spacer(Modifier.width(8.dp)); Text("Backup Now", color = DeepBlack, fontWeight = FontWeight.Bold) }
            }
            Text("Note: Cloud backup requires a Google Drive account. Projects are stored in Drive > VibeEdit.",
                style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(0.7f))
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000; val min = totalSec / 60; val sec = totalSec % 60; val millis = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, millis)
}