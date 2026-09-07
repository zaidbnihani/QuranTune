package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ImmersiveDialogEffect
import com.example.data.AiSupportManager
import com.example.data.AiSupportMessage
import com.example.data.AppIssueLog
import kotlinx.coroutines.launch

/**
 * A beautiful, elegant banner displayed at a prominent position in the main dashboard.
 * Shows:
 * 1. AI Customer Support interaction count with custom badge.
 * 2. Real-time detected issues status (Green clean check vs amber warning).
 * 3. One-tap access to open the full AI Support & Diagnostics Assistant Dialog.
 */
@Composable
fun AiSupportStatusBanner(
    onOpenSupportDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionsCount by AiSupportManager.interactionsCount.collectAsState()
    val activeIssues by AiSupportManager.activeIssues.collectAsState()

    // Periodically or on resume run diagnostic
    LaunchedEffect(Unit) {
        AiSupportManager.runDiagnostics(context)
    }

    val hasIssues = activeIssues.isNotEmpty()
    val errorCount = activeIssues.count { it.severity == AppIssueLog.IssueSeverity.ERROR }
    val warningCount = activeIssues.size - errorCount

    // Glowing subtle gradient border and container
    val containerBorderColor = if (hasIssues) {
        if (errorCount > 0) Color(0xFFE57373).copy(alpha = 0.8f) else Color(0xFFFFB74D).copy(alpha = 0.8f)
    } else {
        Color(0xFFD4AF37).copy(alpha = 0.55f)
    }

    val backgroundGradient = Brush.horizontalGradient(
        colors = if (hasIssues) {
            listOf(
                Color(0xFF331414).copy(alpha = 0.85f),
                Color(0xFF1B0B0B).copy(alpha = 0.90f)
            )
        } else {
            listOf(
                Color(0xFF0D3823).copy(alpha = 0.85f),
                Color(0xFF072416).copy(alpha = 0.90f)
            )
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundGradient)
            .border(
                BorderStroke(1.2.dp, containerBorderColor),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onOpenSupportDialog() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sparkle / Robot icon with glowing circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasIssues) Color(0xFFFFB74D).copy(alpha = 0.18f)
                        else Color(0xFFD4AF37).copy(alpha = 0.18f)
                    )
                    .border(
                        1.2.dp,
                        if (hasIssues) Color(0xFFFFB74D).copy(alpha = 0.6f)
                        else Color(0xFFD4AF37).copy(alpha = 0.6f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasIssues) Icons.Default.WarningAmber else Icons.Default.SmartToy,
                    contentDescription = "خدمة العملاء الذكية",
                    tint = if (hasIssues) Color(0xFFFFB74D) else Color(0xFFD4AF37),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info & Status
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "خدمة العملاء الذكية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Interaction Count Pill
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.8.dp, Color(0xFFD4AF37).copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$interactionsCount تواصل",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD4AF37)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Diagnostic State
                if (hasIssues) {
                    Text(
                        text = "تم رصد ${activeIssues.size} ملاحظات تشغيل: ${activeIssues.first().title}",
                        fontSize = 11.sp,
                        color = Color(0xFFFFCC80),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            text = "النظام يعمل بشكل سليم • اضغط للتحدث مع الدعم",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Chevron or Problem Indicator
            if (hasIssues) {
                Surface(
                    color = Color(0xFFFF5252).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "فحص المشكلة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A80),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "فتح",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Full Dialog providing:
 * 1. Real-time list of detected technical issues (network, player, sync, background).
 * 2. Interactive chat with Gemini-powered AI Customer Support Assistant.
 * 3. Interactions counter badge and reset diagnostics button.
 */
@Composable
fun AiCustomerSupportDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val interactionsCount by AiSupportManager.interactionsCount.collectAsState()
    val activeIssues by AiSupportManager.activeIssues.collectAsState()
    val chatMessages by AiSupportManager.chatMessages.collectAsState()
    val isSending by AiSupportManager.isSending.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when messages update
    LaunchedEffect(chatMessages.size, isSending) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ImmersiveDialogEffect()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .fillMaxHeight(0.92f)
                        .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.65f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF082215).copy(alpha = 0.98f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD4AF37).copy(alpha = 0.15f))
                                        .border(1.2.dp, Color(0xFFD4AF37), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "خدمة العملاء الذكية (AI)",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD4AF37)
                                    )
                                    Text(
                                        text = "تم التواصل: $interactionsCount مرة مع الذكاء الاصطناعي",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://t.me/Gegeeggerhddhdhhdhbot")
                                            )
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "تعذر فتح الرابط: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "فتح بوت الدعم على تليجرام",
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { AiSupportManager.runDiagnostics(context) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "إعادة فحص المشاكل",
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Issues Diagnosis Section
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (activeIssues.isNotEmpty()) Color(0xFF261010).copy(alpha = 0.9f)
                            else Color(0xFF0B2E1C).copy(alpha = 0.7f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (activeIssues.isNotEmpty()) Color(0xFFFFB74D).copy(alpha = 0.5f)
                                else Color(0xFF4CAF50).copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (activeIssues.isNotEmpty()) Icons.Default.BugReport else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (activeIssues.isNotEmpty()) Color(0xFFFFB74D) else Color(0xFF4CAF50),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (activeIssues.isNotEmpty()) "المشاكل التي يواجهها التطبيق حالياً (${activeIssues.size})"
                                            else "الفحص الذكي: لا توجد مشاكل",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeIssues.isNotEmpty()) Color(0xFFFFB74D) else Color(0xFF4CAF50)
                                        )
                                    }

                                    if (activeIssues.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    AiSupportManager.sendMessage(
                                                        context,
                                                        "كيف يمكنني حل المشاكل المكتشفة في التطبيق الآن؟"
                                                    )
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "اسأل الذكاء عن الحل",
                                                fontSize = 11.sp,
                                                color = Color(0xFFD4AF37),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (activeIssues.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    activeIssues.forEach { issue ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("• ", color = Color(0xFFFFB74D), fontSize = 12.sp)
                                            Column {
                                                Text(
                                                    text = issue.title,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = issue.details,
                                                    color = Color.White.copy(alpha = 0.75f),
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "الاتصال بالإنترنت يعمل، التلاوات جاهزة، والمزامنة مستقرة.",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Chat Messages List
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            if (chatMessages.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HeadsetMic,
                                        contentDescription = null,
                                        tint = Color(0xFFD4AF37).copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "مرحباً بك في خدمة العملاء الذكية",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "يمكنك طرح أي استفسار حول استخدام التطبيق، طريقة المزامنة، أو حل أي عطل تواجهه وسيجيبك الذكاء الاصطناعي فوراً.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.65f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Direct Telegram Support Button
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://t.me/Gegeeggerhddhdhhdhbot")
                                                )
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "تعذر فتح الرابط: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF29B6F6).copy(alpha = 0.2f),
                                            contentColor = Color(0xFF81D4FA)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF29B6F6).copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("التواصل المباشر عبر تليجرام", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Quick questions chips
                                    val quickQuestions = listOf(
                                        "لماذا لا يعمل الصوت لبعض السور؟",
                                        "كيف أربط التطبيق مع جهاز آخر؟",
                                        "كيف أضمن عمل المزامنة بالخلفية؟"
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        quickQuestions.forEach { question ->
                                            Surface(
                                                color = Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            AiSupportManager.sendMessage(context, question)
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = question,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFD4AF37),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(chatMessages, key = { it.id }) { msg ->
                                        AiChatMessageBubble(message = msg)
                                    }

                                    if (isSending) {
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Start,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = Color(0xFF0F3E26),
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            color = Color(0xFFD4AF37),
                                                            strokeWidth = 1.8.dp
                                                        )
                                                        Text(
                                                            text = "الذكاء الاصطناعي يفكر في الإجابة...",
                                                            fontSize = 12.sp,
                                                            color = Color.White.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text input field and send button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        "اكتب استفسارك أو مشكلتك هنا...",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotBlank() && !isSending) {
                                            val query = inputText
                                            inputText = ""
                                            coroutineScope.launch {
                                                AiSupportManager.sendMessage(context, query)
                                            }
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                    cursorColor = Color(0xFFD4AF37)
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank() && !isSending) {
                                        val query = inputText
                                        inputText = ""
                                        coroutineScope.launch {
                                            AiSupportManager.sendMessage(context, query)
                                        }
                                    }
                                },
                                enabled = inputText.isNotBlank() && !isSending,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (inputText.isNotBlank() && !isSending) Color(0xFFD4AF37)
                                        else Color.White.copy(alpha = 0.15f)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "إرسال",
                                    tint = if (inputText.isNotBlank() && !isSending) Color(0xFF082215)
                                    else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
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
fun AiChatMessageBubble(message: AiSupportMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isUser) Color(0xFFD4AF37).copy(alpha = 0.22f)
            else Color(0xFF0F3E26),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (message.isUser) 14.dp else 2.dp,
                bottomEnd = if (message.isUser) 2.dp else 14.dp
            ),
            border = BorderStroke(
                1.dp,
                if (message.isUser) Color(0xFFD4AF37).copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (!message.isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "خدمة العملاء (ذكاء اصطناعي)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                    }
                }
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
