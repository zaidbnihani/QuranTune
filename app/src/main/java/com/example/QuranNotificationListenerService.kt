package com.example

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.QuranAudioPlayer
import com.example.data.QuranDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class QuranNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        // Track last processed notifications by key using a thread-safe LRU cache of size 150
        private val processedNotifications = java.util.Collections.synchronizedMap(
            object : java.util.LinkedHashMap<String, String>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: Map.Entry<String, String>?): Boolean {
                    return size > 150
                }
            }
        )
        
        // Track last processed texts to completely prevent duplicate triggers from any source
        private val processedTexts = java.util.Collections.synchronizedMap(
            object : java.util.LinkedHashMap<String, Long>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: Map.Entry<String, Long>?): Boolean {
                    return size > 50
                }
            }
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("QuranNotification", "Notification listener connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("QuranNotification", "Notification listener disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        try {
            sbn?.let { notification ->
                // 1. Exclude our own notifications if needed to prevent loops
                if (notification.packageName == packageName) {
                    return
                }

                // 2. Ignore ongoing notifications (e.g. music/media player, progress bars, system tasks)
                if (notification.isOngoing) {
                    return
                }

                // 3. Ignore old notifications sitting in the notification drawer (e.g. on startup or service reconnect)
                val postTime = notification.postTime
                val currentTime = System.currentTimeMillis()
                if (Math.abs(currentTime - postTime) > 10000) {
                    Log.d("QuranNotification", "Ignored old notification from package ${notification.packageName} posted at $postTime")
                    return
                }

                val extras = notification.notification.extras
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                if (text.isBlank()) {
                    return
                }

                // 4. Check cache to completely prevent duplicate triggers from frequent notification updates
                val key = notification.key ?: ""
                val lastProcessedText = processedNotifications[key]
                if (lastProcessedText == text) {
                    return
                }
                
                processedNotifications[key] = text

                // 4b. Check duplicate text within a 3-second window
                val lastProcessedTime = processedTexts[text]
                if (lastProcessedTime != null && (currentTime - lastProcessedTime) < 3000) {
                    Log.d("QuranNotification", "Ignored duplicate text within 3 seconds: $text")
                    return
                }
                processedTexts[text] = currentTime

                val lowerText = text.lowercase()

                // 5. Check if notification text contains stop keyword
                if (lowerText.contains("إيقاف") || lowerText.contains("ايقاف") || lowerText.contains("توقف") || lowerText.contains("stop")) {
                    Log.d("QuranNotification", "Stop keyword detected, stopping audio.")
                    QuranAudioPlayer.stopAudio()
                    return
                }

                serviceScope.launch {
                    try {
                        val db = QuranDatabase.getDatabase(applicationContext)
                        val cards = db.quranCardDao().getAllCards().first()
                        
                        val matchingCards = mutableListOf<Pair<Int, com.example.data.QuranCard>>()
                        
                        // We only trigger if the message text contains an explicit play command word,
                        // OR if the message text is exactly equal to the trigger word itself.
                        val playKeywords = listOf(
                            "شغل", "تشغيل", "تشغل", "يشغل", "شغلي",
                            "اقرا", "اقرأ", "تلاوه", "تلاوة",
                            "ابدأ", "ابدا", "سمعنا", "سمعني",
                            "play", "start", "recite", "quran", "قرآن", "قران"
                        )
                        val normalizedText = ArabicUtils.normalizeArabic(text)
                        val isExplicitPlayCommand = playKeywords.any { keyword -> 
                            ArabicUtils.matches(text, keyword) || normalizedText.contains(keyword)
                        }
                        
                        for (card in cards) {
                            val triggerWord = card.notificationTriggerWord?.trim()
                            if (!triggerWord.isNullOrEmpty()) {
                                val normalizedTrigger = ArabicUtils.normalizeArabic(triggerWord)
                                val isMatchedInText = ArabicUtils.matches(text, triggerWord) || normalizedText.contains(normalizedTrigger)
                                
                                // Trigger if it's an explicit play command and the card is mentioned, 
                                // OR if the entire message is exactly the trigger word
                                if (isMatchedInText && (isExplicitPlayCommand || normalizedText == normalizedTrigger)) {
                                    val startIndex = findMatchIndex(normalizedText, triggerWord)
                                    matchingCards.add(Pair(startIndex, card))
                                }
                            }
                        }
                        
                        if (matchingCards.isNotEmpty()) {
                            // Sort cards by their position in the notification text
                            matchingCards.sortBy { it.first }
                            val sortedCards = matchingCards.map { it.second }
                            
                            Log.d("QuranNotification", "Triggered ${sortedCards.size} cards. First: ${sortedCards.first().title}")
                            
                            if (sortedCards.size == 1) {
                                QuranAudioPlayer.playAudio(
                                    context = applicationContext,
                                    reciterId = sortedCards.first().reciterIdentifier,
                                    surahNumber = sortedCards.first().clipboardText,
                                    title = sortedCards.first().title,
                                    cardId = sortedCards.first().id.toString(),
                                    youtubeUrl = sortedCards.first().youtubeUrl
                                )
                            } else {
                                QuranAudioPlayer.playSequence(
                                    context = applicationContext,
                                    cards = sortedCards
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("QuranNotification", "Error reading DB or playing", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("QuranNotification", "Error in onNotificationPosted", e)
        }
    }

    private fun findMatchIndex(normalizedText: String, triggerWord: String): Int {
        val normTrigger = ArabicUtils.normalizeArabic(triggerWord)
        val directIndex = normalizedText.indexOf(normTrigger)
        if (directIndex != -1) return directIndex

        // Fallback: search for stemmed words
        val queryWords = normTrigger.split(" ").map { ArabicUtils.normalizeWord(it) }.filter { it.isNotEmpty() }
        if (queryWords.isNotEmpty()) {
            val firstQueryWord = queryWords.first()
            val textWordsSplit = normalizedText.split(" ")
            val textWordsNormalized = textWordsSplit.map { ArabicUtils.normalizeWord(it) }
            val wordIndex = textWordsNormalized.indexOf(firstQueryWord)
            if (wordIndex != -1) {
                var charIndex = 0
                for (i in 0 until minOf(wordIndex, textWordsSplit.size)) {
                    charIndex += textWordsSplit[i].length + 1
                }
                return charIndex
            }
        }
        return 0
    }
}
