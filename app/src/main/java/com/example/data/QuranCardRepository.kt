package com.example.data

import kotlinx.coroutines.flow.Flow

class QuranCardRepository(private val quranCardDao: QuranCardDao) {
    val allCards: Flow<List<QuranCard>> = quranCardDao.getAllCards()

    suspend fun insertCard(card: QuranCard) {
        val maxOrder = quranCardDao.getMaxSortOrder() ?: -1
        // If it's a new card (id == 0), assign next sortOrder
        val cardWithOrder = if (card.id == 0) {
            card.copy(sortOrder = maxOrder + 1)
        } else {
            card
        }
        quranCardDao.insertCard(cardWithOrder)
    }

    suspend fun updateCard(card: QuranCard) {
        quranCardDao.updateCard(card)
    }

    suspend fun deleteCard(card: QuranCard) {
        quranCardDao.deleteCard(card)
    }

    suspend fun insertCards(cards: List<QuranCard>) {
        quranCardDao.insertCards(cards)
    }

    suspend fun updateCardOrders(cards: List<QuranCard>) {
        val updatedCards = cards.mapIndexed { index, item ->
            item.copy(sortOrder = index)
        }
        quranCardDao.insertCards(updatedCards)
    }
}
