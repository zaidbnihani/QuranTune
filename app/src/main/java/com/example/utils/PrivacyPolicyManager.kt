package com.example.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlin.system.exitProcess

/**
 * المفاتيح المستخدمة للتحكم بسياسة الخصوصية في SharedPreferences
 */
object PrefsKeys {
    const val PREFS_NAME = "quran_app_privacy_prefs"
    const val PRIVACY_POLICY_ACCEPTED = "privacy_policy_accepted"
    const val PRIVACY_POLICY_VERSION = "privacy_policy_version"
}

/**
 * مدير سياسة الخصوصية لفحص وحفظ موافقة المستخدم
 */
object PrivacyPolicyManager {
    // رقم نسخة سياسة الخصوصية الحالية (عند تحديث السياسة مستقبلاً، قم برفع هذا الرقم لإعادة أخذ الموافقة)
    const val CURRENT_PRIVACY_POLICY_VERSION = 1
    
    // رابط سياسة الخصوصية الخاص بالتطبيق
    const val PRIVACY_POLICY_URL = "https://quranplayerzaid.blogspot.com/2026/08/privacy-policy.html"

    /**
     * يفحص ما إذا كان المستخدم قد وافق سابقاً على النسخة الحالية لسياسة الخصوصية
     */
    fun isPrivacyPolicyAccepted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val isAccepted = prefs.getBoolean(PrefsKeys.PRIVACY_POLICY_ACCEPTED, false)
        val savedVersion = prefs.getInt(PrefsKeys.PRIVACY_POLICY_VERSION, 0)
        
        return isAccepted && savedVersion >= CURRENT_PRIVACY_POLICY_VERSION
    }

    /**
     * حفظ حالة الموافقة ورقم النسخة الحالية
     */
    fun setPrivacyPolicyAccepted(context: Context, accepted: Boolean) {
        val prefs = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(PrefsKeys.PRIVACY_POLICY_ACCEPTED, accepted)
            if (accepted) {
                putInt(PrefsKeys.PRIVACY_POLICY_VERSION, CURRENT_PRIVACY_POLICY_VERSION)
            }
            apply()
        }
    }
}

/**
 * Composable يربط حالة الفحص بالـ Dialog بحيث يُعرض مرة واحدة فقط عند الحاجة
 */
@Composable
fun PrivacyPolicyChecker(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // فحص الشروط وحفظ الحالة لمنع إعادة القراءة المتكررة أثناء الـ recomposition
    var showDialog by rememberSaveable {
        mutableStateOf(!PrivacyPolicyManager.isPrivacyPolicyAccepted(context))
    }

    if (showDialog) {
        PrivacyPolicyDialog(
            onAccept = {
                // حفظ الموافقة وإغلاق الحوار
                PrivacyPolicyManager.setPrivacyPolicyAccepted(context, true)
                showDialog = false
            },
            onDecline = {
                // عند الرفض: إغلاق التطبيق بالكامل بدون حفظ أي بيانات
                (context as? Activity)?.finishAffinity()
                exitProcess(0)
            }
        )
    }
}

/**
 * نافذة سياسة الخصوصية غير القابلة للإغلاق إلا عبر الأزرار
 */
@Composable
fun PrivacyPolicyDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // بناء النص التفاعلي الذي يحتوي على رابط سياسة الخصوصية
    val annotatedText = buildAnnotatedString {
        append("أهلاً بك في تطبيق مشغل القرآن. نحن نحترم خصوصيتك وأمان بياناتك بشدة.\n\n")
        append("هذا التطبيق لا يجمع ولا يشارك أي بيانات شخصية غير ضرورية، ويحرص على توفير تجربة آمنة ومريحة.\n\n")
        
        pushStringAnnotation(tag = "URL", annotation = PrivacyPolicyManager.PRIVACY_POLICY_URL)
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("اضغط هنا لقراءة سياسة الخصوصية الكاملة")
        }
        pop()
    }

    AlertDialog(
        // منع إغلاق النافذة عند الضغط خارجها أو زر الرجوع
        onDismissRequest = { /* لا شيء هنا لضمان عدم الإغلاق التلقائي */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "سياسة الخصوصية",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start
                    ),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    uriHandler.openUri(annotation.item)
                                } catch (e: Exception) {
                                    // احتياطي في حال عدم دعم UriHandler
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                    context.startActivity(intent)
                                }
                            }
                    }
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // زر أوافق
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "أوافق",
                        fontWeight = FontWeight.Bold
                    )
                }

                // زر رفض
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "رفض",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}
