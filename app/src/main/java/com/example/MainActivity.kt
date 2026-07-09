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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.QrCodeDisplayDialog
import com.example.ui.QrScannerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel(this)

        // Request notification permission automatically on startup for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Start MQTT Service
        startService(Intent(this, MqttService::class.java))
        
        setContent {
            MyApplicationTheme {
                // Force Right-to-Left layout for full Arabic interface immersive experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        QuranAppDashboard(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
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

@Composable
fun QuranAppDashboard(
    modifier: Modifier = Modifier,
    viewModel: QuranCardViewModel = viewModel()
) {
    val cards by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedCardToEdit by remember { mutableStateOf<QuranCard?>(null) }
    
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
    
    // Stable state references for callbacks to avoid stale closures
    val updatedCards = rememberUpdatedState(cards)
    val updatedViewModel = rememberUpdatedState(viewModel)
    val updatedCurrentPlayingIndex = rememberUpdatedState(currentPlayingIndex)

    DisposableEffect(Unit) {
        QuranAudioPlayer.onPlaybackStateChanged = { isPlaying, title, cardId ->
            if (!isPlaying) {
                currentPlayingIndex = -1
            }
        }
        
        QuranAudioPlayer.onPlaybackCompleted = {
            currentPlayingIndex = -1
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White,
        floatingActionButton = {
            // Smaller, elegant Floating Action Button (FAB) moved to the side as requested
            FloatingActionButton(
                onClick = {
                    selectedCardToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("add_card_fab")
                    .padding(bottom = 16.dp, end = 16.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة بطاقة جديدة", modifier = Modifier.size(20.dp))
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
                .background(Color.White)
        ) {
            // Elegant Compact Header (Saves vertical space, no useless black/dark banners)
            val isSyncActive by SyncManager.isSyncActive.collectAsState()
            val lastEvent by SyncManager.lastSyncEvent.collectAsState()
            
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSyncActive) Color(0xFFF5F5F5) else Color(0xFFFFF3E0))
                        .clickable { showLinkingDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isSyncActive) Color(0xFF4CAF50) else Color(0xFFF44336))
                            .border(1.5.dp, Color.White, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "إعدادات الربط والتحكم",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (lastEvent != null) {
                        Text(
                            text = lastEvent!!,
                            fontSize = 10.sp,
                            color = Color.Gray,
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
                        .background(Color.White)
                        .padding(horizontal = 0.dp, vertical = 0.dp)
                        .border(
                            BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .background(Color(0xFFFAFAF7), shape = RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Soft circular decorative container
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                            .clickable { showLinkingDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                            contentDescription = "ربط الأجهزة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مشغل القرآن الكريـم",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "اضغط على أي بطاقة لتشغيل وتفعيل التلاوة تلقائياً",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Gear settings icon for import/export
    IconButton(
        onClick = { showSettingsDialog = true },
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "الإعدادات والنسخ الاحتياطي",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
                }
            }
        }

            Spacer(modifier = Modifier.height(6.dp))

            // WIDE full-width section style list ("أقسام عريضة مرتبة")
            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.no_cards_yet),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.no_cards_desc),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clickable { 
                                    currentPlayingIndex = -1
                                    viewModel.stopAudio() 
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "إيقاف التلاوة",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    items(cards.indices.toList(), key = { cards[it].id }) { index ->
                        val card = cards[index]
                        QuranCardWideRowItem(
                            card = card,
                            onCardClick = {
                                currentPlayingIndex = index
                                viewModel.playAudio(context, card.reciterIdentifier, card.clipboardText, card.title, card.id.toString())
                                Toast.makeText(context, "جاري تشغيل التلاوة...", Toast.LENGTH_SHORT).show()
                            },
                            onLongClick = {
                                selectedCardForActions = card
                                showActionMenuDialog = true
                            }
                        )
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
                    viewModel.addCard(title, surahNumber, null, "green", reciter, triggerWord)
                } else {
                    viewModel.updateCard(selectedCardToEdit!!, title, surahNumber, null, "green", reciter, triggerWord)
                }
                showAddEditDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
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
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showSettingsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    Text(
                        text = "يمكنك تصدير بطاقاتك الحالية في ملف وحفظه لتتمكن من استيرادها لاحقاً في أي هاتف آخر بسهولة.",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
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
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("استيراد البطاقات (استعادة النسخة)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Close Button
                    Button(
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF5F5F5),
                            contentColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(14.dp)
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
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
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentCard.title,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.secondary,
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
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            containerColor = Color(0xFFF9F9F9),
                            contentColor = Color(0xFF444444)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            containerColor = Color(0xFFF9F9F9),
                            contentColor = Color(0xFF444444)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("حذف البطاقة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Cancel button
                    Button(
                        onClick = { showActionMenuDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFAFAFA),
                            contentColor = Color(0xFF888888)
                        ),
                        shape = RoundedCornerShape(12.dp)
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
    onCardClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_item_${card.id}")
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = 1.2.dp,
            color = Color(0xFFF2F2F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Display only the Beautiful Title clearly without any distracting subtitle/clipboard texts!
            Text(
                text = card.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Beautiful three dots action trigger (for editing, deleting, moving)
            IconButton(
                onClick = onLongClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "المزيد من الخيارات",
                    tint = Color(0xFF999999),
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
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
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) },
                            modifier = Modifier.size(28.dp).testTag("pick_audio_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "اختر ملف صوتي", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Title Input Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم البطاقة") },
                    placeholder = { Text("مثال: سورة الكهف - القارئ فلان") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color(0xFF777777)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                if (customAudioUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "تم اختيار ملف صوتي من الجهاز",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Selection (Reciter + Surah)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, Color(0xFFE5E5E5), RoundedCornerShape(14.dp))
                            .clickable { expandedReciter = true }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = reciters.find { it.identifier == selectedReciter }?.name ?: "اختر القارئ",
                            color = if (selectedReciter != null) Color.Black else Color(0xFF777777)
                        )
                        DropdownMenu(
                            expanded = expandedReciter,
                            onDismissRequest = { 
                                expandedReciter = false
                                searchReciter = ""
                            },
                            modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 300.dp)
                        ) {
                            OutlinedTextField(
                                value = searchReciter,
                                onValueChange = { searchReciter = it },
                                placeholder = { Text("بحث عن قارئ...", fontSize = 13.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            reciters.filter { it.name.contains(searchReciter, ignoreCase = true) }.forEach { reciter ->
                                DropdownMenuItem(
                                    text = { Text(reciter.name) },
                                    onClick = {
                                        selectedReciter = reciter.identifier
                                        expandedReciter = false
                                        searchReciter = ""
                                    }
                                )
                            }
                        }
                    }

                    // Surah Selection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, Color(0xFFE5E5E5), RoundedCornerShape(14.dp))
                            .clickable { expandedSurah = true }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = surahs.find { it.number.toString() == selectedSurahNumber }?.name ?: "اختر السورة",
                            color = Color.Black
                        )
                        DropdownMenu(
                            expanded = expandedSurah,
                            onDismissRequest = { 
                                expandedSurah = false
                                searchSurah = ""
                            },
                            modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 300.dp)
                        ) {
                            OutlinedTextField(
                                value = searchSurah,
                                onValueChange = { searchSurah = it },
                                placeholder = { Text("بحث عن سورة...", fontSize = 13.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            surahs.filter { it.name.contains(searchSurah, ignoreCase = true) }.forEach { surah ->
                                DropdownMenuItem(
                                    text = { Text(surah.name) },
                                    onClick = {
                                        selectedSurahNumber = surah.number.toString()
                                        expandedSurah = false
                                        searchSurah = ""
                                    }
                                )
                            }
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الإعدادات الإضافية (تشغيل عند وصول إشعار)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
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
                                shape = RoundedCornerShape(14.dp)
                            )
                            Text(
                                text = "سيتم تشغيل هذه التلاوة تلقائياً عندما يصل إشعار يحتوي على هذه الكلمة.",
                                fontSize = 11.sp,
                                color = Color.Gray,
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
                            containerColor = Color(0xFFF5F5F5),
                            contentColor = Color(0xFF555555)
                        ),
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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
        QrCodeDisplayDialog(deviceId = deviceId) { showQrCode = false }
    }
    
    if (showScanner) {
        QrScannerDialog(
            onQrScanned = { result ->
                remoteId = result
                SyncManager.setLinkedId(context, result.trim())
                showScanner = false
                Toast.makeText(context, "تم التعرف والربط تلقائياً: $result", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showScanner = false }
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .border(1.2.dp, Color(0xFF2E7D32).copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
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
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعدادات الربط",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "معرف جهازك الفريد (ثابت لجهازك):",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF1F8E9),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                        ) {
                            Text(
                                text = deviceId,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(20.dp))
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF1976D2)),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("مسح الرمز", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "أدخل معرف الجهاز الآخر للربط:",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = remoteId,
                            onValueChange = { remoteId = it },
                            placeholder = { Text("أدخل ID الشخص الآخر هنا") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2E7D32),
                                unfocusedBorderColor = Color(0xFFE5E5E5)
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
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (SyncManager.isLinked(context)) {
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
                                    contentColor = Color.Red
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
                            color = Color(0xFF999999),
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

