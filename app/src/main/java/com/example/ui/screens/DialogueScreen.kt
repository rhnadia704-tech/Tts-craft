package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DialogueParticipant
import com.example.data.model.DialoguePresets
import com.example.data.model.Voice
import com.example.data.model.VoiceCatalog
import com.example.ui.MainViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.VoiceSelectorModal
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueScreen(
    viewModel: MainViewModel,
    onNavigateToAiEnhancement: (audioId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.dialogueUiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val testingVoiceId by viewModel.testingVoiceId.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var participantToEdit by remember { mutableStateOf<DialogueParticipant?>(null) }

    var showVoiceModalForParticipant by remember { mutableStateOf<DialogueParticipant?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val presets = remember { DialoguePresets.getPresets() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("dialogue_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "Dialogues",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Studio de Dialogue Multi-Voix",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Créez des conversations naturelles avec plusieurs voix Edge TTS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Presets row
        Text(
            text = "Modèles de dialogues prêts à l'emploi :",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { preset ->
                SuggestionChip(
                    onClick = { viewModel.applyDialoguePreset(preset) },
                    label = { Text(preset.title) },
                    modifier = Modifier.testTag("preset_${preset.title.take(6)}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dialogue title input
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.updateDialogueTitle(it) },
            label = { Text("Titre du dialogue") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialogue_title_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: PARTICIPANTS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("participants_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "1. Participants (${uiState.participants.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Attribuez une voix neuronale distincte à chaque personnage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showAddParticipantDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_participant_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                uiState.participants.forEach { participant ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("participant_row_${participant.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(participant.colorHex).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = participant.voice.flagEmoji,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = participant.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Voix : ${participant.voice.name} (${participant.voice.locale})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    showVoiceModalForParticipant = participant
                                },
                                modifier = Modifier.testTag("change_voice_btn_${participant.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Changer la voix",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { participantToEdit = participant },
                                modifier = Modifier.testTag("edit_participant_btn_${participant.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifier",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (uiState.participants.size > 1) {
                                IconButton(
                                    onClick = { viewModel.removeParticipant(participant.id) },
                                    modifier = Modifier.testTag("delete_participant_btn_${participant.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: AI SCRIPT GENERATOR BOX
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_script_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Générateur de scénario par IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Décrivez un thème et l'IA créera les répliques distribuées entre vos personnages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.aiScriptPrompt,
                        onValueChange = { viewModel.updateAiScriptPrompt(it) },
                        placeholder = { Text("Ex: Interview sur les secrets de l'espace...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_script_prompt_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.generateDialogueScriptWithAi() },
                        enabled = !uiState.isGeneratingAiScript && uiState.aiScriptPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("generate_script_btn")
                    ) {
                        if (uiState.isGeneratingAiScript) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Générer ✨", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: DIALOGUE LINES
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialogue_lines_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Répliques (${uiState.lines.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = {
                            val defaultPid = uiState.participants.firstOrNull()?.id ?: "part_1"
                            viewModel.addDialogueLine(defaultPid)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_line_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter une réplique", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                uiState.lines.forEachIndexed { index, line ->
                    val participant = uiState.participants.find { it.id == line.participantId }
                        ?: uiState.participants.firstOrNull()
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .testTag("line_card_${line.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Speaker selector and line number
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    Surface(
                                        onClick = { dropdownExpanded = true },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (participant != null) Color(participant.colorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.testTag("speaker_chip_${line.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = participant?.voice?.flagEmoji ?: "👤",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = participant?.name ?: "Sélectionner",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        uiState.participants.forEach { p ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(p.voice.flagEmoji)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("${p.name} (${p.voice.name})")
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.updateDialogueLine(line.id, p.id, line.text, line.pauseAfterMs)
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (uiState.lines.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteDialogueLine(line.id) },
                                            modifier = Modifier.size(32.dp).testTag("delete_line_${line.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Supprimer réplique",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = line.text,
                                onValueChange = { newText ->
                                    viewModel.updateDialogueLine(line.id, line.participantId, newText, line.pauseAfterMs)
                                },
                                placeholder = { Text("Texte prononcé par ce personnage...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("line_text_input_${line.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Pause control
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pause après réplique : ${line.pauseAfterMs} ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = line.pauseAfterMs.toFloat(),
                                    onValueChange = {
                                        viewModel.updateDialogueLine(line.id, line.participantId, line.text, it.toInt())
                                    },
                                    valueRange = 100f..1000f,
                                    steps = 8,
                                    modifier = Modifier
                                        .width(160.dp)
                                        .testTag("pause_slider_${line.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message if any
        AnimatedVisibility(visible = uiState.errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
            uiState.errorMessage?.let { errorMsg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Generate Dialogue Audio Button
        Button(
            onClick = { viewModel.synthesizeDialogue() },
            enabled = !uiState.isSynthesizing && uiState.lines.any { it.text.isNotBlank() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("synthesize_dialogue_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (uiState.isSynthesizing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Génération du dialogue multi-voix…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Générer le Dialogue (${uiState.participants.size} Voix)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Generated Dialogue Output Card
        uiState.lastGeneratedDialogueRecord?.let { record ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Dialogue généré avec succès 🎉",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            AudioPlayerCard(
                record = record,
                playbackState = playbackState,
                onPlay = { viewModel.playRecord(record) },
                onPause = { viewModel.pausePlayback() },
                onSeek = { viewModel.seekPlaybackTo(it) },
                onEnhanceWithAi = {
                    viewModel.selectAudioForEnhancement(record)
                    onNavigateToAiEnhancement(record.id)
                },
                onShare = { viewModel.shareRecord(record) },
                onToggleFavorite = { viewModel.toggleFavorite(record) },
                onDelete = { viewModel.deleteRecord(record) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal to change voice for a participant
    showVoiceModalForParticipant?.let { participant ->
        VoiceSelectorModal(
            sheetState = sheetState,
            selectedVoice = participant.voice,
            onVoiceSelected = { voice ->
                viewModel.updateParticipant(participant.copy(voice = voice))
            },
            onDismiss = {
                viewModel.stopVoiceTesting()
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    showVoiceModalForParticipant = null
                }
            },
            onTestVoice = { voice ->
                viewModel.testVoice(voice)
            },
            testingVoiceId = testingVoiceId,
            isPlayingTest = playbackState.isPlaying && playbackState.audioId == -1L
        )
    }

    // Dialog to add a participant
    if (showAddParticipantDialog) {
        var newName by remember { mutableStateOf("") }
        var selectedVoice by remember { mutableStateOf(VoiceCatalog.ALL_VOICES[uiState.participants.size % VoiceCatalog.ALL_VOICES.size]) }

        AlertDialog(
            onDismissRequest = { showAddParticipantDialog = false },
            title = { Text("Nouveau Personnage") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nom du personnage (ex: Marc, Julie)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Voix : ${selectedVoice.name} (${selectedVoice.languageDisplayName})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addParticipant(newName, selectedVoice)
                        showAddParticipantDialog = false
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddParticipantDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialog to edit participant details (Speed / Pitch / Name)
    participantToEdit?.let { participant ->
        var editName by remember { mutableStateOf(participant.name) }
        var editSpeed by remember { mutableStateOf(participant.speed) }
        var editPitch by remember { mutableStateOf(participant.pitch) }

        AlertDialog(
            onDismissRequest = { participantToEdit = null },
            title = { Text("Modifier ${participant.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Vitesse : ${String.format(Locale.US, "%.1fx", editSpeed)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = editSpeed,
                        onValueChange = { editSpeed = it },
                        valueRange = 0.5f..2.0f
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tonalité (Pitch) : ${editPitch.toInt()} Hz",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = editPitch,
                        onValueChange = { editPitch = it },
                        valueRange = -25f..25f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateParticipant(
                            participant.copy(
                                name = editName,
                                speed = editSpeed,
                                pitch = editPitch
                            )
                        )
                        participantToEdit = null
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { participantToEdit = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
