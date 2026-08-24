package com.example

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "تنبيهات مشغل القرآن"
        val descriptionText = "قناة مخصصة لإرسال تنبيهات تشغيل سور وآيات القرآن الكريـم"
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = android.app.NotificationChannel("QURAN_NOTIFICATIONS", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: android.app.NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun sendQuranNotification(context: Context, title: String, text: String) {
    createNotificationChannel(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Can't show Toast easily from background service without Looper
            return
        }
    }

    val builder = NotificationCompat.Builder(context, "QURAN_NOTIFICATIONS")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    try {
        val notificationManager = NotificationManagerCompat.from(context)
        // Use a fixed ID for sync notifications so they update instead of stacking
        val notificationId = if (title == "تزامن المصحف") 1002 else System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
    } catch (e: SecurityException) {
        // Silently fail if permission is missing in background
    }
}
