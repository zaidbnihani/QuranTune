package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
 * and local persistence to ensure users only see each announcement once.
 */
object FirebaseAnnouncementManager {
    private const val PREFS_NAME = "app_broadcast_prefs"
    private const val KEY_LAST_SEEN_ID = "last_seen_announcement_id"
    private const val KEY_LAST_SEEN_TIME = "last_seen_announcement_time"
    private const val KEY_ADMIN_UNLOCKED = "admin_secret_code_unlocked"

    // The secret code required to unlock admin settings and broadcast button
    const val SECRET_CODE = "321465"

    private fun getDb(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance()
        } catch (_: Exception) {
            try {
                FirebaseDatabase.getInstance("https://quran-player-e25b0-default-rtdb.firebaseio.com")
            } catch (_: Exception) {
                FirebaseDatabase.getInstance("https://quran-player-e25b0-default-rtdb.europe-west1.firebasedatabase.app")
            }
        }
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

    fun markAnnouncementSeen(context: Context, id: String, timestamp: Long) {
        getPrefs(context).edit()
            .putString(KEY_LAST_SEEN_ID, id)
            .putLong(KEY_LAST_SEEN_TIME, timestamp)
            .apply()
        Log.d("FirebaseAnnouncement", "Marked announcement as seen: $id, timestamp: $timestamp")
    }

    /**
     * Publishes a new announcement text to Firebase Realtime Database.
     */
    fun publishAnnouncement(
        text: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onComplete(false, "النص فارغ")
            return
        }

        val currentTime = System.currentTimeMillis()
        val announcement = mapOf(
            "id" to currentTime.toString(),
            "text" to trimmed,
            "timestamp" to currentTime
        )

        try {
            val db = getDb()
            db.getReference("app_announcement").setValue(announcement)
                .addOnSuccessListener {
                    Log.d("FirebaseAnnouncement", "Announcement published successfully to primary DB")
                    onComplete(true, null)
                }
                .addOnFailureListener { error ->
                    Log.w("FirebaseAnnouncement", "Failed primary publish, trying fallback", error)
                    try {
                        FirebaseDatabase.getInstance().getReference("app_announcement")
                            .setValue(announcement)
                            .addOnSuccessListener {
                                onComplete(true, null)
                            }
                            .addOnFailureListener { e ->
                                onComplete(false, e.message ?: error.message)
                            }
                    } catch (e: Exception) {
                        onComplete(false, error.message)
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseAnnouncement", "Error publishing announcement", e)
            onComplete(false, e.message)
        }
    }

    /**
     * Listens for the latest announcement from Firebase.
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

                    if (text.isNotBlank() && timestamp > 0L) {
                        onAnnouncement(AppAnnouncement(id = id, text = text, timestamp = timestamp))
                    } else {
                        onAnnouncement(null)
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseAnnouncement", "Error parsing announcement snapshot", e)
                    onAnnouncement(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebaseAnnouncement", "Firebase announcement listener cancelled: ${error.message}")
            }
        }

        try {
            getDb().getReference("app_announcement").addValueEventListener(listener)
        } catch (_: Exception) {
            try {
                FirebaseDatabase.getInstance().getReference("app_announcement").addValueEventListener(listener)
            } catch (_: Exception) {}
        }

        return listener
    }

    fun removeListener(listener: ValueEventListener) {
        try {
            getDb().getReference("app_announcement").removeEventListener(listener)
        } catch (_: Exception) {
            try {
                FirebaseDatabase.getInstance().getReference("app_announcement").removeEventListener(listener)
            } catch (_: Exception) {}
        }
    }
}
