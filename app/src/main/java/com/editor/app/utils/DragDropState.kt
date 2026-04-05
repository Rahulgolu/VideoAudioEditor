package com.editor.app.utils

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

// Holds the current drag session state
@Stable
class DragDropState {
    // Which clip is being dragged
    var draggingClipId by mutableStateOf<String?>(null)
    var draggingTrackId by mutableStateOf<String?>(null)

    // Current drag position on screen
    var dragOffset by mutableStateOf(Offset.Zero)

    // Where the drag started (to compute delta)
    var dragStartOffset by mutableStateOf(Offset.Zero)

    // Original start time of clip before drag
    var originalStartMs by mutableStateOf(0L)

    // Computed new start time as user drags
    var pendingStartMs by mutableStateOf(0L)

    // Target track being hovered over
    var hoveredTrackId by mutableStateOf<String?>(null)

    val isDragging get() = draggingClipId != null

    fun startDrag(
        clipId: String,
        trackId: String,
        startOffset: Offset,
        originalStartMs: Long
    ) {
        draggingClipId = clipId
        draggingTrackId = trackId
        dragStartOffset = startOffset
        dragOffset = startOffset
        this.originalStartMs = originalStartMs
        pendingStartMs = originalStartMs
    }

    fun updateDrag(offset: Offset, pxPerMs: Float) {
        dragOffset = offset
        val deltaX = offset.x - dragStartOffset.x
        val deltaMs = (deltaX / pxPerMs).toLong()
        pendingStartMs = (originalStartMs + deltaMs).coerceAtLeast(0L)
    }

    fun endDrag() {
        draggingClipId = null
        draggingTrackId = null
        hoveredTrackId = null
        dragOffset = Offset.Zero
        dragStartOffset = Offset.Zero
    }
}

@Composable
fun rememberDragDropState() = remember { DragDropState() }