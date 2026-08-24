package com.example.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

object SyncManager {
    private var deviceRepo: DeviceLinkRepository? = null
    private var context: Context? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val listeners = CopyOnWriteArrayList<(String, String?, String?) -> Unit>()

    private var isWaitingForPairRequest = false
    private var pairTimerJob: Job? = null

    private val _incomingPairRequest = MutableStateFlow<String?>(null)
    val incomingPairRequest: StateFlow<String?> = _incomingPairRequest

    private var incomingPairPublicKey: String? = null
    private var incomingPairIsQr: Boolean = false

    fun startWaitingForPair() {
        isWaitingForPairRequest = true
        Log.d("SyncManager", "isWaitingForPairRequest is now true for 60 seconds")
        pairTimerJob?.cancel()
        pairTimerJob = scope.launch {
            delay(60000)
            isWaitingForPairRequest = false
            Log.d("SyncManager", "isWaitingForPairRequest is now false (timed out)")
        }
    }

    fun stopWaitingForPair() {
        isWaitingForPairRequest = false
        pairTimerJob?.cancel()
        Log.d("SyncManager", "isWaitingForPairRequest is now false")
    }

    fun clearIncomingPairRequest() {
        _incomingPairRequest.value = null
    }

    fun isWaitingForPair(): Boolean = isWaitingForPairRequest

    private fun getRepo(context: Context): DeviceLinkRepository {
        this.context = context.applicationContext
        if (deviceRepo == null) {
            deviceRepo = DeviceLinkRepository(context.applicationContext)
        }
        return deviceRepo!!
    }

    private fun handleRemoteEvent(
        type: String, 
        title: String?, 
        cardId: String?,
        reciterId: String? = null,
        surahNumber: String? = null,
        youtubeUrl: String? = null
    ) {
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

        // Trigger local player state changes to mirror remote commands
        val ctx = context
        if (ctx != null) {
            if (type == "play") {
                scope.launch {
                    try {
                        val db = QuranDatabase.getDatabase(ctx)
                        val cards = db.quranCardDao().getAllCards().first()
                        val card = cards.firstOrNull { it.id.toString() == cardId } ?: cards.firstOrNull { it.title == title }
                        
                        val finalReciterId = card?.reciterIdentifier ?: reciterId
                        val finalSurahNumber = card?.clipboardText ?: surahNumber
                        val finalTitle = card?.title ?: title
                        val finalCardId = card?.id?.toString() ?: cardId
                        val finalYoutubeUrl = card?.youtubeUrl ?: youtubeUrl

                        if (finalSurahNumber != null || !finalYoutubeUrl.isNullOrBlank()) {
                            QuranAudioPlayer.playAudio(
                                context = ctx,
                                reciterId = finalReciterId,
                                surahNumber = finalSurahNumber ?: "",
                                title = finalTitle,
                                cardId = finalCardId,
                                youtubeUrl = finalYoutubeUrl,
                                shouldSync = false // Do not echo back sync event
                            )
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (!finalTitle.isNullOrBlank()) {
                                    Toast.makeText(ctx, "جاري تشغيل من الجهاز الآخر: $finalTitle", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Error handling remote playback play", e)
                    }
                }
            } else if (type == "stop") {
                QuranAudioPlayer.stopAudio(shouldSync = false)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(ctx, "إيقاف التشغيل من الجهاز الآخر", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val processedMessageIds = java.util.Collections.synchronizedSet(LinkedHashSet<String>())

    /**
     * Decodes and processes raw messages received from either Supabase Realtime or MQTT
     */
    private val genericMessageListener: (String) -> Unit = msgListener@{ rawMessage ->
        try {
            val repo = context?.let { getRepo(it) }
            val secret = repo?.getSharedSecret()

            // Attempt to decrypt if payload was encrypted
            var decryptedMessage = rawMessage
            if (secret != null) {
                val decrypted = CryptoHelper.decrypt(rawMessage, secret)
                if (decrypted != null) {
                    decryptedMessage = decrypted
                }
            }

            val json = JSONObject(decryptedMessage)
            val type = json.optString("type")
            val senderId = json.optString("senderId")
            val msgId = json.optString("messageId")
            
            if (!msgId.isNullOrEmpty()) {
                if (processedMessageIds.contains(msgId)) {
                    Log.d("SyncManager", "Ignored duplicate message ID: $msgId")
                    return@msgListener
                }
                processedMessageIds.add(msgId)
                if (processedMessageIds.size > 100) {
                    val first = processedMessageIds.iterator().next()
                    processedMessageIds.remove(first)
                }
            }
            
            if (type == "heartbeat") {
                Log.d("SyncManager", "Heartbeat received from $senderId")
                return@msgListener
            }

            if (type == "pair") {
                Log.d("SyncManager", "Received pairing request/notice from $senderId.")
                val myId = repo?.getDeviceId() ?: return@msgListener

                val qrSecret = json.optString("sharedSecret").takeIf { !it.isNullOrBlank() }
                val secretToUse = qrSecret ?: CryptoHelper.generateDeterministicSecret(myId, senderId)

                repo.setLinkedId(senderId)
                repo.setSharedSecret(secretToUse)
                MqttManager.setSharedSecret(secretToUse)
                MqttManager.initialize(myId, senderId)
                _isSyncActive.value = true

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "تم المزامنة والربط تلقائياً مع الجهاز: $senderId", Toast.LENGTH_LONG).show()
                }

                // Send pair_ack back to sender
                scope.launch {
                    try {
                        val ackJson = JSONObject()
                        ackJson.put("type", "pair_ack")
                        ackJson.put("senderId", myId)
                        ackJson.put("timestamp", System.currentTimeMillis())
                        publishRaw(senderId, ackJson.toString(), secretToUse)
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Error sending pair_ack", e)
                    }
                }
                return@msgListener
            }

            if (type == "pair_ack") {
                Log.d("SyncManager", "Received pair_ack from $senderId.")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "تم تأكيد الاتصال والمزامنة مع الجهاز: $senderId", Toast.LENGTH_SHORT).show()
                }
                return@msgListener
            }

            if (type == "sync_request") {
                Log.d("SyncManager", "Received sync_request from $senderId. Sending status...")
                val lastType = repo?.getLastStateType() ?: "stop"
                val lastCardId = repo?.getLastStateCardId()
                val lastTitle = repo?.getLastStateTitle()
                
                val response = JSONObject()
                response.put("type", "sync_response")
                response.put("status", lastType)
                response.put("cardId", lastCardId ?: "")
                response.put("title", lastTitle ?: "")
                response.put("senderId", repo?.getDeviceId() ?: "")
                publishRaw(senderId, response.toString(), secret)
                return@msgListener
            }

            if (type == "sync_response") {
                val status = json.optString("status")
                val cardId = json.optString("cardId")
                val title = json.optString("title")
                Log.d("SyncManager", "Received sync_response: $status, $cardId, $title")
                
                if (!status.isNullOrBlank()) {
                    handleRemoteEvent(status, title, cardId)
                }
                return@msgListener
            }

            val title = if (json.has("title")) json.getString("title") else null
            val cardId = if (json.has("cardId")) json.getString("cardId") else null
            val reciterId = if (json.has("reciterId")) json.getString("reciterId") else null
            val surahNumber = if (json.has("surahNumber")) json.getString("surahNumber") else null
            val youtubeUrl = if (json.has("youtubeUrl")) json.getString("youtubeUrl") else null
            
            Log.d("SyncManager", "Sync message processed: type=$type, title=$title, cardId=$cardId, reciterId=$reciterId, surahNumber=$surahNumber, youtubeUrl=$youtubeUrl")
            handleRemoteEvent(type, title, cardId, reciterId, surahNumber, youtubeUrl)
        } catch (e: Exception) {
            Log.e("SyncManager", "Error parsing sync message", e)
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
    private var isProcessingRemoteEvent = false

    private val _lastSyncEvent = MutableStateFlow<String?>(null)
    val lastSyncEvent: StateFlow<String?> = _lastSyncEvent

    private val _isSyncActive = MutableStateFlow(false)
    val isSyncActive: StateFlow<Boolean> = _isSyncActive

    fun getDeviceId(context: Context): String {
        val repo = getRepo(context)
        val id = repo.getDeviceId()
        val linkedId = repo.getLinkedId()
        MqttManager.setSharedSecret(repo.getSharedSecret())
        MqttManager.initialize(id, linkedId)
        SupabaseManager.initialize(context, id)
        return id
    }

    fun getLinkedId(context: Context): String? {
        return getRepo(context).getLinkedId()
    }

    fun setLinkedId(context: Context, linkedId: String?, sharedSecretFromQr: String? = null, keepExistingSecret: Boolean = false) {
        val myId = getDeviceId(context)
        if (linkedId == myId) {
            Log.w("SyncManager", "Cannot link device to itself")
            return
        }
        
        val repo = getRepo(context)
        repo.setLinkedId(linkedId)

        if (linkedId == null) {
            repo.setSharedSecret(null)
            MqttManager.setSharedSecret(null)
            repo.setTempPrivateKey(null)
            MqttManager.initialize(myId, null)
            _isSyncActive.value = false
            return
        }

        val secretToUse = if (keepExistingSecret) {
            repo.getSharedSecret()
        } else if (sharedSecretFromQr != null) {
            sharedSecretFromQr
        } else {
            // Generate deterministic shared secret so both devices match instantly
            CryptoHelper.generateDeterministicSecret(myId, linkedId)
        }

        repo.setSharedSecret(secretToUse)
        MqttManager.setSharedSecret(secretToUse)
        MqttManager.initialize(myId, linkedId)
        SupabaseManager.initialize(context, myId)
        _isSyncActive.value = true
        
        // Notify the other device via Supabase Realtime & MQTT that we linked to it
        scope.launch {
            try {
                val json = JSONObject()
                json.put("type", "pair")
                json.put("senderId", myId)
                json.put("sharedSecret", secretToUse)
                json.put("timestamp", System.currentTimeMillis())
                publishRaw(linkedId, json.toString(), secretToUse)
            } catch (e: Exception) {
                Log.e("SyncManager", "Error sending pairing request", e)
            }
        }
        
        startListening(context)
    }

    fun acceptIncomingPairRequest(context: Context) {
        val senderId = _incomingPairRequest.value ?: return
        _incomingPairRequest.value = null
        stopWaitingForPair()

        val repo = getRepo(context)
        val myId = getDeviceId(context)

        if (incomingPairIsQr) {
            setLinkedId(context, senderId, keepExistingSecret = true)
        } else {
            val remotePublicKey = incomingPairPublicKey
            if (!remotePublicKey.isNullOrBlank()) {
                val keyPair = CryptoHelper.generateEcKeyPair()
                if (keyPair != null) {
                    val myPublicKeyStr = CryptoHelper.getPublicKeyString(keyPair)
                    val myPrivateKeyStr = CryptoHelper.getPrivateKeyString(keyPair)
                    
                    val computedSecret = CryptoHelper.computeECDHSharedSecret(myPrivateKeyStr, remotePublicKey)
                    if (computedSecret != null) {
                        repo.setLinkedId(senderId)
                        repo.setSharedSecret(computedSecret)
                        MqttManager.setSharedSecret(computedSecret)
                        MqttManager.initialize(myId, senderId)
                        SupabaseManager.initialize(context, myId)
                        
                        scope.launch {
                            try {
                                val response = JSONObject()
                                response.put("type", "pair_accept")
                                response.put("senderId", myId)
                                response.put("publicKey", myPublicKeyStr)
                                response.put("timestamp", System.currentTimeMillis())
                                publishRaw(senderId, response.toString(), computedSecret)
                            } catch (e: Exception) {
                                Log.e("SyncManager", "Error sending pair_accept message", e)
                            }
                        }
                        
                        startListening(context)
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "تم قبول طلب المزامنة وإكمال تبادل المفاتيح الآمن بنجاح!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    fun isLinked(context: Context): Boolean {
        return getRepo(context).isLinked()
    }

    fun startListening(context: Context) {
        this.context = context.applicationContext
        val repo = getRepo(context)
        val linkedId = repo.getLinkedId()
        if (linkedId.isNullOrBlank()) {
            _isSyncActive.value = false
            return
        }
        
        val myId = repo.getDeviceId()
        Log.d("SyncManager", "Starting Realtime sync listeners for device: $myId")
        
        // Initialize Supabase Realtime listeners
        SupabaseManager.initialize(context, myId)
        SupabaseManager.removeDeviceSyncListener(genericMessageListener)
        SupabaseManager.addDeviceSyncListener(genericMessageListener)

        // Initialize MQTT as well for double redundancy
        MqttManager.setSharedSecret(repo.getSharedSecret())
        MqttManager.initialize(myId, linkedId)
        MqttManager.removeMessageListener(genericMessageListener)
        MqttManager.addMessageListener(genericMessageListener)

        _isSyncActive.value = true
    }

    private fun publishRaw(targetId: String, jsonString: String, secret: String?) {
        val payloadToSend = if (secret != null) {
            CryptoHelper.encrypt(jsonString, secret) ?: jsonString
        } else {
            jsonString
        }

        // 1. Supabase Realtime Delivery (Broadcast + Postgres CDC)
        SupabaseManager.publishDeviceSyncMessage(targetId, payloadToSend)

        // 2. MQTT Dual-delivery
        try {
            MqttManager.publish(targetId, jsonString, false)
        } catch (e: Exception) {
            Log.w("SyncManager", "MQTT publish skipped or failed", e)
        }
    }

    fun publishState(
        context: Context, 
        type: String, 
        title: String?, 
        cardId: String? = null,
        reciterId: String? = null,
        surahNumber: String? = null,
        youtubeUrl: String? = null
    ) {
        if (isProcessingRemoteEvent) {
            Log.d("SyncManager", "Skipping publishState: triggered by remote event")
            return
        }
        
        val repo = getRepo(context)
        val linkedId = repo.getLinkedId() ?: return
        val myId = repo.getDeviceId()
        val secret = repo.getSharedSecret()

        Log.d("SyncManager", "Publishing Realtime state ($type) to device $linkedId: title=$title, cardId=$cardId")
        
        // Save state locally
        repo.saveLastState(type, cardId, title)

        scope.launch {
            try {
                val json = JSONObject()
                json.put("messageId", UUID.randomUUID().toString())
                json.put("type", type)
                json.put("senderId", myId)
                json.put("title", title ?: "")
                if (cardId != null) json.put("cardId", cardId)
                if (reciterId != null) json.put("reciterId", reciterId)
                if (surahNumber != null) json.put("surahNumber", surahNumber)
                if (youtubeUrl != null) json.put("youtubeUrl", youtubeUrl)
                json.put("timestamp", System.currentTimeMillis())
                
                publishRaw(linkedId, json.toString(), secret)
            } catch (e: Exception) {
                Log.e("SyncManager", "Error publishing state", e)
            }
        }
    }
    
    // Compatibility method to avoid breaking existing calls
    fun publishState(context: Context, isPlaying: Boolean, title: String?) {
        publishState(context, if (isPlaying) "play" else "stop", title)
    }
}
