package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class DeviceLinkRepository(private val context: Context) {
    private val PREFS_NAME = "quran_sync_prefs"
    private val KEY_DEVICE_ID = "device_id"
    private val KEY_LINKED_ID = "linked_id"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString().substring(0, 8).uppercase()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id!!
    }

    fun getLinkedId(): String? {
        return prefs.getString(KEY_LINKED_ID, null)
    }

    fun setLinkedId(linkedId: String?) {
        prefs.edit().putString(KEY_LINKED_ID, linkedId).apply()
    }

    fun isLinked(): Boolean {
        return !getLinkedId().isNullOrBlank()
    }

    // Last known state storage
    fun saveLastState(type: String, cardId: String?, title: String?) {
        prefs.edit()
            .putString("last_state_type", type)
            .putString("last_state_card_id", cardId)
            .putString("last_state_title", title)
            .putLong("last_state_timestamp", System.currentTimeMillis())
            .apply()
    }

    fun getLastStateType(): String = prefs.getString("last_state_type", "stop") ?: "stop"
    fun getLastStateCardId(): String? = prefs.getString("last_state_card_id", null)
    fun getLastStateTitle(): String? = prefs.getString("last_state_title", null)
    fun getLastStateTimestamp(): Long = prefs.getLong("last_state_timestamp", 0L)
}
