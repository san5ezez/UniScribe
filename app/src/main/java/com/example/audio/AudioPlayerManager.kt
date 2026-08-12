package com.example.audio

import android.content.Context
import android.media.MediaPlayer
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

class AudioPlayerManager(private val context: Context) {

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _currentSpeed = MutableStateFlow(1.0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()


    companion object {
        private const val TAG = "AudioPlayerManager"
    }

    fun playAudio(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "Audio file does not exist at $filePath")
            return
        }

        if (_currentFilePath.value == filePath && mediaPlayer != null) {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
            return
        }

        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = durationMs.value
                    progressJob?.cancel()
                }
            }

            mediaPlayer = player
            _currentFilePath.value = filePath
            _durationMs.value = player.duration
            _currentPositionMs.value = 0

            setPlaybackSpeed(_currentSpeed.value)

            player.start()
            _isPlaying.value = true
            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio file", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { player ->
            player.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _currentSpeed.value = speed
        mediaPlayer?.let { player ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting playback speed", e)
                }
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _currentFilePath.value = null
            _isPlaying.value = false
            _currentPositionMs.value = 0
            _durationMs.value = 0
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    } catch (e: Exception) {
                        // Player state invalid
                    }
                }
                delay(200)
            }
        }
    }
}
