package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioPlayerManager
import com.example.data.audio.PlaybackState
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecordEntity
import com.example.data.model.DialogueLineItem
import com.example.data.model.DialogueParticipant
import com.example.data.model.DialoguePreset
import com.example.data.model.DialoguePresets
import com.example.data.model.Voice
import com.example.data.model.VoiceCatalog
import com.example.data.remote.AndroidTtsFallback
import com.example.data.remote.EdgeTtsService
import com.example.data.remote.GeminiAiService
import com.example.data.repository.TtsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class HistoryFilter {
    ALL, DIALOGUES, ORIGINALS, AI_ENHANCED, FAVORITES
}

data class SynthesisUiState(
    val text: String = "Bonjour et bienvenue sur EdgeTTS Pro ! Cette application transforme vos textes en voix neuronale ultra-réaliste.",
    val selectedVoice: Voice = VoiceCatalog.getDefaultVoice(),
    val speed: Float = 1.0f,
    val pitch: Float = 0.0f,
    val volume: Float = 1.0f,
    val isSynthesizing: Boolean = false,
    val lastSynthesizedRecord: AudioRecordEntity? = null,
    val errorMessage: String? = null
)

data class AiEnhancerUiState(
    val selectedSourceAudio: AudioRecordEntity? = null,
    val prompt: String = "",
    val isEnhancing: Boolean = false,
    val enhancedRecord: AudioRecordEntity? = null,
    val errorMessage: String? = null
)

data class DialogueUiState(
    val title: String = "Débat Tech & IA",
    val participants: List<DialogueParticipant> = DialoguePresets.getPresets().first().participants,
    val lines: List<DialogueLineItem> = DialoguePresets.getPresets().first().lines,
    val isSynthesizing: Boolean = false,
    val isGeneratingAiScript: Boolean = false,
    val aiScriptPrompt: String = "",
    val lastGeneratedDialogueRecord: AudioRecordEntity? = null,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TtsRepository

    private val _synthesisUiState = MutableStateFlow(SynthesisUiState())
    val synthesisUiState: StateFlow<SynthesisUiState> = _synthesisUiState.asStateFlow()

    private val _aiEnhancerUiState = MutableStateFlow(AiEnhancerUiState())
    val aiEnhancerUiState: StateFlow<AiEnhancerUiState> = _aiEnhancerUiState.asStateFlow()

    private val _dialogueUiState = MutableStateFlow(DialogueUiState())
    val dialogueUiState: StateFlow<DialogueUiState> = _dialogueUiState.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val playbackState: StateFlow<PlaybackState>

    val filteredRecords: StateFlow<List<AudioRecordEntity>>

    init {
        val database = AppDatabase.getInstance(application)
        val edgeTts = EdgeTtsService(application)
        val androidTts = AndroidTtsFallback(application)
        val geminiService = GeminiAiService()
        val playerManager = AudioPlayerManager(application)

        repository = TtsRepository(
            context = application,
            audioRecordDao = database.audioRecordDao(),
            edgeTtsService = edgeTts,
            androidTtsFallback = androidTts,
            geminiAiService = geminiService,
            playerManager = playerManager
        )

        playbackState = repository.playerManager.playbackState

        filteredRecords = combine(
            repository.allRecords,
            _historyFilter,
            _searchQuery
        ) { records, filter, query ->
            records.filter { record ->
                val matchesFilter = when (filter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.DIALOGUES -> record.isDialogue
                    HistoryFilter.ORIGINALS -> !record.isAiEnhanced && !record.isDialogue
                    HistoryFilter.AI_ENHANCED -> record.isAiEnhanced
                    HistoryFilter.FAVORITES -> record.isFavorite
                }
                val matchesQuery = query.isBlank() ||
                        record.title.contains(query, ignoreCase = true) ||
                        record.text.contains(query, ignoreCase = true) ||
                        record.voiceName.contains(query, ignoreCase = true)

                matchesFilter && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // --- Synthesis actions ---
    fun updateInputText(newText: String) {
        _synthesisUiState.value = _synthesisUiState.value.copy(text = newText, errorMessage = null)
    }

    fun selectVoice(voice: Voice) {
        _synthesisUiState.value = _synthesisUiState.value.copy(selectedVoice = voice)
    }

    fun setSpeed(speed: Float) {
        _synthesisUiState.value = _synthesisUiState.value.copy(speed = speed)
    }

    fun setPitch(pitch: Float) {
        _synthesisUiState.value = _synthesisUiState.value.copy(pitch = pitch)
    }

    fun setVolume(volume: Float) {
        _synthesisUiState.value = _synthesisUiState.value.copy(volume = volume)
    }

    fun generateSpeech() {
        val state = _synthesisUiState.value
        val text = state.text.trim()
        if (text.isEmpty()) {
            _synthesisUiState.value = state.copy(errorMessage = "Veuillez saisir du texte à synthétiser")
            return
        }

        _synthesisUiState.value = state.copy(isSynthesizing = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.synthesizeSpeech(
                text = text,
                voice = state.selectedVoice,
                speed = state.speed,
                pitch = state.pitch,
                volume = state.volume
            )

            result.fold(
                onSuccess = { record ->
                    _synthesisUiState.value = _synthesisUiState.value.copy(
                        isSynthesizing = false,
                        lastSynthesizedRecord = record,
                        errorMessage = null
                    )
                    repository.playerManager.playAudio(File(record.filePath), record.id)
                },
                onFailure = { error ->
                    _synthesisUiState.value = _synthesisUiState.value.copy(
                        isSynthesizing = false,
                        errorMessage = error.localizedMessage ?: "Erreur de génération audio"
                    )
                }
            )
        }
    }

    // --- Dialogue Studio actions ---
    fun updateDialogueTitle(title: String) {
        _dialogueUiState.value = _dialogueUiState.value.copy(title = title, errorMessage = null)
    }

    fun addParticipant(name: String, voice: Voice) {
        val current = _dialogueUiState.value.participants.toMutableList()
        val colors = listOf(0xFF00E5FF, 0xFFFFB300, 0xFFB388FF, 0xFF00E676, 0xFFFF5252, 0xFFFF80AB)
        val assignedColor = colors[current.size % colors.size]

        val newParticipant = DialogueParticipant(
            id = "part_${UUID.randomUUID().toString().take(6)}",
            name = name.ifBlank { "Personnage ${current.size + 1}" },
            voice = voice,
            colorHex = assignedColor
        )
        current.add(newParticipant)
        _dialogueUiState.value = _dialogueUiState.value.copy(participants = current)
    }

    fun updateParticipant(updated: DialogueParticipant) {
        val current = _dialogueUiState.value.participants.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            current[index] = updated
            _dialogueUiState.value = _dialogueUiState.value.copy(participants = current)
        }
    }

    fun removeParticipant(participantId: String) {
        val current = _dialogueUiState.value.participants.toMutableList()
        if (current.size <= 1) {
            _dialogueUiState.value = _dialogueUiState.value.copy(errorMessage = "Il faut au moins 1 participant dans le dialogue")
            return
        }
        current.removeAll { it.id == participantId }
        val remainingLines = _dialogueUiState.value.lines.filter { it.participantId != participantId }
        _dialogueUiState.value = _dialogueUiState.value.copy(participants = current, lines = remainingLines)
    }

    fun addDialogueLine(participantId: String, text: String = "", pauseAfterMs: Int = 350) {
        val current = _dialogueUiState.value.lines.toMutableList()
        current.add(
            DialogueLineItem(
                id = "line_${UUID.randomUUID().toString().take(6)}",
                participantId = participantId,
                text = text,
                pauseAfterMs = pauseAfterMs
            )
        )
        _dialogueUiState.value = _dialogueUiState.value.copy(lines = current)
    }

    fun updateDialogueLine(lineId: String, participantId: String, text: String, pauseAfterMs: Int) {
        val current = _dialogueUiState.value.lines.toMutableList()
        val index = current.indexOfFirst { it.id == lineId }
        if (index >= 0) {
            current[index] = DialogueLineItem(
                id = lineId,
                participantId = participantId,
                text = text,
                pauseAfterMs = pauseAfterMs
            )
            _dialogueUiState.value = _dialogueUiState.value.copy(lines = current)
        }
    }

    fun deleteDialogueLine(lineId: String) {
        val current = _dialogueUiState.value.lines.toMutableList()
        current.removeAll { it.id == lineId }
        _dialogueUiState.value = _dialogueUiState.value.copy(lines = current)
    }

    fun applyDialoguePreset(preset: DialoguePreset) {
        _dialogueUiState.value = _dialogueUiState.value.copy(
            title = preset.title,
            participants = preset.participants,
            lines = preset.lines,
            lastGeneratedDialogueRecord = null,
            errorMessage = null
        )
    }

    fun updateAiScriptPrompt(prompt: String) {
        _dialogueUiState.value = _dialogueUiState.value.copy(aiScriptPrompt = prompt)
    }

    fun generateDialogueScriptWithAi() {
        val state = _dialogueUiState.value
        val prompt = state.aiScriptPrompt.trim()
        if (prompt.isBlank()) {
            _dialogueUiState.value = state.copy(errorMessage = "Veuillez entrer une idée ou un thème de dialogue pour l'IA")
            return
        }

        _dialogueUiState.value = state.copy(isGeneratingAiScript = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val newLines = repository.generateDialogueScript(prompt, state.participants)
                if (newLines.isNotEmpty()) {
                    _dialogueUiState.value = _dialogueUiState.value.copy(
                        lines = newLines,
                        isGeneratingAiScript = false,
                        title = prompt.take(30).trim(),
                        errorMessage = null
                    )
                } else {
                    _dialogueUiState.value = _dialogueUiState.value.copy(
                        isGeneratingAiScript = false,
                        errorMessage = "Impossible de générer le script pour ce thème"
                    )
                }
            } catch (e: Exception) {
                _dialogueUiState.value = _dialogueUiState.value.copy(
                    isGeneratingAiScript = false,
                    errorMessage = e.localizedMessage ?: "Erreur génération IA"
                )
            }
        }
    }

    fun synthesizeDialogue() {
        val state = _dialogueUiState.value
        if (state.lines.none { it.text.isNotBlank() }) {
            _dialogueUiState.value = state.copy(errorMessage = "Ajoutez au moins une réplique avec du texte dans le dialogue")
            return
        }

        _dialogueUiState.value = state.copy(isSynthesizing = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.synthesizeDialogue(
                dialogueTitle = state.title,
                participants = state.participants,
                lines = state.lines
            )

            result.fold(
                onSuccess = { record ->
                    _dialogueUiState.value = _dialogueUiState.value.copy(
                        isSynthesizing = false,
                        lastGeneratedDialogueRecord = record,
                        errorMessage = null
                    )
                    repository.playerManager.playAudio(File(record.filePath), record.id)
                },
                onFailure = { error ->
                    _dialogueUiState.value = _dialogueUiState.value.copy(
                        isSynthesizing = false,
                        errorMessage = error.localizedMessage ?: "Erreur synthèse dialogue"
                    )
                }
            )
        }
    }

    // --- AI Audio Enhancer actions ---
    fun selectAudioForEnhancement(audio: AudioRecordEntity) {
        _aiEnhancerUiState.value = _aiEnhancerUiState.value.copy(
            selectedSourceAudio = audio,
            enhancedRecord = null,
            errorMessage = null
        )
    }

    fun updateEnhancementPrompt(prompt: String) {
        _aiEnhancerUiState.value = _aiEnhancerUiState.value.copy(prompt = prompt, errorMessage = null)
    }

    fun applyEnhancementPreset(presetTitle: String, presetPrompt: String) {
        _aiEnhancerUiState.value = _aiEnhancerUiState.value.copy(prompt = presetPrompt)
    }

    fun runAiEnhancement() {
        val state = _aiEnhancerUiState.value
        val source = state.selectedSourceAudio
        if (source == null) {
            _aiEnhancerUiState.value = state.copy(errorMessage = "Veuillez sélectionner un audio à améliorer")
            return
        }

        val prompt = state.prompt.trim()
        if (prompt.isEmpty()) {
            _aiEnhancerUiState.value = state.copy(errorMessage = "Veuillez décrire les améliorations souhaitées")
            return
        }

        _aiEnhancerUiState.value = state.copy(isEnhancing = true, errorMessage = null)

        viewModelScope.launch {
            val result = repository.enhanceExistingAudioWithAi(
                sourceAudio = source,
                userPrompt = prompt
            )

            result.fold(
                onSuccess = { enhanced ->
                    _aiEnhancerUiState.value = _aiEnhancerUiState.value.copy(
                        isEnhancing = false,
                        enhancedRecord = enhanced,
                        errorMessage = null
                    )
                    repository.playerManager.playAudio(File(enhanced.filePath), enhanced.id)
                },
                onFailure = { error ->
                    _aiEnhancerUiState.value = _aiEnhancerUiState.value.copy(
                        isEnhancing = false,
                        errorMessage = error.localizedMessage ?: "Erreur lors de l'amélioration IA"
                    )
                }
            )
        }
    }

    // --- Playback controls ---
    fun playRecord(record: AudioRecordEntity) {
        repository.playerManager.playAudio(File(record.filePath), record.id)
    }

    fun pausePlayback() {
        repository.playerManager.pause()
    }

    fun resumePlayback() {
        repository.playerManager.resume()
    }

    fun seekPlaybackTo(positionMs: Long) {
        repository.playerManager.seekTo(positionMs)
    }

    fun stopPlayback() {
        repository.playerManager.stop()
    }

    // --- History controls ---
    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteRecord(record: AudioRecordEntity) {
        if (playbackState.value.audioId == record.id) {
            stopPlayback()
        }
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun toggleFavorite(record: AudioRecordEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(record)
        }
    }

    fun shareRecord(record: AudioRecordEntity) {
        repository.shareAudio(record)
    }

    override fun onCleared() {
        super.onCleared()
        repository.playerManager.release()
    }
}
