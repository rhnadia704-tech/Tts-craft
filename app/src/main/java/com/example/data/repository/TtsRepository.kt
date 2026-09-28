package com.example.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.audio.AudioPlayerManager
import com.example.data.local.AudioRecordDao
import com.example.data.model.AudioRecordEntity
import com.example.data.model.Voice
import com.example.data.model.VoiceCatalog
import com.example.data.remote.AndroidTtsFallback
import com.example.data.remote.EdgeTtsService
import com.example.data.remote.GeminiAiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class TtsRepository(
    private val context: Context,
    private val audioRecordDao: AudioRecordDao,
    private val edgeTtsService: EdgeTtsService,
    private val androidTtsFallback: AndroidTtsFallback,
    private val geminiAiService: GeminiAiService,
    val playerManager: AudioPlayerManager
) {
    val allRecords: Flow<List<AudioRecordEntity>> = audioRecordDao.getAllRecords()
    val originalRecords: Flow<List<AudioRecordEntity>> = audioRecordDao.getOriginalRecords()
    val aiEnhancedRecords: Flow<List<AudioRecordEntity>> = audioRecordDao.getAiEnhancedRecords()

    suspend fun synthesizeSpeech(
        text: String,
        voice: Voice,
        speed: Float = 1.0f,
        pitch: Float = 0.0f,
        volume: Float = 1.0f
    ): Result<AudioRecordEntity> = withContext(Dispatchers.IO) {
        // Try Edge TTS first
        val edgeResult = edgeTtsService.synthesize(
            text = text,
            voice = voice,
            rateFactor = speed,
            pitchHz = pitch,
            volumeFactor = volume
        )

        val finalResult = if (edgeResult.isSuccess) {
            edgeResult.getOrThrow()
        } else {
            // Fallback to Native Android TTS
            val fallbackResult = androidTtsFallback.synthesizeToFile(
                text = text,
                voice = voice,
                rateFactor = speed,
                pitchHz = pitch
            )
            if (fallbackResult.isSuccess) {
                fallbackResult.getOrThrow()
            } else {
                return@withContext Result.failure(edgeResult.exceptionOrNull() ?: Exception("Synthèse échouée"))
            }
        }

        val title = if (text.length > 35) text.take(35).trim() + "…" else text.trim()

        val record = AudioRecordEntity(
            title = title,
            text = text,
            voiceId = voice.id,
            voiceName = "${voice.flagEmoji} ${voice.name}",
            language = voice.languageDisplayName,
            speed = speed,
            pitch = pitch,
            volume = volume,
            filePath = finalResult.file.absolutePath,
            durationMs = finalResult.durationMs,
            fileSizeBytes = finalResult.file.length(),
            timestamp = System.currentTimeMillis(),
            isAiEnhanced = false
        )

        val id = audioRecordDao.insertRecord(record)
        Result.success(record.copy(id = id))
    }

    suspend fun enhanceExistingAudioWithAi(
        sourceAudio: AudioRecordEntity,
        userPrompt: String
    ): Result<AudioRecordEntity> = withContext(Dispatchers.IO) {
        val voice = VoiceCatalog.findById(sourceAudio.voiceId)

        // 1. Get AI prosodic and acoustic enhancements from Gemini
        val enhancement = geminiAiService.naturalizeAudioSpeech(
            originalText = sourceAudio.text,
            voiceLocale = voice.locale,
            userImprovementPrompt = userPrompt
        )

        // 2. Synthesize using the enhanced SSML fragment and optimized pitch/speed
        val finalPitch = if (sourceAudio.pitch != 0.0f) sourceAudio.pitch else enhancement.suggestedPitch
        val finalSpeed = if (sourceAudio.speed != 1.0f) sourceAudio.speed else enhancement.suggestedSpeed

        val edgeResult = edgeTtsService.synthesize(
            text = enhancement.enhancedText,
            voice = voice,
            rateFactor = finalSpeed,
            pitchHz = finalPitch,
            volumeFactor = sourceAudio.volume,
            customSsmlContent = enhancement.ssmlFragment
        )

        val finalSynth = if (edgeResult.isSuccess) {
            edgeResult.getOrThrow()
        } else {
            // Fallback to native engine with punctuated enhanced text
            val fallbackResult = androidTtsFallback.synthesizeToFile(
                text = enhancement.enhancedText,
                voice = voice,
                rateFactor = finalSpeed,
                pitchHz = finalPitch
            )
            if (fallbackResult.isSuccess) {
                fallbackResult.getOrThrow()
            } else {
                return@withContext Result.failure(edgeResult.exceptionOrNull() ?: Exception("Échec de l'amélioration"))
            }
        }

        val improvementsSummary = enhancement.improvements.joinToString(" • ")

        val enhancedRecord = AudioRecordEntity(
            title = "✨ IA: " + sourceAudio.title,
            text = enhancement.enhancedText,
            voiceId = sourceAudio.voiceId,
            voiceName = sourceAudio.voiceName,
            language = sourceAudio.language,
            speed = finalSpeed,
            pitch = finalPitch,
            volume = sourceAudio.volume,
            filePath = finalSynth.file.absolutePath,
            durationMs = finalSynth.durationMs,
            fileSizeBytes = finalSynth.file.length(),
            timestamp = System.currentTimeMillis(),
            isAiEnhanced = true,
            originalRecordId = sourceAudio.id,
            aiPrompt = userPrompt,
            aiModificationsSummary = improvementsSummary
        )

        val id = audioRecordDao.insertRecord(enhancedRecord)
        Result.success(enhancedRecord.copy(id = id))
    }

    suspend fun getRecordById(id: Long): AudioRecordEntity? = audioRecordDao.getRecordById(id)

    suspend fun synthesizeDialogue(
        dialogueTitle: String,
        participants: List<com.example.data.model.DialogueParticipant>,
        lines: List<com.example.data.model.DialogueLineItem>
    ): Result<AudioRecordEntity> = withContext(Dispatchers.IO) {
        val turns = lines.mapNotNull { line ->
            val participant = participants.find { it.id == line.participantId }
            if (participant != null && line.text.isNotBlank()) {
                EdgeTtsService.DialogueTurnData(
                    voiceId = participant.voice.id,
                    text = line.text,
                    speed = participant.speed,
                    pitch = participant.pitch,
                    pauseAfterMs = line.pauseAfterMs
                )
            } else null
        }

        if (turns.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Aucune réplique valide dans le dialogue"))
        }

        val defaultLocale = participants.firstOrNull()?.voice?.locale ?: "fr-FR"
        val synthResult = edgeTtsService.synthesizeDialogue(turns, defaultLocale)

        val finalResult = if (synthResult.isSuccess) {
            synthResult.getOrThrow()
        } else {
            return@withContext Result.failure(synthResult.exceptionOrNull() ?: Exception("Synthèse dialogue échouée"))
        }

        val fullScript = lines.joinToString("\n") { line ->
            val speaker = participants.find { it.id == line.participantId }?.name ?: "Inconnu"
            "$speaker : ${line.text}"
        }

        val participantsNames = participants.joinToString(", ") { "${it.name} (${it.voice.name})" }

        val record = AudioRecordEntity(
            title = if (dialogueTitle.isNotBlank()) dialogueTitle else "Dialogue (${participants.size} participants)",
            text = fullScript,
            voiceId = participants.firstOrNull()?.voice?.id ?: "multi",
            voiceName = "👥 ${participants.size} Personnages",
            language = defaultLocale,
            speed = 1.0f,
            pitch = 0.0f,
            volume = 1.0f,
            filePath = finalResult.file.absolutePath,
            durationMs = finalResult.durationMs,
            fileSizeBytes = finalResult.file.length(),
            timestamp = System.currentTimeMillis(),
            isAiEnhanced = false,
            isDialogue = true,
            participantsSummary = participantsNames
        )

        val id = audioRecordDao.insertRecord(record)
        Result.success(record.copy(id = id))
    }

    suspend fun generateDialogueScript(
        prompt: String,
        participants: List<com.example.data.model.DialogueParticipant>
    ): List<com.example.data.model.DialogueLineItem> {
        return geminiAiService.generateDialogueScript(prompt, participants)
    }

    suspend fun deleteRecord(record: AudioRecordEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(record.filePath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
        audioRecordDao.deleteRecord(record)
    }

    suspend fun toggleFavorite(record: AudioRecordEntity) = withContext(Dispatchers.IO) {
        audioRecordDao.updateFavorite(record.id, !record.isFavorite)
    }

    fun shareAudio(record: AudioRecordEntity) {
        try {
            val file = File(record.filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, record.title)
                putExtra(Intent.EXTRA_TEXT, "Audio généré avec EdgeTTS Pro: ${record.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Partager l'audio").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
