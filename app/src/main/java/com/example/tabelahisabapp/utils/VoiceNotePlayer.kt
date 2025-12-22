package com.example.tabelahisabapp.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File

class VoiceNotePlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var currentlyPlaying = false

    fun play(file: File, onCompletion: (() -> Unit)? = null) {
        stopPlayback() // Stop any currently playing audio
        try {
            player = MediaPlayer.create(context, Uri.fromFile(file))?.apply {
                setOnCompletionListener {
                    currentlyPlaying = false
                    onCompletion?.invoke()
                }
                start()
                currentlyPlaying = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentlyPlaying = false
        }
    }

    fun stop() {
        stopPlayback()
    }

    private fun stopPlayback() {
        try {
            player?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        player = null
        currentlyPlaying = false
    }

    fun isCurrentlyPlaying(): Boolean = currentlyPlaying
}
