package com.example.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

data class RemoteMessage(
    val id: Long,
    val app_id: String,
    val message: String,
    val version: Int,
    val is_active: Boolean,
    val created_at: String? = null,
    val updated_at: String? = null
)

object SupabaseManager {
    private const val TAG = "SupabaseManager"
    private const val PREFS_NAME = "supabase_config_prefs"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    // Default configuration (users can change this via the settings UI inside the app)
    private const val DEFAULT_URL = "https://rqhqxjukliipqyqodkbo.supabase.co"
    private const val DEFAULT_ANON_KEY = "sb_publishable_ninoPDXy-vzW-lRDAw61xg_R3uqEXza"

    private val _currentMessage = MutableStateFlow<RemoteMessage?>(null)
    val currentMessage: StateFlow<RemoteMessage?> = _currentMessage.asStateFlow()

    private val _isRealtimeConnected = MutableStateFlow(false)
    val isRealtimeConnected: StateFlow<Boolean> = _isRealtimeConnected.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var appContext: Context? = null
    private var myDeviceId: String? = null

    private val deviceSyncListeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val processedMsgIds = Collections.synchronizedSet(LinkedHashSet<String>())

    fun initialize(context: Context, deviceId: String? = null) {
        appContext = context.applicationContext
        if (deviceId != null) {
            myDeviceId = deviceId
        } else {
            myDeviceId = DeviceLinkRepository(context).getDeviceId()
        }
        Log.d(TAG, "Initializing SupabaseManager with deviceId=$myDeviceId...")
        fetchCurrentMessage()
        fetchRecentSyncMessages()
        connectRealtime()
    }

    fun setDeviceId(deviceId: String) {
        myDeviceId = deviceId
    }

    fun addDeviceSyncListener(listener: (String) -> Unit) {
        if (!deviceSyncListeners.contains(listener)) {
            deviceSyncListeners.add(listener)
        }
    }

    fun removeDeviceSyncListener(listener: (String) -> Unit) {
        deviceSyncListeners.remove(listener)
    }

    fun getSupabaseUrl(context: Context = appContext!!): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        return saved.trim().removeSuffix("/")
    }

    fun getSupabaseAnonKey(context: Context = appContext!!): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.getString(KEY_ANON_KEY, DEFAULT_ANON_KEY) ?: DEFAULT_ANON_KEY).trim()
    }

    fun updateConfig(context: Context, url: String, anonKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_URL, url.trim())
            .putString(KEY_ANON_KEY, anonKey.trim())
            .apply()
        
        Log.d(TAG, "Configuration updated. Re-fetching and reconnecting...")
        _errorState.value = null
        
        fetchCurrentMessage()
        fetchRecentSyncMessages()
        connectRealtime()
    }

    fun isConfigured(context: Context = appContext!!): Boolean {
        val url = getSupabaseUrl(context)
        val key = getSupabaseAnonKey(context)
        return url.isNotBlank() && key.isNotBlank()
    }

    /**
     * Fetches the current active message for qurantune via REST API (GET)
     */
    fun fetchCurrentMessage() {
        val context = appContext ?: return
        if (!isConfigured(context)) return

        val url = "${getSupabaseUrl(context)}/rest/v1/remote_messages?app_id=eq.qurantune&is_active=eq.true&select=*"
        val apiKey = getSupabaseAnonKey(context)

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to fetch remote message", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to fetch remote message: ${response.code}")
                        return
                    }

                    try {
                        val bodyString = response.body?.string() ?: "[]"
                        val gson = Gson()
                        val list = gson.fromJson(bodyString, Array<RemoteMessage>::class.java)
                        
                        val activeMessage = list.firstOrNull { it.is_active }
                        _currentMessage.value = activeMessage
                        Log.d(TAG, "Successfully fetched remote message: ${activeMessage?.message}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing remote message response", e)
                    }
                }
            }
        })
    }

    /**
     * Fetches recent sync messages directed to this device in case any were missed while offline
     */
    fun fetchRecentSyncMessages() {
        val context = appContext ?: return
        val target = myDeviceId ?: return
        if (!isConfigured(context)) return

        managerScope.launch {
            try {
                val url = "${getSupabaseUrl(context)}/rest/v1/device_sync_messages?target_id=eq.$target&order=created_at.desc&limit=5"
                val apiKey = getSupabaseAnonKey(context)

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.w(TAG, "Failed to query recent sync messages", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) return
                            try {
                                val bodyString = response.body?.string() ?: "[]"
                                val jsonArray = org.json.JSONArray(bodyString)
                                for (i in 0 until jsonArray.length()) {
                                    val item = jsonArray.getJSONObject(i)
                                    val id = item.optString("id")
                                    val payload = item.optString("payload")
                                    if (id.isNotEmpty() && !processedMsgIds.contains(id) && payload.isNotEmpty()) {
                                        processedMsgIds.add(id)
                                        Log.d(TAG, "Delivering missed sync message: $id")
                                        deliverDeviceSyncPayload(payload)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing recent sync messages", e)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchRecentSyncMessages", e)
            }
        }
    }

    /**
     * Publishes a device sync message to targetId via Supabase REST API & WebSocket Broadcast
     */
    fun publishDeviceSyncMessage(targetId: String, payload: String, type: String = "event") {
        val context = appContext ?: return
        val sender = myDeviceId ?: DeviceLinkRepository(context).getDeviceId()
        if (!isConfigured(context)) {
            Log.w(TAG, "Supabase is not configured. Cannot publish device message.")
            return
        }

        managerScope.launch {
            // 1. Send via WebSocket Broadcast for instant sub-second delivery
            try {
                val broadcastMsg = mapOf(
                    "topic" to "realtime:device_sync",
                    "event" to "broadcast",
                    "payload" to mapOf(
                        "type" to "broadcast",
                        "event" to "sync_event",
                        "payload" to mapOf(
                            "sender_id" to sender,
                            "target_id" to targetId,
                            "payload" to payload,
                            "type" to type,
                            "timestamp" to System.currentTimeMillis()
                        )
                    ),
                    "ref" to "sync_bc_${System.currentTimeMillis()}"
                )
                webSocket?.send(Gson().toJson(broadcastMsg))
                Log.d(TAG, "Broadcast device message sent over WebSocket to $targetId")
            } catch (e: Exception) {
                Log.w(TAG, "WebSocket broadcast send failed, falling back to REST insert", e)
            }

            // 2. Insert into device_sync_messages table via REST API
            try {
                val url = "${getSupabaseUrl(context)}/rest/v1/device_sync_messages"
                val apiKey = getSupabaseAnonKey(context)

                val bodyJson = JSONObject().apply {
                    put("sender_id", sender)
                    put("target_id", targetId)
                    put("payload", payload)
                    put("type", type)
                }

                val requestBody = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Prefer", "return=minimal")
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed to insert device sync message via REST", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) {
                                Log.e(TAG, "REST insert device_sync_messages returned code: ${response.code}")
                            } else {
                                Log.d(TAG, "Successfully inserted device sync message into Supabase for target $targetId")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error in publishDeviceSyncMessage REST", e)
            }
        }
    }

    private fun deliverDeviceSyncPayload(payload: String) {
        for (listener in deviceSyncListeners) {
            try {
                listener.invoke(payload)
            } catch (e: Exception) {
                Log.e(TAG, "Error in device sync listener", e)
            }
        }
    }

    /**
     * Establishes a WebSocket connection to Supabase Realtime to stream database changes live
     */
    @Synchronized
    fun connectRealtime() {
        val context = appContext ?: return
        if (!isConfigured(context)) {
            Log.w(TAG, "Supabase is not configured. Skipping Realtime connection.")
            return
        }

        closeWebSocket()

        val baseUrl = getSupabaseUrl(context).replace("https://", "wss://").replace("http://", "ws://")
        val apiKey = getSupabaseAnonKey(context)
        val socketUrl = "$baseUrl/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"

        Log.d(TAG, "Connecting to Supabase Realtime WebSocket: $socketUrl")

        val request = Request.Builder()
            .url(socketUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Supabase Realtime WebSocket Connected successfully!")
                _isRealtimeConnected.value = true
                _errorState.value = null

                reconnectJob?.cancel()

                val gson = Gson()

                // 1. Join remote_messages channel for system broadcast announcements
                val joinRemoteMessages = mapOf(
                    "topic" to "realtime:public:remote_messages",
                    "event" to "phx_join",
                    "payload" to emptyMap<String, Any>(),
                    "ref" to "join_remote_messages"
                )
                webSocket.send(gson.toJson(joinRemoteMessages))

                // 2. Join device_sync_messages channel for Postgres Changes
                val joinDeviceSyncMessages = mapOf(
                    "topic" to "realtime:public:device_sync_messages",
                    "event" to "phx_join",
                    "payload" to mapOf(
                        "config" to mapOf(
                            "broadcast" to mapOf("self" to false),
                            "postgres_changes" to listOf(
                                mapOf(
                                    "event" to "INSERT",
                                    "schema" to "public",
                                    "table" to "device_sync_messages"
                                )
                            )
                        )
                    ),
                    "ref" to "join_device_sync_messages"
                )
                webSocket.send(gson.toJson(joinDeviceSyncMessages))

                // 3. Join device_sync broadcast channel for direct real-time messaging
                val joinBroadcastChannel = mapOf(
                    "topic" to "realtime:device_sync",
                    "event" to "phx_join",
                    "payload" to mapOf(
                        "config" to mapOf("broadcast" to mapOf("self" to false))
                    ),
                    "ref" to "join_broadcast_channel"
                )
                webSocket.send(gson.toJson(joinBroadcastChannel))

                Log.d(TAG, "Joined all Supabase Realtime channels")
                startHeartbeats(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val gson = Gson()
                    val rawMap = gson.fromJson(text, Map::class.java) ?: return
                    val event = rawMap["event"] as? String
                    val topic = rawMap["topic"] as? String

                    // 1. Remote messages table update
                    if (event == "postgres_changes" && topic == "realtime:public:remote_messages") {
                        val payload = rawMap["payload"] as? Map<*, *>
                        val data = payload?.get("data") as? Map<*, *>
                        val record = data?.get("record") as? Map<*, *>

                        if (record != null) {
                            val appId = record["app_id"] as? String
                            if (appId == "qurantune") {
                                val message = record["message"] as? String ?: ""
                                val version = (record["version"] as? Double)?.toInt() ?: 1
                                val isActive = record["is_active"] as? Boolean ?: false
                                val id = (record["id"] as? Double)?.toLong() ?: 0L

                                Log.d(TAG, "Realtime remote message parsed: active=$isActive, msg=$message")
                                if (isActive) {
                                    _currentMessage.value = RemoteMessage(id, appId, message, version, true)
                                } else {
                                    _currentMessage.value = null
                                }
                            }
                        }
                    }

                    // 2. Device sync postgres_changes (Postgres INSERT event)
                    if (event == "postgres_changes" && topic == "realtime:public:device_sync_messages") {
                        val payload = rawMap["payload"] as? Map<*, *>
                        val data = payload?.get("data") as? Map<*, *>
                        val record = data?.get("record") as? Map<*, *>
                        if (record != null) {
                            val targetId = record["target_id"] as? String ?: ""
                            val senderId = record["sender_id"] as? String ?: ""
                            val recordId = record["id"]?.toString() ?: ""
                            val syncPayload = record["payload"] as? String ?: ""

                            val currentMyId = myDeviceId ?: appContext?.let { DeviceLinkRepository(it).getDeviceId() }
                            if (targetId == currentMyId && syncPayload.isNotEmpty()) {
                                if (recordId.isNotEmpty()) {
                                    if (processedMsgIds.contains(recordId)) return
                                    processedMsgIds.add(recordId)
                                    if (processedMsgIds.size > 200) {
                                        val first = processedMsgIds.iterator().next()
                                        processedMsgIds.remove(first)
                                    }
                                }
                                Log.d(TAG, "Received Postgres CDC device sync message from $senderId")
                                deliverDeviceSyncPayload(syncPayload)
                            }
                        }
                    }

                    // 3. Direct WebSocket broadcast message
                    if (event == "broadcast" && topic == "realtime:device_sync") {
                        val payloadObj = rawMap["payload"] as? Map<*, *>
                        val innerPayload = payloadObj?.get("payload") as? Map<*, *>
                        if (innerPayload != null) {
                            val targetId = innerPayload["target_id"] as? String ?: ""
                            val senderId = innerPayload["sender_id"] as? String ?: ""
                            val syncPayload = innerPayload["payload"] as? String ?: ""
                            val timestamp = innerPayload["timestamp"]?.toString() ?: ""

                            val currentMyId = myDeviceId ?: appContext?.let { DeviceLinkRepository(it).getDeviceId() }
                            if (targetId == currentMyId && syncPayload.isNotEmpty()) {
                                val dedupKey = "bc_${senderId}_$timestamp"
                                if (processedMsgIds.contains(dedupKey)) return
                                processedMsgIds.add(dedupKey)
                                if (processedMsgIds.size > 200) {
                                    val first = processedMsgIds.iterator().next()
                                    processedMsgIds.remove(first)
                                }
                                Log.d(TAG, "Received instant Broadcast device sync message from $senderId")
                                deliverDeviceSyncPayload(syncPayload)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing WebSocket message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "Realtime WebSocket closing: $code / $reason")
                _isRealtimeConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "Realtime WebSocket closed: $code / $reason")
                _isRealtimeConnected.value = false
                scheduleReconnection()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Realtime WebSocket Failure", t)
                _isRealtimeConnected.value = false
                scheduleReconnection()
            }
        })
    }

    private fun startHeartbeats(socket: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = managerScope.launch {
            while (isActive) {
                delay(30000) // Phoenix heartbeat ping every 30 seconds
                try {
                    val heartbeat = mapOf(
                        "topic" to "phoenix",
                        "event" to "heartbeat",
                        "payload" to emptyMap<String, Any>(),
                        "ref" to "heartbeat_${System.currentTimeMillis()}"
                    )
                    socket.send(Gson().toJson(heartbeat))
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending heartbeat", e)
                }
            }
        }
    }

    private fun scheduleReconnection() {
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        reconnectJob = managerScope.launch {
            delay(4000)
            Log.d(TAG, "Attempting to reconnect to Realtime WebSocket...")
            connectRealtime()
        }
    }

    private fun closeWebSocket() {
        heartbeatJob?.cancel()
        try {
            webSocket?.close(1000, "Normal Close")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing websocket", e)
        }
        webSocket = null
        _isRealtimeConnected.value = false
    }

    fun destroy() {
        closeWebSocket()
        reconnectJob?.cancel()
        managerScope.cancel()
    }
}
