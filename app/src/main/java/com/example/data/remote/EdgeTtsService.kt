package com.example.data.remote

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.data.model.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class EdgeTtsService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "EdgeTtsService"
        private const val BASE_WSS_URL =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
    }

    data class SynthesisResult(
        val file: File,
        val durationMs: Long,
        val isFallback: Boolean = false
    )

    data class DialogueTurnData(
        val voiceId: String,
        val text: String,
        val speed: Float = 1.0f,
        val pitch: Float = 0.0f,
        val pauseAfterMs: Int = 300
    )

    suspend fun synthesize(
        text: String,
        voice: Voice,
        rateFactor: Float = 1.0f,
        pitchHz: Float = 0.0f,
        volumeFactor: Float = 1.0f,
        customSsmlContent: String? = null
    ): Result<SynthesisResult> = withContext(Dispatchers.IO) {
        val ratePercent = ((rateFactor - 1.0f) * 100).toInt()
        val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
        val pitchStr = if (pitchHz.toInt() >= 0) "+${pitchHz.toInt()}Hz" else "${pitchHz.toInt()}Hz"
        val volumePercent = (volumeFactor * 100).toInt().coerceIn(0, 100)
        val volumeStr = "$volumePercent%"

        val escapedText = text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        val ssmlBody = customSsmlContent ?: escapedText

        val ssml = """
            <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='${voice.locale}'>
                <voice name='${voice.id}'>
                    <prosody pitch='$pitchStr' rate='$rateStr' volume='$volumeStr'>
                        $ssmlBody
                    </prosody>
                </voice>
            </speak>
        """.trimIndent()

        // Attempt synthesis with 1 automatic retry on clock skew / 403
        var result = synthesizeSsml(ssml)
        if (result.isFailure) {
            Log.w(TAG, "First synthesis attempt failed. Syncing server clock skew and retrying...")
            syncClockSkew()
            result = synthesizeSsml(ssml)
        }
        result
    }

    suspend fun synthesizeDialogue(
        turns: List<DialogueTurnData>,
        defaultLocale: String = "fr-FR"
    ): Result<SynthesisResult> = withContext(Dispatchers.IO) {
        val turnsSsml = StringBuilder()
        turns.forEach { turn ->
            val escapedText = turn.text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")

            val ratePercent = ((turn.speed - 1.0f) * 100).toInt()
            val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
            val pitchStr = if (turn.pitch.toInt() >= 0) "+${turn.pitch.toInt()}Hz" else "${turn.pitch.toInt()}Hz"

            turnsSsml.append("""
                <voice name='${turn.voiceId}'>
                    <prosody pitch='$pitchStr' rate='$rateStr'>
                        $escapedText
                    </prosody>
                </voice>
                <break time='${turn.pauseAfterMs}ms'/>
            """.trimIndent()).append("\n")
        }

        val ssml = """
            <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$defaultLocale'>
                $turnsSsml
            </speak>
        """.trimIndent()

        var result = synthesizeSsml(ssml)
        if (result.isFailure) {
            Log.w(TAG, "First dialogue synthesis attempt failed. Syncing clock skew and retrying...")
            syncClockSkew()
            result = synthesizeSsml(ssml)
        }
        result
    }

    private suspend fun syncClockSkew() = withContext(Dispatchers.IO) {
        try {
            val listUrl = "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list?trustedclienttoken=${DRM.TRUSTED_CLIENT_TOKEN}&Sec-MS-GEC=${DRM.generateSecMsGec()}&Sec-MS-GEC-Version=${DRM.SEC_MS_GEC_VERSION}"
            val checkRequest = Request.Builder()
                .url(listUrl)
                .addHeader("User-Agent", DRM.CHROMIUM_USER_AGENT)
                .addHeader("Origin", DRM.CHROMIUM_ORIGIN)
                .get()
                .build()
            client.newCall(checkRequest).execute().use { response ->
                val serverDate = response.header("Date")
                if (!serverDate.isNullOrBlank()) {
                    DRM.adjustClockSkew(serverDate)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncClockSkew notice: ${e.message}")
        }
    }

    private suspend fun synthesizeSsml(ssml: String): Result<SynthesisResult> = withContext(Dispatchers.IO) {
        val audioDir = File(context.filesDir, "audio_outputs").apply { mkdirs() }
        val outputFile = File(audioDir, "edge_tts_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.mp3")

        val completionDeferred = CompletableDeferred<Boolean>()
        val audioBuffer = ByteArrayOutputStream()

        val requestId = UUID.randomUUID().toString().replace("-", "")
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val secMsGec = DRM.generateSecMsGec()
        val muid = DRM.generateMuid()

        val requestUrl = "$BASE_WSS_URL?TrustedClientToken=${DRM.TRUSTED_CLIENT_TOKEN}&Sec-MS-GEC=$secMsGec&Sec-MS-GEC-Version=${DRM.SEC_MS_GEC_VERSION}&ConnectionId=$connectionId"
        
        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("User-Agent", DRM.CHROMIUM_USER_AGENT)
            .addHeader("Origin", DRM.CHROMIUM_ORIGIN)
            .addHeader("Cookie", "muid=$muid;")
            .addHeader("Pragma", "no-cache")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                try {
                    val dateHeader = response.header("Date")
                    if (!dateHeader.isNullOrBlank()) {
                        DRM.adjustClockSkew(dateHeader)
                    }

                    val dateStr = getCurrentUtcDate()
                    // 1. Send speech config
                    val configMsg = "X-Timestamp:$dateStr\r\n" +
                            "Content-Type:application/json; charset=utf-8\r\n" +
                            "Path:speech.config\r\n\r\n" +
                            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                    webSocket.send(configMsg)

                    // 2. Send SSML message
                    val ssmlMsg = "X-RequestId:$requestId\r\n" +
                            "Content-Type:application/ssml+xml\r\n" +
                            "X-Timestamp:$dateStr\r\n" +
                            "Path:ssml\r\n\r\n" +
                            ssml
                    webSocket.send(ssmlMsg)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onOpen send: ${e.message}", e)
                    completionDeferred.complete(false)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                try {
                    val data = bytes.toByteArray()
                    if (data.size >= 2) {
                        val headerLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                        if (data.size > 2 + headerLen) {
                            val headerStr = String(data, 2, headerLen, Charsets.UTF_8)
                            if (headerStr.contains("Path:audio")) {
                                val audioPayloadOffset = 2 + headerLen
                                val audioLen = data.size - audioPayloadOffset
                                audioBuffer.write(data, audioPayloadOffset, audioLen)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing binary audio frame: ${e.message}", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                if (text.contains("Path:turn.end")) {
                    completionDeferred.complete(true)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                val serverDate = response?.header("Date")
                if (!serverDate.isNullOrBlank()) {
                    DRM.adjustClockSkew(serverDate)
                }
                Log.w(TAG, "WebSocket failure: ${t.message} (code: ${response?.code})")
                completionDeferred.complete(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                if (!completionDeferred.isCompleted) {
                    completionDeferred.complete(audioBuffer.size() > 0)
                }
            }
        }

        val ws = client.newWebSocket(request, listener)
        val success = withTimeoutOrNull(25_000) {
            completionDeferred.await()
        } ?: false

        try {
            ws.close(1000, "Done")
        } catch (_: Exception) {}

        if (success && audioBuffer.size() > 0) {
            try {
                FileOutputStream(outputFile).use { fos ->
                    fos.write(audioBuffer.toByteArray())
                    fos.flush()
                }
                val durationMs = extractDurationMs(outputFile)
                Result.success(SynthesisResult(file = outputFile, durationMs = durationMs, isFallback = false))
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(IllegalStateException("Edge TTS synthesis timed out or empty response"))
        }
    }

    private fun getCurrentUtcDate(): String {
        val sdf = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT+0000' '(Coordinated Universal Time)'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
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
}
