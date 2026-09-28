package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AudioRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audio_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AudioRecordEntity>>

    @Query("SELECT * FROM audio_records WHERE isAiEnhanced = 0 ORDER BY timestamp DESC")
    fun getOriginalRecords(): Flow<List<AudioRecordEntity>>

    @Query("SELECT * FROM audio_records WHERE isAiEnhanced = 1 ORDER BY timestamp DESC")
    fun getAiEnhancedRecords(): Flow<List<AudioRecordEntity>>

    @Query("SELECT * FROM audio_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): AudioRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AudioRecordEntity): Long

    @Update
    suspend fun updateRecord(record: AudioRecordEntity)

    @Delete
    suspend fun deleteRecord(record: AudioRecordEntity)

    @Query("DELETE FROM audio_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE audio_records SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
}
