package com.editor.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import androidx.compose.runtime.mutableStateListOf

class AudioMixEngine(private val context: Context) {

    private val players = mutableMapOf<String, MediaPlayer>()
    private var masterVolume = 1f

    // Load a track URI into a MediaPlayer keyed by trackId
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
            // URI may not be playable — safe to skip
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

    fun releaseTrack(trackId: String) {
        players[trackId]?.release()
        players.remove(trackId)
    }

    fun releaseAll() {
        players.values.forEach { try { it.release() } catch (e: Exception) { } }
        players.clear()
    }
}