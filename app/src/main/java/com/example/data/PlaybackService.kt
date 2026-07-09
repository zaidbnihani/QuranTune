package com.example.data

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import android.widget.Toast
import android.content.Intent
import com.example.data.QuranDatabase
import com.example.sendQuranNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private var syncListener: ((String, String?, String?) -> Unit)? = null
    
    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            
        mediaSession = MediaSession.Builder(this, player).build()

        // Start MQTT Service if not started
        startService(Intent(this, MqttService::class.java))

        // Start real-time sync listeners in the service
        SyncManager.startListening(this)

        // Handle remote commands from other devices (Informational Only)
        syncListener = { type, title, cardId ->
            val status = when(type) {
                "play" -> "تشغيل"
                "stop" -> "إيقاف"
                "completed" -> "انتهاء"
                else -> type
            }
            if (title != null) {
                // Show notification even if app is in background to inform the user
                sendQuranNotification(
                    this,
                    "تزامن المصحف",
                    "الجهاز الآخر: $status $title"
                )
            }
        }
        syncListener?.let { SyncManager.addListener(it) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        syncListener?.let { SyncManager.removeListener(it) }
        serviceScope.cancel()
        mediaSession?.player?.release()
        mediaSession?.release()
        super.onDestroy()
    }
}
