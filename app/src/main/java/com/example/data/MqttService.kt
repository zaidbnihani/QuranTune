package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class MqttService : Service() {

    private val CHANNEL_ID = "mqtt_sync_channel"
    private val NOTIFICATION_ID = 2

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MqttService", "MqttService started as Foreground")
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        val deviceRepo = DeviceLinkRepository(this)
        val deviceId = deviceRepo.getDeviceId()
        val linkedId = deviceRepo.getLinkedId()
        
        MqttManager.initialize(deviceId, linkedId)
        
        if (deviceRepo.isLinked()) {
            SyncManager.startListening(this)
        }

        return START_STICKY
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("تزامن المصحف")
        .setContentText("المزامنة بين الأجهزة نشطة في الخلفية")
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

    override fun onDestroy() {
        Log.d("MqttService", "MqttService destroyed")
        super.onDestroy()
    }
}
