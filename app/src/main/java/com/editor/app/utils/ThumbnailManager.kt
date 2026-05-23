package com.editor.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts a single frame from a video URI and saves it as a JPEG thumbnail.
 *
 * Thumbnails are cached in context.cacheDir/thumbnails/ keyed by project id,
 * so they survive between sessions but are cleaned up with cache clearing.
 */
object ThumbnailManager {

    private const val THUMB_WIDTH  = 320
    private const val THUMB_HEIGHT = 180
    private const val QUALITY      = 80

    /**
     * Returns a cached thumbnail file URI for [projectId], generating it from
     * [videoUri] if it doesn't already exist.
     *
     * Returns null if extraction fails (e.g. no video, protected content).
     */
    suspend fun getOrCreate(
        context   : Context,
        projectId : String,
        videoUri  : String?
    ): String? = withContext(Dispatchers.IO) {
        val thumbFile = thumbFile(context, projectId)

        // Return cached copy if fresh
        if (thumbFile.exists() && thumbFile.length() > 0) {
            return@withContext thumbFile.toURI().toString()
        }

        // Need a video URI to extract from
        if (videoUri == null) return@withContext null

        try {
            val bitmap = extractFrame(context, Uri.parse(videoUri))
                ?: return@withContext null

            val scaled = Bitmap.createScaledBitmap(bitmap, THUMB_WIDTH, THUMB_HEIGHT, true)
            bitmap.recycle()

            FileOutputStream(thumbFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
            scaled.recycle()

            thumbFile.toURI().toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Force-regenerates the thumbnail (call after a new video is imported).
     */
    suspend fun regenerate(
        context   : Context,
        projectId : String,
        videoUri  : String
    ): String? = withContext(Dispatchers.IO) {
        val thumbFile = thumbFile(context, projectId)
        if (thumbFile.exists()) thumbFile.delete()
        getOrCreate(context, projectId, videoUri)
    }

    /** Delete the cached thumbnail for a project (call on project delete). */
    fun delete(context: Context, projectId: String) {
        thumbFile(context, projectId).delete()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun thumbFile(context: Context, projectId: String): File {
        val dir = File(context.cacheDir, "thumbnails").also { it.mkdirs() }
        return File(dir, "${projectId}.jpg")
    }

    private fun extractFrame(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            // Extract a frame at 10% into the video for a more representative shot
            val durationUs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.let { it * 1000 }   // ms → µs
                ?: 0L
            val seekUs = (durationUs * 0.10).toLong().coerceAtLeast(0L)
            retriever.getFrameAtTime(seekUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }
}