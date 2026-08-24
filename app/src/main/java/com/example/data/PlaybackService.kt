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

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            
        mediaSession = MediaSession.Builder(this, player).build()

        // Start MQTT Service if not started
        try {
            androidx.core.content.ContextCompat.startForegroundService(this, Intent(this, MqttService::class.java))
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Failed to start MqttService from background", e)
        }

        // Start real-time sync listeners in the service
        SyncManager.startListening(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.player?.release()
        mediaSession?.release()
        super.onDestroy()
    }
}
