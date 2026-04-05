package com.editor.app.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording = false
        private set

    fun startRecording(): File {
        val file = File(context.cacheDir, "voiceover_${System.currentTimeMillis()}.m4a")
        outputFile = file

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }

        recorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        isRecording = true
        return file
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply { stop(); release() }
            recorder = null
            isRecording = false
            outputFile
        } catch (e: Exception) {
            recorder = null
            isRecording = false
            null
        }
    }

    fun getAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) { 0 }
    }

    fun release() {
        recorder?.release()
        recorder = null
        isRecording = false
    }
}