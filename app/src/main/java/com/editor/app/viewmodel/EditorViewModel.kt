package com.editor.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.editor.app.data.*
import com.editor.app.repository.ProjectRepository
import com.editor.app.utils.AudioMixEngine
import com.editor.app.utils.PlayerManager
import com.editor.app.utils.VoiceRecorder
import com.editor.app.utils.WaveformExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(context: Context) : ViewModel() {

    // Player

    val playerManager = PlayerManager(context)
    private val repository = ProjectRepository(context)
    private val voiceRecorder = VoiceRecorder(context)
    val audioMixEngine = AudioMixEngine(context)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionSync() else stopPositionSync()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val videoDuration = playerManager.getDuration()
                val totalDuration = _uiState.value.durationMs

                if (totalDuration > videoDuration) {
                    // Audio track is longer — keep audio + scrubber going
                    playerManager.pause()
                    startAudioOnlyTick(from = videoDuration)
                } else {
                    // Everything finished — STOP and reset, do NOT auto-play
                    stopPositionSync()
                    audioMixEngine.pauseAll()

                    // Reset ExoPlayer so it's ready for next manual play
                    playerManager.player.seekTo(0)
                    playerManager.player.prepare()
                    playerManager.player.playWhenReady = false  // ← prevents auto-play

                    // Reset UI to start
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            currentPositionMs = 0L
                        )
                    }
                }
            }
        }
    }

    private fun startAudioOnlyTick(from: Long) {
        stopPositionSync()
        positionSyncJob = viewModelScope.launch {
            var position = from
            val totalDuration = _uiState.value.durationMs

            while (position < totalDuration) {
                delay(50L)
                position += 50L
                _uiState.update { it.copy(currentPositionMs = position) }
            }

            // Audio finished — STOP and reset, do NOT auto-play
            audioMixEngine.pauseAll()

            // Reset ExoPlayer ready for next manual play
            playerManager.player.seekTo(0)
            playerManager.player.prepare()
            playerManager.player.playWhenReady = false  // ← prevents auto-play

            _uiState.update {
                it.copy(
                    isPlaying = false,
                    currentPositionMs = 0L   // ← scrubber back to start
                )
            }
        }
    }

    // ─── UI State ─────────────────────────────────────────────────────────────

    private var positionSyncJob: Job? = null
    private var hasRealVideo = false

    init {
        playerManager.addListener(playerListener)
    }


    // ─── Playback ─────────────────────────────────────────────────────────────

    // REPLACE existing togglePlayPause with this:
    fun togglePlayPause() {
        if (hasRealVideo) {
            if (playerManager.isPlaying()) {
                playerManager.pause()
                audioMixEngine.pauseAll()
            } else {
                // ← ADD THIS: reset player if it reached end state
                if (playerManager.player.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    playerManager.player.seekTo(0)
                    playerManager.player.prepare()
                }
                playerManager.play()
                audioMixEngine.playAll(playerManager.getCurrentPosition())
            }
        } else {
            val playing = !_uiState.value.isPlaying
            _uiState.update { it.copy(isPlaying = playing) }
            if (playing) {
                // ← Reset position to start if at end
                if (_uiState.value.currentPositionMs >= _uiState.value.durationMs) {
                    _uiState.update { it.copy(currentPositionMs = 0L) }
                }
                startDemoTick()
                audioMixEngine.playAll(_uiState.value.currentPositionMs)
            } else {
                stopPositionSync()
                audioMixEngine.pauseAll()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _uiState.value.durationMs)
        _uiState.update { it.copy(currentPositionMs = clamped) }
        if (hasRealVideo) {
            playerManager.seekTo(clamped)
            // ← ADD THIS: if player ended, prepare it again so it can replay
            if (playerManager.player.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                playerManager.player.seekTo(clamped)
                playerManager.player.prepare()
            }
        }
    }

    fun seekToStart() = seekTo(0L)
    fun seekToEnd() = seekTo(_uiState.value.durationMs)
    fun skipBackward(ms: Long = 5_000L) = seekTo(_uiState.value.currentPositionMs - ms)
    fun skipForward(ms: Long = 5_000L) = seekTo(_uiState.value.currentPositionMs + ms)

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    // Syncs UI position with real ExoPlayer position
    private fun startPositionSync() {
        stopPositionSync()
        positionSyncJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                val pos = playerManager.getCurrentPosition()
                _uiState.update { it.copy(currentPositionMs = pos) }
            }
        }
    }

    // Demo tick when no real video loaded
    private fun startDemoTick() {
        stopPositionSync()
        positionSyncJob = viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                delay(50L)
                _uiState.update { state ->
                    val next = state.currentPositionMs + 50L
                    if (next >= state.durationMs) {
                        state.copy(currentPositionMs = state.durationMs, isPlaying = false)
                    } else {
                        state.copy(currentPositionMs = next)
                    }
                }
                if (!_uiState.value.isPlaying) break
            }
        }
    }

    private fun stopPositionSync() {
        positionSyncJob?.cancel()
        positionSyncJob = null
    }

    // ─── Volume & Speed ───────────────────────────────────────────────────────

    fun setMasterVolume(volume: Float) {
        playerManager.setVolume(volume)
        _uiState.update { it.copy(masterVolume = volume.coerceIn(0f, 1f)) }
    }

    // ─── Tab Navigation ───────────────────────────────────────────────────────

    fun setActiveTab(tab: EditorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ─── Clip Selection ───────────────────────────────────────────────────────

    fun selectClip(clipId: String?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    // ─── Track Controls ───────────────────────────────────────────────────────

    fun toggleTrackMute(trackId: String) {
        _uiState.update { state ->
            state.copy(tracks = state.tracks.map { track ->
                if (track.id == trackId) track.copy(isMuted = !track.isMuted) else track
            })
        }
    }

    fun toggleTrackLock(trackId: String) {
        _uiState.update { state ->
            state.copy(tracks = state.tracks.map { track ->
                if (track.id == trackId) track.copy(isLocked = !track.isLocked) else track
            })
        }
    }

    // REPLACE existing setTrackVolume with this:
    fun setTrackVolume(trackId: String, volume: Float) {
        _uiState.update { state ->
            state.copy(tracks = state.tracks.map { track ->
                if (track.id == trackId) {
                    track.clips.forEach { clip ->
                        audioMixEngine.setTrackVolume(
                            "${trackId}_${clip.id}", volume, track.pan, track.isMuted
                        )
                    }
                    track.copy(volume = volume)
                } else track
            })
        }
    }


    fun setTrackPan(trackId: String, pan: Float) {
        _uiState.update { state ->
            state.copy(tracks = state.tracks.map { track ->
                if (track.id == trackId) track.copy(pan = pan.coerceIn(-1f, 1f)) else track
            })
        }
    }

    // ─── Clip Operations ──────────────────────────────────────────────────────

    fun deleteClip(trackId: String, clipId: String) {
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(
                tracks = state.tracks.map { track ->
                    if (track.id == trackId)
                        track.copy(clips = track.clips.filter { it.id != clipId })
                    else track
                },
                selectedClipId = if (state.selectedClipId == clipId) null else state.selectedClipId
            )
        }
    }

    fun splitClipAtPlayhead(trackId: String, clipId: String) {
        pushUndoSnapshot()
        val playhead = _uiState.value.currentPositionMs
        _uiState.update { state ->
            state.copy(tracks = state.tracks.map { track ->
                if (track.id != trackId) return@map track
                track.copy(clips = track.clips.flatMap { clip ->
                    if (clip.id != clipId) return@flatMap listOf(clip)
                    val splitPoint = playhead - clip.startTimeMs
                    if (splitPoint <= 0 || splitPoint >= clip.durationMs) return@flatMap listOf(clip)
                    listOf(
                        clip.copy(id = "${clip.id}_L", durationMs = splitPoint),
                        clip.copy(id = "${clip.id}_R",
                            startTimeMs = clip.startTimeMs + splitPoint,
                            durationMs = clip.durationMs - splitPoint)
                    )
                })
            })
        }
    }

    fun moveClipToTrack(
        fromTrackId: String,
        toTrackId: String,
        clipId: String,
        newStartMs: Long
    ) {
        pushUndoSnapshot()
        _uiState.update { state ->
            // Find the clip being moved
            val clip = state.tracks
                .find { it.id == fromTrackId }
                ?.clips?.find { it.id == clipId } ?: return@update state

            state.copy(
                tracks = state.tracks.map { track ->
                    when (track.id) {
                        // Remove clip from source track
                        fromTrackId -> track.copy(
                            clips = track.clips.filter { it.id != clipId }
                        )
                        // Add clip to destination track with new start time
                        toTrackId -> track.copy(
                            clips = track.clips + clip.copy(
                                startTimeMs = newStartMs.coerceAtLeast(0L)
                            )
                        )
                        else -> track
                    }
                }
            )
        }
    }

    fun addTrack(type: TrackType, name: String) {
        val trackColor = when (type) {
            TrackType.VIDEO   -> androidx.compose.ui.graphics.Color(0xFF0066DD)
            TrackType.AUDIO   -> androidx.compose.ui.graphics.Color(0xFF00AA66)
            TrackType.EFFECTS -> androidx.compose.ui.graphics.Color(0xFF9900DD)
        }
        val newTrack = TimelineTrack(
            id    = "${type.name.lowercase()}_${System.currentTimeMillis()}",
            name  = name,
            type  = type,
            color = trackColor,
            clips = emptyList()
        )
        _uiState.update { state ->
            state.copy(tracks = state.tracks + newTrack)
        }
    }

    fun removeTrack(trackId: String) {
        // Never remove the last track of a type
        val state = _uiState.value
        val typeToRemove = state.tracks.find { it.id == trackId }?.type ?: return
        val tracksOfType = state.tracks.count { it.type == typeToRemove }
        if (tracksOfType <= 1) return   // keep at least one of each type

        pushUndoSnapshot()
        _uiState.update { s ->
            s.copy(tracks = s.tracks.filter { it.id != trackId })
        }
    }

    // ─── Effects ──────────────────────────────────────────────────────────────

    fun toggleEffect(effectId: String) {
        _uiState.update { state ->
            state.copy(effects = state.effects.map { effect ->
                if (effect.id == effectId) effect.copy(isApplied = !effect.isApplied) else effect
            })
        }
    }

    fun setEffectIntensity(effectId: String, intensity: Float) {
        _uiState.update { state ->
            state.copy(effects = state.effects.map { effect ->
                if (effect.id == effectId) effect.copy(intensity = intensity.coerceIn(0f, 1f)) else effect
            })
        }
    }

    // ─── Text Overlays ────────────────────────────────────────────────────────

    fun addTextOverlay(text: String, style: String, color: androidx.compose.ui.graphics.Color) {
        val overlay = TextOverlay(
            id = "txt_${System.currentTimeMillis()}",
            text = text, style = style, color = color,
            startTimeMs = _uiState.value.currentPositionMs,
            durationMs = 3_000L
        )
        _uiState.update { it.copy(textOverlays = it.textOverlays + overlay) }
    }

    fun removeTextOverlay(id: String) {
        _uiState.update { it.copy(textOverlays = it.textOverlays.filter { o -> o.id != id }) }
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    fun updateExportConfig(config: ExportConfig) {
        _uiState.update { it.copy(exportConfig = config) }
    }

    fun startExport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f) }
            repeat(100) { i ->
                delay(60L)
                _uiState.update { it.copy(exportProgress = (i + 1) / 100f) }
            }
            _uiState.update { it.copy(isExporting = false, exportProgress = 0f) }
        }
    }

    // ─── Undo / Redo ──────────────────────────────────────────────────────────

    private fun pushUndoSnapshot() {
        _uiState.update { state ->
            state.copy(
                undoStack = (state.undoStack + listOf(state.tracks)).takeLast(20),
                redoStack = emptyList()
            )
        }
    }

    fun undo() {
        val state = _uiState.value
        if (state.undoStack.isEmpty()) return
        _uiState.update {
            it.copy(
                tracks = it.undoStack.last(),
                undoStack = it.undoStack.dropLast(1),
                redoStack = listOf(it.tracks) + it.redoStack
            )
        }
    }

    fun redo() {
        val state = _uiState.value
        if (state.redoStack.isEmpty()) return
        _uiState.update {
            it.copy(
                tracks = it.redoStack.first(),
                undoStack = it.undoStack + listOf(it.tracks),
                redoStack = it.redoStack.drop(1)
            )
        }
    }

    // ─── Zoom ─────────────────────────────────────────────────────────────────

    fun setZoom(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom.coerceIn(0.5f, 4f)) }
    }

    fun setProjectName(name: String) {
        _uiState.update { it.copy(projectName = name) }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
        audioMixEngine.releaseAll()   // ← added
        playerManager.release()
        positionSyncJob?.cancel()
    }

    // ─── Factory ──────────────────────────────────────────────────────────────

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EditorViewModel(context) as T
                }
            }
        }
    }


    // ─── Media Import ─────────────────────────────────────────────────────────────

    fun importVideoClip(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            // Grant permission to read this URI
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Non-persistable URI — still try to use it
            }

            val name = getMediaName(context, uri)
            val durationMs = getMediaDuration(context, uri).takeIf { it > 0L } ?: 10_000L

            val clip = MediaClip(
                id = "vid_${System.currentTimeMillis()}",
                name = name.substringBeforeLast("."),
                type = ClipType.VIDEO,
                durationMs = durationMs,
                startTimeMs = nextAvailableStart("video_1"),
                color = androidx.compose.ui.graphics.Color(0xFF0066DD),
                uri = uri.toString()
            )

            pushUndoSnapshot()
            addClipToTrack("video_1", clip)

            // Load URI directly into ExoPlayer — NOT the string, the actual Uri object
            hasRealVideo = true
            playerManager.loadUri(uri)

            // Wait for player to prepare then read duration
            kotlinx.coroutines.delay(800)
            val playerDuration = playerManager.getDuration()
            if (playerDuration > 0) {
                _uiState.update { it.copy(durationMs = playerDuration) }
            }
        }
    }

    fun importAudioClip(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { }

            val name = getMediaName(context, uri)
            val durationMs = getMediaDuration(context, uri).takeIf { it > 0L } ?: 10_000L

            val clip = MediaClip(
                id = "aud_${System.currentTimeMillis()}",
                name = name.substringBeforeLast("."),
                type = ClipType.AUDIO,
                durationMs = durationMs,
                startTimeMs = nextAvailableStart("audio_1"),
                color = androidx.compose.ui.graphics.Color(0xFF00AA66),
                uri = uri.toString(),
                waveformData = WaveformExtractor.extract(context, uri, targetBars = 60)
            )

            pushUndoSnapshot()
            addClipToTrack("audio_1", clip)

            // ← REMOVED playerManager.loadUri() — don't overwrite video
            // Load audio into AudioMixEngine instead
            audioMixEngine.loadTrack(
                trackId = "audio_1_${clip.id}",
                uriString = uri.toString(),
                volume = 1f,
                pan = 0f,
                isMuted = false
            )
        }
    }

    // ← ADD HERE
    fun syncAudioEngine() {
        val state = _uiState.value
        state.tracks.forEach { track ->
            track.clips.forEach { clip ->
                if (clip.type == ClipType.AUDIO && clip.uri != null) {
                    audioMixEngine.loadTrack(
                        trackId = "${track.id}_${clip.id}",
                        uriString = clip.uri,
                        volume = track.volume,
                        pan = track.pan,
                        isMuted = track.isMuted
                    )
                }
            }
        }
    }


    fun importImageClip(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { }

            val name = getMediaName(context, uri)

            val clip = MediaClip(
                id = "img_${System.currentTimeMillis()}",
                name = name.substringBeforeLast("."),
                type = ClipType.IMAGE,
                durationMs = 5_000L,
                startTimeMs = nextAvailableStart("video_1"),
                color = androidx.compose.ui.graphics.Color(0xFF006688),
                uri = uri.toString()
            )

            pushUndoSnapshot()
            addClipToTrack("video_1", clip)

            // Only show image preview if NO video is loaded
            // ← CHANGED: only set previewImageUri if no real video
            if (!hasRealVideo) {
                _uiState.update { it.copy(previewImageUri = uri.toString()) }
            }
        }
    }

    private fun getMediaDuration(context: Context, uri: android.net.Uri): Long {
        return try {
            val mmr = android.media.MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            val duration = mmr.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            mmr.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }

    private fun getMediaName(context: Context, uri: android.net.Uri): String {
        return try {
            val projection = arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            android.provider.MediaStore.MediaColumns.DISPLAY_NAME
                        )
                    )
                } else "Clip"
            } ?: uri.lastPathSegment ?: "Clip"
        } catch (e: Exception) {
            "Clip"
        }
    }

    // Find the next free start time on a track (after last clip)
    private fun nextAvailableStart(trackId: String): Long {
        val track = _uiState.value.tracks.find { it.id == trackId } ?: return 0L
        return track.clips.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
    }

    // Add clip to a specific track
    private fun addClipToTrack(trackId: String, clip: MediaClip) {
        _uiState.update { state ->
            state.copy(
                tracks = state.tracks.map { track ->
                    if (track.id == trackId) track.copy(clips = track.clips + clip)
                    else track
                },
                durationMs = maxOf(
                    state.durationMs,
                    clip.startTimeMs + clip.durationMs + 2_000L
                )
            )
        }
    }

    // ─── Project Save / Load ──────────────────────────────────────────────────────

    fun saveProject() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val savedId = repository.saveProject(_uiState.value)
                _uiState.update {
                    it.copy(
                        projectId = savedId,
                        isSaving = false,
                        lastSavedAt = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun loadProject(id: String) {
        viewModelScope.launch {
            val loadedState = repository.loadProject(id) ?: return@launch
            _uiState.value = loadedState
            // If loaded project has a video clip, reload into ExoPlayer
            val firstVideoUri = loadedState.tracks
                .flatMap { it.clips }
                .firstOrNull { it.type == ClipType.VIDEO && it.uri != null }
                ?.uri
            if (firstVideoUri != null) {
                hasRealVideo = true
                playerManager.loadUri(android.net.Uri.parse(firstVideoUri))
            }
        }
    }

    fun newProject() {
        stopPositionSync()
        playerManager.pause()
        _uiState.value = EditorUiState()
        hasRealVideo = false
    }

    // ─── Clip Trim ────────────────────────────────────────────────────────────────

    fun trimClipStart(trackId: String, clipId: String, trimStartMs: Long) {
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(tracks = state.tracks.map { track ->
                if (track.id != trackId) return@map track
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id != clipId) return@map clip
                    // Clamp: can't trim more than duration minus 500ms minimum
                    val maxTrim = clip.durationMs - clip.trimEndMs - 500L
                    clip.copy(trimStartMs = trimStartMs.coerceIn(0L, maxTrim))
                })
            })
        }
    }

    fun trimClipEnd(trackId: String, clipId: String, trimEndMs: Long) {
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(tracks = state.tracks.map { track ->
                if (track.id != trackId) return@map track
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id != clipId) return@map clip
                    val maxTrim = clip.durationMs - clip.trimStartMs - 500L
                    clip.copy(trimEndMs = trimEndMs.coerceIn(0L, maxTrim))
                })
            })
        }
    }

    // ─── Voiceover Recording ──────────────────────────────────────────────────────

    private var recordingStartTime = 0L
    private var recordingJob: kotlinx.coroutines.Job? = null

    fun startRecording() {
        if (_uiState.value.isRecording) return
        voiceRecorder.startRecording()
        recordingStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(isRecording = true, recordingDurationMs = 0L) }

        // Tick to update duration and amplitude live
        recordingJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val amplitude = voiceRecorder.getAmplitude()
                _uiState.update { it.copy(
                    recordingDurationMs = elapsed,
                    recordingAmplitude = amplitude
                )}
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        val file = voiceRecorder.stopRecording()
        _uiState.update { it.copy(isRecording = false, recordingAmplitude = 0) }

        if (file != null && file.exists()) {
            val durationMs = System.currentTimeMillis() - recordingStartTime

            val clip = MediaClip(
                id = "rec_${System.currentTimeMillis()}",
                name = "Voiceover",
                type = ClipType.AUDIO,
                durationMs = durationMs,
                startTimeMs = nextAvailableStart("voiceover_1"),
                color = androidx.compose.ui.graphics.Color(0xFFFF6B00),
                uri = file.toURI().toString(),
                waveformData = List(60) { (0.3f + Math.random().toFloat() * 0.7f) }
            )
            pushUndoSnapshot()
            addClipToTrack("voiceover_1", clip)
        }
    }

    // ─── Multi-select ─────────────────────────────────────────────────────────────

    fun toggleMultiSelect(clipId: String) {
        _uiState.update { state ->
            val newSet = if (clipId in state.selectedClipIds) {
                state.selectedClipIds - clipId
            } else {
                state.selectedClipIds + clipId
            }
            state.copy(
                selectedClipIds = newSet,
                isMultiSelectMode = newSet.isNotEmpty()
            )
        }
    }

    fun clearMultiSelect() {
        _uiState.update { it.copy(selectedClipIds = emptySet(), isMultiSelectMode = false) }
    }

    fun deleteSelectedClips() {
        pushUndoSnapshot()
        val toDelete = _uiState.value.selectedClipIds
        _uiState.update { state ->
            state.copy(
                tracks = state.tracks.map { track ->
                    track.copy(clips = track.clips.filter { it.id !in toDelete })
                },
                selectedClipIds = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    fun duplicateSelectedClips() {
        pushUndoSnapshot()
        _uiState.update { state ->
            val selected = state.selectedClipIds
            state.copy(
                tracks = state.tracks.map { track ->
                    val newClips = track.clips.filter { it.id in selected }.map { clip ->
                        clip.copy(
                            id = "dup_${System.currentTimeMillis()}_${clip.id}",
                            startTimeMs = clip.startTimeMs + clip.durationMs
                        )
                    }
                    track.copy(clips = track.clips + newClips)
                }
            )
        }
    }

    fun setClipTransition(trackId: String, clipId: String, transition: ClipTransition) {
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(tracks = state.tracks.map { track ->
                if (track.id != trackId) return@map track
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id != clipId) return@map clip
                    clip.copy(transition = transition)
                })
            })
        }
    }
}