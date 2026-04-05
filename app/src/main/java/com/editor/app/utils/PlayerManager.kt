package com.editor.app.utils

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class PlayerManager(private val context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        playWhenReady = false
    }

    // ─── Load and play a URI ──────────────────────────────────────────────────

    fun loadUri(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    fun loadAndPlay(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun play() { player.play() }
    fun pause() { player.pause() }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.25f, 4f))
    }

    fun getCurrentPosition(): Long = player.currentPosition
    fun getDuration(): Long = player.duration.takeIf { it > 0 } ?: 0L
    fun isPlaying(): Boolean = player.isPlaying

    fun addListener(listener: Player.Listener) = player.addListener(listener)
    fun removeListener(listener: Player.Listener) = player.removeListener(listener)

    fun release() = player.release()
}