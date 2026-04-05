package com.editor.app.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.*

data class MediaItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long = 0L,
    val type: MediaType,
    val sizeBytes: Long = 0L
)

enum class MediaType { VIDEO, AUDIO, IMAGE }

// Reads real duration from MediaStore for video/audio
fun getMediaDuration(context: Context, uri: Uri): Long {
    return try {
        val projection = arrayOf(MediaStore.Video.Media.DURATION)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
            } else 0L
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}

fun getMediaName(context: Context, uri: Uri): String {
    return try {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
            } else "Unknown"
        } ?: "Unknown"
    } catch (e: Exception) {
        uri.lastPathSegment ?: "Unknown"
    }
}

fun getMediaSize(context: Context, uri: Uri): Long {
    return try {
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
            } else 0L
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}