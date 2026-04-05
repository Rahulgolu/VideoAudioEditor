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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import coil.compose.AsyncImage
import com.editor.app.utils.rememberDragDropState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.activity.compose.BackHandler

// ─── Root Editor Screen ───────────────────────────────────────────────────────

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val state by viewModel.uiState.collectAsState()
    var showImportSheet    by remember { mutableStateOf(false) }
    var showVoiceoverSheet by remember { mutableStateOf(false) }

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

            EditorTabBar(
                activeTab   = state.activeTab,
                onTabChange = { viewModel.setActiveTab(it) }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (state.activeTab) {

                    EditorTab.EDIT -> TimelinePanel(
                        tracks            = state.tracks,
                        currentMs         = state.currentPositionMs,
                        durationMs        = state.durationMs,
                        zoomLevel         = state.zoomLevel,
                        selectedClipId    = state.selectedClipId,
                        selectedClipIds   = state.selectedClipIds,
                        isMultiSelectMode = state.isMultiSelectMode,
                        onClipSelect      = { viewModel.selectClip(it) },
                        onSeek            = { viewModel.seekTo(it) },
                        onMuteTrack       = { viewModel.toggleTrackMute(it) },
                        onLockTrack       = { viewModel.toggleTrackLock(it) },
                        onDeleteClip      = { trackId, clipId -> viewModel.deleteClip(trackId, clipId) },
                        onSplitClip       = { trackId, clipId -> viewModel.splitClipAtPlayhead(trackId, clipId) },
                        onZoomChange      = { viewModel.setZoom(it) },
                        onMoveClip        = { fromTrack, toTrack, clipId, newStartMs ->
                            viewModel.moveClipToTrack(fromTrack, toTrack, clipId, newStartMs)
                        },
                        onTrimClip        = { trackId, clipId, trimMs, isStart ->
                            if (isStart) viewModel.trimClipStart(trackId, clipId, trimMs)
                            else viewModel.trimClipEnd(trackId, clipId, trimMs)
                        },
                        onMultiSelectToggle  = { viewModel.toggleMultiSelect(it) },
                        onDeleteSelected     = { viewModel.deleteSelectedClips() },
                        onDuplicateSelected  = { viewModel.duplicateSelectedClips() },
                        onClearMultiSelect   = { viewModel.clearMultiSelect() },
                        onTransitionChange   = { trackId, clipId, transition ->
                            viewModel.setClipTransition(trackId, clipId, transition)
                        },
                        // ── Feature 3 ─────────────────────────────────────────
                        onAddTrack    = { type, name -> viewModel.addTrack(type, name) },
                        onRemoveTrack = { viewModel.removeTrack(it) }
                    )

                    EditorTab.AUDIO -> AudioMixerPanel(
                        tracks              = state.tracks,
                        masterVolume        = state.masterVolume,
                        onTrackVolumeChange = { id, v -> viewModel.setTrackVolume(id, v) },
                        onTrackPanChange    = { id, p -> viewModel.setTrackPan(id, p) },
                        onTrackMuteToggle   = { viewModel.toggleTrackMute(it) },
                        onMasterVolumeChange = { viewModel.setMasterVolume(it) }
                    )

                    EditorTab.EFFECTS -> EffectsPanel(
                        effects        = state.effects,
                        onToggleEffect = { viewModel.toggleEffect(it) },
                        onSetIntensity = { id, v -> viewModel.setEffectIntensity(id, v) }
                    )

                    EditorTab.TEXT -> TextPanel(
                        overlays       = state.textOverlays,
                        onAddOverlay   = { text, style, color -> viewModel.addTextOverlay(text, style, color) },
                        onRemoveOverlay = { viewModel.removeTextOverlay(it) }
                    )

                    EditorTab.EXPORT -> ExportPanel(
                        config          = state.exportConfig,
                        isExporting     = state.isExporting,
                        exportProgress  = state.exportProgress,
                        onConfigChange  = { viewModel.updateExportConfig(it) },
                        onExport        = { viewModel.startExport() }
                    )
                }
            }
        }

        if (showImportSheet) {
            MediaPickerSheet(viewModel = viewModel, onDismiss = { showImportSheet = false })
        }
        if (showVoiceoverSheet) {
            VoiceoverSheet(viewModel = viewModel, onDismiss = { showVoiceoverSheet = false })
        }
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
                ) {
                    Text("Rename", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
            }

            // Tappable project name → opens rename dialog
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { renameText = projectName; showRenameDialog = true }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    projectName,
                    style    = MaterialTheme.typography.titleLarge,
                    color    = TextPrimary,
                    maxLines = 1
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint     = TextSecondary.copy(0.5f),
                    modifier = Modifier.size(13.dp)
                )
            }

            IconButton(onClick = onRecord, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Mic, "Record", tint = AccentOrange)
            }
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Undo, "Undo",
                    tint = if (canUndo) TextSecondary else TextSecondary.copy(0.3f))
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Redo, "Redo",
                    tint = if (canRedo) TextSecondary else TextSecondary.copy(0.3f))
            }
            IconButton(onClick = onImport, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, "Import", tint = NeonCyan)
            }
            IconButton(onClick = onSave, enabled = !isSaving, modifier = Modifier.size(36.dp)) {
                if (isSaving) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, "Save",
                        tint = if (lastSavedAt > 0) NeonCyan else TextSecondary)
                }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonCyan.copy(0.06f))
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan.copy(0.7f)
            )
        }
    }
}

fun formatSaveTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000      -> "just now"
        diff < 3_600_000   -> "${diff / 60_000}m ago"
        else               -> "${diff / 3_600_000}h ago"
    }
}

// ─── Video Preview ────────────────────────────────────────────────────────────

@Composable
fun VideoPreviewArea(
    isPlaying       : Boolean,
    currentMs       : Long,
    durationMs      : Long,
    previewImageUri : String?,
    playerManager   : com.editor.app.utils.PlayerManager
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = playerManager.player
                    useController = false
                }
            },
            update  = { it.player = playerManager.player },
            modifier = Modifier.fillMaxSize()
        )

        if (previewImageUri != null) {
            AsyncImage(
                model              = previewImageUri,
                contentDescription = "Image preview",
                contentScale       = androidx.compose.ui.layout.ContentScale.Fit,
                modifier           = Modifier.fillMaxSize()
            )
        }

        if (!isPlaying && currentMs == 0L && previewImageUri == null) {
            Icon(Icons.Default.PlayCircle, "Play",
                tint = Color.White.copy(0.3f), modifier = Modifier.size(56.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(Color.Black.copy(0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "${formatTime(currentMs)} / ${formatTime(durationMs)}",
                style      = MaterialTheme.typography.bodySmall,
                color      = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize   = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(NeonCyan.copy(0.15f), RoundedCornerShape(6.dp))
                .border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text("1080p", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontSize = 10.sp)
        }
    }
}

// ─── Playback Controls ────────────────────────────────────────────────────────

@Composable
fun PlaybackControlsBar(
    isPlaying     : Boolean,
    currentMs     : Long,
    durationMs    : Long,
    masterVolume  : Float,
    playbackSpeed : Float,
    onPlayPause   : () -> Unit,
    onSeek        : (Long) -> Unit,
    onSkipBack    : () -> Unit,
    onSkipForward : () -> Unit,
    onRewind      : () -> Unit,
    onFastForward : () -> Unit,
    onSpeedChange : (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        val progress = if (durationMs > 0) currentMs.toFloat() / durationMs else 0f
        Slider(
            value         = progress,
            onValueChange = { onSeek((it * durationMs).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor         = NeonCyan,
                activeTrackColor   = NeonCyan,
                inactiveTrackColor = Color(0xFF33334A)
            ),
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onSkipBack, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Start", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onRewind, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.FastRewind, "Rewind", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick  = onPlayPause,
                    modifier = Modifier.size(44.dp).background(NeonCyan, CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint     = DeepBlack,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = onFastForward, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.FastForward, "Forward", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onSkipForward, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipNext, "End", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(CardDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, "Volume",
                        tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Text("${(masterVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }

                var showSpeedMenu by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .background(CardDark, RoundedCornerShape(8.dp))
                            .clickable { showSpeedMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Speed, "Speed",
                            tint = if (playbackSpeed != 1f) NeonCyan else TextSecondary,
                            modifier = Modifier.size(14.dp))
                        Text("${playbackSpeed}×",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (playbackSpeed != 1f) NeonCyan else TextSecondary)
                    }
                    DropdownMenu(
                        expanded         = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                        modifier         = Modifier.background(CardDark)
                    ) {
                        listOf(0.25f, 0.5f, 1f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text("${speed}×",
                                        color      = if (speed == playbackSpeed) NeonCyan else TextPrimary,
                                        fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal)
                                },
                                leadingIcon = {
                                    if (speed == playbackSpeed)
                                        Icon(Icons.Default.Check, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                },
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
    val tabs = listOf(
        EditorTab.EDIT    to "Edit",
        EditorTab.AUDIO   to "Audio",
        EditorTab.EFFECTS to "Effects",
        EditorTab.TEXT    to "Text",
        EditorTab.EXPORT  to "Export"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDarker)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp)
    ) {
        tabs.forEach { (tab, label) ->
            val isActive = tab == activeTab
            TextButton(
                onClick  = { onTabChange(tab) },
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                colors   = ButtonDefaults.textButtonColors(
                    contentColor = if (isActive) NeonCyan else TextSecondary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 13.sp)
                    if (isActive) {
                        Box(Modifier.width(20.dp).height(2.dp).background(NeonCyan, RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
    }
}

// ─── Timeline Panel ───────────────────────────────────────────────────────────

@Composable
fun TimelinePanel(
    tracks            : List<TimelineTrack>,
    currentMs         : Long,
    durationMs        : Long,
    zoomLevel         : Float,
    selectedClipId    : String?,
    selectedClipIds   : Set<String>,
    isMultiSelectMode : Boolean,
    onClipSelect      : (String) -> Unit,
    onSeek            : (Long) -> Unit,
    onMuteTrack       : (String) -> Unit,
    onLockTrack       : (String) -> Unit,
    onDeleteClip      : (String, String) -> Unit,
    onSplitClip       : (String, String) -> Unit,
    onZoomChange      : (Float) -> Unit,
    onMoveClip        : (fromTrack: String, toTrack: String, clipId: String, newStartMs: Long) -> Unit,
    onTrimClip        : (trackId: String, clipId: String, trimMs: Long, isStart: Boolean) -> Unit,
    onTransitionChange: (trackId: String, clipId: String, transition: ClipTransition) -> Unit,
    onMultiSelectToggle : (String) -> Unit,
    onDeleteSelected    : () -> Unit,
    onDuplicateSelected : () -> Unit,
    onClearMultiSelect  : () -> Unit,
    // ── Feature 3: Add / Remove track ─────────────────────────────────────────
    onAddTrack    : (TrackType, String) -> Unit,
    onRemoveTrack : (String) -> Unit
) {
    val pxPerMs   = 0.08f * zoomLevel
    val dragState = rememberDragDropState()

    Column(modifier = Modifier.fillMaxSize().background(CardDarker)) {

        // Zoom toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Timeline", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { onZoomChange(zoomLevel - 0.25f) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ZoomOut, "Zoom out", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                Text("${(zoomLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                IconButton(onClick = { onZoomChange(zoomLevel + 0.25f) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ZoomIn, "Zoom in", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Multi-select action bar
        if (isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonPurple.copy(0.1f))
                    .border(BorderStroke(1.dp, NeonPurple.copy(0.3f)))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("${selectedClipIds.size} selected",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = NeonPurple,
                    fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionChip("Duplicate", NeonCyan)  { onDuplicateSelected() }
                    SmallActionChip("Delete",    NeonPink)  { onDeleteSelected() }
                    SmallActionChip("Clear",     TextSecondary) { onClearMultiSelect() }
                }
            }
        }

        TimelineRuler(durationMs = durationMs, pxPerMs = pxPerMs)

        // Drag position banner
        if (dragState.isDragging) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonCyan.copy(0.08f))
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Moving clip → ${formatTime(dragState.pendingStartMs)}",
                    style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                Text("Release to drop",
                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            tracks.forEach { track ->
                TrackRow(
                    track             = track,
                    durationMs        = durationMs,
                    pxPerMs           = pxPerMs,
                    currentMs         = currentMs,
                    selectedClipId    = selectedClipId,
                    selectedClipIds   = selectedClipIds,
                    isMultiSelectMode = isMultiSelectMode,
                    dragState         = dragState,
                    onClipSelect      = onClipSelect,
                    onMute            = { onMuteTrack(track.id) },
                    onLock            = { onLockTrack(track.id) },
                    onDeleteClip      = { onDeleteClip(track.id, it) },
                    onSplitClip       = { onSplitClip(track.id, it) },
                    onDropClip        = { clipId, newStartMs ->
                        val fromTrack = dragState.draggingTrackId ?: track.id
                        onMoveClip(fromTrack, track.id, clipId, newStartMs)
                    },
                    onTrimClip        = { clipId, trimMs, isStart ->
                        onTrimClip(track.id, clipId, trimMs, isStart)
                    },
                    onMultiSelectToggle = { onMultiSelectToggle(it) },
                    onTransitionChange  = { clipId, transition ->
                        onTransitionChange(track.id, clipId, transition)
                    },
                    // ── Feature 3 ──────────────────────────────────────────────
                    onRemoveTrack = { onRemoveTrack(track.id) }
                )
            }
            // ── Feature 3: wired Add Track button ─────────────────────────────
            AddTrackRow(onAddTrack = onAddTrack)
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
fun SmallActionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color.copy(0.12f), RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, color.copy(0.3f)), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimelineRuler(durationMs: Long, pxPerMs: Float) {
    val totalWidthDp = (durationMs * pxPerMs).dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(SurfaceDark)
            .horizontalScroll(rememberScrollState())
    ) {
        Canvas(modifier = Modifier.width(maxOf(totalWidthDp, 600.dp)).fillMaxHeight()) {
            var ms = 0L
            while (ms <= durationMs) {
                val x       = ms * pxPerMs * density
                val isMajor = ms % 10_000L == 0L
                drawLine(
                    color       = if (isMajor) Color(0xFF555566) else Color(0xFF2E2E3E),
                    start       = Offset(x, if (isMajor) 0f else size.height * 0.6f),
                    end         = Offset(x, size.height),
                    strokeWidth = if (isMajor) 1.5f else 1f
                )
                ms += 5_000L
            }
        }
    }
}

// ─── Track Row ────────────────────────────────────────────────────────────────

@Composable
fun TrackRow(
    track             : TimelineTrack,
    durationMs        : Long,
    pxPerMs           : Float,
    currentMs         : Long,
    selectedClipId    : String?,
    selectedClipIds   : Set<String>,
    isMultiSelectMode : Boolean,
    onMultiSelectToggle : (String) -> Unit,
    dragState         : com.editor.app.utils.DragDropState,
    onClipSelect      : (String) -> Unit,
    onMute            : () -> Unit,
    onLock            : () -> Unit,
    onDeleteClip      : (String) -> Unit,
    onSplitClip       : (String) -> Unit,
    onDropClip        : (clipId: String, newStartMs: Long) -> Unit,
    onTrimClip        : (clipId: String, trimMs: Long, isStart: Boolean) -> Unit,
    onTransitionChange: (clipId: String, transition: ClipTransition) -> Unit,
    // ── Feature 3 ─────────────────────────────────────────────────────────────
    onRemoveTrack     : () -> Unit
) {
    val isHovered = dragState.hoveredTrackId == track.id && dragState.isDragging

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .border(
                BorderStroke(
                    if (isHovered) 1.dp else 0.5.dp,
                    if (isHovered) NeonCyan.copy(0.6f) else Color(0xFF1A1A2A)
                )
            )
            .onGloballyPositioned { coords ->
                if (dragState.isDragging) {
                    val bounds = coords.boundsInRoot()
                    if (bounds.contains(dragState.dragOffset)) {
                        dragState.hoveredTrackId = track.id
                    }
                }
            }
    ) {
        // ── Track header ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(84.dp)
                .fillMaxHeight()
                .background(SurfaceDark)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Name row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(8.dp).background(track.color, CircleShape))
                Text(
                    track.name,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TextPrimary,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
            // Controls row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        if (track.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        "Mute",
                        tint     = if (track.isMuted) NeonPink else TextSecondary,
                        modifier = Modifier.size(14.dp).clickable { onMute() }
                    )
                    Icon(
                        if (track.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        "Lock",
                        tint     = if (track.isLocked) AccentYellow else TextSecondary,
                        modifier = Modifier.size(14.dp).clickable { onLock() }
                    )
                }
                // Remove track × button
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove track",
                    tint     = TextSecondary.copy(0.4f),
                    modifier = Modifier
                        .size(12.dp)
                        .clickable { onRemoveTrack() }
                )
            }
        }

        // ── Clip canvas ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isHovered) TimelineBar.copy(alpha = 0.7f) else TimelineBar)
                .horizontalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .width(maxOf((durationMs * pxPerMs).dp, 600.dp))
                    .fillMaxHeight()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var x = 0f
                    while (x < size.width) {
                        drawLine(Color(0xFF1E1E2E), Offset(x, 0f), Offset(x, size.height), 0.5f)
                        x += 40f
                    }
                }

                if (isHovered && dragState.isDragging) {
                    val dropX = (dragState.pendingStartMs * pxPerMs).dp
                    Box(
                        modifier = Modifier
                            .offset(x = dropX)
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(NeonCyan)
                    )
                }

                track.clips.forEach { clip ->
                    ClipBlock(
                        clip                = clip,
                        pxPerMs             = pxPerMs,
                        isSelected          = clip.id == selectedClipId,
                        isMultiSelected     = clip.id in selectedClipIds,
                        isMultiSelectMode   = isMultiSelectMode,
                        isTrackLocked       = track.isLocked,
                        isDragging          = dragState.draggingClipId == clip.id,
                        onSelect            = { onClipSelect(clip.id) },
                        onMultiSelectToggle = { onMultiSelectToggle(clip.id) },
                        onDelete            = { onDeleteClip(clip.id) },
                        onSplit             = { onSplitClip(clip.id) },
                        onTransitionChange  = { transition -> onTransitionChange(clip.id, transition) },
                        onDragStart         = { startOffset ->
                            if (!track.isLocked) {
                                dragState.startDrag(
                                    clipId         = clip.id,
                                    trackId        = track.id,
                                    startOffset    = startOffset,
                                    originalStartMs = clip.startTimeMs
                                )
                            }
                        },
                        onDragUpdate = { offset -> dragState.updateDrag(offset, pxPerMs) },
                        onDragEnd    = { onDropClip(clip.id, dragState.pendingStartMs); dragState.endDrag() },
                        onTrimStart  = { newTrimMs -> onTrimClip(clip.id, newTrimMs, true) },
                        onTrimEnd    = { newTrimMs -> onTrimClip(clip.id, newTrimMs, false) }
                    )
                }

                // Playhead
                Box(
                    Modifier
                        .offset(x = (currentMs * pxPerMs).dp - 1.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(NeonCyan)
                )
            }
        }
    }
}

// ─── Clip Block ───────────────────────────────────────────────────────────────

@Composable
fun ClipBlock(
    clip                : MediaClip,
    pxPerMs             : Float,
    isSelected          : Boolean,
    isMultiSelected     : Boolean,
    isMultiSelectMode   : Boolean,
    isTrackLocked       : Boolean,
    isDragging          : Boolean,
    onSelect            : () -> Unit,
    onDelete            : () -> Unit,
    onTransitionChange  : (ClipTransition) -> Unit,
    onSplit             : () -> Unit,
    onMultiSelectToggle : () -> Unit,
    onDragStart         : (Offset) -> Unit,
    onDragUpdate        : (Offset) -> Unit,
    onDragEnd           : () -> Unit,
    onTrimStart         : (Long) -> Unit,
    onTrimEnd           : (Long) -> Unit
) {
    var showMenu             by remember { mutableStateOf(false) }
    var showTransitionPicker by remember { mutableStateOf(false) }

    if (showTransitionPicker) {
        AlertDialog(
            onDismissRequest = { showTransitionPicker = false },
            containerColor   = CardDark,
            title = { Text("Choose Transition", color = TextPrimary) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransitionType.values().forEach { type ->
                        val isActive = clip.transition.type == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isActive) NeonPurple.copy(0.1f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onTransitionChange(ClipTransition(type = type)); showTransitionPicker = false }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                type.name.replace("_", " "),
                                color      = if (isActive) NeonPurple else TextPrimary,
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isActive) Icon(Icons.Default.Check, null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    val alpha by animateFloatAsState(
        targetValue   = if (isDragging) 0.4f else 1f,
        animationSpec = tween(150),
        label         = "clipAlpha"
    )

    val startX = ((clip.startTimeMs + clip.trimStartMs) * pxPerMs).dp
    val width  = (clip.visibleDurationMs * pxPerMs).dp

    Box(
        modifier = Modifier
            .offset(x = startX)
            .width(width)
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .alpha(alpha)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isMultiSelected) clip.color.copy(0.6f)
                    else clip.color.copy(if (isSelected) 1f else 0.8f)
                )
                .border(
                    BorderStroke(
                        when { isMultiSelected -> 2.dp; isSelected -> 1.5.dp; else -> 0.5.dp },
                        when { isMultiSelected -> NeonPurple; isSelected -> Color.White.copy(0.9f); else -> Color.White.copy(0.2f) }
                    ),
                    RoundedCornerShape(6.dp)
                )
                .pointerInput(clip.id, isTrackLocked, isMultiSelectMode) {
                    if (isTrackLocked) return@pointerInput
                    if (isMultiSelectMode) {
                        detectTapGestures { onMultiSelectToggle() }
                    } else {
                        detectDragGesturesAfterLongPress(
                            onDragStart  = { offset -> onSelect(); onDragStart(offset) },
                            onDrag       = { change, _ -> change.consume(); onDragUpdate(change.position) },
                            onDragEnd    = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
                }
                .clickable(enabled = !isTrackLocked) {
                    if (isMultiSelectMode) onMultiSelectToggle()
                    else { onSelect(); showMenu = true }
                }
        ) {
            if (clip.type == ClipType.AUDIO && clip.waveformData.isNotEmpty()) {
                WaveformView(waveformData = clip.waveformData, color = Color.White.copy(0.4f))
            }

            Text(
                clip.name,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(0.9f),
                fontSize = 9.sp,
                maxLines = 1
            )

            if (clip.transition.type != TransitionType.NONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(8.dp)
                        .background(NeonPurple, CircleShape)
                )
            }

            if (isMultiSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(3.dp)
                        .size(14.dp)
                        .background(NeonPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }

            if (!isMultiSelectMode) {
                DropdownMenu(
                    expanded         = showMenu && isSelected,
                    onDismissRequest = { showMenu = false },
                    modifier         = Modifier.background(CardDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Split at playhead", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCut, null, tint = NeonCyan, modifier = Modifier.size(16.dp)) },
                        onClick = { onSplit(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Transition: ${clip.transition.type.name}", color = TextPrimary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Transform, null, tint = NeonPurple, modifier = Modifier.size(16.dp)) },
                        trailingIcon = { Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(14.dp)) },
                        onClick = { showTransitionPicker = true; showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = NeonPink, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = NeonPink, modifier = Modifier.size(16.dp)) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }

        // Left trim handle
        if (isSelected && !isTrackLocked && !isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(12.dp)
                    .fillMaxHeight(0.7f)
                    .background(Color.White, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .pointerInput(clip.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onTrimStart(clip.trimStartMs + (dragAmount.x / pxPerMs).toLong())
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    repeat(3) {
                        Box(Modifier.size(width = 2.dp, height = 6.dp).background(Color.Black.copy(0.5f)))
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }

        // Right trim handle
        if (isSelected && !isTrackLocked && !isMultiSelectMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(12.dp)
                    .fillMaxHeight(0.7f)
                    .background(Color.White, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .pointerInput(clip.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onTrimEnd(clip.trimEndMs - (dragAmount.x / pxPerMs).toLong())
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    repeat(3) {
                        Box(Modifier.size(width = 2.dp, height = 6.dp).background(Color.Black.copy(0.5f)))
                        Spacer(Modifier.height(2.dp))
                    }
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
            val barH = amp * size.height * 0.7f
            val x    = i * barWidth + barWidth / 2
            drawLine(color,
                Offset(x, size.height / 2 - barH / 2),
                Offset(x, size.height / 2 + barH / 2),
                strokeWidth = max(barWidth * 0.6f, 1f))
        }
    }
}

// ─── Add Track Row (Feature 3) ────────────────────────────────────────────────

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

                    Text("Track Type",
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrackType.values().forEach { type ->
                            val isSelected = type == selectedType
                            val typeColor  = when (type) {
                                TrackType.VIDEO   -> NeonCyan
                                TrackType.AUDIO   -> WaveformGreen
                                TrackType.EFFECTS -> NeonPurple
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) typeColor.copy(0.15f) else CardDarker,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) typeColor else Color(0xFF2A2A3A)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedType = type }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style      = MaterialTheme.typography.bodySmall,
                                    color      = if (isSelected) typeColor else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    LaunchedEffect(selectedType) {
                        if (trackName.isBlank() || trackName.startsWith("New ")) {
                            trackName = when (selectedType) {
                                TrackType.VIDEO   -> "New Video"
                                TrackType.AUDIO   -> "New Audio"
                                TrackType.EFFECTS -> "New FX"
                            }
                        }
                    }

                    OutlinedTextField(
                        value         = trackName,
                        onValueChange = { trackName = it },
                        label         = { Text("Track name", color = TextSecondary) },
                        singleLine    = true,
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddTrack(selectedType, trackName.trim().ifBlank { "Track" })
                        showSheet = false
                        trackName = ""
                    },
                    enabled = trackName.isNotBlank()
                ) {
                    Text("Add Track", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSheet = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(CardDarker)
            .border(BorderStroke(0.5.dp, Color(0xFF22223A)))
            .clickable { showSheet = true }
            .padding(horizontal = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(NeonCyan.copy(0.12f), CircleShape)
                .border(1.dp, NeonCyan.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, "Add track", tint = NeonCyan, modifier = Modifier.size(14.dp))
        }
        Text("Add Track",
            style      = MaterialTheme.typography.bodySmall,
            color      = NeonCyan,
            fontWeight = FontWeight.Medium)
    }
}

// ─── Audio Mixer Panel ────────────────────────────────────────────────────────

@Composable
fun AudioMixerPanel(
    tracks               : List<TimelineTrack>,
    masterVolume         : Float,
    onTrackVolumeChange  : (String, Float) -> Unit,
    onTrackPanChange     : (String, Float) -> Unit,
    onTrackMuteToggle    : (String) -> Unit,
    onMasterVolumeChange : (Float) -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(CardDarker),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Audio Mixer", style = MaterialTheme.typography.headlineMedium, color = TextPrimary) }
        items(tracks) { track ->
            AudioChannelStrip(
                track          = track,
                onVolumeChange = { onTrackVolumeChange(track.id, it) },
                onPanChange    = { onTrackPanChange(track.id, it) },
                onMuteToggle   = { onTrackMuteToggle(track.id) }
            )
        }
        item { MasterVolumeStrip(volume = masterVolume, onVolumeChange = onMasterVolumeChange) }
    }
}

@Composable
fun AudioChannelStrip(
    track          : TimelineTrack,
    onVolumeChange : (Float) -> Unit,
    onPanChange    : (Float) -> Unit,
    onMuteToggle   : () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = CardDark),
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, Color(0xFF22223A))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(track.color, CircleShape))
                    Text(track.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MixerChip(
                        label = if (track.isMuted) "MUTED" else "LIVE",
                        color = if (track.isMuted) NeonPink else WaveformGreen,
                        onClick = onMuteToggle
                    )
                    MixerChip("EQ", NeonCyan)
                    MixerChip("FX", NeonPurple)
                }
            }
            LabeledSlider("VOL", track.volume, track.color, "${(track.volume * 100).toInt()}%", onVolumeChange)
            val panLabel = when {
                track.pan < -0.05f -> "L${(-track.pan * 100).toInt()}"
                track.pan > 0.05f  -> "R${(track.pan * 100).toInt()}"
                else               -> "C"
            }
            LabeledSlider("PAN", (track.pan + 1f) / 2f, AccentOrange, panLabel) { onPanChange(it * 2f - 1f) }
            VUMeter(volume = track.volume, color = track.color)
        }
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, color: Color, valueText: String, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(28.dp))
        Slider(
            value          = value,
            onValueChange  = onValueChange,
            modifier       = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = Color(0xFF33334A))
        )
        Text(valueText, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(36.dp))
    }
}

@Composable
fun VUMeter(volume: Float, color: Color) {
    val segments = 20
    Row(modifier = Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(segments) { i ->
            val threshold = (i + 1f) / segments
            val isActive  = volume >= threshold
            val segColor  = when {
                i >= segments * 0.85f -> if (isActive) Color(0xFFFF4444) else Color(0xFFFF4444).copy(0.15f)
                i >= segments * 0.70f -> if (isActive) AccentYellow else AccentYellow.copy(0.15f)
                else                  -> if (isActive) color else color.copy(0.15f)
            }
            Box(Modifier.weight(1f).fillMaxHeight().background(segColor, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
fun MixerChip(label: String, color: Color, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .background(color.copy(0.12f), RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, color.copy(0.35f)), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MasterVolumeStrip(volume: Float, onVolumeChange: (Float) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF0D0D1A)),
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, NeonCyan.copy(0.3f))
    ) {
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

@Composable
fun EffectsPanel(
    effects        : List<EditEffect>,
    onToggleEffect : (String) -> Unit,
    onSetIntensity : (String, Float) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(EffectCategory.FILTER) }
    val filtered         = effects.filter { it.category == selectedCategory }
    val selectedEffect   = effects.firstOrNull { it.isApplied && it.category == selectedCategory }

    Column(modifier = Modifier.fillMaxSize().background(CardDarker)) {
        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EffectCategory.values()) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .background(if (isSelected) NeonPurple.copy(0.2f) else CardDark, RoundedCornerShape(20.dp))
                        .border(BorderStroke(1.dp, if (isSelected) NeonPurple else Color(0xFF22223A)), RoundedCornerShape(20.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        cat.name.replace("_", " "),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (isSelected) NeonPurple else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered) { effect ->
                EffectCard(effect = effect, onClick = { onToggleEffect(effect.id) })
            }
        }

        HorizontalDivider(color = Color(0xFF22223A), modifier = Modifier.padding(vertical = 8.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (selectedEffect != null) "Adjust: ${selectedEffect.name}" else "Select an effect to adjust",
                style = MaterialTheme.typography.titleLarge,
                color = if (selectedEffect != null) TextPrimary else TextSecondary
            )
            if (selectedEffect != null) {
                LabeledSlider("Intensity", selectedEffect.intensity, NeonPurple,
                    "${(selectedEffect.intensity * 100).toInt()}%") { onSetIntensity(selectedEffect.id, it) }
                LabeledSlider("Blend", 0.5f, NeonCyan,     "50%") {}
                LabeledSlider("Fade",  0.3f, AccentOrange, "30%") {}
            }
        }
    }
}

@Composable
fun EffectCard(effect: EditEffect, onClick: () -> Unit) {
    Column(
        modifier            = Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(if (effect.isApplied) NeonPurple.copy(0.2f) else CardDark, RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, if (effect.isApplied) NeonPurple else Color(0xFF22223A)), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(effect.emoji, fontSize = 24.sp)
        }
        Text(effect.name, style = MaterialTheme.typography.labelSmall,
            color = if (effect.isApplied) NeonPurple else TextSecondary, fontSize = 10.sp)
    }
}

// ─── Text Panel ───────────────────────────────────────────────────────────────

@Composable
fun TextPanel(
    overlays        : List<TextOverlay>,
    onAddOverlay    : (String, String, Color) -> Unit,
    onRemoveOverlay : (String) -> Unit
) {
    var textInput     by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Title") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    val styles = listOf("Title", "Caption", "Body", "Watermark", "Quote")
    val colorOptions = listOf(Color.White, NeonCyan, NeonPink, NeonPurple, AccentYellow, AccentOrange)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardDarker)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add Text Overlay", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

        OutlinedTextField(
            value         = textInput,
            onValueChange = { textInput = it },
            placeholder   = { Text("Type your text...", color = TextSecondary) },
            modifier      = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = NeonCyan,
                unfocusedBorderColor = Color(0xFF33334A),
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
                cursorColor          = NeonCyan
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Text("Style", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles) { style ->
                Box(
                    modifier = Modifier
                        .background(
                            if (selectedStyle == style) NeonCyan.copy(0.15f) else CardDark,
                            RoundedCornerShape(10.dp)
                        )
                        .border(BorderStroke(1.dp, if (selectedStyle == style) NeonCyan else Color(0xFF22223A)), RoundedCornerShape(10.dp))
                        .clickable { selectedStyle = style }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(style, style = MaterialTheme.typography.bodySmall,
                        color = if (selectedStyle == style) NeonCyan else TextSecondary)
                }
            }
        }

        Text("Color", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colorOptions.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color, CircleShape)
                        .border(
                            BorderStroke(
                                if (selectedColor == color) 2.5.dp else 1.dp,
                                if (selectedColor == color) Color.White else Color.White.copy(0.2f)
                            ),
                            CircleShape
                        )
                        .clickable { selectedColor = color }
                )
            }
        }

        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    onAddOverlay(textInput, selectedStyle, selectedColor)
                    textInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape    = RoundedCornerShape(12.dp),
            enabled  = textInput.isNotBlank()
        ) {
            Icon(Icons.Default.TextFields, "Add", tint = DeepBlack)
            Spacer(Modifier.width(8.dp))
            Text("Add to Timeline", color = DeepBlack, fontWeight = FontWeight.Bold)
        }

        if (overlays.isNotEmpty()) {
            Text("Added Overlays", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            overlays.forEach { overlay ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDark, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(overlay.color, CircleShape))
                        Column {
                            Text(overlay.text, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text("${overlay.style} · ${formatTime(overlay.startTimeMs)}",
                                style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
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
    config         : ExportConfig,
    isExporting    : Boolean,
    exportProgress : Float,
    onConfigChange : (ExportConfig) -> Unit,
    onExport       : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardDarker)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Export Settings", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

        ExportSection("Resolution") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoResolution.values().forEach { res ->
                    SelectableChip(res.label, config.resolution == res) { onConfigChange(config.copy(resolution = res)) }
                }
            }
        }
        ExportSection("Frame Rate") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(24, 30, 60).forEach { fps ->
                    SelectableChip("${fps}fps", config.fps == fps) { onConfigChange(config.copy(fps = fps)) }
                }
            }
        }
        ExportSection("Quality") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoBitrate.values().forEach { bitrate ->
                    SelectableChip(bitrate.label, config.bitrate == bitrate) { onConfigChange(config.copy(bitrate = bitrate)) }
                }
            }
        }
        ExportSection("Format") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoFormat.values().forEach { fmt ->
                    SelectableChip(fmt.label, config.format == fmt) { onConfigChange(config.copy(format = fmt)) }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(0.05f)),
            shape  = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(0.3f))
        ) {
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
                    Text("Exporting...", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text("${(exportProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                }
                LinearProgressIndicator(
                    progress   = { exportProgress },
                    modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color      = NeonCyan,
                    trackColor = Color(0xFF33334A)
                )
            }
        }

        Button(
            onClick  = onExport,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled  = !isExporting,
            colors   = ButtonDefaults.buttonColors(
                containerColor         = NeonCyan,
                disabledContainerColor = NeonCyan.copy(0.3f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isExporting) {
                CircularProgressIndicator(color = DeepBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.FileUpload, "Export", tint = DeepBlack, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export Video", color = DeepBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ExportSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        content()
    }
}

@Composable
fun SelectableChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (isSelected) NeonCyan.copy(0.15f) else CardDark, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF22223A)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.bodySmall,
            color      = if (isSelected) NeonCyan else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ExportSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    val millis   = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, millis)
}