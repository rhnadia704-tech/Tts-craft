package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice as TtsVoice
import android.util.Log
import com.example.data.model.Voice
import com.example.data.model.VoiceCatalog
import com.example.data.remote.AndroidTtsFallback
import com.example.data.remote.EdgeTtsService
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Locale

class EdgeTextToSpeechService : TextToSpeechService() {

    private lateinit var edgeTtsService: EdgeTtsService
    private lateinit var androidTtsFallback: AndroidTtsFallback
    private lateinit var prefs: SharedPreferences
    private var isStopped = false
    private var mediaPlayer: MediaPlayer? = null

    private var currentLang = "fra"
    private var currentCountry = "FRA"
    private var currentVariant = ""

    companion object {
        private const val TAG = "EdgeTtsServiceSys"
        const val PREFS_NAME = "tts_preferences"
        const val KEY_DEFAULT_VOICE = "system_default_voice_id"
    }

    override fun onCreate() {
        super.onCreate()
        edgeTtsService = EdgeTtsService(applicationContext)
        androidTtsFallback = AndroidTtsFallback(applicationContext)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getSavedVoice(): Voice {
        val savedId = prefs.getString(KEY_DEFAULT_VOICE, null)
        return if (!savedId.isNullOrBlank()) {
            VoiceCatalog.findById(savedId)
        } else {
            VoiceCatalog.getDefaultVoice()
        }
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        if (lang.isNullOrBlank()) return TextToSpeech.LANG_NOT_SUPPORTED

        val matchingVoices = VoiceCatalog.ALL_VOICES.filter {
            val loc = it.locale.lowercase(Locale.ROOT)
            loc.startsWith(lang.lowercase(Locale.ROOT))
        }

        if (matchingVoices.isEmpty()) {
            return TextToSpeech.LANG_NOT_SUPPORTED
        }

        if (!country.isNullOrBlank()) {
            val countryMatches = matchingVoices.any {
                it.locale.contains(country, ignoreCase = true)
            }
            return if (countryMatches) TextToSpeech.LANG_COUNTRY_AVAILABLE else TextToSpeech.LANG_AVAILABLE
        }

        return TextToSpeech.LANG_AVAILABLE
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf(currentLang, currentCountry, currentVariant)
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val status = onIsLanguageAvailable(lang, country, variant)
        if (status >= TextToSpeech.LANG_AVAILABLE) {
            currentLang = lang ?: "fra"
            currentCountry = country ?: "FRA"
            currentVariant = variant ?: ""
        }
        return status
    }

    override fun onGetVoices(): MutableList<TtsVoice> {
        return VoiceCatalog.ALL_VOICES.map { v ->
            val parts = v.locale.split("-")
            val locale = if (parts.size >= 2) Locale(parts[0], parts[1]) else Locale(parts[0])
            TtsVoice(
                v.id,
                locale,
                TtsVoice.QUALITY_VERY_HIGH,
                TtsVoice.LATENCY_NORMAL,
                true,
                setOf("neural", "microsoft_edge", "azure")
            )
        }.toMutableList()
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        if (voiceName == null) return TextToSpeech.ERROR
        return if (VoiceCatalog.ALL_VOICES.any { it.id == voiceName }) TextToSpeech.SUCCESS else TextToSpeech.ERROR
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? {
        val savedVoice = getSavedVoice()
        if (lang.isNullOrBlank()) {
            return savedVoice.id
        }
        val targetLocale = if (!country.isNullOrBlank()) {
            "${lang.lowercase()}-${country.lowercase()}"
        } else {
            lang.lowercase()
        }
        val match = VoiceCatalog.ALL_VOICES.firstOrNull {
            it.locale.lowercase().startsWith(targetLocale)
        }
        return (match ?: savedVoice).id
    }

    override fun onLoadVoice(voiceName: String?): Int {
        if (voiceName.isNullOrBlank()) return TextToSpeech.ERROR
        val voice = VoiceCatalog.ALL_VOICES.firstOrNull { it.id == voiceName }
        return if (voice != null) {
            val parts = voice.locale.split("-")
            currentLang = parts[0]
            currentCountry = if (parts.size > 1) parts[1] else ""
            TextToSpeech.SUCCESS
        } else {
            TextToSpeech.ERROR
        }
    }

    override fun onStop() {
        isStopped = true
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        isStopped = false

        val text = request.charSequenceText?.toString()?.trim() ?: ""
        if (text.isEmpty()) {
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return
        }

        val speechRate = (request.speechRate / 100.0f).coerceIn(0.5f, 2.5f)
        val pitchVal = ((request.pitch - 100) / 4.0f).coerceIn(-40.0f, 40.0f)

        val requestedVoiceName = try {
            request.voiceName
        } catch (_: Exception) {
            null
        }

        val targetVoice = if (!requestedVoiceName.isNullOrBlank()) {
            VoiceCatalog.ALL_VOICES.firstOrNull { it.id == requestedVoiceName }
        } else {
            val targetLang = request.language ?: currentLang
            val targetCountry = request.country ?: currentCountry
            VoiceCatalog.ALL_VOICES.firstOrNull {
                it.locale.startsWith("$targetLang-$targetCountry", ignoreCase = true)
            } ?: VoiceCatalog.ALL_VOICES.firstOrNull {
                it.locale.startsWith(targetLang, ignoreCase = true)
            }
        } ?: getSavedVoice()

        runBlocking {
            try {
                if (isStopped) {
                    callback.done()
                    return@runBlocking
                }

                var audioFile: File? = null
                val result = edgeTtsService.synthesize(
                    text = text,
                    voice = targetVoice,
                    rateFactor = speechRate,
                    pitchHz = pitchVal
                )

                if (result.isSuccess) {
                    audioFile = result.getOrThrow().file
                } else {
                    Log.w(TAG, "Edge TTS synthesis returned failure, attempting Android fallback...")
                    val fbResult = androidTtsFallback.synthesizeToFile(text, targetVoice)
                    if (fbResult.isSuccess) {
                        audioFile = fbResult.getOrThrow().file
                    }
                }

                if (audioFile != null && audioFile.exists() && !isStopped) {
                    decodeAudioToPcmAndStream(audioFile, callback)
                } else {
                    Log.e(TAG, "TTS synthesis failed for text: $text")
                    callback.error(TextToSpeech.ERROR_SYNTHESIS)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during onSynthesizeText: ${e.message}", e)
                callback.error(TextToSpeech.ERROR_SYNTHESIS)
            }
        }
    }

    private fun decodeAudioToPcmAndStream(file: File, callback: SynthesisCallback) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                playDirectWithMediaPlayer(file)
                callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
                callback.done()
                return
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mpeg"
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 24000

            val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT, channelCount)

            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false
            var emptyOutputCount = 0

            while (!isOutputEOS && !isStopped && emptyOutputCount < 30) {
                if (!isInputEOS) {
                    val inIndex = codec.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val inBuffer = codec.getInputBuffer(inIndex)
                        inBuffer?.clear()
                        val sampleSize = inBuffer?.let { extractor.readSampleData(it, 0) } ?: -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isInputEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outIndex >= 0) {
                    emptyOutputCount = 0
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0 && !isStopped) {
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val chunk = ByteArray(bufferInfo.size)
                        outBuffer.get(chunk)
                        callback.audioAvailable(chunk, 0, chunk.size)
                    }
                    codec.releaseOutputBuffer(outIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true
                        break
                    }
                } else {
                    if (isInputEOS) {
                        emptyOutputCount++
                    }
                }
            }

            callback.done()
        } catch (e: Exception) {
            Log.e(TAG, "PCM streaming notice: ${e.message}, falling back to MediaPlayer", e)
            playDirectWithMediaPlayer(file)
            callback.done()
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    private fun playDirectWithMediaPlayer(file: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer direct play error: ${e.message}")
        }
    }

    override fun onDestroy() {
        onStop()
        super.onDestroy()
    }
}
