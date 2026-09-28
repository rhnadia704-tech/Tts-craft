package com.example.data.remote

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID

class AndroidTtsFallback(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val initDeferred = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                initDeferred.complete(true)
            } else {
                Log.w("AndroidTtsFallback", "TTS init failed with status: $status")
                initDeferred.complete(false)
            }
        }
    }

    suspend fun synthesizeToFile(
        text: String,
        voice: Voice,
        rateFactor: Float = 1.0f,
        pitchHz: Float = 0.0f
    ): Result<EdgeTtsService.SynthesisResult> = withContext(Dispatchers.IO) {
        val ready = withTimeoutOrNull(5000) { initDeferred.await() } ?: isInitialized
        if (!ready || tts == null) {
            return@withContext Result.failure(IllegalStateException("TextToSpeech not initialized"))
        }

        val engine = tts ?: return@withContext Result.failure(IllegalStateException("TTS null"))
        val locale = try {
            Locale.forLanguageTag(voice.locale)
        } catch (e: Exception) {
            Locale.getDefault()
        }

        try {
            engine.language = locale
            engine.setSpeechRate(rateFactor.coerceIn(0.5f, 2.0f))
            val pitchVal = 1.0f + (pitchHz / 50.0f)
            engine.setPitch(pitchVal.coerceIn(0.5f, 2.0f))
        } catch (e: Exception) {
            Log.w("AndroidTtsFallback", "Could not set TTS params: ${e.message}")
        }

        val audioDir = File(context.filesDir, "audio_outputs").apply { mkdirs() }
        val outputFile = File(audioDir, "native_tts_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.wav")
        val utteranceId = "utt_${System.currentTimeMillis()}"

        val doneDeferred = CompletableDeferred<Boolean>()

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    doneDeferred.complete(true)
                }
            }

            override fun onError(utteranceId: String?) {
                doneDeferred.complete(false)
            }
        })

        val params = Bundle()
        val res = engine.synthesizeToFile(text, params, outputFile, utteranceId)
        if (res != TextToSpeech.SUCCESS) {
            return@withContext Result.failure(IllegalStateException("synthesizeToFile returned $res"))
        }

        val completed = withTimeoutOrNull(20_000) { doneDeferred.await() } ?: false
        if (completed && outputFile.exists() && outputFile.length() > 0) {
            val durationMs = extractDurationMs(outputFile)
            Result.success(EdgeTtsService.SynthesisResult(file = outputFile, durationMs = durationMs, isFallback = true))
        } else {
            Result.failure(IllegalStateException("Native TTS synthesis timed out"))
        }
    }

    private fun extractDurationMs(file: File): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            mmr.release()
            durationStr?.toLongOrNull() ?: 3000L
        } catch (e: Exception) {
            3000L
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
