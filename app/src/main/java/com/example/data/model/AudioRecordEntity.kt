package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_records")
data class AudioRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val text: String,
    val voiceId: String,
    val voiceName: String,
    val language: String,
    val speed: Float = 1.0f,
    val pitch: Float = 0.0f,
    val volume: Float = 1.0f,
    val filePath: String,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val isAiEnhanced: Boolean = false,
    val originalRecordId: Long? = null,
    val aiPrompt: String? = null,
    val aiModificationsSummary: String? = null,
    val isFavorite: Boolean = false,
    val isDialogue: Boolean = false,
    val participantsSummary: String? = null
)
