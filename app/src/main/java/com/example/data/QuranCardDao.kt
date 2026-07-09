package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranCardDao {
    @Query("SELECT * FROM quran_cards ORDER BY sortOrder ASC, timestamp DESC")
    fun getAllCards(): Flow<List<QuranCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: QuranCard): Long

    @Update
    suspend fun updateCard(card: QuranCard)

    @Delete
    suspend fun deleteCard(card: QuranCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<QuranCard>)

    @Query("SELECT MAX(sortOrder) FROM quran_cards")
    suspend fun getMaxSortOrder(): Int?
}
