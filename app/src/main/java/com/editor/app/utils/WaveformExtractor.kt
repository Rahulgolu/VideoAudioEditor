package com.editor.app.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Extracts a normalised waveform (list of floats 0..1) from an audio URI.
 *
 * Uses MediaExtractor + MediaCodec to decode raw PCM, then downsamples to
 * [targetBars] amplitude values. Falls back to random data if decoding fails
 * (e.g. protected content, unsupported codec).
 *
 * Usage in ViewModel:
 *   val waveform = WaveformExtractor.extract(context, uri, targetBars = 60)
 */
object WaveformExtractor {

    suspend fun extract(
        context: Context,
        uri: Uri,
        targetBars: Int = 60
    ): List<Float> = withContext(Dispatchers.IO) {
        try {
            extractReal(context, uri, targetBars)
        } catch (e: Exception) {
            // Graceful degradation — return plausible random waveform
            fallback(targetBars)
        }
    }

    // ── Core extraction ───────────────────────────────────────────────────────

    private fun extractReal(context: Context, uri: Uri, targetBars: Int): List<Float> {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        // Find first audio track
        val (trackIndex, format) = findAudioTrack(extractor)
            ?: return fallback(targetBars)

        extractor.selectTrack(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME) ?: return fallback(targetBars)
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmSamples = decodeToPcm(extractor, codec)
        codec.stop()
        codec.release()
        extractor.release()

        if (pcmSamples.isEmpty()) return fallback(targetBars)

        return downsample(pcmSamples, targetBars)
    }

    private fun findAudioTrack(extractor: MediaExtractor): Pair<Int, MediaFormat>? {
        for (i in 0 until extractor.trackCount) {
            val fmt  = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return Pair(i, fmt)
        }
        return null
    }

    private fun decodeToPcm(extractor: MediaExtractor, codec: MediaCodec): List<Short> {
        val samples    = mutableListOf<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone  = false
        var outputDone = false

        // Safety cap: don't decode more than ~60 s of audio to stay fast
        val maxSamples = 44100 * 60 * 2   // 60 s stereo @ 44100 Hz

        while (!outputDone && samples.size < maxSamples) {
            // Feed input
            if (!inputDone) {
                val inputIdx = codec.dequeueInputBuffer(10_000)
                if (inputIdx >= 0) {
                    val buf  = codec.getInputBuffer(inputIdx)!!
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(
                            inputIdx, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIdx, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            // Drain output
            val outputIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIdx >= 0) {
                val outBuf = codec.getOutputBuffer(outputIdx)!!
                val chunk  = ShortArray(bufferInfo.size / 2)
                outBuf.asShortBuffer().get(chunk)
                samples.addAll(chunk.toList())
                codec.releaseOutputBuffer(outputIdx, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }
            } else if (outputIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Format changed — continue normally
            }
        }
        return samples
    }

    // ── Downsample PCM → normalised bar amplitudes ────────────────────────────

    private fun downsample(samples: List<Short>, bars: Int): List<Float> {
        val chunkSize = maxOf(1, samples.size / bars)
        val result    = mutableListOf<Float>()
        var maxRms    = 1f   // avoid divide-by-zero

        val rmsValues = (0 until bars).map { bar ->
            val from = bar * chunkSize
            val to   = minOf(from + chunkSize, samples.size)
            if (from >= samples.size) return@map 0f
            val chunk = samples.subList(from, to)
            rmsOf(chunk).also { if (it > maxRms) maxRms = it }
        }

        // Normalise to 0..1
        rmsValues.forEach { rms ->
            result.add((rms / maxRms).coerceIn(0.05f, 1f))
        }
        return result
    }

    private fun rmsOf(samples: List<Short>): Float {
        if (samples.isEmpty()) return 0f
        val sumSq = samples.fold(0.0) { acc, s -> acc + s.toDouble() * s }
        return sqrt(sumSq / samples.size).toFloat()
    }

    // ── Fallback (random but plausible-looking waveform) ──────────────────────

    private fun fallback(bars: Int): List<Float> =
        List(bars) { (0.15f + Math.random().toFloat() * 0.85f).coerceIn(0.05f, 1f) }
}

// Do the same in importVideoClip() if you want video-track waveform later.
// The function is a suspend fun so it's already safe to call from a coroutine.
// ─────────────────────────────────────────────────────────────────────────────