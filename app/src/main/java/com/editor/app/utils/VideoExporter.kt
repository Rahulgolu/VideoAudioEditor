package com.editor.app.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.opengl.Matrix
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import com.editor.app.data.ClipType
import com.editor.app.data.EditorUiState
import com.editor.app.data.TransitionType
import com.editor.app.data.VideoFormat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NoVideoClipsException : Exception("No video clips with real URIs found to export")

@OptIn(UnstableApi::class)
object VideoExporter {

    data class VideoExportResult(
        val contentUri: Uri,
        val filePath: String,
        val durationMs: Long
    )

    @OptIn(UnstableApi::class)
    suspend fun export(
        context: Context,
        state: EditorUiState,
        onProgress: suspend (Float) -> Unit
    ): VideoExportResult = withContext(Dispatchers.IO) {

        // ── 1. Collect clips ─────────────────────────────
        val allClips = state.tracks.flatMap { it.clips }

        val videoClips = allClips
            .filter { it.type == ClipType.VIDEO && !it.uri.isNullOrBlank() }
            .sortedBy { it.startTimeMs }

        val audioClips = allClips
            .filter { it.type == ClipType.AUDIO && !it.uri.isNullOrBlank() }
            .sortedBy { it.startTimeMs }

        if (videoClips.isEmpty()) throw NoVideoClipsException()

        // ── 2. File setup ────────────────────────────────
        val ext = state.exportConfig.format.ext
        val mimeType = mimeTypeFor(state.exportConfig.format)

        val fileName = "VibeEdit_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        }.$ext"

        val tempFile = File(context.cacheDir, "export_tmp_$fileName").apply {
            if (exists()) delete()
        }

        try {

            // ── 3. Build VIDEO sequence ───────────────────
            val videoItems = videoClips.map { clip ->

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(clip.uri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.trimStartMs)
                            .setEndPositionMs(
                                (clip.durationMs - clip.trimEndMs)
                                    .coerceAtLeast(clip.trimStartMs + 100L)
                            )
                            .build()
                    )
                    .build()

                EditedMediaItem.Builder(mediaItem)
                    .setRemoveAudio(clip.isMuted) // respect mute
                    .setRemoveVideo(false)
                    .setEffects(Effects(emptyList(), emptyList()))
                    .build()
            }

            val videoSequence = EditedMediaItemSequence(videoItems)

            // ── 4. Build AUDIO sequences ──────────────────
            val audioSequences = audioClips.map { clip ->

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(clip.uri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.trimStartMs)
                            .setEndPositionMs(
                                (clip.durationMs - clip.trimEndMs)
                            )
                            .build()
                    )
                    .build()

                val editedAudio = EditedMediaItem.Builder(mediaItem)
                    .setRemoveVideo(true)
                    .setRemoveAudio(false)
                    .build()

                EditedMediaItemSequence(listOf(editedAudio))
            }

            // ── 5. Combine sequences ──────────────────────
            val sequences = mutableListOf<EditedMediaItemSequence>()
            sequences.add(videoSequence)
            sequences.addAll(audioSequences)

            val composition = Composition.Builder(sequences).build()

            // ── 6. Codec mapping ──────────────────────────
            val videoCodec = when (state.exportConfig.format) {
                VideoFormat.WEBM -> MimeTypes.VIDEO_VP9
                else -> MimeTypes.VIDEO_H264
            }

            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(5_000_000)
                        .build()
                )
                .build()

            val ioScope = CoroutineScope(Dispatchers.Main)
            var progressJob: Job? = null

            // ── 7. Run Transformer ────────────────────────
            val durationMs = withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Long> { cont ->

                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(videoCodec)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .setEncoderFactory(encoderFactory)
                        .addListener(object : Transformer.Listener {

                            override fun onCompleted(
                                composition: Composition,
                                result: ExportResult
                            ) {
                                progressJob?.cancel()
                                if (cont.isActive) cont.resume(result.durationMs)
                            }

                            override fun onError(
                                composition: Composition,
                                result: ExportResult,
                                exception: ExportException
                            ) {
                                progressJob?.cancel()
                                if (cont.isActive) cont.resumeWithException(exception)
                            }
                        })
                        .build()

                    transformer.start(composition, tempFile.absolutePath)

                    cont.invokeOnCancellation {
                        progressJob?.cancel()
                        try { transformer.cancel() } catch (_: Exception) {}
                    }

                    val holder = ProgressHolder()

                    progressJob = ioScope.launch {
                        while (cont.isActive) {
                            if (transformer.getProgress(holder) ==
                                Transformer.PROGRESS_STATE_AVAILABLE
                            ) {
                                onProgress(holder.progress / 100f)
                            }
                            delay(150)
                        }
                    }
                }
            }

            // ── 8. Validate ───────────────────────────────
            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw RuntimeException("Export failed: empty file")
            }

            onProgress(0.98f)

            // ── 9. Save ───────────────────────────────────
            val (uri, path) =
                saveToPublicMovies(context, tempFile, fileName, mimeType)

            VideoExportResult(uri, path, durationMs)

        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }


    // ── Save to gallery ──────────────────────────────────────
    private suspend fun saveToPublicMovies(
        context: Context,
        source: File,
        fileName: String,
        mimeType: String
    ): Pair<Uri, String> = withContext(Dispatchers.IO) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val resolver = context.contentResolver

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                put(MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/VibeEdit")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val contentUri = resolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw RuntimeException("MediaStore insert failed")

            resolver.openOutputStream(contentUri)?.use { out ->
                FileInputStream(source).use { it.copyTo(out) }
            }

            val done = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(contentUri, done, null, null)

            Pair(contentUri, "Movies/VibeEdit/$fileName")

        } else {

            val moviesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "VibeEdit"
            ).apply { mkdirs() }

            val dest = File(moviesDir, fileName)

            FileInputStream(source).use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }

            var scannedUri: Uri? = null
            val latch = java.util.concurrent.CountDownLatch(1)

            MediaScannerConnection.scanFile(
                context,
                arrayOf(dest.absolutePath),
                arrayOf(mimeType)
            ) { _, uri ->
                scannedUri = uri
                latch.countDown()
            }

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)

            Pair(scannedUri ?: Uri.fromFile(dest), dest.absolutePath)
        }
    }

    private fun mimeTypeFor(format: VideoFormat) = when (format) {
        VideoFormat.WEBM -> MimeTypes.VIDEO_WEBM
        else             -> MimeTypes.VIDEO_MP4
    }
}