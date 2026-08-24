package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RemoteMessage(
    val id: Long = 0,
    val app_id: String = "",
    val message: String = "",
    val version: Int = 0,
    val is_active: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

object SupabaseManager {
    private val _currentMessage = MutableStateFlow<RemoteMessage?>(null)
    val currentMessage: StateFlow<RemoteMessage?> = _currentMessage.asStateFlow()

    private val _isRealtimeConnected = MutableStateFlow(false)
    val isRealtimeConnected: StateFlow<Boolean> = _isRealtimeConnected.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    fun initialize(context: Context, deviceId: String? = null) {}
    fun destroy() {}
    fun updateConfig(context: Context, url: String, anonKey: String) {}
    fun getSupabaseUrl(context: Context? = null): String = ""
    fun getSupabaseAnonKey(context: Context? = null): String = ""
    fun addDeviceSyncListener(listener: (String) -> Unit) {}
    fun removeDeviceSyncListener(listener: (String) -> Unit) {}
    fun publishDeviceSyncMessage(targetDeviceId: String, rawMessage: String) {}
}
