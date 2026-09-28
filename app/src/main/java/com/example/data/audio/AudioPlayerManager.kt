package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val audioId: Long? = null,
    val audioPath: String? = null
)

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        currentPositionMs = player.currentPosition.toLong(),
                        durationMs = player.duration.toLong()
                    )
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    fun playAudio(file: File, audioId: Long? = null) {
        if (!file.exists()) return

        if (_playbackState.value.audioPath == file.absolutePath && mediaPlayer != null) {
            if (_playbackState.value.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                setOnPreparedListener { player ->
                    player.start()
                    _playbackState.value = PlaybackState(
                        isPlaying = true,
                        currentPositionMs = 0L,
                        durationMs = player.duration.toLong(),
                        audioId = audioId,
                        audioPath = file.absolutePath
                    )
                    handler.post(progressRunnable)
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = it.duration.toLong()
                    )
                    handler.removeCallbacks(progressRunnable)
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                handler.removeCallbacks(progressRunnable)
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                handler.post(progressRunnable)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        }
    }

    fun stop() {
        handler.removeCallbacks(progressRunnable)
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }

    fun release() {
        stop()
    }
}
