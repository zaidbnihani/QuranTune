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
        // Track last processed notifications by key to avoid triggering on progress/typing/update changes
        private val processedNotifications = java.util.concurrent.ConcurrentHashMap<String, String>()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
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
            
            // Limit cache size to prevent memory leaks
            if (processedNotifications.size > 150) {
                processedNotifications.clear()
            }
            processedNotifications[key] = text

            val lowerText = text.lowercase()

            // 5. Check if notification text contains stop keyword
            if (lowerText.contains("إيقاف") || lowerText.contains("ايقاف") || lowerText.contains("الصوت") || lowerText.contains("توقف") || lowerText.contains("stop")) {
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
                        ArabicUtils.matches(text, keyword) 
                    }
                    
                    for (card in cards) {
                        val triggerWord = card.notificationTriggerWord?.trim()
                        if (!triggerWord.isNullOrEmpty()) {
                            val normalizedTrigger = ArabicUtils.normalizeArabic(triggerWord)
                            
                            val isExactMatch = normalizedText == normalizedTrigger
                            val isMatchedInText = ArabicUtils.matches(text, triggerWord)
                            
                            if (isExactMatch || (isExplicitPlayCommand && isMatchedInText)) {
                                val index = text.lowercase().indexOf(triggerWord.lowercase())
                                val startIndex = if (index != -1) index else 0
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
                                title = sortedCards.first().title
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
    }
}
