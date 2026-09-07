package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Represents a logged diagnostic event or issue detected by the app.
 */
data class AppIssueLog(
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val details: String,
    val severity: IssueSeverity = IssueSeverity.WARNING
) {
    enum class IssueSeverity {
        INFO,
        WARNING,
        ERROR
    }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

/**
 * Represents a single message in the AI Support Assistant chat.
 */
data class AiSupportMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manager handling AI Customer Support interactions and automatic system diagnostic checks.
 */
object AiSupportManager {
    private const val PREFS_NAME = "ai_support_prefs"
    private const val KEY_INTERACTIONS_COUNT = "ai_interactions_count"

    private val _interactionsCount = MutableStateFlow(0)
    val interactionsCount: StateFlow<Int> = _interactionsCount.asStateFlow()

    private val _activeIssues = MutableStateFlow<List<AppIssueLog>>(emptyList())
    val activeIssues: StateFlow<List<AppIssueLog>> = _activeIssues.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<AiSupportMessage>>(emptyList())
    val chatMessages: StateFlow<List<AiSupportMessage>> = _chatMessages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _interactionsCount.value = prefs.getInt(KEY_INTERACTIONS_COUNT, 0)
        runDiagnostics(context)
    }

    /**
     * Run real-time diagnostics on network, sync, player, and card setup.
     */
    fun runDiagnostics(context: Context) {
        val issues = mutableListOf<AppIssueLog>()

        // 1. Check Network connectivity
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val isNetworkConnected = cm?.run {
            getNetworkCapabilities(activeNetwork)?.run {
                hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        (hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            }
        } ?: false

        if (!isNetworkConnected) {
            issues.add(
                AppIssueLog(
                    title = "انقطاع الاتصال بالإنترنت",
                    details = "الجهاز غير متصل بالإنترنت، قد يتعذر تشغيل التلاوات السحابية من الخوادم ومزامنة الأجهزة.",
                    severity = AppIssueLog.IssueSeverity.ERROR
                )
            )
        }

        // 2. Check Device Linking status
        val isLinked = SyncManager.isLinked(context)
        val isSyncActive = SyncManager.isSyncActive.value
        if (isLinked && !isSyncActive) {
            issues.add(
                AppIssueLog(
                    title = "تنبيه اقتران المزامنة",
                    details = "تم تحديد جهاز مقترن ولكن الاتصال المباشر غير متصل حالياً. تأكد من اتصال كلا الجهازين بالإنترنت.",
                    severity = AppIssueLog.IssueSeverity.WARNING
                )
            )
        }

        // 3. Check Battery Optimization restriction (can affect background sync)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (isLinked && powerManager?.isIgnoringBatteryOptimizations(context.packageName) == false) {
                issues.add(
                    AppIssueLog(
                        title = "قيود البطارية على الخلفية",
                        details = "التطبيق يخضع لتحسين البطارية، مما قد يؤخر استقبال أوامر التشغيل عند قفل الشاشة.",
                        severity = AppIssueLog.IssueSeverity.INFO
                    )
                )
            }
        }

        // 4. Check last player playback issue (if any)
        val playerError = QuranAudioPlayer.lastPlaybackError.value
        if (!playerError.isNullOrBlank()) {
            issues.add(
                AppIssueLog(
                    title = "تعذر تشغيل آخر تلاوة",
                    details = playerError,
                    severity = AppIssueLog.IssueSeverity.WARNING
                )
            )
        }

        _activeIssues.value = issues
    }

    /**
     * Record an explicit playback or audio failure as detected issue.
     */
    fun reportPlaybackIssue(context: Context, errorReason: String) {
        val current = _activeIssues.value.toMutableList()
        current.removeAll { it.title == "تعذر تشغيل آخر تلاوة" }
        current.add(
            0,
            AppIssueLog(
                title = "تعذر تشغيل آخر تلاوة",
                details = errorReason,
                severity = AppIssueLog.IssueSeverity.WARNING
            )
        )
        _activeIssues.value = current
    }

    fun clearPlaybackIssue() {
        val current = _activeIssues.value.toMutableList()
        current.removeAll { it.title == "تعذر تشغيل آخر تلاوة" }
        _activeIssues.value = current
    }

    /**
     * Increment support interaction count and save to preferences.
     */
    private fun incrementInteractionCount(context: Context) {
        val newCount = _interactionsCount.value + 1
        _interactionsCount.value = newCount
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_INTERACTIONS_COUNT, newCount).apply()
    }

    /**
     * Send user message to Gemini Customer Support Assistant via REST API.
     */
    suspend fun sendMessage(context: Context, userText: String) {
        if (userText.isBlank() || _isSending.value) return

        val userMessage = AiSupportMessage(isUser = true, text = userText.trim())
        _chatMessages.value = _chatMessages.value + userMessage
        _isSending.value = true
        incrementInteractionCount(context)

        withContext(Dispatchers.IO) {
            try {
                // Refresh diagnostics before query
                withContext(Dispatchers.Main) {
                    runDiagnostics(context)
                }

                val issuesSummary = if (_activeIssues.value.isEmpty()) {
                    "لا توجد مشاكل تقنية مكتشفة حالياً في النظام، والاتصال مستقر."
                } else {
                    _activeIssues.value.joinToString(separator = "\n") { "- ${it.title}: ${it.details}" }
                }

                val isLinked = SyncManager.isLinked(context)
                val isSyncActive = SyncManager.isSyncActive.value
                val isPlaying = QuranAudioPlayer.isPlaying.value
                val playingTitle = QuranAudioPlayer.currentPlayingTitle.value ?: "لا يوجد تلاوة نشطة"

                val systemPrompt = """
                    أنت المساعد الذكي وخدمة العملاء الرسمية لتطبيق "مشغل القرآن الكريم".
                    مهمتك مساعدة المستخدم بلباقة واحترام باللغة العربية، وحل المشكلات الفنية، وتقديم التوجيهات حول ميزات التطبيق.
                    
                    حالة النظام والتطبيق الحالية في جهاز المستخدم:
                    - المشاكل الحالية المكتشفة تلقائياً:
                    $issuesSummary
                    - حالة الاقتران والربط بين الأجهزة: ${if (isLinked) "مقترن (نشط: $isSyncActive)" else "غير مقترن"}
                    - حالة التلاوة الحالية: ${if (isPlaying) "جارٍ التشغيل ($playingTitle)" else "متوقف"}
                    
                    تعليمات الإجابة:
                    1. إذا كان المستخدم يشتكي من مشكلة في الصوت أو السيرفر أو المزامنة، استخدم معلومات المشاكل المكتشفة أعلاه لمساعدته مباشرة واقترح الحل المناسب (مثل فحص الإنترنت، تغيير القارئ، أو مراجعة إعدادات البطارية).
                    2. اجعل إجابتك واضحة، مختصرة، مفيدة وبنبرة إسلامية مهذبة وودودة.
                """.trimIndent()

                val geminiApiKey = BuildConfig.GEMINI_API_KEY
                if (geminiApiKey.isBlank() || geminiApiKey.contains("your_api_key")) {
                    val fallbackResponse = generateLocalSmartResponse(userText, issuesSummary)
                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value + AiSupportMessage(
                            isUser = false,
                            text = fallbackResponse
                        )
                        _isSending.value = false
                    }
                    return@withContext
                }

                // Build contents payload for gemini-3.5-flash
                val rootJson = JSONObject()
                
                // System Instruction
                val systemContent = JSONObject()
                val systemParts = JSONArray().put(JSONObject().put("text", systemPrompt))
                systemContent.put("parts", systemParts)
                rootJson.put("systemInstruction", systemContent)

                // Conversation contents
                val contentsArray = JSONArray()
                // Take recent messages for context
                val recentMessages = _chatMessages.value.takeLast(6)
                for (msg in recentMessages) {
                    val contentObj = JSONObject()
                    contentObj.put("role", if (msg.isUser) "user" else "model")
                    val parts = JSONArray().put(JSONObject().put("text", msg.text))
                    contentObj.put("parts", parts)
                    contentsArray.put(contentObj)
                }
                rootJson.put("contents", contentsArray)

                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$geminiApiKey"
                val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBodyStr = response.body?.string()

                if (response.isSuccessful && !responseBodyStr.isNullOrBlank()) {
                    val responseJson = JSONObject(responseBodyStr)
                    val candidates = responseJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val aiReply = parts?.optJSONObject(0)?.optString("text")

                    val finalAnswer = if (!aiReply.isNullOrBlank()) {
                        aiReply.trim()
                    } else {
                        "أهلاً بك! أنا في خدمتك للمساعدة في استخدام التطبيق أو حل أي مشكلة تواجهك."
                    }

                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value + AiSupportMessage(
                            isUser = false,
                            text = finalAnswer
                        )
                    }
                } else {
                    Log.e("AiSupportManager", "Gemini API error: ${response.code} $responseBodyStr")
                    val localFallback = generateLocalSmartResponse(userText, issuesSummary)
                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value + AiSupportMessage(
                            isUser = false,
                            text = localFallback
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AiSupportManager", "Exception calling Gemini API", e)
                val localFallback = generateLocalSmartResponse(userText, "فشل الاتصال بالخادم: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    _chatMessages.value = _chatMessages.value + AiSupportMessage(
                        isUser = false,
                        text = localFallback
                    )
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isSending.value = false
                }
            }
        }
    }

    /**
     * Intelligent local fallback responses when network or API key is temporarily unavailable.
     */
    private fun generateLocalSmartResponse(userText: String, issuesSummary: String): String {
        val lower = userText.lowercase()
        return when {
            lower.contains("صوت") || lower.contains("مش شغال") || lower.contains("توقف") || lower.contains("مشكلة") -> {
                "أهلاً بك. قمت بفحص التطبيق تلقائياً، وإليك الوضع الحالي:\n$issuesSummary\n\nنصيحة سريعة: إذا تعذر تشغيل سورة معينة، يرجى تجربة تغيير القارئ للبطاقة حيث قد تكون بعض الخوادم الخارجية غير متوفرة مؤقتاً."
            }
            lower.contains("ربط") || lower.contains("مزامنة") || lower.contains("جهاز") -> {
                "بخصوص الربط والمزامنة بين الأجهزة:\nتأكد من فتح شاشة 'إعدادات الربط' ومسح رمز الـ QR أو إدخال معرف الجهاز والضغط على حفظ، مع التأكد من إبقاء الاتصال بالإنترنت نشطاً."
            }
            lower.contains("بطارية") || lower.contains("خلفية") || lower.contains("إشعار") -> {
                "لضمان استمرار عمل التطبيق في الخلفية وتلقي أوامر المزامنة دائماً، افتح إعدادات الربط واضغط على 'السماح بالعمل في الخلفية' لاستثناء التطبيق من قيود تحسين البطارية."
            }
            else -> {
                "أهلاً بك في خدمة العملاء الذكية لتطبيق مشغل القرآن الكريم! قمت بفحص حالة التطبيق، والوضع الفني كالتالي:\n$issuesSummary\n\nكيف يمكنني مساعدتك بشكل أكبر؟"
            }
        }
    }
}
