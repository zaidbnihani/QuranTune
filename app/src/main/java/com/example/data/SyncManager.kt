package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

object SyncManager {
    private var deviceRepo: DeviceLinkRepository? = null
    private var context: Context? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val listeners = CopyOnWriteArrayList<(String, String?, String?) -> Unit>()

    private fun getRepo(context: Context): DeviceLinkRepository {
        this.context = context.applicationContext
        if (deviceRepo == null) {
            deviceRepo = DeviceLinkRepository(context.applicationContext)
        }
        return deviceRepo!!
    }

    private fun handleRemoteEvent(type: String, title: String?, cardId: String?) {
        // Save state locally
        deviceRepo?.saveLastState(type, cardId, title)

        _lastSyncEvent.value = when(type) {
            "play" -> "تشغيل: $title"
            "stop" -> "إيقاف: $title"
            "completed" -> "انتهى: $title"
            else -> "حدث: $type"
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            isProcessingRemoteEvent = true
            Log.d("SyncManager", "Notifying ${listeners.size} listeners of $type")
            listeners.forEach { 
                try {
                    it.invoke(type, title, cardId)
                } catch (e: Exception) {
                    Log.e("SyncManager", "Error in sync listener", e)
                }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isProcessingRemoteEvent = false
            }, 1000)
        }
    }

    private val mqttListener: (String) -> Unit = mqttListener@{ message ->
        try {
            val json = JSONObject(message)
            val type = json.optString("type")
            val senderId = json.optString("senderId")
            
            if (type == "heartbeat") {
                Log.d("SyncManager", "Heartbeat received from $senderId")
                return@mqttListener
            }

            if (type == "pair") {
                Log.d("SyncManager", "Received pairing request from $senderId. Linking back...")
                val repo = getRepo(context!!)
                if (repo.getLinkedId() != senderId) {
                    repo.setLinkedId(senderId)
                    MqttManager.initialize(repo.getDeviceId(), senderId)
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "تم الربط تلقائياً مع جهاز آخر", Toast.LENGTH_SHORT).show()
                        startListening(context!!)
                    }
                }
                return@mqttListener
            }

            if (type == "sync_request") {
                Log.d("SyncManager", "Received sync_request from $senderId. Sending status...")
                val repo = getRepo(context!!)
                val lastType = repo.getLastStateType()
                val lastCardId = repo.getLastStateCardId()
                val lastTitle = repo.getLastStateTitle()
                
                val response = JSONObject()
                response.put("type", "sync_response")
                response.put("status", lastType)
                response.put("cardId", lastCardId ?: "")
                response.put("title", lastTitle ?: "")
                response.put("senderId", repo.getDeviceId())
                MqttManager.publish(senderId!!, response.toString())
                return@mqttListener
            }

            if (type == "sync_response") {
                val status = json.optString("status")
                val cardId = json.optString("cardId")
                val title = json.optString("title")
                Log.d("SyncManager", "Received sync_response: $status, $cardId, $title")
                
                if (!status.isNullOrBlank()) {
                    handleRemoteEvent(status, title, cardId)
                }
                return@mqttListener
            }

            val title = if (json.has("title")) json.getString("title") else null
            val cardId = if (json.has("cardId")) json.getString("cardId") else null
            
            Log.d("SyncManager", "MQTT message processed: type=$type, title=$title, cardId=$cardId")
            handleRemoteEvent(type, title, cardId)
        } catch (e: Exception) {
            Log.e("SyncManager", "Error parsing MQTT message", e)
        }
    }

    fun addListener(listener: (String, String?, String?) -> Unit) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: (String, String?, String?) -> Unit) {
        listeners.remove(listener)
    }
    
    // Prevent synchronization loops
    private var lastReceivedTimestamp: Long = 0
    private var isProcessingRemoteEvent = false

    private val _lastSyncEvent = MutableStateFlow<String?>(null)
    val lastSyncEvent: StateFlow<String?> = _lastSyncEvent

    private val _isSyncActive = MutableStateFlow(false)
    val isSyncActive: StateFlow<Boolean> = _isSyncActive

    fun getDeviceId(context: Context): String {
        val repo = getRepo(context)
        val id = repo.getDeviceId()
        val linkedId = repo.getLinkedId()
        MqttManager.initialize(id, linkedId)
        return id
    }

    fun getLinkedId(context: Context): String? {
        return getRepo(context).getLinkedId()
    }

    fun setLinkedId(context: Context, linkedId: String?) {
        val myId = getDeviceId(context)
        if (linkedId == myId) {
            Log.w("SyncManager", "Cannot link device to itself")
            return
        }
        
        getRepo(context).setLinkedId(linkedId)
        MqttManager.initialize(myId, linkedId)
        
        // Notify the other device via MQTT that we linked to it, so it can link back
        if (linkedId != null) {
            scope.launch {
                try {
                    val json = JSONObject()
                    json.put("type", "pair")
                    json.put("senderId", myId)
                    json.put("timestamp", System.currentTimeMillis())
                    MqttManager.publish(linkedId, json.toString(), true)
                } catch (e: Exception) {
                    Log.e("SyncManager", "Error sending MQTT pairing request", e)
                }
            }
        }
        
        startListening(context)
    }

    fun isLinked(context: Context): Boolean {
        return getRepo(context).isLinked()
    }

    fun startListening(context: Context) {
        val repo = getRepo(context)
        val linkedId = repo.getLinkedId()
        if (linkedId.isNullOrBlank()) {
            _isSyncActive.value = false
            return
        }
        
        val myId = repo.getDeviceId()
        Log.d("SyncManager", "Starting MQTT sync listeners for device: $myId")
        
        MqttManager.initialize(myId, linkedId)
        MqttManager.removeMessageListener(mqttListener)
        MqttManager.addMessageListener(mqttListener)
        _isSyncActive.value = true
    }

    fun publishState(context: Context, type: String, title: String?, cardId: String? = null) {
        if (isProcessingRemoteEvent) {
            Log.d("SyncManager", "Skipping publishState: triggered by remote event")
            return
        }
        
        val repo = getRepo(context)
        val linkedId = repo.getLinkedId() ?: return
        val myId = repo.getDeviceId()

        Log.d("SyncManager", "Publishing MQTT state ($type) to device $linkedId: title=$title, cardId=$cardId")
        
        // Save state locally
        repo.saveLastState(type, cardId, title)

        scope.launch {
            try {
                val json = JSONObject()
                json.put("type", type)
                json.put("senderId", myId)
                json.put("title", title ?: "")
                if (cardId != null) json.put("cardId", cardId)
                json.put("timestamp", System.currentTimeMillis())
                
                // Important events require ACK and retries
                val requireAck = type == "play" || type == "stop" || type == "completed"
                MqttManager.publish(linkedId, json.toString(), requireAck)
            } catch (e: Exception) {
                Log.e("SyncManager", "Error publishing MQTT state", e)
            }
        }
    }
    
    // Compatibility method to avoid breaking existing calls
    fun publishState(context: Context, isPlaying: Boolean, title: String?) {
        publishState(context, if (isPlaying) "play" else "stop", title)
    }
}
