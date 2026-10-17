package com.example.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AdManager

/**
 * Dialog shown when user has spent 1 hour in the app.
 * User must watch 2 rewarded ads to continue.
 */
@Composable
fun OneHourSessionAdDialog(activity: Activity) {
    val showDialog by AdManager.showOneHourDialog.collectAsState()
    val remainingAds by AdManager.remainingHourAds.collectAsState()
    val isAdLoading by AdManager.isAdLoading.collectAsState()
    val statusMsg by AdManager.adStatusMessage.collectAsState()

    if (!showDialog) return

    Dialog(
        onDismissRequest = { /* Modal: user must complete or snooze */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 440.dp)
                        .padding(16.dp)
                        .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF072013)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD4AF37).copy(alpha = 0.18f))
                                .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Title
                        Text(
                            text = "استراحة إيمانية مباركة ⏰",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37),
                            textAlign = TextAlign.Center
                        )

                        // Message
                        Text(
                            text = "لقد قضيت ساعة كاملة في الاستماع واستخدام التطبيق، تقبل الله منا ومنكم صالح الأعمال.\n\nلمواصلة الاستماع والتشغيل، يُرجى مشاهدة إعلانين قصيرين بمكافأة لدعم استمرار هذا العمل القرآني مجاناً للجميع.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.90f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        // Remaining ads badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "متبقي لمتابعة الساعة: $remainingAds من 2 إعلان",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE0C475)
                            )
                        }

                        // Status / error message if any
                        AnimatedVisibility(visible = statusMsg != null, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = statusMsg ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFFFB74D),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Watch Ad button
                        Button(
                            onClick = {
                                AdManager.watchAdForOneHourSession(activity)
                            },
                            enabled = !isAdLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4AF37),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isAdLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تحميل الإعلان...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.OndemandVideo,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (remainingAds == 2) "مشاهدة الإعلان الأول (1/2)" else "مشاهدة الإعلان الثاني (2/2)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Snooze 5 minutes option
                        TextButton(
                            onClick = {
                                AdManager.snoozeOneHourDialog(5)
                            }
                        ) {
                            Text(
                                text = "تخطي مؤقت (تأجيل 5 دقائق)",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog shown when user has 5 cards and wants to add another.
 * "تحدي الأخضر خمس بطاقات إذا بدك تزود على كل بطاقة إعلانات"
 */
@Composable
fun GreenChallengeAdDialog(
    activity: Activity,
    onCardUnlocked: () -> Unit
) {
    val showDialog by AdManager.showGreenChallengeDialog.collectAsState()
    val extraSlots by AdManager.extraCardSlots.collectAsState()
    val isAdLoading by AdManager.isAdLoading.collectAsState()
    val statusMsg by AdManager.adStatusMessage.collectAsState()

    if (!showDialog) return

    val totalAllowed = AdManager.BASE_CARD_LIMIT + extraSlots

    Dialog(
        onDismissRequest = { AdManager.closeGreenChallengeDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.80f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 440.dp)
                        .padding(16.dp)
                        .border(1.5.dp, Color(0xFF4CAF50).copy(alpha = 0.8f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0A2B18)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Badge / Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2E7D32), Color(0xFF4CAF50))
                                    )
                                )
                                .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Title
                        Text(
                            text = "تحدي البطاقات الخضراء 🎴",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37),
                            textAlign = TextAlign.Center
                        )

                        // Description
                        Text(
                            text = "لقد بلغت الحد الأساسي المجاني ($totalAllowed بطاقات).\n\nضمن تحدي البطاقات الخضراء، يمكنك زيادة بطاقة جديدة إضافية لكل إعلان مكافأة تشاهده!",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        // Capacity display
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "السعة الحالية: $totalAllowed بطاقات (مفتوح إضافي: $extraSlots)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // Status message
                        AnimatedVisibility(visible = statusMsg != null, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = statusMsg ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFFFB74D),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Watch ad to unlock button
                        Button(
                            onClick = {
                                AdManager.watchAdForGreenChallenge(activity) {
                                    onCardUnlocked()
                                }
                            },
                            enabled = !isAdLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            if (isAdLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تحميل الإعلان...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.OndemandVideo,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = Color(0xFFD4AF37)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "مشاهدة إعلان لفتح بطاقة إضافية",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Dismiss
                        OutlinedButton(
                            onClick = { AdManager.closeGreenChallengeDialog() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("إلغاء", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overlay shown when an ad is loading for feature actions (Backup, Restore, Sync)
 */
@Composable
fun AdLoadingOverlay() {
    val isAdLoading by AdManager.isAdLoading.collectAsState()
    val showOneHourDialog by AdManager.showOneHourDialog.collectAsState()
    val showGreenChallengeDialog by AdManager.showGreenChallengeDialog.collectAsState()

    if (isAdLoading && !showOneHourDialog && !showGreenChallengeDialog) {
        Dialog(
            onDismissRequest = { /* Loading ad in progress - cannot dismiss */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF072013),
                    border = BorderStroke(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFFD4AF37),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "جاري تجهيز الإعلان...",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

