package com.example.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class MqttService : Service() {

    private val CHANNEL_ID = "mqtt_sync_channel"
    private val NOTIFICATION_ID = 2
    private var syncListener: ((String, String?, String?) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d("MqttService", "Broadcast received in background service: $action")
            val deviceRepo = DeviceLinkRepository(this@MqttService)
            if (deviceRepo.isLinked()) {
                val deviceId = deviceRepo.getDeviceId()
                val linkedId = deviceRepo.getLinkedId()
                MqttManager.initialize(deviceId, linkedId)
                MqttManager.connect()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Register receiver for screen on, user present, and network connectivity changes
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            @Suppress("DEPRECATION")
            addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION)
        }
        registerReceiver(receiver, filter)
        scheduleRepeatingKeepAlive()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MqttService", "MqttService started as Foreground")
        
        startForeground(NOTIFICATION_ID, createNotification("المزامنة بين الأجهزة نشطة في الخلفية"))
        
        val deviceRepo = DeviceLinkRepository(this)
        val deviceId = deviceRepo.getDeviceId()
        val linkedId = deviceRepo.getLinkedId()
        
        MqttManager.initialize(deviceId, linkedId)
        
        // Handle alarm keep alive triggers
        if (intent?.action == "ACTION_KEEP_ALIVE") {
            Log.d("MqttService", "Keep-alive alarm triggered, checking connectivity...")
            if (deviceRepo.isLinked()) {
                MqttManager.connect()
            }
            return START_STICKY
        }
        
        if (deviceRepo.isLinked()) {
            SyncManager.startListening(this)
            
            // Remove old listener if exists
            syncListener?.let { SyncManager.removeListener(it) }
            
            // Define active background listener to notify user even when app is closed
            syncListener = { type, title, cardId ->
                val status = when(type) {
                    "play" -> "تشغيل"
                    "stop" -> "إيقاف"
                    "completed" -> "انتهاء"
                    else -> type
                }
                if (title != null) {
                    val message = "الجهاز الآخر: $status $title"
                    updateForegroundNotification(message)
                    
                    // Show a high priority heads-up alert for "play" events
                    if (type == "play") {
                        com.example.sendQuranNotification(
                            this,
                            "تزامن المصحف",
                            message
                        )
                    }
                } else {
                    updateForegroundNotification("المزامنة بين الأجهزة نشطة في الخلفية")
                }
            }
            syncListener?.let { SyncManager.addListener(it) }
        }

        return START_STICKY
    }

    private fun scheduleRepeatingKeepAlive() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MqttService::class.java).apply {
            action = "ACTION_KEEP_ALIVE"
        }
        val pendingIntent = PendingIntent.getService(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val interval = 15 * 60 * 1000L // 15 minutes
        val triggerAt = SystemClock.elapsedRealtime() + interval
        try {
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                interval,
                pendingIntent
            )
            Log.d("MqttService", "Scheduled inexact repeating keep-alive alarm successfully")
        } catch (e: Exception) {
            Log.e("MqttService", "Failed to schedule repeating keep-alive alarm", e)
        }
    }

    private fun updateForegroundNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    private fun createNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("تزامن المصحف")
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MQTT Sync Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("MqttService", "Task removed, restarting service in 1 second")
        val restartServiceIntent = Intent(applicationContext, this::class.java)
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        try {
            alarmService.set(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
        } catch (e: Exception) {
            // Fallback starting directly
            try {
                startService(restartServiceIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.d("MqttService", "MqttService destroyed")
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        syncListener?.let { SyncManager.removeListener(it) }
        super.onDestroy()
    }
}
