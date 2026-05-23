package com.editor.app.data

import androidx.compose.ui.graphics.Color

// ─── Enums ────────────────────────────────────────────────────────────────────

enum class ClipType { VIDEO, AUDIO, IMAGE, TEXT }
enum class TrackType { VIDEO, AUDIO, EFFECTS }
enum class EditorTab { EDIT, AUDIO, EFFECTS, TEXT, EXPORT }
enum class EffectCategory { FILTER, TRANSITION, AUDIO_FX, SPEED, COLOR, TEXT, STICKER }

enum class TransitionType {
    NONE, FADE, DISSOLVE, WIPE_LEFT, WIPE_RIGHT, ZOOM_IN, ZOOM_OUT
}

data class ClipTransition(
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = 500L
)

enum class VideoResolution(val label: String, val width: Int, val height: Int) {
    SD_480P("480p", 854, 480),
    HD_720P("720p", 1280, 720),
    HD_1080P("1080p", 1920, 1080),
    UHD_4K("4K", 3840, 2160)
}
enum class VideoBitrate(val label: String, val kbps: Int) {
    LOW("Low", 2000),
    MEDIUM("Medium", 5000),
    HIGH("High", 10000),
    ULTRA("Ultra", 20000)
}
enum class VideoFormat(val label: String, val ext: String) {
    MP4("MP4", "mp4"),
    MOV("MOV", "mov"),
    WEBM("WebM", "webm")
}

// ─── Core Models ──────────────────────────────────────────────────────────────

data class MediaClip(
    val id: String,
    val name: String,
    val type: ClipType,
    val durationMs: Long,
    val startTimeMs: Long = 0L,
    val trimStartMs: Long = 0L,       // ← add: trim from beginning
    val trimEndMs: Long = 0L,         // ← add: trim from end
    val color: Color = Color(0xFF0088FF),
    val waveformData: List<Float> = emptyList(),
    val uri: String? = null,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val isMuted: Boolean = false,
    val transition: ClipTransition = ClipTransition()
) {
    // Actual visible duration after trim
    val visibleDurationMs get() = durationMs - trimStartMs - trimEndMs
}

data class TimelineTrack(
    val id: String,
    val name: String,
    val type: TrackType,
    val clips: List<MediaClip> = emptyList(),
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val volume: Float = 1f,
    val pan: Float = 0f,
    val color: Color = Color(0xFF0088FF),
    val eqBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val fxType: String = "None",
    val fxIntensity: Float = 0f
)

data class EditEffect(
    val id: String,
    val name: String,
    val category: EffectCategory,
    val emoji: String,
    val isApplied: Boolean = false,
    val intensity: Float = 0.5f,
    val blend: Float = 0.5f,
    val fade: Float = 0.3f
)

data class ExportConfig(
    val resolution: VideoResolution = VideoResolution.HD_1080P,
    val fps: Int = 30,
    val bitrate: VideoBitrate = VideoBitrate.HIGH,
    val format: VideoFormat = VideoFormat.MP4
)

data class TextOverlay(
    val id: String,
    val text: String,
    val style: String = "Title",
    val color: Color = Color.White,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 3000L
)

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val tracks: List<TimelineTrack>,
    val effects: List<EditEffect> = emptyList()
)

data class EditorUiState(
    val projectId: String = "",
    val projectName: String = "Untitled Project",
    val createdAt: Long = 0L,
    val tracks: List<TimelineTrack> = defaultTracks(),
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 30_000L,
    val isPlaying: Boolean = false,
    val zoomLevel: Float = 1f,
    val selectedClipId: String? = null,
    val selectedClipIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val activeTab: EditorTab = EditorTab.EDIT,
    val masterVolume: Float = 1f,
    val playbackSpeed: Float = 1f,
    val previewImageUri: String? = null,
    val effects: List<EditEffect> = defaultEffects(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val exportConfig: ExportConfig = ExportConfig(),
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val isSaving: Boolean = false,
    val exportError     : String? = null,
    val exportedFilePath : String? = null,
    val exportedFileUri : String? = null,
    val isSimulatedExport : Boolean = false,
    val lastSavedAt: Long = 0L,
    val undoStack: List<List<TimelineTrack>> = emptyList(),
    val redoStack: List<List<TimelineTrack>> = emptyList(),
    val isRecording: Boolean = false,
    val recordingAmplitude: Int = 0,
    val recordingDurationMs: Long = 0L,
    val selectedTemplateId: String? = null,
    val showSettings: Boolean = false,
    val showNotifications: Boolean = false
)

// ─── Seed Data ────────────────────────────────────────────────────────────────

fun defaultTracks(): List<TimelineTrack> = listOf(
    TimelineTrack(
        id = "video_1", name = "Video 1", type = TrackType.VIDEO,
        color = Color(0xFF0066DD),
        clips = listOf(
            MediaClip("c1", "Clip 1", ClipType.VIDEO, 8_000L, color = Color(0xFF0066DD)),
            MediaClip("c2", "Clip 2", ClipType.VIDEO, 6_000L, 9_000L, color = Color(0xFF0044BB)),
            MediaClip("c3", "Clip 3", ClipType.VIDEO, 10_000L, 16_000L, color = Color(0xFF0066DD))
        )
    ),
    TimelineTrack(
        id = "audio_1", name = "Music", type = TrackType.AUDIO,
        color = Color(0xFF00AA66),
        clips = listOf(
            MediaClip(
                "a1", "BG Music", ClipType.AUDIO, 28_000L,
                color = Color(0xFF00AA66),
                waveformData = List(80) { (0.2f + Math.random().toFloat() * 0.8f) }
            )
        )
    ),
    TimelineTrack(
        id = "voiceover_1", name = "Voiceover", type = TrackType.AUDIO,  // ← id changed from audio_2
        color = Color(0xFFFF6B00),                                         // ← color changed to orange
        clips = listOf(
            MediaClip(
                "a2", "Narration", ClipType.AUDIO, 12_000L, 3_000L,
                color = Color(0xFFAA5500),
                waveformData = List(40) { (0.1f + Math.random().toFloat() * 0.9f) }
            )
        )
    ),
    TimelineTrack(
        id = "fx_1", name = "FX", type = TrackType.EFFECTS,
        color = Color(0xFF9900DD),
        clips = listOf(
            MediaClip("e1", "Fade In", ClipType.IMAGE, 2_000L, color = Color(0xFF9900DD)),
            MediaClip("e2", "Zoom", ClipType.IMAGE, 3_000L, 14_000L, color = Color(0xFF7700BB))
        )
    )
)

fun defaultEffects(): List<EditEffect> = listOf(
    EditEffect("f1", "Vintage", EffectCategory.FILTER, "🎞"),
    EditEffect("f2", "Noir", EffectCategory.FILTER, "⬛"),
    EditEffect("f3", "Vivid", EffectCategory.FILTER, "🌈"),
    EditEffect("f4", "Cool", EffectCategory.FILTER, "❄️"),
    EditEffect("t1", "Fade", EffectCategory.TRANSITION, "↔️"),
    EditEffect("t2", "Wipe", EffectCategory.TRANSITION, "➡️"),
    EditEffect("t3", "Zoom", EffectCategory.TRANSITION, "🔍"),
    EditEffect("t4", "Spin", EffectCategory.TRANSITION, "🔄"),
    EditEffect("a1", "Reverb", EffectCategory.AUDIO_FX, "🔊"),
    EditEffect("a2", "Echo", EffectCategory.AUDIO_FX, "📡"),
    EditEffect("a3", "Pitch Up", EffectCategory.AUDIO_FX, "⬆️"),
    EditEffect("a4", "Bass Boost", EffectCategory.AUDIO_FX, "🎵"),
    EditEffect("s1", "Slow Mo", EffectCategory.SPEED, "🐌"),
    EditEffect("s2", "Fast Fwd", EffectCategory.SPEED, "⚡"),
    EditEffect("c1", "Brightness", EffectCategory.COLOR, "☀️"),
    EditEffect("c2", "Contrast", EffectCategory.COLOR, "◑")
)