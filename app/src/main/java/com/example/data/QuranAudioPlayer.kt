package com.example.data

import android.content.Context
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioOutputDevice {
    BLUETOOTH,
    PHONE_SPEAKER
}

object QuranAudioPlayer {
    private var appContext: Context? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    var playingCardId: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingTitle = MutableStateFlow<String?>(null)
    val currentPlayingTitle: StateFlow<String?> = _currentPlayingTitle.asStateFlow()

    private val _currentPlayingCardId = MutableStateFlow<String?>(null)
    val currentPlayingCardId: StateFlow<String?> = _currentPlayingCardId.asStateFlow()

    private var _onPlaybackStateChanged: ((Boolean, String?, String?) -> Unit)? = null
    var onPlaybackStateChanged: ((Boolean, String?, String?) -> Unit)?
        get() = _onPlaybackStateChanged
        set(value) {
            _onPlaybackStateChanged = { isPlaying, title, cardId ->
                playingCardId = if (isPlaying) cardId else null
                _isPlaying.value = isPlaying
                _currentPlayingTitle.value = if (isPlaying) title else null
                _currentPlayingCardId.value = if (isPlaying) cardId else null
                value?.invoke(isPlaying, title, cardId)
            }
        }
    var onPlaybackCompleted: (() -> Unit)? = null
    
    private val _audioOutput = MutableStateFlow(AudioOutputDevice.PHONE_SPEAKER)
    val audioOutput: StateFlow<AudioOutputDevice> = _audioOutput.asStateFlow()
    
    private val _currentQueue = MutableStateFlow<List<com.example.data.QuranCard>>(emptyList())
    val currentQueue: StateFlow<List<com.example.data.QuranCard>> = _currentQueue.asStateFlow()
    
    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()
    
    private var audioManager: AudioManager? = null
    private var deviceCallback: AudioDeviceCallback? = null
    
    private var currentListener: Player.Listener? = null

    fun getSurahName(number: Int): String {
        return when (number) {
            -1 -> "أذكار الصباح"
            -2 -> "أذكار المساء"
            -3 -> "جزء عم"
            1 -> "سورة الفاتحة"
            2 -> "سورة البقرة"
            3 -> "سورة آل عمران"
            4 -> "سورة النساء"
            5 -> "سورة المائدة"
            6 -> "سورة الأنعام"
            7 -> "سورة الأعراف"
            8 -> "سورة الأنفال"
            9 -> "سورة التوبة"
            10 -> "سورة يونس"
            11 -> "سورة هود"
            12 -> "سورة يوسف"
            13 -> "سورة الرعد"
            14 -> "سورة إبراهيم"
            15 -> "سورة الحجر"
            16 -> "سورة النحل"
            17 -> "سورة الإسراء"
            18 -> "سورة الكهف"
            19 -> "سورة مريم"
            20 -> "سورة طه"
            21 -> "سورة الأنبياء"
            22 -> "سورة الحج"
            23 -> "سورة المؤمنون"
            24 -> "سورة النور"
            25 -> "سورة الفرقان"
            26 -> "سورة الشعراء"
            27 -> "سورة النمل"
            28 -> "سورة القصص"
            29 -> "سورة العنكبوت"
            30 -> "سورة الروم"
            31 -> "سورة لقمان"
            32 -> "سورة السجدة"
            33 -> "سورة الأحزاب"
            34 -> "سورة سبأ"
            35 -> "سورة فاطر"
            36 -> "سورة يس"
            37 -> "سورة الصافات"
            38 -> "سورة ص"
            39 -> "سورة الزمر"
            40 -> "سورة غافر"
            41 -> "سورة فصلت"
            42 -> "سورة الشورى"
            43 -> "سورة الزخرف"
            44 -> "سورة الدخان"
            45 -> "سورة الجاثية"
            46 -> "سورة الأحقاف"
            47 -> "سورة محمد"
            48 -> "سورة الفتح"
            49 -> "سورة الحجرات"
            50 -> "سورة ق"
            51 -> "سورة الذاريات"
            52 -> "سورة الطور"
            53 -> "سورة النجم"
            54 -> "سورة القمر"
            55 -> "سورة الرحمن"
            56 -> "سورة الواقعة"
            57 -> "سورة الحديد"
            58 -> "سورة المجادلة"
            59 -> "سورة الحشر"
            60 -> "سورة الممتحنة"
            61 -> "سورة الصف"
            62 -> "سورة الجمعة"
            63 -> "سورة المنافقون"
            64 -> "سورة التغابن"
            65 -> "سورة الطلاق"
            66 -> "سورة التحريم"
            67 -> "سورة الملك"
            68 -> "سورة القلم"
            69 -> "سورة الحاقة"
            70 -> "سورة المعارج"
            71 -> "سورة نوح"
            72 -> "سورة الجن"
            73 -> "سورة المزمل"
            74 -> "سورة المدثر"
            75 -> "سورة القيامة"
            76 -> "سورة الإنسان"
            77 -> "سورة المرسلات"
            78 -> "سورة النبأ"
            79 -> "سورة النازعات"
            80 -> "سورة عبس"
            81 -> "سورة التكوير"
            82 -> "سورة الانفطار"
            83 -> "سورة المطففين"
            84 -> "سورة الانشقاق"
            85 -> "سورة البروج"
            86 -> "سورة الطارق"
            87 -> "سورة الأعلى"
            88 -> "سورة الغاشية"
            89 -> "سورة الفجر"
            90 -> "سورة البلد"
            91 -> "سورة الشمس"
            92 -> "سورة الليل"
            93 -> "سورة الضحى"
            94 -> "سورة الشرح"
            95 -> "سورة التين"
            96 -> "سورة العلق"
            97 -> "سورة القدر"
            98 -> "سورة البينة"
            99 -> "سورة الزلزلة"
            100 -> "سورة العاديات"
            101 -> "سورة القارعة"
            102 -> "سورة التكاثر"
            103 -> "سورة العصر"
            104 -> "سورة الهمزة"
            105 -> "سورة الفيل"
            106 -> "سورة قريش"
            107 -> "سورة الماعون"
            108 -> "سورة الكوثر"
            109 -> "سورة الكافرون"
            110 -> "سورة النصر"
            111 -> "سورة المسد"
            112 -> "سورة الإخلاص"
            113 -> "سورة الفلق"
            114 -> "سورة الناس"
            else -> "سورة $number"
        }
    }

    private fun createMediaItem(uriString: String, title: String?, reciterId: String?): MediaItem {
        val metadataBuilder = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title ?: "تلاوة القرآن الكريم")
        
        val reciterName = if (!reciterId.isNullOrBlank()) {
            val matched = builtInReciters.firstOrNull { it.identifier == reciterId || it.identifier.contains(reciterId) }
            matched?.name ?: "علي جابر"
        } else {
            "علي جابر"
        }
        metadataBuilder.setArtist(reciterName)

        return MediaItem.Builder()
            .setUri(android.net.Uri.parse(uriString))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun initPlayer(context: Context) {
        getController(context) { /* Just to initialize */ }
    }

    private fun getController(context: Context, onReady: (MediaController) -> Unit) {
        if (appContext == null) appContext = context.applicationContext
        appContext?.let { initAudioRoutingListener(it) }
        if (controller != null) {
            onReady(controller!!)
            return
        }
        if (controllerFuture == null) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        }
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                controller?.let { onReady(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun initAudioRoutingListener(context: Context) {
        if (audioManager != null) return // Already initialized
        
        val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = am
        
        updateAudioOutputState(am)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    var addedBluetooth = false
                    if (addedDevices != null) {
                        for (device in addedDevices) {
                            val type = device.type
                            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                                type == AudioDeviceInfo.TYPE_BLE_BROADCAST) {
                                addedBluetooth = true
                                break
                            }
                        }
                    }
                    
                    updateAudioOutputState(am)
                    
                    // Audio output state updated automatically
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    var removedBluetooth = false
                    if (removedDevices != null) {
                        for (device in removedDevices) {
                            val type = device.type
                            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                                type == AudioDeviceInfo.TYPE_BLE_BROADCAST) {
                                removedBluetooth = true
                                break
                            }
                        }
                    }
                    
                    // Capture playing state before updating/handling disconnection
                    val wasPlaying = controller?.isPlaying == true || playingCardId != null
                    
                    updateAudioOutputState(am)
                    
                    if (removedBluetooth) {
                        if (wasPlaying) {
                            // Automatically resume playback on the phone speaker after a tiny delay
                            // to let ExoPlayer complete its noisy pause routine first.
                            Handler(Looper.getMainLooper()).postDelayed({
                                controller?.let { player ->
                                    if (!player.isPlaying) {
                                        player.play()
                                    }
                                }
                            }, 800)
                        }
                    }
                }
            }
            deviceCallback = callback
            am.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }
    }

    private fun updateAudioOutputState(am: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var hasBluetooth = false
            for (device in devices) {
                val type = device.type
                if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    type == AudioDeviceInfo.TYPE_BLE_BROADCAST) {
                    hasBluetooth = true
                    break
                }
            }
            _audioOutput.value = if (hasBluetooth) AudioOutputDevice.BLUETOOTH else AudioOutputDevice.PHONE_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            val hasBluetooth = am.isBluetoothA2dpOn || am.isBluetoothScoOn
            _audioOutput.value = if (hasBluetooth) AudioOutputDevice.BLUETOOTH else AudioOutputDevice.PHONE_SPEAKER
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playAudio(context: Context, reciterId: String?, surahNumber: String, title: String? = null, cardId: String? = null, youtubeUrl: String? = null, shouldSync: Boolean = true) {
        val appContext = context.applicationContext
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                playAudioInternal(appContext, reciterId, surahNumber, title, cardId, youtubeUrl, shouldSync)
            }
        } else {
            playAudioInternal(appContext, reciterId, surahNumber, title, cardId, youtubeUrl, shouldSync)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun playAudioInternal(context: Context, reciterId: String?, surahNumber: String, title: String?, cardId: String?, youtubeUrl: String? = null, shouldSync: Boolean = true) {
        _currentQueue.value = emptyList()
        _currentQueueIndex.value = -1
        if (!youtubeUrl.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val videoId = com.example.utils.YoutubeUtils.extractVideoId(youtubeUrl)
                val fileName = if (videoId != null) "youtube_${videoId}.mp3" else null
                val localFile = if (fileName != null) java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), fileName) else null

                if (localFile != null && localFile.exists() && localFile.length() > 0) {
                    // Play downloaded/cached local file directly and smoothly
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onPlaybackStateChanged?.invoke(true, title, cardId)
                        if (shouldSync) SyncManager.publishState(context, "play", title, cardId, reciterId, surahNumber, youtubeUrl)
                        Toast.makeText(context, "تشغيل الملف الصوتي من الذاكرة المحلية...", Toast.LENGTH_SHORT).show()
                        getController(context) { player ->
                            try {
                                currentListener?.let { player.removeListener(it) }
                                player.stop()
                                player.clearMediaItems()
                                
                                val mediaItem = createMediaItem(localFile.toURI().toString(), title, reciterId)
                                player.setMediaItem(mediaItem)
                                player.prepare()
                                player.playWhenReady = true
                                
                                currentListener = object : Player.Listener {
                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                        if (playbackState == Player.STATE_ENDED) {
                                            QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                                            if (shouldSync) SyncManager.publishState(context, "completed", title, cardId, reciterId, surahNumber, youtubeUrl)
                                            QuranAudioPlayer.onPlaybackCompleted?.invoke()
                                            player.stop()
                                            player.clearMediaItems()
                                        }
                                    }
                                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                        Toast.makeText(context, "فشل تشغيل صوت اليوتيوب المحفوظ محلياً.", Toast.LENGTH_LONG).show()
                                        QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                                        if (shouldSync) SyncManager.publishState(context, "stop", title, cardId, reciterId, surahNumber, youtubeUrl)
                                        player.stop()
                                        player.clearMediaItems()
                                    }
                                }
                                player.addListener(currentListener!!)
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل تشغيل الصوت: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    // Not downloaded yet, download it once
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onPlaybackStateChanged?.invoke(true, title, cardId)
                        if (shouldSync) SyncManager.publishState(context, "play", title, cardId, reciterId, surahNumber, youtubeUrl)
                        Toast.makeText(context, "جاري استخراج صوت اليوتيوب...", Toast.LENGTH_SHORT).show()
                    }
                    try {
                        val streamUrl = com.example.utils.YoutubeUtils.fetchAudioStreamUrl(youtubeUrl)
                        if (!streamUrl.isNullOrBlank()) {
                            if (localFile != null) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    Toast.makeText(context, "جاري تحميل وتخزين الملف لمرة واحدة للاستماع السلس لاحقاً...", Toast.LENGTH_LONG).show()
                                }
                                val success = com.example.utils.YoutubeUtils.downloadFile(streamUrl, localFile)
                                if (success && localFile.exists() && localFile.length() > 0) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        Toast.makeText(context, "اكتمل التحميل بنجاح! جاري التشغيل المباشر...", Toast.LENGTH_SHORT).show()
                                        getController(context) { player ->
                                            try {
                                                currentListener?.let { player.removeListener(it) }
                                                player.stop()
                                                player.clearMediaItems()
                                                
                                                val mediaItem = createMediaItem(localFile.toURI().toString(), title, reciterId)
                                                player.setMediaItem(mediaItem)
                                                player.prepare()
                                                player.playWhenReady = true
                                                
                                                currentListener = object : Player.Listener {
                                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                                        if (playbackState == Player.STATE_ENDED) {
                                                            QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                                                            if (shouldSync) SyncManager.publishState(context, "completed", title, cardId, reciterId, surahNumber, youtubeUrl)
                                                            QuranAudioPlayer.onPlaybackCompleted?.invoke()
                                                            player.stop()
                                                            player.clearMediaItems()
                                                        }
                                                    }
                                                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                                        Toast.makeText(context, "فشل تشغيل ملف اليوتيوب المحمل.", Toast.LENGTH_LONG).show()
                                                        QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                                                        if (shouldSync) SyncManager.publishState(context, "stop", title, cardId, reciterId, surahNumber, youtubeUrl)
                                                        player.stop()
                                                        player.clearMediaItems()
                                                    }
                                                }
                                                player.addListener(currentListener!!)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "فشل تشغيل يوتيوب: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    // Fallback: Streaming directly
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        Toast.makeText(context, "جاري تشغيل البث المباشر...", Toast.LENGTH_SHORT).show()
                                        getController(context) { player ->
                                            try {
                                                currentListener?.let { player.removeListener(it) }
                                                player.stop()
                                                player.clearMediaItems()
                                                
                                                val mediaItem = createMediaItem(streamUrl, title, reciterId)
                                                player.setMediaItem(mediaItem)
                                                player.prepare()
                                                player.playWhenReady = true
                                                
                                                currentListener = object : Player.Listener {
                                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                                        if (playbackState == Player.STATE_ENDED) {
                                                            QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                                                            if (shouldSync) SyncManager.publishState(context, "completed", title, cardId, reciterId, surahNumber, youtubeUrl)
                                                            QuranAudioPlayer.onPlaybackCompleted?.invoke()
                                                            player.stop()
                                                            player.clearMediaItems()
                                                        }
                                                    }
                                                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                                        Toast.makeText(context, "فشل تشغيل البث المباشر لصوت اليوتيوب.", Toast.LENGTH_LONG).show()
                                                        QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                                                        if (shouldSync) SyncManager.publishState(context, "stop", title, cardId, reciterId, surahNumber, youtubeUrl)
                                                        player.stop()
                                                        player.clearMediaItems()
                                                    }
                                                }
                                                player.addListener(currentListener!!)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "فشل تشغيل يوتيوب: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                Toast.makeText(context, "عذراً، فشل استخراج صوت اليوتيوب.", Toast.LENGTH_LONG).show()
                                onPlaybackStateChanged?.invoke(false, title, cardId)
                                if (shouldSync) SyncManager.publishState(context, "stop", title, cardId, reciterId, surahNumber, youtubeUrl)
                            }
                        }
                    } catch(e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            onPlaybackStateChanged?.invoke(false, title, cardId)
                            if (shouldSync) SyncManager.publishState(context, "stop", title, cardId, reciterId, surahNumber, youtubeUrl)
                        }
                    }
                }
            }
            return
        }

        val isCustomUri = surahNumber.startsWith("content://") || surahNumber.startsWith("file://")
        val validSurah = surahNumber.toIntOrNull()
        if (validSurah == null && !isCustomUri) {
            Toast.makeText(context, "الرجاء تعديل البطاقة واختيار السورة الصحيحة", Toast.LENGTH_LONG).show()
            return
        }
        
        if (!isCustomUri && validSurah !in 1..114 && validSurah !in -3..-1) {
            Toast.makeText(context, "الرجاء تعديل البطاقة واختيار السورة الصحيحة", Toast.LENGTH_LONG).show()
            return
        }

        onPlaybackStateChanged?.invoke(true, title, cardId)
        if (shouldSync) SyncManager.publishState(context, "play", title, cardId, reciterId, surahNumber, youtubeUrl)
        
        CoroutineScope(Dispatchers.IO).launch {
            val getLocalFile: (String) -> java.io.File? = { fileName ->
                val file = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), fileName)
                if (file.exists() && file.length() > 0) file else null
            }
            
            val mediaItems = mutableListOf<MediaItem>()
            
            if (isCustomUri) {
                mediaItems.add(createMediaItem(surahNumber, title, reciterId))
            } else if (validSurah == -1) {
                val local = getLocalFile("athkar_sabah.mp3")
                val uri = if (local != null) local.toURI().toString() else "https://backup.qurango.net/radio/athkar_sabah"
                mediaItems.add(createMediaItem(uri, "أذكار الصباح", reciterId))
            } else if (validSurah == -2) {
                val local = getLocalFile("athkar_masa.mp3")
                val uri = if (local != null) local.toURI().toString() else "https://backup.qurango.net/radio/athkar_masa"
                mediaItems.add(createMediaItem(uri, "أذكار المساء", reciterId))
            } else if (validSurah == -3) {
                var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                val baseServer = if (server.endsWith("/")) server else "$server/"
                for (s in 78..114) {
                    val surahTitle = getSurahName(s)
                    val local = getLocalFile("quran_${reciterId.hashCode()}_${s}.mp3")
                    val uri = if (local != null) local.toURI().toString() else {
                        val formattedSurah = String.format(java.util.Locale.US, "%03d", s)
                        "$baseServer$formattedSurah.mp3"
                    }
                    mediaItems.add(createMediaItem(uri, surahTitle, reciterId))
                }
            } else if (validSurah != null && validSurah in 1..114) {
                val local = getLocalFile("quran_${reciterId.hashCode()}_${validSurah}.mp3")
                val uri = if (local != null) {
                    local.toURI().toString()
                } else {
                    var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                    if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                    val baseServer = if (server.endsWith("/")) server else "$server/"
                    val formattedSurah = String.format(java.util.Locale.US, "%03d", validSurah)
                    "$baseServer$formattedSurah.mp3"
                }
                mediaItems.add(createMediaItem(uri, title ?: getSurahName(validSurah), reciterId))
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                getController(context) { player ->
                    try {
                        currentListener?.let { player.removeListener(it) }
                        player.stop()
                        player.clearMediaItems()
                        
                        player.setMediaItems(mediaItems)
                        player.prepare()
                        player.playWhenReady = true
                        
                        currentListener = object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                                    if (shouldSync) SyncManager.publishState(context, "completed", title, cardId, reciterId, surahNumber, youtubeUrl)
                                    QuranAudioPlayer.onPlaybackCompleted?.invoke()
                                    player.stop()
                                    player.clearMediaItems()
                                }
                            }
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                super.onMediaItemTransition(mediaItem, reason)
                                mediaItem?.mediaMetadata?.title?.toString()?.let { currentTitle ->
                                    QuranAudioPlayer.onPlaybackStateChanged?.invoke(true, currentTitle, cardId)
                                    if (shouldSync) SyncManager.publishState(context, "play", currentTitle, cardId, reciterId, surahNumber, youtubeUrl)
                                }
                            }
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                Toast.makeText(context, "فشل تشغيل الصوت، قد يكون القارئ غير متوفر لهذه السورة.", Toast.LENGTH_LONG).show()
                                QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, title, cardId)
                                if (shouldSync) SyncManager.publishState(context, "stop", title, cardId, reciterId, surahNumber, youtubeUrl)
                                player.stop()
                                player.clearMediaItems()
                            }
                        }
                        player.addListener(currentListener!!)
                    } catch (e: Exception) {
                        Toast.makeText(context, "فشل تشغيل الصوت: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun getRemainingTime(): String? {
        val player = controller ?: return null
        if (!player.isPlaying) return null
        val duration = player.duration
        val currentPosition = player.currentPosition
        if (duration < 0 || currentPosition < 0) return null
        
        val remainingMs = duration - currentPosition
        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remainingMs)
        val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playSequence(context: Context, cards: List<com.example.data.QuranCard>) {
        val appContext = context.applicationContext
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                playSequenceInternal(appContext, cards)
            }
        } else {
            playSequenceInternal(appContext, cards)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun playSequenceInternal(context: Context, cards: List<com.example.data.QuranCard>) {
        if (cards.isEmpty()) return
        
        // Update current queue state instantly for UI responsiveness
        _currentQueue.value = cards
        _currentQueueIndex.value = 0

        val appContext = context.applicationContext
        
        CoroutineScope(Dispatchers.IO).launch {
            val mediaItems = mutableListOf<MediaItem>()
            val resolvedCards = mutableListOf<com.example.data.QuranCard>()
            
            val getLocalFile: (String) -> java.io.File? = { fileName ->
                val file = java.io.File(appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), fileName)
                if (file.exists() && file.length() > 0) file else null
            }

            for (card in cards) {
                try {
                    val surahNumber = card.clipboardText
                    val reciterId = card.reciterIdentifier
                    val youtubeUrl = card.youtubeUrl

                    if (!youtubeUrl.isNullOrBlank()) {
                        // YouTube handling
                        val videoId = com.example.utils.YoutubeUtils.extractVideoId(youtubeUrl)
                        val fileName = if (videoId != null) "youtube_${videoId}.mp3" else null
                        val localFile = if (fileName != null) java.io.File(appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), fileName) else null

                        if (localFile != null && localFile.exists() && localFile.length() > 0) {
                            mediaItems.add(createMediaItem(localFile.toURI().toString(), card.title, reciterId))
                            resolvedCards.add(card)
                        } else {
                            val streamUrl = com.example.utils.YoutubeUtils.fetchAudioStreamUrl(youtubeUrl)
                            if (!streamUrl.isNullOrBlank()) {
                                mediaItems.add(createMediaItem(streamUrl, card.title, reciterId))
                                resolvedCards.add(card)
                            } else {
                                android.util.Log.e("QuranAudioPlayer", "Could not resolve stream URL for YouTube card: ${card.title}")
                            }
                        }
                    } else {
                        // Regular Card handling
                        val isCustomUri = surahNumber.startsWith("content://") || surahNumber.startsWith("file://")
                        val validSurah = surahNumber.toIntOrNull()

                        if (isCustomUri) {
                            mediaItems.add(createMediaItem(surahNumber, card.title, reciterId))
                            resolvedCards.add(card)
                        } else if (validSurah == -1) {
                            val local = getLocalFile("athkar_sabah.mp3")
                            val uri = if (local != null) local.toURI().toString() else "https://backup.qurango.net/radio/athkar_sabah"
                            mediaItems.add(createMediaItem(uri, "أذكار الصباح", reciterId))
                            resolvedCards.add(card)
                        } else if (validSurah == -2) {
                            val local = getLocalFile("athkar_masa.mp3")
                            val uri = if (local != null) local.toURI().toString() else "https://backup.qurango.net/radio/athkar_masa"
                            mediaItems.add(createMediaItem(uri, "أذكار المساء", reciterId))
                            resolvedCards.add(card)
                        } else if (validSurah == -3) {
                            var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                            if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                            val baseServer = if (server.endsWith("/")) server else "$server/"
                            for (s in 78..114) {
                                val local = getLocalFile("quran_${reciterId.hashCode()}_${s}.mp3")
                                val uri = if (local != null) local.toURI().toString() else {
                                    val formattedSurah = String.format(java.util.Locale.US, "%03d", s)
                                    "$baseServer$formattedSurah.mp3"
                                }
                                mediaItems.add(createMediaItem(uri, getSurahName(s), reciterId))
                                resolvedCards.add(card)
                            }
                        } else if (validSurah != null && validSurah in 1..114) {
                            val local = getLocalFile("quran_${reciterId.hashCode()}_${validSurah}.mp3")
                            val uri = if (local != null) {
                                local.toURI().toString()
                            } else {
                                var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                                if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                                val baseServer = if (server.endsWith("/")) server else "$server/"
                                val formattedSurah = String.format(java.util.Locale.US, "%03d", validSurah)
                                "$baseServer$formattedSurah.mp3"
                            }
                            mediaItems.add(createMediaItem(uri, card.title, reciterId))
                            resolvedCards.add(card)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QuranAudioPlayer", "Error resolving card: ${card.title}", e)
                }
            }

            if (mediaItems.isEmpty()) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(appContext, "لم يتم العثور على أي مقاطع صالحة للتشغيل.", Toast.LENGTH_LONG).show()
                    _currentQueue.value = emptyList()
                    _currentQueueIndex.value = -1
                }
                return@launch
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                _currentQueue.value = resolvedCards
                _currentQueueIndex.value = 0

                val firstCard = resolvedCards.firstOrNull()
                val sequenceTitle = if (resolvedCards.size > 1) "${resolvedCards.first().title} + ${resolvedCards.size - 1} أخرى" else resolvedCards.firstOrNull()?.title ?: "قائمة تشغيل"
                val firstCardId = resolvedCards.firstOrNull()?.id?.toString()

                onPlaybackStateChanged?.invoke(true, sequenceTitle, firstCardId)
                SyncManager.publishState(appContext, "play", sequenceTitle, firstCardId, firstCard?.reciterIdentifier, firstCard?.clipboardText, firstCard?.youtubeUrl)

                getController(appContext) { player ->
                    try {
                        currentListener?.let { player.removeListener(it) }
                        player.stop()
                        player.clearMediaItems()

                        player.setMediaItems(mediaItems)
                        player.prepare()
                        player.playWhenReady = true

                        currentListener = object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    if (!player.hasNextMediaItem()) {
                                        QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, null, null)
                                        SyncManager.publishState(appContext, "completed", null, null)
                                        QuranAudioPlayer.onPlaybackCompleted?.invoke()
                                        player.stop()
                                        player.clearMediaItems()
                                        _currentQueue.value = emptyList()
                                        _currentQueueIndex.value = -1
                                    }
                                }
                            }

                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                super.onMediaItemTransition(mediaItem, reason)
                                mediaItem?.mediaMetadata?.title?.toString()?.let { currentTitle ->
                                    val index = resolvedCards.indexOfFirst { it.title == currentTitle }
                                    if (index != -1) {
                                        _currentQueueIndex.value = index
                                    }
                                    val matchingCard = resolvedCards.getOrNull(index) ?: firstCard
                                    QuranAudioPlayer.onPlaybackStateChanged?.invoke(true, currentTitle, matchingCard?.id?.toString())
                                    SyncManager.publishState(
                                        appContext,
                                        "play",
                                        currentTitle,
                                        matchingCard?.id?.toString(),
                                        matchingCard?.reciterIdentifier,
                                        matchingCard?.clipboardText,
                                        matchingCard?.youtubeUrl
                                    )
                                }
                            }

                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                Toast.makeText(appContext, "فشل تشغيل أحد المقاطع، جاري الانتقال للتالي...", Toast.LENGTH_SHORT).show()
                                if (player.hasNextMediaItem()) {
                                    player.seekToNext()
                                    player.prepare()
                                    player.play()
                                } else {
                                    QuranAudioPlayer.onPlaybackStateChanged?.invoke(false, null, null)
                                    SyncManager.publishState(appContext, "stop", null)
                                    player.stop()
                                    player.clearMediaItems()
                                    _currentQueue.value = emptyList()
                                    _currentQueueIndex.value = -1
                                }
                            }
                        }
                        player.addListener(currentListener!!)
                    } catch (e: Exception) {
                        Toast.makeText(appContext, "فشل تشغيل القائمة المتتابعة", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun stopAudio(shouldSync: Boolean = true) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                stopAudioInternal(shouldSync)
            }
        } else {
            stopAudioInternal(shouldSync)
        }
    }
    
    private fun stopAudioInternal(shouldSync: Boolean) {
        _currentQueue.value = emptyList()
        _currentQueueIndex.value = -1
        onPlaybackStateChanged?.invoke(false, null, null)
        if (shouldSync) {
            appContext?.let { 
                SyncManager.publishState(it, "stop", null)
            }
        }
        
        currentListener?.let { controller?.removeListener(it) }
        currentListener = null
        
        controller?.stop()
        controller?.clearMediaItems()
    }
}
