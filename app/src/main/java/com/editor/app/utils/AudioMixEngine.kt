package com.editor.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.MediaPlayer
import androidx.compose.runtime.mutableStateListOf

class AudioMixEngine(private val context: Context) {

    private val players = mutableMapOf<String, MediaPlayer>()
    private val equalizers = mutableMapOf<String, Equalizer>()
    private val reverbs = mutableMapOf<String, PresetReverb>()
    private var masterVolume = 1f

    fun loadTrack(trackId: String, uriString: String, volume: Float, pan: Float, isMuted: Boolean) {
        players[trackId]?.release()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse(uriString))
                prepare()
                isLooping = false
                setVolume(
                    if (isMuted) 0f else (volume * masterVolume * (1f - pan.coerceAtLeast(0f))),
                    if (isMuted) 0f else (volume * masterVolume * (1f + pan.coerceAtMost(0f)))
                )
            }
            players[trackId] = player
        } catch (e: Exception) {
        }
    }

    fun playAll(positionMs: Long) {
        players.values.forEach { player ->
            try {
                player.seekTo(positionMs.toInt())
                player.start()
            } catch (e: Exception) { }
        }
    }

    fun pauseAll() {
        players.values.forEach { player ->
            try { if (player.isPlaying) player.pause() } catch (e: Exception) { }
        }
    }

    fun setTrackVolume(trackId: String, volume: Float, pan: Float, isMuted: Boolean) {
        players[trackId]?.setVolume(
            if (isMuted) 0f else (volume * masterVolume * (1f - pan.coerceAtLeast(0f))),
            if (isMuted) 0f else (volume * masterVolume * (1f + pan.coerceAtMost(0f)))
        )
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume
        players.forEach { (_, player) ->
            try { player.setVolume(volume, volume) } catch (e: Exception) { }
        }
    }

    fun setEqBands(trackId: String, bands: List<Float>) {

        val player = players[trackId] ?: return

        try {

            var eq = equalizers[trackId]

            if (eq == null) {
                eq = Equalizer(0, player.audioSessionId)
                eq.enabled = true
                equalizers[trackId] = eq
            }

            val numBands = eq.numberOfBands.toInt().coerceAtMost(bands.size)

            val minLevel = eq.bandLevelRange[0].toInt()
            val maxLevel = eq.bandLevelRange[1].toInt()

            for (i in 0 until numBands) {

                val level = (
                        bands[i] / 12f * (maxLevel - minLevel) + minLevel
                        ).toInt()

                eq.setBandLevel(
                    i.toShort(),
                    level.toShort()
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setFx(trackId: String, fxType: String, intensity: Float) {
        val player = players[trackId] ?: return
        try {
            if (fxType == "Reverb") {
                var reverb = reverbs[trackId]
                if (reverb == null) {
                    reverb = PresetReverb(0, player.audioSessionId)
                    reverb.enabled = true
                    reverbs[trackId] = reverb
                }
                val preset = when {
                    intensity < 0.3f -> PresetReverb.PRESET_SMALLROOM
                    intensity < 0.6f -> PresetReverb.PRESET_MEDIUMROOM
                    intensity < 0.8f -> PresetReverb.PRESET_LARGEHALL
                    else -> PresetReverb.PRESET_LARGEROOM
                }
                reverb.preset = preset
            } else {
                reverbs[trackId]?.enabled = false
            }
        } catch (e: Exception) {
        }
    }

    fun releaseTrack(trackId: String) {
        players[trackId]?.release()
        players.remove(trackId)
        equalizers[trackId]?.release()
        equalizers.remove(trackId)
        reverbs[trackId]?.release()
        reverbs.remove(trackId)
    }

    fun releaseAll() {
        players.values.forEach { try { it.release() } catch (e: Exception) { } }
        players.clear()
        equalizers.values.forEach { try { it.release() } catch (e: Exception) { } }
        equalizers.clear()
        reverbs.values.forEach { try { it.release() } catch (e: Exception) { } }
        reverbs.clear()
    }
}