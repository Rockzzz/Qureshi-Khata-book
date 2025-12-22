package com.example.tabelahisabapp.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFilePath: String? = null

    fun startRecording(): String? {
        return try {
            val voiceNotesDir = File(context.filesDir, "voice_notes")
            if (!voiceNotesDir.exists()) {
                voiceNotesDir.mkdirs()
            }

            val fileName = "voice_${System.currentTimeMillis()}.3gp"
            val file = File(voiceNotesDir, fileName)
            currentFilePath = file.absolutePath

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                setMaxDuration(60000) // 60 seconds max
                
                prepare()
                start()
            }

            currentFilePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val path = currentFilePath
            currentFilePath = null
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isRecording(): Boolean = recorder != null

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            
            // Delete the file if recording was cancelled
            currentFilePath?.let { path ->
                File(path).delete()
            }
            currentFilePath = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteVoiceNote(filePath: String?) {
        filePath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

