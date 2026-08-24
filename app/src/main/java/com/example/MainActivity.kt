package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.ArrowDropDown
import com.example.ui.QrCodeDisplayDialog
import com.example.ui.QrScannerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.QuranAudioPlayer
import com.example.data.AudioOutputDevice
import com.example.data.QuranCard
import com.example.data.QuranCardViewModel
import com.example.data.SyncManager
import com.example.data.MqttManager
import com.example.data.MqttService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.UpdateCheckerEffect
import com.example.utils.PrivacyPolicyChecker
import android.net.Uri
import android.widget.VideoView
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun BackgroundVideoPlayer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/raw/background_video")
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier.fillMaxSize()
    )
}


class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "تم تفعيل صلاحية الإشعارات بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "لن تتمكن من استلام إشعارات التلاوة بدون الصلاحية", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemUI()

        createNotificationChannel(this)

        // Request notification permission automatically on startup for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Start MQTT Service
        try {
            androidx.core.content.ContextCompat.startForegroundService(this, Intent(this, MqttService::class.java))
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start MqttService", e)
        }
        
        // Initialize SupabaseManager for remote messages
        com.example.data.SupabaseManager.initialize(this)
        
        setContent {
            MyApplicationTheme {
                // Force Right-to-Left layout for full Arabic interface immersive experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    PrivacyPolicyChecker()
                    UpdateCheckerEffect()
                    QuranAppDashboard(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.data.SupabaseManager.destroy()
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "تنبيهات مشغل القرآن"
        val descriptionText = "قناة مخصصة لإرسال تنبيهات تشغيل سور وآيات القرآن الكريـم"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("QURAN_NOTIFICATIONS", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranAppDashboard(
    modifier: Modifier = Modifier,
    viewModel: QuranCardViewModel = viewModel()
) {
    val cards by viewModel.uiState.collectAsState()
    val currentQueue by com.example.data.QuranAudioPlayer.currentQueue.collectAsState()
    val currentQueueIndex by com.example.data.QuranAudioPlayer.currentQueueIndex.collectAsState()
    val context = LocalContext.current

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedCardToEdit by remember { mutableStateOf<QuranCard?>(null) }
    
    // Drag-and-drop reordering states
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    
    // Settings and backup dialog state
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLinkingDialog by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Ensure sync listeners are active on app startup if a device is linked
        if (SyncManager.isLinked(context)) {
            Toast.makeText(context, "جاري تفعيل المزامنة...", Toast.LENGTH_SHORT).show()
            SyncManager.startListening(context)
            // Initialize controller to force start PlaybackService and its sync listeners
            QuranAudioPlayer.initPlayer(context)
        } else {
            Toast.makeText(context, "الجهاز غير مربوط بمزامنة", Toast.LENGTH_SHORT).show()
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportCardsToUri(context, it)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importCardsFromUri(context, it) { successCount ->
                if (successCount >= 0) {
                    Toast.makeText(context, "تم استيراد $successCount بطاقات بنجاح", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فشل الاستيراد، يرجى التأكد من صحة الملف", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    // Dialog/Menu state to handle actions on long-press
    var showActionMenuDialog by remember { mutableStateOf(false) }
    var selectedCardForActions by remember { mutableStateOf<QuranCard?>(null) }

    // State for sequence playback
    var currentPlayingIndex by remember { mutableStateOf(-1) }
    var activePlayingCardId by remember { mutableStateOf<String?>(null) }
    
    // Stable state references for callbacks to avoid stale closures
    val updatedCards = rememberUpdatedState(cards)
    val updatedViewModel = rememberUpdatedState(viewModel)
    val updatedCurrentPlayingIndex = rememberUpdatedState(currentPlayingIndex)

    LaunchedEffect(cards, activePlayingCardId) {
        if (activePlayingCardId != null) {
            val index = cards.indexOfFirst { it.id.toString() == activePlayingCardId }
            if (index != -1) {
                currentPlayingIndex = index
            }
        } else {
            currentPlayingIndex = -1
        }
    }

    DisposableEffect(Unit) {
        QuranAudioPlayer.onPlaybackStateChanged = { isPlaying, title, cardId ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                activePlayingCardId = if (isPlaying) cardId else null
                if (!isPlaying) {
                    currentPlayingIndex = -1
                } else {
                    val index = updatedCards.value.indexOfFirst { 
                        it.id.toString() == cardId || (!title.isNullOrBlank() && it.title == title)
                    }
                    if (index != -1) {
                        currentPlayingIndex = index
                        activePlayingCardId = updatedCards.value[index].id.toString()
                    }
                }
            }
        }
        
        QuranAudioPlayer.onPlaybackCompleted = {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                activePlayingCardId = null
                currentPlayingIndex = -1
            }
        }
        
        onDispose {
            QuranAudioPlayer.onPlaybackStateChanged = null
            QuranAudioPlayer.onPlaybackCompleted = null
        }
    }

    DisposableEffect(Unit) {
        // Handle UI specific sync events if needed
        val listener: (String, String?, String?) -> Unit = { type, title, cardId ->
            // Informational only: Sync events are now displayed in the top status bar via SyncManager.lastSyncEvent
        }
        
        SyncManager.addListener(listener)
        
        onDispose {
            SyncManager.removeListener(listener)
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_quran_background_crescent),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        BackgroundVideoPlayer(modifier = Modifier.fillMaxSize())
        // Soft translucent dark spiritual green/teal overlay for elegant readability and contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color(0xFF071F18).copy(alpha = 0.30f),
                            Color(0xFF030D0B).copy(alpha = 0.60f)
                        )
                    )
                )
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
            // Smaller, elegant Floating Action Button (FAB) styled in Gold to match design
            FloatingActionButton(
                onClick = {
                    selectedCardToEdit = null
                    showAddEditDialog = true
                },
                containerColor = Color(0xFFD4AF37),
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("add_card_fab")
                    .padding(bottom = 16.dp, end = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة بطاقة جديدة", modifier = Modifier.size(20.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "بطاقة جديدة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Compact Header (Saves vertical space, no useless black/dark banners)
            val isSyncActive by SyncManager.isSyncActive.collectAsState()
            val lastEvent by SyncManager.lastSyncEvent.collectAsState()
            val incomingPairRequest by SyncManager.incomingPairRequest.collectAsState()
            
            if (incomingPairRequest != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { SyncManager.clearIncomingPairRequest() },
                    title = { Text("طلب ربط جهاز جديد") },
                    text = { Text("وصلك طلب ربط من جهاز جديد ($incomingPairRequest) - هل توافق على ربط هذا الجهاز والتحكم به عن بعد؟") },
                    confirmButton = {
                        Button(
                            onClick = {
                                SyncManager.acceptIncomingPairRequest(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("موافق")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                SyncManager.clearIncomingPairRequest()
                            }
                        ) {
                            Text("رفض")
                        }
                    }
                )
            }
            
            // Spacer to push content down below the camera cutout/notch in immersive fullscreen
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // --- Supabase Remote Messages Announcement Banner ---
                val remoteMessage by com.example.data.SupabaseManager.currentMessage.collectAsState()
                AnimatedVisibility(
                    visible = remoteMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    val msg = remoteMessage
                    if (msg != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFD4AF37).copy(alpha = 0.15f))
                                .border(
                                    BorderStroke(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD4AF37).copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "تنبيه مركزي",
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تنبيه مركزي (إصدار ${msg.version})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD4AF37)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.message,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            BorderStroke(1.2.dp, Color.White.copy(alpha = 0.22f)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { showLinkingDialog = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSyncActive) Color(0xFF4CAF50) else Color(0xFFF44336))
                            .border(1.5.dp, Color.White, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "إعدادات الربط والتحكم",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (lastEvent != null) {
                        Text(
                            text = lastEvent!!,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 74.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            BorderStroke(1.2.dp, Color.White.copy(alpha = 0.22f)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Soft circular decorative container
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.5.dp, Color(0xFFD4AF37), CircleShape)
                                .clickable { showLinkingDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                contentDescription = "ربط الأجهزة",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مشغل القرآن الكريـم",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            if (currentQueue.size > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "تشغيل القائمة: ",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD4AF37),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        itemsIndexed(currentQueue) { qIndex, qCard ->
                                            val isCurrent = qIndex == currentQueueIndex
                                            val textColor = if (isCurrent) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.6f)
                                            val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                            
                                            Text(
                                                text = qCard.title,
                                                fontSize = 11.sp,
                                                color = textColor,
                                                fontWeight = fontWeight
                                            )
                                            if (qIndex < currentQueue.size - 1) {
                                                Text(
                                                    text = "➔",
                                                    fontSize = 10.sp,
                                                    color = Color.White.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "اضغط على أي بطاقة لتشغيل وتفعيل التلاوة تلقائياً",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Gear settings icon for import/export
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات والنسخ الاحتياطي",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // WIDE full-width section style list ("أقسام عريضة مرتبة") with static rendering
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 540.dp)
                        .weight(1f)
                ) {
                    if (cards.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(id = R.string.no_cards_yet),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(id = R.string.no_cards_desc),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(86.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(
                                            BorderStroke(1.2.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { 
                                            currentPlayingIndex = -1
                                            viewModel.stopAudio() 
                                        }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Stop",
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "إيقاف التلاوة",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF5350)
                                        )
                                    }
                                }
                            }
                            items(cards.indices.toList(), key = { cards[it].id }) { index ->
                                val card = cards[index]
                                val isPlaying = activePlayingCardId == card.id.toString() || (currentPlayingIndex == index)
                                
                                val isCurrentlyDragged = draggedItemIndex == index
                                val translationY = if (isCurrentlyDragged) dragOffset else 0f
                                
                                val animatedScale by animateFloatAsState(
                                    targetValue = if (isCurrentlyDragged) 1.05f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "scale"
                                )
                                val animatedElevation by animateDpAsState(
                                    targetValue = if (isCurrentlyDragged) 14.dp else 0.dp,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "elevation"
                                )
                                
                                val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 86.dp.toPx() }

                                QuranCardWideRowItem(
                                    card = card,
                                    isPlaying = isPlaying,
                                    onCardClick = {
                                        currentPlayingIndex = index
                                        viewModel.playAudio(context, card.reciterIdentifier, card.clipboardText, card.title, card.id.toString(), card.youtubeUrl)
                                        Toast.makeText(context, "جاري تشغيل التلاوة...", Toast.LENGTH_SHORT).show()
                                    },
                                    onLongClick = {
                                        selectedCardForActions = card
                                        showActionMenuDialog = true
                                    },
                                    modifier = Modifier
                                        .graphicsLayer {
                                            this.translationY = translationY
                                            this.scaleX = animatedScale
                                            this.scaleY = animatedScale
                                        }
                                        .zIndex(if (isCurrentlyDragged) 10f else 1f)
                                        .shadow(animatedElevation, shape = RoundedCornerShape(16.dp))
                                        .pointerInput(index) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { _ ->
                                                    draggedItemIndex = index
                                                    dragOffset = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                    
                                                    if (dragOffset > itemHeightPx && index < cards.size - 1) {
                                                        viewModel.moveCardDown(card)
                                                        draggedItemIndex = index + 1
                                                        dragOffset -= itemHeightPx
                                                    } else if (dragOffset < -itemHeightPx && index > 0) {
                                                        viewModel.moveCardUp(card)
                                                        draggedItemIndex = index - 1
                                                        dragOffset += itemHeightPx
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedItemIndex = null
                                                    dragOffset = 0f
                                                },
                                                onDragCancel = {
                                                    draggedItemIndex = null
                                                    dragOffset = 0f
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog View
    if (showAddEditDialog) {
        AddEditCardDialogSimple(
            viewModel = viewModel,
            card = selectedCardToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { title, surahNumber, reciter, triggerWord ->
                if (selectedCardToEdit == null) {
                    viewModel.addCard(title, surahNumber, null, "green", reciter, triggerWord, null)
                } else {
                    viewModel.updateCard(selectedCardToEdit!!, title, surahNumber, null, "green", reciter, triggerWord, null)
                }
                showAddEditDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            ImmersiveDialogEffect()
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF042416).copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إعدادات النسخ الاحتياطي",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                        IconButton(onClick = { showSettingsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }

                    Text(
                        text = "يمكنك تصدير بطاقاتك الحالية في ملف وحفظه لتتمكن من استيرادها لاحقاً في أي هاتف آخر بسهولة.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 1. Export Button
                    Button(
                        onClick = {
                            showSettingsDialog = false
                            createDocumentLauncher.launch("quran_cards_backup.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD4AF37),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("تصدير البطاقات (نسخ احتياطي)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    // 2. Import Button
                    Button(
                        onClick = {
                            showSettingsDialog = false
                            openDocumentLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("استيراد البطاقات (استعادة النسخة)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // --- Supabase Remote Messages Configuration Section ---
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.15f)))

                    Text(
                        text = "نظام الرسائل المركزي (Supabase)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )

                    var supabaseUrlInput by remember { mutableStateOf(com.example.data.SupabaseManager.getSupabaseUrl(context)) }
                    var supabaseKeyInput by remember { mutableStateOf(com.example.data.SupabaseManager.getSupabaseAnonKey(context)) }
                    val isRealtimeConnected by com.example.data.SupabaseManager.isRealtimeConnected.collectAsState()
                    val errorState by com.example.data.SupabaseManager.errorState.collectAsState()

                    OutlinedTextField(
                        value = supabaseUrlInput,
                        onValueChange = { supabaseUrlInput = it },
                        label = { Text("رابط مشروع Supabase (URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedLabelColor = Color(0xFFD4AF37),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = supabaseKeyInput,
                        onValueChange = { supabaseKeyInput = it },
                        label = { Text("مفتاح المشروع العام (Anon Key)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedLabelColor = Color(0xFFD4AF37),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Connection Status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRealtimeConnected) Color(0xFF4CAF50) else Color(0xFFF44336))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRealtimeConnected) "متصل بالبث المباشر" else "غير متصل بالبث",
                                fontSize = 12.sp,
                                color = if (isRealtimeConnected) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f)
                            )
                        }

                        // Save Button
                        Button(
                            onClick = {
                                com.example.data.SupabaseManager.updateConfig(context, supabaseUrlInput, supabaseKeyInput)
                                Toast.makeText(context, "تم حفظ وتحديث اتصال Supabase", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4AF37),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ الاتصال", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (errorState != null) {
                        Text(
                            text = errorState!!,
                            color = Color(0xFFF44336),
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Close Button
                    Button(
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            contentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text("إغلاق", fontSize = 15.sp)
                    }
                }
            }
        }
    }

    if (showLinkingDialog) {
        LinkingDialog(
            onDismiss = { showLinkingDialog = false }
        )
    }

    // Options menu dialog for simple editing/deletion on long click/three dots click
    if (showActionMenuDialog && selectedCardForActions != null) {
        val currentCard = selectedCardForActions!!
        Dialog(onDismissRequest = { showActionMenuDialog = false }) {
            ImmersiveDialogEffect()
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF042416).copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "خيارات البطاقة",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentCard.title,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Edit Option
                    Button(
                        onClick = {
                            selectedCardToEdit = currentCard
                            showActionMenuDialog = false
                            showAddEditDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD4AF37),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("تعديل البطاقة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    // 2. Move Up Option
                    Button(
                        onClick = {
                            viewModel.moveCardUp(currentCard)
                            showActionMenuDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("تحريك لأعلى ترتيباً", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }

                    // 3. Move Down Option
                    Button(
                        onClick = {
                            viewModel.moveCardDown(currentCard)
                            showActionMenuDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("تحريك لأسفل ترتيباً", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }

                    // 4. Delete Option
                    Button(
                        onClick = {
                            viewModel.deleteCard(currentCard)
                            showActionMenuDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = Color(0xFFEF5350)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFEF5350))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("حذف البطاقة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Cancel button
                    Button(
                        onClick = { showActionMenuDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            contentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
    } // Closing brace for the Box
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranCardWideRowItem(
    card: QuranCard,
    isPlaying: Boolean = false,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                BorderStroke(
                    width = 1.2.dp,
                    color = if (isPlaying) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.22f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onCardClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "سحب للترتيب",
                    tint = if (isPlaying) Color(0xFFD4AF37).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "جاري التشغيل",
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                
                Text(
                    text = card.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) Color(0xFFD4AF37) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onLongClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "المزيد من الخيارات",
                    tint = if (isPlaying) Color(0xFFD4AF37).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardDialogSimple(
    viewModel: QuranCardViewModel,
    card: QuranCard?,
    onDismiss: () -> Unit,
    onSave: (title: String, surahNumber: String, reciter: String?, notificationTriggerWord: String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(card?.title ?: "") }
    var selectedSurahNumber by remember { mutableStateOf(card?.clipboardText ?: "1") }
    
    val reciters by viewModel.recitersList.collectAsState()
    val surahs by viewModel.surahsList.collectAsState()
    
    var selectedReciter by remember { mutableStateOf(card?.reciterIdentifier) }
    
    val matchedReciter = remember(selectedReciter, reciters) {
        if (selectedReciter == null) null
        else {
            reciters.find { r ->
                r.identifier == selectedReciter || r.styles.any { s -> s.serverUrl == selectedReciter }
            }
        }
    }
    val tajweedStyle = remember(matchedReciter) {
        matchedReciter?.styles?.firstOrNull { it.name.contains("مجود") || it.name.contains("تجويد") }
    }
    val tilawahStyle = remember(matchedReciter) {
        matchedReciter?.styles?.firstOrNull { !it.name.contains("مجود") && !it.name.contains("تجويد") }
            ?: matchedReciter?.styles?.firstOrNull()
    }
    val hasBothStyles = tajweedStyle != null && tilawahStyle != null
    
    var expandedReciter by remember { mutableStateOf(false) }
    var expandedSurah by remember { mutableStateOf(false) }
    
    var searchReciter by remember { mutableStateOf("") }
    var searchSurah by remember { mutableStateOf("") }

    var triggerWord by remember { mutableStateOf(card?.notificationTriggerWord ?: "") }
    var expandedSettings by remember { mutableStateOf(false) }

    var customAudioUri by remember { mutableStateOf<String?>(
        if (card?.clipboardText?.startsWith("content://") == true || card?.clipboardText?.startsWith("file://") == true) {
            card.clipboardText
        } else null
    ) }

    var imageUri by remember { mutableStateOf(card?.imageUri) }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }
            customAudioUri = it.toString()
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }
            imageUri = it.toString()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ImmersiveDialogEffect()
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .border(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF042416).copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Dialog Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (card == null) "إضافة بطاقة جديدة" else "تعديل البطاقة",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) },
                            modifier = Modifier.size(28.dp).testTag("pick_audio_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "اختر ملف صوتي", tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                // Title Input Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم البطاقة") },
                    placeholder = { Text("سيتم ملؤه تلقائياً أو اكتب الاسم هنا...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedLabelColor = Color(0xFFD4AF37),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                if (customAudioUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "تم اختيار ملف صوتي من الجهاز",
                            color = Color(0xFFD4AF37),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Selection (Reciter + Surah)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(
                                1.2.dp,
                                Color.White.copy(alpha = 0.22f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { expandedReciter = true }
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = reciters.find { it.identifier == selectedReciter || it.styles.any { s -> s.serverUrl == selectedReciter } }?.name ?: "اختر القارئ",
                                color = if (selectedReciter != null) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "فتح القائمة",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    if (hasBothStyles) {
                        var expandedStyle by remember { mutableStateOf(false) }
                        val currentStyleName = if (selectedReciter == tajweedStyle?.serverUrl) "تجويد" else "تلاوة"
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .border(
                                    1.2.dp,
                                    Color.White.copy(alpha = 0.22f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { expandedStyle = true }
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "تلاوة: ",
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = currentStyleName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "تغيير النوع",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = expandedStyle,
                                onDismissRequest = { expandedStyle = false },
                                modifier = Modifier
                                    .background(Color(0xFF042416))
                                    .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("تلاوة (مرتل)", color = Color.White) },
                                    onClick = {
                                        tilawahStyle?.let { selectedReciter = it.serverUrl }
                                        expandedStyle = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تجويد (مجود)", color = Color.White) },
                                    onClick = {
                                        tajweedStyle?.let { selectedReciter = it.serverUrl }
                                        expandedStyle = false
                                    }
                                )
                            }
                        }
                    }

                    // Surah Selection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(
                                1.2.dp,
                                Color.White.copy(alpha = 0.22f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { expandedSurah = true }
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = surahs.find { it.number.toString() == selectedSurahNumber }?.name ?: "اختر السورة",
                                color = if (selectedSurahNumber.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "فتح القائمة",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Additional Settings (Notification Trigger)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSettings = !expandedSettings }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (expandedSettings) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الإعدادات الإضافية (تشغيل عند وصول إشعار)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                    }

                    if (expandedSettings) {
                        val isListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                        if (!isListenerEnabled) {
                            Button(
                                onClick = {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    Toast.makeText(context, "الرجاء تفعيل الصلاحية ثم العودة للتطبيق", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White)
                            ) {
                                Text("تفعيل صلاحية قراءة الإشعارات", fontSize = 13.sp)
                            }
                        } else {
                            OutlinedTextField(
                                value = triggerWord,
                                onValueChange = { triggerWord = it },
                                label = { Text("كلمة مفتاحية في الإشعار") },
                                placeholder = { Text("مثال: سورة البقرة") },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                    focusedLabelColor = Color(0xFFD4AF37),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Text(
                                text = "سيتم تشغيل هذه التلاوة تلقائياً عندما يصل إشعار يحتوي على هذه الكلمة.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }
                    }
                }

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            contentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("إلغاء", fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                if (customAudioUri != null) {
                                    onSave(title, customAudioUri!!, selectedReciter, triggerWord)
                                } else {
                                    onSave(title, selectedSurahNumber, selectedReciter, triggerWord)
                                }
                            } else {
                                Toast.makeText(context, "الرجاء تعبئة العنوان", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD4AF37),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    if (expandedReciter) {
        val recitersItems = remember(reciters) {
            reciters.map { SelectionItem(it.identifier, it.name) }
        }
        FastSelectionDialog(
            title = "اختر القارئ",
            searchPlaceholder = "بحث عن قارئ...",
            items = recitersItems,
            selectedId = matchedReciter?.identifier ?: selectedReciter,
            onDismiss = { expandedReciter = false },
            onSelect = { selectedReciter = it }
        )
    }

    if (expandedSurah) {
        val surahsItems = remember(surahs) {
            surahs.map { SelectionItem(it.number.toString(), it.name) }
        }
        FastSelectionDialog(
            title = "اختر السورة",
            searchPlaceholder = "بحث عن سورة...",
            items = surahsItems,
            selectedId = selectedSurahNumber,
            onDismiss = { expandedSurah = false },
            onSelect = { selectedSurahNumber = it }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LinkingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    val deviceId = remember { SyncManager.getDeviceId(context) }
    var remoteId by remember { mutableStateOf(SyncManager.getLinkedId(context) ?: "") }
    var showQrCode by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    if (showQrCode) {
        val currentSecret = remember {
            val repo = com.example.data.DeviceLinkRepository(context)
            repo.getSharedSecret() ?: com.example.data.CryptoHelper.generateSharedSecret().also {
                repo.setSharedSecret(it)
                com.example.data.MqttManager.setSharedSecret(it)
            }
        }
        LaunchedEffect(Unit) {
            SyncManager.startWaitingForPair()
        }
        QrCodeDisplayDialog(deviceId = deviceId, sharedSecret = currentSecret) { 
            showQrCode = false 
            SyncManager.stopWaitingForPair()
        }
    }
    
    if (showScanner) {
        QrScannerDialog(
            onQrScanned = { result ->
                // النص الممسوح يكون بصيغة "deviceId|sharedSecret"
                val parts = result.trim().split("|")
                val scannedDeviceId = parts.getOrNull(0) ?: result.trim()
                val scannedSecret = parts.getOrNull(1)

                remoteId = scannedDeviceId
                SyncManager.setLinkedId(context, scannedDeviceId, scannedSecret)
                showScanner = false
                Toast.makeText(context, "تم التعرف والربط تلقائياً: $scannedDeviceId", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showScanner = false }
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        ImmersiveDialogEffect()
        val isOptimizing = remember {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                false
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF042416).copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعدادات الربط",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "معرف جهازك الفريد (ثابت لجهازك):",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = deviceId,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showQrCode = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("عرض الرمز", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    if (cameraPermissionState.status.isGranted) {
                                        showScanner = true
                                    } else {
                                        Toast.makeText(context, "طلب إذن الكاميرا...", Toast.LENGTH_SHORT).show()
                                        cameraPermissionState.launchPermissionRequest()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.2f), contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("مسح الرمز", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "أدخل معرف الجهاز الآخر للربط:",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = remoteId,
                            onValueChange = { remoteId = it },
                            placeholder = { Text("أدخل ID الشخص الآخر هنا", color = Color.White.copy(alpha = 0.4f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFFD4AF37),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                SyncManager.setLinkedId(context, remoteId.trim())
                                Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4AF37),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (SyncManager.isLinked(context)) {
                        
                        if (isOptimizing) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFFBC02D).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFBC02D).copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFFFBC02D),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "استقرار المزامنة في الخلفية",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFBC02D)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "لضمان بقاء المزامنة نشطة واستقبل إشعارات التشغيل حتى لو كان التطبيق مغلقاً لساعات، يُنصح باستثناء التطبيق من قيود تحسين البطارية.",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            lineHeight = 18.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "الرجاء البحث عن 'تحسين البطارية' في الإعدادات واستثناء التطبيق", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFBC02D),
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Text("السماح بالعمل في الخلفية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    SyncManager.setLinkedId(context, null)
                                    remoteId = ""
                                    Toast.makeText(context, "تم إلغاء الربط", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFEF5350)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("إلغاء الربط الحالي")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "عند تفعيل الربط، ستصلك إشعارات عند تشغيل الشخص الآخر لأي سورة من التطبيق.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

data class SelectionItem(
    val id: String,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastSelectionDialog(
    title: String,
    searchPlaceholder: String,
    items: List<SelectionItem>,
    selectedId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(searchQuery, items) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ImmersiveDialogEffect()
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .border(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF042416)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(searchPlaceholder, color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedLabelColor = Color(0xFFD4AF37),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredItems) { item ->
                        val isSelected = item.id == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFFD4AF37).copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelect(item.id)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFD4AF37) else Color.White
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "محدد",
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد نتائج مطابقة",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImmersiveDialogEffect() {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        if (window != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.hide(
                    android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
                )
                window.insetsController?.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
        }
        onDispose {}
    }
}


