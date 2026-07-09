package com.example.data

import android.content.Context
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture

object QuranAudioPlayer {
    private var appContext: Context? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    var onPlaybackStateChanged: ((Boolean, String?, String?) -> Unit)? = null
    var onPlaybackCompleted: (() -> Unit)? = null
    
    private var currentListener: Player.Listener? = null

    fun initPlayer(context: Context) {
        getController(context) { /* Just to initialize */ }
    }

    private fun getController(context: Context, onReady: (MediaController) -> Unit) {
        if (appContext == null) appContext = context.applicationContext
        if (controller != null) {
            onReady(controller!!)
            return
        }
        if (controllerFuture == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        }
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.let { onReady(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playAudio(context: Context, reciterId: String?, surahNumber: String, title: String? = null, cardId: String? = null) {
        val appContext = context.applicationContext
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                playAudioInternal(appContext, reciterId, surahNumber, title, cardId)
            }
        } else {
            playAudioInternal(appContext, reciterId, surahNumber, title, cardId)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun playAudioInternal(context: Context, reciterId: String?, surahNumber: String, title: String?, cardId: String?) {
        val isCustomUri = surahNumber.startsWith("content://") || surahNumber.startsWith("file://")
        val validSurah = surahNumber.toIntOrNull()
        if (validSurah == null && !isCustomUri) {
            Toast.makeText(context, "الرجاء تعديل البطاقة واختيار السورة الصحيحة", Toast.LENGTH_LONG).show()
            return
        }
        
        if (!isCustomUri && validSurah !in 1..114 && validSurah !in -3..-1) {
            Toast.makeText(context, "الرجاء تعديل البطاقة واختيار السورة الصحيحة", Toast.LENGTH_LONG).show()
            return
        }

        onPlaybackStateChanged?.invoke(true, title, cardId)
        SyncManager.publishState(context, "play", title, cardId)
        
        val getLocalFile: (String) -> java.io.File? = { fileName ->
            val file = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), fileName)
            if (file.exists() && file.length() > 0) file else null
        }
        
        getController(context) { player ->
            try {
                currentListener?.let { player.removeListener(it) }
                player.stop()
                player.clearMediaItems()
                
                if (isCustomUri) {
                    player.setMediaItem(MediaItem.fromUri(surahNumber))
                } else if (validSurah == -1) {
                    val local = getLocalFile("athkar_sabah.mp3")
                    if (local != null) player.setMediaItem(MediaItem.fromUri(local.toURI().toString()))
                    else player.setMediaItem(MediaItem.fromUri("https://backup.qurango.net/radio/athkar_sabah"))
                } else if (validSurah == -2) {
                    val local = getLocalFile("athkar_masa.mp3")
                    if (local != null) player.setMediaItem(MediaItem.fromUri(local.toURI().toString()))
                    else player.setMediaItem(MediaItem.fromUri("https://backup.qurango.net/radio/athkar_masa"))
                } else if (validSurah == -3) {
                    var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                    if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                    val baseServer = if (server.endsWith("/")) server else "$server/"
                    val items = (78..114).map { s ->
                        val local = getLocalFile("quran_${reciterId.hashCode()}_${s}.mp3")
                        if (local != null) {
                            MediaItem.fromUri(local.toURI().toString())
                        } else {
                            val formattedSurah = String.format(java.util.Locale.US, "%03d", s)
                            MediaItem.fromUri("$baseServer$formattedSurah.mp3")
                        }
                    }
                    player.setMediaItems(items)
                } else if (validSurah != null && validSurah in 1..114) {
                    val local = getLocalFile("quran_${reciterId.hashCode()}_${validSurah}.mp3")
                    if (local != null) {
                        player.setMediaItem(MediaItem.fromUri(local.toURI().toString()))
                    } else {
                        var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                        if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                        val baseServer = if (server.endsWith("/")) server else "$server/"
                        val formattedSurah = String.format(java.util.Locale.US, "%03d", validSurah)
                        val url = "$baseServer$formattedSurah.mp3"
                        player.setMediaItem(MediaItem.fromUri(url))
                    }
                }
                
                player.prepare()
                player.playWhenReady = true
                
                currentListener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                            SyncManager.publishState(context, "completed", title, cardId)
                            QuranAudioPlayer.onPlaybackCompleted?.invoke()
                            player.stop()
                            player.clearMediaItems()
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Toast.makeText(context, "فشل تشغيل الصوت، قد يكون القارئ غير متوفر لهذه السورة.", Toast.LENGTH_LONG).show()
                        QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                        SyncManager.publishState(context, false, title)
                        player.stop()
                        player.clearMediaItems()
                    }
                }
                player.addListener(currentListener!!)
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تشغيل الصوت: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getRemainingTime(): String? {
        val player = controller ?: return null
        if (!player.isPlaying) return null
        val duration = player.duration
        val currentPosition = player.currentPosition
        if (duration < 0 || currentPosition < 0) return null
        
        val remainingMs = duration - currentPosition
        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remainingMs)
        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playSequence(context: Context, cards: List<com.example.data.QuranCard>) {
        val appContext = context.applicationContext
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                playSequenceInternal(appContext, cards)
            }
        } else {
            playSequenceInternal(appContext, cards)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun playSequenceInternal(context: Context, cards: List<com.example.data.QuranCard>) {
        if (cards.isEmpty()) return
        
        val getLocalFile: (String) -> java.io.File? = { fileName ->
            val file = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), fileName)
            if (file.exists() && file.length() > 0) file else null
        }
        
        val mediaItems = mutableListOf<MediaItem>()
        
        for (card in cards) {
            val surahNumber = card.clipboardText
            val reciterId = card.reciterIdentifier
            
            val isCustomUri = surahNumber.startsWith("content://") || surahNumber.startsWith("file://")
            val validSurah = surahNumber.toIntOrNull()
            
            if (isCustomUri) {
                mediaItems.add(MediaItem.fromUri(surahNumber))
            } else if (validSurah == -1) {
                val local = getLocalFile("athkar_sabah.mp3")
                if (local != null) mediaItems.add(MediaItem.fromUri(local.toURI().toString()))
                else mediaItems.add(MediaItem.fromUri("https://backup.qurango.net/radio/athkar_sabah"))
            } else if (validSurah == -2) {
                val local = getLocalFile("athkar_masa.mp3")
                if (local != null) mediaItems.add(MediaItem.fromUri(local.toURI().toString()))
                else mediaItems.add(MediaItem.fromUri("https://backup.qurango.net/radio/athkar_masa"))
            } else if (validSurah == -3) {
                var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                val baseServer = if (server.endsWith("/")) server else "$server/"
                for (s in 78..114) {
                    val local = getLocalFile("quran_${reciterId.hashCode()}_${s}.mp3")
                    if (local != null) {
                        mediaItems.add(MediaItem.fromUri(local.toURI().toString()))
                    } else {
                        val formattedSurah = String.format(java.util.Locale.US, "%03d", s)
                        mediaItems.add(MediaItem.fromUri("$baseServer$formattedSurah.mp3"))
                    }
                }
            } else if (validSurah != null && validSurah in 1..114) {
                val local = getLocalFile("quran_${reciterId.hashCode()}_${validSurah}.mp3")
                if (local != null) {
                    mediaItems.add(MediaItem.fromUri(local.toURI().toString()))
                } else {
                    var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                    if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                    val baseServer = if (server.endsWith("/")) server else "$server/"
                    val formattedSurah = String.format(java.util.Locale.US, "%03d", validSurah)
                    mediaItems.add(MediaItem.fromUri("$baseServer$formattedSurah.mp3"))
                }
            }
        }
        
        if (mediaItems.isEmpty()) return
        
        val sequenceTitle = if (cards.size > 1) "${cards.first().title} + ${cards.size - 1} أخرى" else cards.firstOrNull()?.title ?: "قائمة تشغيل"
        val firstCardId = cards.firstOrNull()?.id?.toString()

        onPlaybackStateChanged?.invoke(true, sequenceTitle, firstCardId)
        SyncManager.publishState(context, "play", sequenceTitle, firstCardId)
        
        getController(context) { player ->
            try {
                currentListener?.let { player.removeListener(it) }
                player.stop()
                player.clearMediaItems()
                
                player.setMediaItems(mediaItems)
                player.prepare()
                player.playWhenReady = true
                
                currentListener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, null, null)
                            SyncManager.publishState(context, "completed", null, null)
                            QuranAudioPlayer.onPlaybackCompleted?.invoke()
                            player.stop()
                            player.clearMediaItems()
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Toast.makeText(context, "فشل تشغيل أحد المقاطع.", Toast.LENGTH_LONG).show()
                        if (player.hasNextMediaItem()) {
                            player.seekToNext()
                            player.prepare()
                            player.play()
                        } else {
                            QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, null, null)
                            SyncManager.publishState(context, false, null)
                            player.stop()
                            player.clearMediaItems()
                        }
                    }
                }
                player.addListener(currentListener!!)
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تشغيل الصوت", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopAudio() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                stopAudioInternal()
            }
        } else {
            stopAudioInternal()
        }
    }
    
    private fun stopAudioInternal() {
        onPlaybackStateChanged?.invoke(false, null, null)
        appContext?.let { 
            SyncManager.publishState(it, "stop", null)
        }
        
        currentListener?.let { controller?.removeListener(it) }
        currentListener = null
        
        controller?.stop()
        controller?.clearMediaItems()
        
        // Fully release controller if requested for thorough cleanup
        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        controllerFuture = null
        controller = null
    }
}
