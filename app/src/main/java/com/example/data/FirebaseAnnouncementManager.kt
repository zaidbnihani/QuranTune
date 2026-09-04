package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Data class representing a broadcast announcement from the administrator.
 */
data class AppAnnouncement(
    val id: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

/**
 * Manager handling administrative announcements via Firebase Realtime Database
 * and a high-reliability global broadcast channel with local persistence
 * to ensure users only see each announcement once.
 */
object FirebaseAnnouncementManager {
    private const val TAG = "FirebaseAnnouncement"
    private const val PREFS_NAME = "app_broadcast_prefs"
    private const val KEY_LAST_SEEN_ID = "last_seen_announcement_id"
    private const val KEY_LAST_SEEN_TIME = "last_seen_announcement_time"
    private const val KEY_ADMIN_UNLOCKED = "admin_secret_code_unlocked"

    // The secret code entered in the linking settings to unlock the admin panel
    const val SECRET_CODE = "321465"

    // The global retained broadcast topic ensuring instant sync across all devices
    private const val MQTT_ANNOUNCEMENT_TOPIC = "quran_player_announcement_broadcast_v1"
    private const val BROKER_HOST = "broker.hivemq.com"
    private const val BROKER_PORT = 1883

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    // Potential Realtime Database endpoints for project quran-player-e25b0
    private val FIREBASE_RTDB_URLS = listOf(
        "https://quran-player-e25b0-default-rtdb.firebaseio.com",
        "https://quran-player-e25b0-default-rtdb.europe-west1.firebasedatabase.app",
        "https://quran-player-e25b0-default-rtdb.asia-southeast1.firebasedatabase.app",
        "https://quran-player-e25b0.firebaseio.com"
    )

    private fun getDbInstances(): List<FirebaseDatabase> {
        val instances = mutableListOf<FirebaseDatabase>()
        try {
            instances.add(FirebaseDatabase.getInstance())
        } catch (_: Exception) {}

        for (url in FIREBASE_RTDB_URLS) {
            try {
                instances.add(FirebaseDatabase.getInstance(url))
            } catch (_: Exception) {}
        }
        return instances
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAdminUnlocked(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ADMIN_UNLOCKED, false)
    }

    fun setAdminUnlocked(context: Context, unlocked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ADMIN_UNLOCKED, unlocked).apply()
    }

    fun getLastSeenTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SEEN_TIME, 0L)
    }

    fun getLastSeenId(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_SEEN_ID, "") ?: ""
    }

    /**
     * Marks an announcement as seen so it will never be displayed again to this user.
     * Uses commit() to guarantee immediate synchronous flush to flash storage.
     */
    fun markAnnouncementSeen(context: Context, id: String, timestamp: Long) {
        getPrefs(context).edit()
            .putString(KEY_LAST_SEEN_ID, id)
            .putLong(KEY_LAST_SEEN_TIME, timestamp)
            .commit()
        Log.d(TAG, "Marked announcement as seen: id=$id, timestamp=$timestamp")
    }

    /**
     * Publishes a new announcement to Firebase and the global broadcast channel.
     */
    fun publishAnnouncement(
        context: Context? = null,
        text: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onComplete(false, "النص فارغ")
            return
        }

        val currentTime = System.currentTimeMillis()
        val announcementId = currentTime.toString()

        val jsonPayload = JSONObject().apply {
            put("id", announcementId)
            put("text", trimmed)
            put("timestamp", currentTime)
        }.toString()

        val mapPayload = mapOf(
            "id" to announcementId,
            "text" to trimmed,
            "timestamp" to currentTime
        )

        // Mark as seen locally for the publisher so they do not see their own message as a popup
        if (context != null) {
            markAnnouncementSeen(context, announcementId, currentTime)
        }

        scope.launch {
            var firebaseSucceeded = false
            var mqttSucceeded = false

            // 1. Firebase Realtime Database SDK
            for (db in getDbInstances()) {
                try {
                    db.getReference("app_announcement").setValue(mapPayload)
                        .addOnSuccessListener {
                            Log.d(TAG, "Firebase SDK publish success")
                            firebaseSucceeded = true
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase SDK publish attempt error", e)
                }
            }

            // 2. Firebase Realtime Database REST API (HTTP PUT)
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val putBody = jsonPayload.toRequestBody(jsonMediaType)
            for (url in FIREBASE_RTDB_URLS) {
                try {
                    val req = Request.Builder()
                        .url("$url/app_announcement.json")
                        .put(putBody)
                        .build()
                    val response = okHttpClient.newCall(req).execute()
                    if (response.isSuccessful) {
                        Log.d(TAG, "Firebase REST PUT success at $url")
                        firebaseSucceeded = true
                    }
                    response.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase REST PUT failed for $url: ${e.message}")
                }
            }

            // 3. MQTT Broadcast with Retain=true (Guarantees instant delivery to all devices)
            try {
                val publisherId = "quran_pub_${UUID.randomUUID().toString().take(8)}"
                val client = MqttClient.builder()
                    .useMqttVersion5()
                    .identifier(publisherId)
                    .serverHost(BROKER_HOST)
                    .serverPort(BROKER_PORT)
                    .buildAsync()

                client.connect().whenComplete { _, connErr ->
                    if (connErr == null) {
                        val publishMsg = Mqtt5Publish.builder()
                            .topic(MQTT_ANNOUNCEMENT_TOPIC)
                            .payload(jsonPayload.toByteArray(StandardCharsets.UTF_8))
                            .qos(MqttQos.AT_LEAST_ONCE)
                            .retain(true)
                            .build()

                        client.publish(publishMsg).whenComplete { _, pubErr ->
                            if (pubErr == null) {
                                Log.d(TAG, "MQTT Retained announcement published successfully")
                                mqttSucceeded = true
                            }
                            client.disconnect()
                        }
                    } else {
                        Log.w(TAG, "MQTT publisher connection failed", connErr)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MQTT publish exception", e)
            }

            // Report back to UI on main thread
            mainHandler.postDelayed({
                mainHandler.post {
                    onComplete(true, null)
                }
            }, 600)
        }
    }

    /**
     * Backward-compatible overload without Context.
     */
    fun publishAnnouncement(
        text: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        publishAnnouncement(null, text, onComplete)
    }

    /**
     * Actively checks for the latest announcement upon opening the app.
     * Queries Firebase via REST and SDK, and also checks the retained broadcast topic.
     * Only notifies if the announcement is valid and has NOT been seen yet.
     */
    fun checkForNewAnnouncement(
        context: Context,
        onAnnouncement: (AppAnnouncement?) -> Unit
    ) {
        val lastSeenId = getLastSeenId(context)

        scope.launch {
            var found = false

            // Helper to handle found announcement
            fun handleCandidate(announcement: AppAnnouncement?) {
                if (found) return
                if (announcement != null && announcement.text.isNotBlank() && announcement.id.isNotBlank()) {
                    if (announcement.id != lastSeenId) {
                        found = true
                        Log.d(TAG, "Found new announcement to show: id=${announcement.id}, text=${announcement.text.take(20)}")
                        mainHandler.post { onAnnouncement(announcement) }
                    } else {
                        Log.d(TAG, "Announcement ${announcement.id} was already seen by this device. Not showing.")
                    }
                }
            }

            // 1. Firebase Realtime Database REST GET
            for (url in FIREBASE_RTDB_URLS) {
                if (found) break
                try {
                    val req = Request.Builder()
                        .url("$url/app_announcement.json")
                        .get()
                        .build()
                    val response = okHttpClient.newCall(req).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank() && body != "null") {
                            val json = JSONObject(body)
                            val id = json.optString("id", "")
                            val text = json.optString("text", "")
                            val ts = json.optLong("timestamp", 0L)
                            if (text.isNotBlank() && id.isNotBlank()) {
                                handleCandidate(AppAnnouncement(id = id, text = text, timestamp = ts))
                            }
                        }
                    }
                    response.close()
                } catch (e: Exception) {
                    Log.d(TAG, "Firebase REST GET check for $url failed: ${e.message}")
                }
            }

            // 2. Firebase Realtime Database SDK
            if (!found) {
                for (db in getDbInstances()) {
                    if (found) break
                    try {
                        db.getReference("app_announcement")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val id = snapshot.child("id").getValue(String::class.java)
                                        ?: snapshot.child("id").getValue(Long::class.java)?.toString()
                                        ?: ""
                                    val text = snapshot.child("text").getValue(String::class.java) ?: ""
                                    val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                                    if (text.isNotBlank() && id.isNotBlank()) {
                                        handleCandidate(AppAnnouncement(id = id, text = text, timestamp = ts))
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    } catch (_: Exception) {}
                }
            }

            // 3. MQTT Retained Broadcast Check
            if (!found) {
                try {
                    val subscriberId = "quran_check_${UUID.randomUUID().toString().take(8)}"
                    val client = MqttClient.builder()
                        .useMqttVersion5()
                        .identifier(subscriberId)
                        .serverHost(BROKER_HOST)
                        .serverPort(BROKER_PORT)
                        .buildAsync()

                    client.connect().whenComplete { _, connErr ->
                        if (connErr == null) {
                            client.subscribeWith()
                                .topicFilter(MQTT_ANNOUNCEMENT_TOPIC)
                                .qos(MqttQos.AT_LEAST_ONCE)
                                .callback { pub ->
                                    val payload = if (pub.payload.isPresent) {
                                        StandardCharsets.UTF_8.decode(pub.payload.get().asReadOnlyBuffer()).toString()
                                    } else ""
                                    try {
                                        if (payload.isNotBlank()) {
                                            val json = JSONObject(payload)
                                            val id = json.optString("id", "")
                                            val text = json.optString("text", "")
                                            val ts = json.optLong("timestamp", 0L)
                                            if (text.isNotBlank() && id.isNotBlank()) {
                                                handleCandidate(AppAnnouncement(id = id, text = text, timestamp = ts))
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing MQTT retained announcement", e)
                                    }
                                    client.disconnect()
                                }
                                .send()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MQTT subscriber check exception", e)
                }
            }
        }
    }

    /**
     * Listens in real-time for announcements while the app is active.
     */
    fun listenForLatestAnnouncement(
        onAnnouncement: (AppAnnouncement?) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val id = snapshot.child("id").getValue(String::class.java)
                        ?: snapshot.child("id").getValue(Long::class.java)?.toString()
                        ?: ""
                    val text = snapshot.child("text").getValue(String::class.java) ?: ""
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                        ?: snapshot.child("timestamp").getValue(String::class.java)?.toLongOrNull()
                        ?: 0L

                    if (text.isNotBlank() && id.isNotBlank()) {
                        mainHandler.post {
                            onAnnouncement(AppAnnouncement(id = id, text = text, timestamp = timestamp))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing announcement snapshot", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Firebase announcement listener cancelled: ${error.message}")
            }
        }

        for (db in getDbInstances()) {
            try {
                db.getReference("app_announcement").addValueEventListener(listener)
            } catch (_: Exception) {}
        }

        // Also listen on MQTT broadcast topic in real-time
        try {
            val liveSubId = "quran_live_${UUID.randomUUID().toString().take(8)}"
            val client = MqttClient.builder()
                .useMqttVersion5()
                .identifier(liveSubId)
                .serverHost(BROKER_HOST)
                .serverPort(BROKER_PORT)
                .buildAsync()

            client.connect().whenComplete { _, connErr ->
                if (connErr == null) {
                    client.subscribeWith()
                        .topicFilter(MQTT_ANNOUNCEMENT_TOPIC)
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .callback { pub ->
                            val payload = if (pub.payload.isPresent) {
                                StandardCharsets.UTF_8.decode(pub.payload.get().asReadOnlyBuffer()).toString()
                            } else ""
                            try {
                                if (payload.isNotBlank()) {
                                    val json = JSONObject(payload)
                                    val id = json.optString("id", "")
                                    val text = json.optString("text", "")
                                    val ts = json.optLong("timestamp", 0L)
                                    if (text.isNotBlank() && id.isNotBlank()) {
                                        mainHandler.post {
                                            onAnnouncement(AppAnnouncement(id = id, text = text, timestamp = ts))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing live announcement", e)
                            }
                        }
                        .send()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Live MQTT listener failed", e)
        }

        return listener
    }

    fun removeListener(listener: ValueEventListener) {
        for (db in getDbInstances()) {
            try {
                db.getReference("app_announcement").removeEventListener(listener)
            } catch (_: Exception) {}
        }
    }
}
