package com.example.data

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

object MqttManager {
    private const val BROKER_HOST = "broker.hivemq.com"
    private const val BROKER_PORT = 1883
    
    private var client: Mqtt5AsyncClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private val messageListeners = CopyOnWriteArrayList<(String) -> Unit>()
    private var myDeviceId: String? = null
    private var targetDeviceId: String? = null
    private var isConnecting = false
    private var wasConnected = false

    private val pendingAcks = mutableMapOf<String, JSONObject>()
    private var heartbeatJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun initialize(deviceId: String, targetId: String? = null) {
        if (client != null && myDeviceId == deviceId) {
            targetDeviceId = targetId
            val c = client
            if (c != null && !c.state.isConnected && !isConnecting) {
                Log.d("MqttManager", "Client exists but not connected during initialize, retrying...")
                connect()
            }
            return
        }
        
        myDeviceId = deviceId
        targetDeviceId = targetId
        client = MqttClient.builder()
            .useMqttVersion5()
            .identifier("quran_player_$deviceId")
            .serverHost(BROKER_HOST)
            .serverPort(BROKER_PORT)
            .automaticReconnectWithDefaultConfig()
            .simpleAuth() 
                .username("quran_user_$deviceId")
                .applySimpleAuth()
            .buildAsync()

        connect()
    }

    fun connect() {
        if (isConnecting) return
        val c = client ?: return
        if (c.state.isConnected) {
            _isConnected.value = true
            return
        }
        isConnecting = true
        
        Log.d("MqttManager", "Connecting to MQTT broker...")
        c.connectWith()
            .cleanStart(true) // Fresh session state to avoid subscription ghost states
            .keepAlive(60) // Keep-alive pings to keep TCP connection alive on mobile networks
            .sessionExpiryInterval(3600 * 24)
            .send()
            .whenComplete { _, throwable ->
                isConnecting = false
                if (throwable != null) {
                    Log.e("MqttManager", "Failed to connect", throwable)
                    _isConnected.value = false
                } else {
                    Log.d("MqttManager", "Connected successfully")
                    _isConnected.value = true
                    subscribeToMyTopic()
                    startHeartbeat()
                    
                    targetDeviceId?.let { target ->
                        val syncReq = JSONObject()
                        syncReq.put("type", "sync_request")
                        syncReq.put("senderId", myDeviceId)
                        publish(target, syncReq.toString())
                    }
                }
            }
        
        startHeartbeat()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            var tick = 0
            while (true) {
                delay(15000)
                tick++
                try {
                    val c = client
                    if (c != null) {
                        val connected = c.state.isConnected
                        _isConnected.value = connected
                        
                        if (connected) {
                            if (!wasConnected || tick % 8 == 0) {
                                Log.d("MqttManager", "Self-healing: ensuring subscription is active")
                                subscribeToMyTopic()
                                wasConnected = true
                            }
                            
                            val target = targetDeviceId
                            if (target != null) {
                                val msg = JSONObject()
                                msg.put("type", "heartbeat")
                                msg.put("deviceId", myDeviceId)
                                publish(target, msg.toString())
                            }
                        } else {
                            Log.w("MqttManager", "Self-healing: disconnected, retrying connect")
                            wasConnected = false
                            connect()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MqttManager", "Error in heartbeat self-healing check", e)
                }
            }
        }
    }

    private fun subscribeToMyTopic() {
        val deviceId = myDeviceId ?: return
        val topic = "quran/device/$deviceId"
        
        client?.subscribeWith()
            ?.topicFilter(topic)
            ?.qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                val payload = if (publish.payload.isPresent) {
                    StandardCharsets.UTF_8.decode(publish.payload.get().asReadOnlyBuffer()).toString()
                } else ""
                Log.d("MqttManager", "Received: $payload")
                
                try {
                    val json = JSONObject(payload)
                    val type = json.optString("type")
                    val messageId = json.optString("messageId")
                    
                    // Send ACK if messageId exists and it's not an ACK itself
                    if (!messageId.isNullOrEmpty() && type != "ack") {
                        sendAck(json.optString("senderId"), messageId)
                    }
                    
                    if (type == "ack") {
                        val ackId = json.optString("messageId")
                        pendingAcks.remove(ackId)
                        Log.d("MqttManager", "ACK received for $ackId")
                    } else {
                        messageListeners.forEach { it.invoke(payload) }
                    }
                } catch (e: Exception) {
                    Log.e("MqttManager", "Error processing message", e)
                }
            }
            ?.send()
    }

    private fun sendAck(senderId: String, messageId: String) {
        if (senderId.isEmpty()) return
        val ack = JSONObject()
        ack.put("type", "ack")
        ack.put("messageId", messageId)
        publish(senderId, ack.toString())
    }

    fun publish(targetId: String, message: String, requireAck: Boolean = false) {
        val topic = "quran/device/$targetId"
        
        var finalMessage = message
        if (requireAck) {
            try {
                val json = JSONObject(message)
                val msgId = UUID.randomUUID().toString()
                json.put("messageId", msgId)
                json.put("senderId", myDeviceId)
                finalMessage = json.toString()
                
                pendingAcks[msgId] = json
                // Retry logic
                scope.launch {
                    var retries = 0
                    while (pendingAcks.containsKey(msgId) && retries < 3) {
                        delay(6000)
                        if (pendingAcks.containsKey(msgId)) {
                            Log.d("MqttManager", "Retrying message $msgId (attempt ${retries + 1})...")
                            publishInternal(topic, finalMessage, true)
                            retries++
                        }
                    }
                    if (pendingAcks.containsKey(msgId)) {
                        Log.w("MqttManager", "Delivery failed for $msgId after retries")
                        pendingAcks.remove(msgId)
                    }
                }
            } catch (e: Exception) {
                Log.e("MqttManager", "Error preparing message for ACK", e)
            }
        }

        publishInternal(topic, finalMessage, requireAck)
    }

    private fun publishInternal(topic: String, message: String, useQos1: Boolean = false) {
        val c = client
        if (c == null || !c.state.isConnected) {
            Log.w("MqttManager", "Not connected, skipping publish to $topic")
            return
        }
        
        val publishBuilder = c.publishWith()
            .topic(topic)
            .payload(message.toByteArray(StandardCharsets.UTF_8))
        
        if (useQos1) {
            publishBuilder.qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
        }

        publishBuilder.send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MqttManager", "Publish failed to $topic", throwable)
                } else {
                    Log.d("MqttManager", "Sent to $topic (QoS ${if (useQos1) 1 else 0})")
                }
            }
    }

    fun addMessageListener(listener: (String) -> Unit) {
        if (!messageListeners.contains(listener)) {
            messageListeners.add(listener)
        }
    }

    fun removeMessageListener(listener: (String) -> Unit) {
        messageListeners.remove(listener)
    }
}
