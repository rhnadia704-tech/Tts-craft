package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioPlayerManager
import com.example.data.audio.PlaybackState
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecordEntity
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

enum class HistoryFilter {
    ALL, ORIGINALS, AI_ENHANCED, FAVORITES
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TtsRepository

    private val _synthesisUiState = MutableStateFlow(SynthesisUiState())
    val synthesisUiState: StateFlow<SynthesisUiState> = _synthesisUiState.asStateFlow()

    private val _aiEnhancerUiState = MutableStateFlow(AiEnhancerUiState())
    val aiEnhancerUiState: StateFlow<AiEnhancerUiState> = _aiEnhancerUiState.asStateFlow()

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
                    HistoryFilter.ORIGINALS -> !record.isAiEnhanced
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
                    // Auto play newly synthesized audio
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
                    // Play the enhanced audio
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
