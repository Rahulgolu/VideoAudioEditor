# 🎬 Vibe Edit — Video & Audio Editor (Jetpack Compose)

A professional-grade Android video and audio editing app built with Jetpack Compose,
Material 3, and ExoPlayer — following clean MVVM architecture with unidirectional data flow.

---

## ✨ Features Implemented

### 🔐 Permission Screen
- Smart permission handling based on Android version (API 26–34+)
- Per-permission cards showing granted/pending status in real time
- Auto-skips to Home if all permissions already granted
- Skip option for limited-feature usage
- Permissions: READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, READ_MEDIA_IMAGES,
  READ_EXTERNAL_STORAGE (legacy), RECORD_AUDIO

### 🏠 Home Screen
- Project library grid with thumbnail, duration badge, date, color tag
- Quick-start actions: New Video, New Audio, Import, Templates
- Live search bar filters projects by name with clear button
- Empty state placeholder when no projects exist

### 📥 Media Import (Gallery Picker)
- Bottom sheet with three separate import options
- **Video** — picks from gallery, adds to Video 1 track, loads into ExoPlayer
- **Audio** — picks MP3/WAV/AAC, adds to Music track with generated waveform
- **Image** — picks JPG/PNG, adds as 5-second clip, shown via Coil in preview
- Real duration read using `MediaMetadataRetriever`
- Real filename read from `MediaStore.MediaColumns.DISPLAY_NAME`
- Persistable URI permission granted for each picked file
- Clip placed automatically after last existing clip on track
- Project duration auto-expands when clip exceeds current end
- Undo snapshot saved on every import

### 🎬 Video Editor

#### Preview Panel
- 16:9 aspect ratio video surface via ExoPlayer `PlayerView`
- Image preview overlay via Coil `AsyncImage` for imported images
- Timecode overlay (current / total) bottom-left
- Resolution badge (1080p) top-right
- Idle play icon shown when no media playing

#### Playback Controls
- Scrubber slider synced with real ExoPlayer position (100ms tick)
- Play / Pause — controls real ExoPlayer or demo tick
- Skip to Start / End
- Rewind / Fast-forward (±5 seconds)
- Volume display chip (live %)
- Speed picker dropdown: 0.25×, 0.5×, 1×, 1.5×, 2× — highlighted when not 1×
- Speed chip turns cyan when non-default speed is active

#### Multi-Track Timeline
- Video, Audio, and Effects tracks with color-coded headers
- Track mute toggle (icon turns pink when muted)
- Track lock toggle (icon turns amber when locked)
- Zoom in / out toolbar (50% – 400%, shown as %)
- Timeline ruler with major (10s) and minor (5s) tick marks
- Playhead line (cyan) scrolls with current position
- Waveform visualization for audio clips
- Add Track row at bottom

#### Clip Operations
- **Tap** — selects clip, shows context menu
- **Long press + drag** — starts drag session
- **Drag left/right** — moves clip, cyan drop indicator shows pending position
- **Drag to different track** — track highlights with cyan border when hovered
- **Release** — clip moves to new position, undo snapshot saved
- **Split at playhead** — splits selected clip into two at current position
- **Delete** — removes clip from track
- Locked tracks block all interactions
- Dragging clip fades to 40% opacity
- Drag handle shown on right edge of selected clip
- Live position banner shown during drag ("Moving clip → 00:04.20")

### 🎵 Audio Mixer
- Per-channel volume slider (0–100%) with live % display
- Per-channel pan slider (L100 ← C → R100)
- VU meter per track (green → yellow → red zones), reacts live to volume
- LIVE / MUTED chip toggles mute per track
- EQ and FX chips (UI ready)
- Master Output strip with its own volume slider and VU meter
- All changes bound to ViewModel state (survive tab switches)

### ✨ Effects Panel
- Category tabs: FILTER, TRANSITION, AUDIO_FX, SPEED, COLOR, TEXT, STICKER
- Effect cards with emoji icons, toggle applied state
- Applied effect highlighted in purple
- Intensity slider for currently applied effect
- Blend and Fade sliders (UI ready)

### 📝 Text Overlay Panel
- Free text input with blank-check validation
- Style presets: Title, Caption, Body, Watermark, Quote
- Color picker: White, Cyan, Pink, Purple, Yellow, Orange
- Overlay added at current playhead position with 3s default duration
- Added overlays listed below with style label, timestamp, remove button

### 📤 Export Panel
- Resolution: 480p, 720p, 1080p, 4K
- Frame rate: 24, 30, 60 fps
- Quality: Low, Medium, High, Ultra
- Format: MP4, MOV, WebM
- Live export summary (resolution, fps, bitrate, format, estimated size)
- Animated linear progress bar + live percentage during export
- Export button shows spinner and disables during export
- Simulated export progress via coroutine (real export via MediaMuxer — roadmap)

### ↩️ Undo / Redo
- 20-step undo history stack
- Undo and Redo buttons in top bar grey out when unavailable
- Triggered by: clip delete, split, move, drag-drop, media import

---

## 🏗️ Architecture
```
UI (Composables)  →  calls ViewModel functions
        ↑
StateFlow<EditorUiState>  ←  EditorViewModel (single source of truth)
        ↑
PlayerManager (ExoPlayer wrapper)
```

- **Screens** never mutate state — only call ViewModel functions
- **EditorViewModel** owns all business logic, coroutines, ExoPlayer lifecycle
- **StateFlow** emits new `EditorUiState` on every change
- **ViewModel** survives tab switches and screen rotation
- **PlayerManager** wraps ExoPlayer, released in `onCleared()`
- **DragDropState** is a `@Stable` class held in Compose `remember`

---

## 🚀 Setup

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 26+ (minSdk), targetSdk 34

### Steps
1. Clone or download the project
2. Open in Android Studio
3. Click **Sync Now** when prompted
4. Run on emulator or physical device (API 26+)
5. Grant permissions on first launch (or tap Skip)

---

## 📦 Tech Stack

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2024.06.00 | Declarative UI |
| Material 3 | via BOM | Design system, components |
| ViewModel Compose | 2.8.3 | MVVM, StateFlow, coroutine scope |
| Kotlin Coroutines | via Kotlin | Playback tick, export simulation |
| Media3 ExoPlayer | 1.3.1 | Real video/audio playback |
| Media3 UI | 1.3.1 | PlayerView surface |
| Coil Compose | 2.6.0 | Image preview in editor |
| Accompanist Permissions | 0.34.0 | Runtime permission handling |
| Navigation Compose | 2.7.7 | Screen routing |
| AppCompat | 1.7.0 | Base theme support |
| Material Icons Extended | via BOM | Full icon set |

---

## 🎨 Design System

| Token | Hex | Usage |
|---|---|---|
| `DeepBlack` | `#0A0A0F` | App background |
| `SurfaceDark` | `#12121A` | Top bars, track headers |
| `CardDark` | `#1A1A26` | Cards, panels, bottom sheet |
| `CardDarker` | `#0F0F18` | Timeline background |
| `TimelineBar` | `#16161F` | Clip area background |
| `NeonCyan` | `#00F5FF` | Primary accent, playhead, active |
| `NeonPurple` | `#BF00FF` | Effects, applied state |
| `NeonPink` | `#FF006E` | Mute, delete actions |
| `WaveformGreen` | `#00FF88` | LIVE chip |
| `AccentYellow` | `#FFD600` | Lock state, VU warning |
| `AccentOrange` | `#FF6B00` | Pan slider |
| `TextPrimary` | `#E8E8F0` | Main text |
| `TextSecondary` | `#7A7A9A` | Labels, hints, icons |

---

## 📁 Project Structure
```
app/src/main/java/com/editor/app/
├── MainActivity.kt                  # Entry point, screen navigation
├── data/
│   └── Models.kt                    # EditorUiState, MediaClip, TimelineTrack,
│                                    # EditEffect, ExportConfig, TextOverlay,
│                                    # enums (ClipType, TrackType, EditorTab...)
├── viewmodel/
│   └── EditorViewModel.kt           # All business logic + StateFlow
│                                    # Playback, import, undo/redo,
│                                    # clip ops, effects, export
├── utils/
│   ├── PlayerManager.kt             # ExoPlayer wrapper (load, play, seek,
│   │                                # volume, speed, listener, release)
│   └── DragDropState.kt             # Drag session state (@Stable class)
└── ui/
    ├── theme/
    │   ├── Theme.kt                  # Color tokens + MaterialTheme setup
    │   └── Typography.kt            # EditorTypography type scale
    └── screens/
        ├── PermissionScreen.kt      # Runtime permission UI
        ├── HomeScreen.kt            # Project grid + quick actions
        ├── MediaPickerSheet.kt      # Import bottom sheet (video/audio/image)
        └── EditorScreen.kt          # Full editor — all 5 tabs + timeline
```

---

## 🔐 Permissions

| Permission | Android | Purpose |
|---|---|---|
| `READ_MEDIA_VIDEO` | API 33+ | Import video files |
| `READ_MEDIA_AUDIO` | API 33+ | Import audio files |
| `READ_MEDIA_IMAGES` | API 33+ | Import images |
| `READ_EXTERNAL_STORAGE` | API 26–32 | Import media (legacy) |
| `RECORD_AUDIO` | All | Voiceover recording (roadmap) |

---

## 🗺️ Roadmap

- [ ] Real export using MediaMuxer / FFmpeg
- [ ] Project save & load (Room database)
- [ ] Voiceover recording with AudioRecord
- [ ] Clip trim handles (drag edges to resize)
- [ ] Multi-select clips
- [ ] Transition animations between clips
- [ ] Real audio mixing with AudioTrack
- [ ] Cloud project backup

---

## 📝 License
MIT — free to use and modify.