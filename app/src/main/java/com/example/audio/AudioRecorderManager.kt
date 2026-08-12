package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var startTimeMillis: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val TAG = "AudioRecorderManager"
    }

    fun startRecording(): Boolean {
        if (_isRecording.value) return false

        try {
            val lecturesDir = File(context.filesDir, "lectures")
            if (!lecturesDir.exists()) {
                lecturesDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioFile = File(lecturesDir, "lecture_$timestamp.m4a")
            currentAudioFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            startTimeMillis = System.currentTimeMillis()
            _recordingDurationSeconds.value = 0

            startTimerAndAmplitudeTracker()
            Log.d(TAG, "Started recording audio to ${audioFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            stopRecordingInternal()
            return false
        }
    }

    fun stopRecording(): RecordingResult? {
        if (!_isRecording.value) return null

        val durationSec = _recordingDurationSeconds.value
        val file = currentAudioFile

        stopRecordingInternal()

        return if (file != null && file.exists() && file.length() > 0) {
            RecordingResult(file, durationSec)
        } else {
            null
        }
    }

    private fun stopRecordingInternal() {
        timerJob?.cancel()
        timerJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            _currentAmplitude.value = 0
        }
    }

    private fun startTimerAndAmplitudeTracker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _isRecording.value) {
                val elapsed = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
                _recordingDurationSeconds.value = elapsed

                mediaRecorder?.let { recorder ->
                    try {
                        val maxAmp = recorder.maxAmplitude
                        _currentAmplitude.value = maxAmp
                    } catch (e: Exception) {
                        _currentAmplitude.value = 0
                    }
                }
                delay(100)
            }
        }
    }

    data class RecordingResult(
        val audioFile: File,
        val durationSeconds: Int
    )
}
