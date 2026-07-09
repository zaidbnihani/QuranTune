package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quran_cards")
data class QuranCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val clipboardText: String,
    val imageUri: String? = null,
    val presetResName: String? = null, // "green", "blue", or null for default/custom
    val reciterIdentifier: String? = null,
    val notificationTriggerWord: String? = null,
    val sortOrder: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
