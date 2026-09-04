package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ImmersiveDialogEffect
import com.example.data.AppAnnouncement
import com.example.data.FirebaseAnnouncementManager

/**
 * Dialog prompting for the secret PIN (321465) to protect "إعدادات الربط والتحكم"
 * and unlock the administrator broadcast button.
 */
@Composable
fun AdminPinDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pinText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val verifyPin = {
        if (pinText.trim() == FirebaseAnnouncementManager.SECRET_CODE) {
            FirebaseAnnouncementManager.setAdminUnlocked(context, true)
            Toast.makeText(context, "تم التحقق بنجاح وتفعيل الميزات الإدارية", Toast.LENGTH_SHORT).show()
            onSuccess()
        } else {
            errorMessage = "رمز الحماية غير صحيح، يرجى المحاولة مرة أخرى."
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ImmersiveDialogEffect()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0A2E1C).copy(alpha = 0.98f),
                    border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .widthIn(max = 400.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with Lock Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD4AF37).copy(alpha = 0.15f))
                                .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل الحماية",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "التحقق من صلاحية الإدارة",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "لوحة نشر الرسائل محمية برمز سري خاص. يرجى إدخال الرمز السري للمتابعة:",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password Field
                        OutlinedTextField(
                            value = pinText,
                            onValueChange = {
                                pinText = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("رمز الحماية السري", color = Color.White.copy(alpha = 0.8f)) },
                            placeholder = { Text("أدخل الرمز هنا...", color = Color.White.copy(alpha = 0.4f)) },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { verifyPin() }),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "تبديل الرؤية",
                                        tint = Color(0xFFD4AF37)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = Color(0xFFD4AF37)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_pin_input")
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إلغاء", color = Color.White.copy(alpha = 0.8f))
                            }

                            Button(
                                onClick = verifyPin,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_pin_confirm_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("تأكيد", color = Color(0xFF0A2E1C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for the administrator to compose and publish an announcement to Firebase Realtime Database.
 */
@Composable
fun AdminBroadcastEditorDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ImmersiveDialogEffect()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0A2E1C).copy(alpha = 0.98f),
                    border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 460.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        // Title Bar
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
                                        .background(Color(0xFFD4AF37).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "نشر رسالة للمستخدمين",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD4AF37)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isPublishing
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White.copy(alpha = 0.7f))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "سيتم حفظ هذا النص في فايربيس، وسيظهر لجميع المستخدمين عند فتح التطبيق لمرة واحدة فقط. وإذا قمت بكتابة نص أحدث لاحقاً سيظهر لهم النص الجديد.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Message Text Area
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = {
                                Text(
                                    "اكتب هنا نص الرسالة أو التنبيه الذي تود عرضه للمستخدمين...",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 14.sp
                                )
                            },
                            minLines = 4,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = Color(0xFFD4AF37)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("broadcast_message_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isPublishing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إلغاء", color = Color.White.copy(alpha = 0.8f))
                            }

                            Button(
                                onClick = {
                                    val trimmed = messageText.trim()
                                    if (trimmed.isEmpty()) {
                                        Toast.makeText(context, "الرجاء كتابة نص أولاً", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isPublishing = true
                                    FirebaseAnnouncementManager.publishAnnouncement(context, trimmed) { success, error ->
                                        isPublishing = false
                                        if (success) {
                                            Toast.makeText(context, "تم حفظ ونشر الرسالة بنجاح", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "فشل النشر: ${error ?: "خطأ غير معروف"}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isPublishing && messageText.isNotBlank(),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("broadcast_publish_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD4AF37),
                                    disabledContainerColor = Color(0xFFD4AF37).copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isPublishing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF0A2E1C),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جارٍ الحفظ...", color = Color(0xFF0A2E1C), fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color(0xFF0A2E1C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("نشر في فايربيس", color = Color(0xFF0A2E1C), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The user-facing dialog that automatically pops up once on app start when a new announcement exists.
 * Once confirmed or dismissed, it is recorded in local storage and will never appear again
 * until a newer announcement is published.
 */
@Composable
fun BroadcastMessageDisplayDialog(
    announcement: AppAnnouncement,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ImmersiveDialogEffect()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF082215).copy(alpha = 0.98f),
                    border = BorderStroke(1.8.dp, Color(0xFFD4AF37)),
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .widthIn(max = 420.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "رسالة هامة",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = announcement.text,
                            fontSize = 15.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Confirmation Button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("announcement_ack_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "تم",
                                color = Color(0xFF0A2E1C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
