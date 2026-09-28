package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.screens.AiEnhancementScreen
import com.example.ui.screens.DialogueScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SynthesisScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SYNTHESIS(
        title = "Synthèse",
        selectedIcon = Icons.Filled.RecordVoiceOver,
        unselectedIcon = Icons.Outlined.RecordVoiceOver,
        testTag = "nav_synthesis"
    ),
    DIALOGUES(
        title = "Dialogues",
        selectedIcon = Icons.Filled.Forum,
        unselectedIcon = Icons.Outlined.Forum,
        testTag = "nav_dialogues"
    ),
    AI_ENHANCEMENT(
        title = "Amélioration IA",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_ai_enhancement"
    ),
    HISTORY(
        title = "Historique",
        selectedIcon = Icons.AutoMirrored.Filled.QueueMusic,
        unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
        testTag = "nav_history"
    ),
    SETTINGS(
        title = "Paramètres",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.SYNTHESIS) }
    var selectedAudioIdForEnhancement by remember { mutableStateOf<Long?>(null) }

    // Handle back button on sub-screens
    BackHandler(enabled = currentDestination != AppDestination.SYNTHESIS) {
        currentDestination = AppDestination.SYNTHESIS
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                AppDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            if (destination == AppDestination.AI_ENHANCEMENT) {
                                BadgedBox(badge = { Badge { Text("IA") } }) {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag(destination.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition",
            modifier = Modifier.padding(innerPadding)
        ) { destination ->
            when (destination) {
                AppDestination.SYNTHESIS -> {
                    SynthesisScreen(
                        viewModel = viewModel,
                        onNavigateToAiEnhancement = { audioId ->
                            selectedAudioIdForEnhancement = audioId
                            currentDestination = AppDestination.AI_ENHANCEMENT
                        },
                        onNavigateToDialogues = {
                            currentDestination = AppDestination.DIALOGUES
                        },
                        onNavigateToSettings = {
                            currentDestination = AppDestination.SETTINGS
                        }
                    )
                }
                AppDestination.DIALOGUES -> {
                    DialogueScreen(
                        viewModel = viewModel,
                        onNavigateToAiEnhancement = { audioId ->
                            selectedAudioIdForEnhancement = audioId
                            currentDestination = AppDestination.AI_ENHANCEMENT
                        }
                    )
                }
                AppDestination.AI_ENHANCEMENT -> {
                    AiEnhancementScreen(
                        viewModel = viewModel,
                        preselectedAudioId = selectedAudioIdForEnhancement,
                        onNavigateToSynthesis = {
                            currentDestination = AppDestination.SYNTHESIS
                        }
                    )
                }
                AppDestination.HISTORY -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onNavigateToAiEnhancement = { audioId ->
                            selectedAudioIdForEnhancement = audioId
                            currentDestination = AppDestination.AI_ENHANCEMENT
                        }
                    )
                }
                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
