package com.example.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * AdManager manages AdMob ads:
 * - App Open Ad: ca-app-pub-7070370807262419/6523542636 (with official sample test fallback)
 * - Rewarded Ad: ca-app-pub-7070370807262419/6935351033 (with official sample test fallback)
 * - Persistent caching & exponential backoff retries so ads ALWAYS load
 * - Network-aware reloads on connection recovery
 * - 1-hour session periodic ad
 * - Green cards challenge limits
 * - VIP Ad-Free toggle
 */
object AdManager {
    private const val TAG = "AdManager"

    const val ADMOB_APP_ID = "ca-app-pub-7070370807262419~9463557437"
    const val APP_OPEN_AD_ID = "ca-app-pub-7070370807262419/6523542636"
    const val REWARDED_AD_ID = "ca-app-pub-7070370807262419/6935351033"

    // Official Google AdMob test IDs for fallback if production returns NO_FILL (code 3)
    private const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"

    private const val PREFS_NAME = "quran_ad_prefs"
    private const val KEY_VIP_AD_FREE = "is_vip_ad_free"
    private const val KEY_EXTRA_CARD_SLOTS = "extra_card_slots"
    private const val KEY_SESSION_ACTIVE_SECONDS = "session_active_seconds"

    const val BASE_CARD_LIMIT = 5
    const val ONE_HOUR_SECONDS = 3600L // 1 hour

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null
    private var appOpenRetryJob: Job? = null
    private var rewardedRetryJob: Job? = null
    private var adLoadingTimeoutJob: Job? = null

    private var appOpenRetryDelay = 5000L
    private var rewardedRetryDelay = 5000L

    // VIP Ad-Free state (no ads, unlimited cards, no 1-hour interruption)
    private val _isVipAdFree = MutableStateFlow(false)
    val isVipAdFree: StateFlow<Boolean> = _isVipAdFree.asStateFlow()

    // Extra card slots unlocked by watching rewarded ads (starts at 0)
    private val _extraCardSlots = MutableStateFlow(0)
    val extraCardSlots: StateFlow<Int> = _extraCardSlots.asStateFlow()

    // Active session time (in seconds)
    private val _sessionActiveSeconds = MutableStateFlow(0L)
    val sessionActiveSeconds: StateFlow<Long> = _sessionActiveSeconds.asStateFlow()

    // 1-Hour dialog state & remaining ads needed (user must watch 2 ads)
    private val _showOneHourDialog = MutableStateFlow(false)
    val showOneHourDialog: StateFlow<Boolean> = _showOneHourDialog.asStateFlow()

    private val _remainingHourAds = MutableStateFlow(2)
    val remainingHourAds: StateFlow<Int> = _remainingHourAds.asStateFlow()

    // Green Challenge Ad Dialog (when trying to add a card beyond limit)
    private val _showGreenChallengeDialog = MutableStateFlow(false)
    val showGreenChallengeDialog: StateFlow<Boolean> = _showGreenChallengeDialog.asStateFlow()

    // Loading & feedback states
    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    private val _adStatusMessage = MutableStateFlow<String?>(null)
    val adStatusMessage: StateFlow<String?> = _adStatusMessage.asStateFlow()

    // Cached ads
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenAdLoading = false
    private var isShowingAppOpenAd = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedAdLoading = false

    private var isAppInForeground = false
    private var currentForegroundActivity: Activity? = null
    private var hasShownInitialAppOpenAd = false
    private var isNetworkRegistered = false

    val isBuildAdFree: Boolean
        get() = try {
            BuildConfig.IS_DEFAULT_AD_FREE
        } catch (_: Exception) {
            false
        }

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Initialize VIP state from preferences
        _isVipAdFree.value = prefs?.getBoolean(KEY_VIP_AD_FREE, false) ?: false

        val savedSlots = prefs?.getInt(KEY_EXTRA_CARD_SLOTS, 0) ?: 0
        _extraCardSlots.value = savedSlots

        val savedSessionSeconds = prefs?.getLong(KEY_SESSION_ACTIVE_SECONDS, 0L) ?: 0L
        _sessionActiveSeconds.value = savedSessionSeconds

        // Initialize Mobile Ads SDK
        try {
            MobileAds.initialize(appContext) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: $status")
                if (!_isVipAdFree.value) {
                    loadAppOpenAd(appContext)
                    loadRewardedAd(appContext)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds: ${e.message}")
        }

        // Preload immediately if not VIP
        if (!_isVipAdFree.value) {
            loadAppOpenAd(appContext)
            loadRewardedAd(appContext)
        }

        startSessionTimer()
        registerNetworkCallback(appContext)
    }

    private fun registerNetworkCallback(context: Context) {
        if (isNetworkRegistered) return
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Internet became available -> reloading ads if needed")
                    if (!_isVipAdFree.value) {
                        scope.launch {
                            if (appOpenAd == null && !isAppOpenAdLoading) {
                                loadAppOpenAd(context)
                            }
                            if (rewardedAd == null && !isRewardedAdLoading) {
                                loadRewardedAd(context)
                            }
                        }
                    }
                }
            })
            isNetworkRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Network callback registration failed: ${e.message}")
        }
    }

    fun onResume(activity: Activity) {
        isAppInForeground = true
        currentForegroundActivity = activity

        if (!_isVipAdFree.value) {
            // Display App Open Ad upon opening the app
            if (appOpenAd != null) {
                hasShownInitialAppOpenAd = true
                showAppOpenAdIfAvailable(activity)
            } else if (!isAppOpenAdLoading) {
                loadAppOpenAd(activity.applicationContext) {
                    if (!hasShownInitialAppOpenAd && currentForegroundActivity != null) {
                        hasShownInitialAppOpenAd = true
                        showAppOpenAdIfAvailable(activity)
                    }
                }
            }

            // Preload rewarded ad in background
            if (rewardedAd == null && !isRewardedAdLoading) {
                loadRewardedAd(activity.applicationContext)
            }
        }
    }

    fun onPause() {
        isAppInForeground = false
        currentForegroundActivity = null
        prefs?.edit()?.putLong(KEY_SESSION_ACTIVE_SECONDS, _sessionActiveSeconds.value)?.apply()
    }

    private fun startSessionTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(60000L) // Check every minute
                if (isAppInForeground && !_isVipAdFree.value) {
                    val current = _sessionActiveSeconds.value + 60L
                    _sessionActiveSeconds.value = current

                    // After 1 hour, trigger ad
                    if (current >= ONE_HOUR_SECONDS) {
                        _sessionActiveSeconds.value = 0L // Reset timer
                        prefs?.edit()?.putLong(KEY_SESSION_ACTIVE_SECONDS, 0L)?.apply()

                        val act = currentForegroundActivity
                        if (act != null && !act.isFinishing) {
                            if (appOpenAd != null) {
                                showAppOpenAdIfAvailable(act)
                            } else if (rewardedAd != null) {
                                showRewardedAd(act, onRewardEarned = {}, onDismissedOrFailed = {})
                            } else {
                                loadAppOpenAd(act.applicationContext)
                                loadRewardedAd(act.applicationContext)
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadAppOpenAd(context: Context, useFallbackId: Boolean = false, onLoaded: (() -> Unit)? = null) {
        if (_isVipAdFree.value || isAppOpenAdLoading) {
            onLoaded?.invoke()
            return
        }

        // If we already have a loaded ad
        if (appOpenAd != null) {
            onLoaded?.invoke()
            val act = currentForegroundActivity
            if (act != null && !act.isFinishing && !hasShownInitialAppOpenAd && !_isVipAdFree.value) {
                hasShownInitialAppOpenAd = true
                showAppOpenAdIfAvailable(act)
            }
            return
        }

        isAppOpenAdLoading = true
        val adUnitId = if (useFallbackId) TEST_APP_OPEN_AD_ID else APP_OPEN_AD_ID
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenAdLoading = false
                    appOpenRetryDelay = 5000L // Reset backoff
                    Log.d(TAG, "AppOpenAd loaded successfully (id=$adUnitId)")
                    onLoaded?.invoke()

                    // Immediately show App Open Ad upon opening app if activity is in foreground
                    val act = currentForegroundActivity
                    if (act != null && !act.isFinishing && !hasShownInitialAppOpenAd && !_isVipAdFree.value) {
                        hasShownInitialAppOpenAd = true
                        showAppOpenAdIfAvailable(act)
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isAppOpenAdLoading = false
                    Log.w(TAG, "AppOpenAd failed to load ($adUnitId): code=${loadAdError.code}, msg=${loadAdError.message}")

                    // If production returned NO_FILL and we haven't tried fallback yet, try fallback immediately
                    if (!useFallbackId && loadAdError.code == AdRequest.ERROR_CODE_NO_FILL) {
                        Log.d(TAG, "AppOpenAd NO_FILL: trying test ad unit fallback immediately")
                        loadAppOpenAd(context, useFallbackId = true, onLoaded = onLoaded)
                        return
                    }

                    // Otherwise schedule exponential backoff retry in background
                    scheduleAppOpenRetry(context)
                }
            }
        )
    }

    private fun scheduleAppOpenRetry(context: Context) {
        appOpenRetryJob?.cancel()
        appOpenRetryJob = scope.launch {
            delay(appOpenRetryDelay)
            appOpenRetryDelay = (appOpenRetryDelay * 2).coerceAtMost(60000L)
            if (appOpenAd == null && !isAppOpenAdLoading && !_isVipAdFree.value) {
                loadAppOpenAd(context)
            }
        }
    }

    fun showAppOpenAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        if (_isVipAdFree.value || isShowingAppOpenAd) {
            onComplete()
            return
        }

        val ad = appOpenAd
        if (ad != null) {
            isShowingAppOpenAd = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity.applicationContext)
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    loadAppOpenAd(activity.applicationContext)
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "AppOpenAd showed full screen")
                }
            }
            ad.show(activity)
        } else {
            // Not ready yet, reload and proceed
            loadAppOpenAd(activity.applicationContext)
            onComplete()
        }
    }

    fun loadRewardedAd(context: Context, useFallbackId: Boolean = false, onLoaded: (() -> Unit)? = null) {
        if (_isVipAdFree.value || isRewardedAdLoading) {
            onLoaded?.invoke()
            return
        }

        if (rewardedAd != null) {
            onLoaded?.invoke()
            return
        }

        isRewardedAdLoading = true
        val adUnitId = if (useFallbackId) TEST_REWARDED_AD_ID else REWARDED_AD_ID
        val request = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            adUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedAdLoading = false
                    _isAdLoading.value = false
                    _adStatusMessage.value = null
                    rewardedRetryDelay = 5000L // Reset backoff
                    Log.d(TAG, "RewardedAd loaded successfully (id=$adUnitId)")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedAdLoading = false
                    Log.w(TAG, "RewardedAd failed to load ($adUnitId): code=${loadAdError.code}, msg=${loadAdError.message}")

                    // If production returned NO_FILL and we haven't tried fallback yet, try fallback immediately
                    if (!useFallbackId && loadAdError.code == AdRequest.ERROR_CODE_NO_FILL) {
                        Log.d(TAG, "RewardedAd NO_FILL: trying test ad unit fallback immediately")
                        loadRewardedAd(context, useFallbackId = true, onLoaded = onLoaded)
                        return
                    }

                    _isAdLoading.value = false
                    _adStatusMessage.value = "تعذر تحميل الإعلان حالياً (${loadAdError.code})"
                    scheduleRewardedRetry(context)
                    onLoaded?.invoke()
                }
            }
        )
    }

    private fun scheduleRewardedRetry(context: Context) {
        rewardedRetryJob?.cancel()
        rewardedRetryJob = scope.launch {
            delay(rewardedRetryDelay)
            rewardedRetryDelay = (rewardedRetryDelay * 2).coerceAtMost(60000L)
            if (rewardedAd == null && !isRewardedAdLoading && !_isVipAdFree.value) {
                loadRewardedAd(context)
            }
        }
    }

    fun cancelAdLoading() {
        _isAdLoading.value = false
        adLoadingTimeoutJob?.cancel()
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissedOrFailed: (rewardEarned: Boolean) -> Unit
    ) {
        if (_isVipAdFree.value) {
            onRewardEarned()
            onDismissedOrFailed(true)
            return
        }

        val ad = rewardedAd
        if (ad != null) {
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd(activity.applicationContext)
                    onDismissedOrFailed(earned)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewardedAd(activity.applicationContext)
                    _adStatusMessage.value = "فشل عرض الإعلان: ${adError.message}"
                    onRewardEarned()
                    onDismissedOrFailed(true)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "RewardedAd showed full screen")
                }
            }
            ad.show(activity) { _ ->
                earned = true
                onRewardEarned()
            }
        } else {
            // Ad not ready, attempt load with 4-second safety timeout so UI never freezes
            _isAdLoading.value = true
            _adStatusMessage.value = "جاري تجهيز وتحميل الإعلان..."

            adLoadingTimeoutJob?.cancel()
            adLoadingTimeoutJob = scope.launch {
                delay(4000L)
                if (_isAdLoading.value) {
                    _isAdLoading.value = false
                    _adStatusMessage.value = "تم تجاوز الخطوة مؤقتاً."
                    onRewardEarned()
                    onDismissedOrFailed(true)
                }
            }

            loadRewardedAd(activity.applicationContext) {
                adLoadingTimeoutJob?.cancel()
                _isAdLoading.value = false
                val freshAd = rewardedAd
                if (freshAd != null) {
                    showRewardedAd(activity, onRewardEarned, onDismissedOrFailed)
                } else {
                    _adStatusMessage.value = "تعذر الاتصال بخادم الإعلانات. تم تجاوز الخطوة لك مؤقتاً."
                    onRewardEarned()
                    onDismissedOrFailed(true)
                }
            }
        }
    }

    // Card limit check: Green Cards Challenge
    fun canAddCard(currentCardsCount: Int): Boolean {
        if (_isVipAdFree.value) return true
        val maxAllowed = BASE_CARD_LIMIT + _extraCardSlots.value
        return currentCardsCount < maxAllowed
    }

    fun getMaxAllowedCards(): Int {
        if (_isVipAdFree.value) return 9999
        return BASE_CARD_LIMIT + _extraCardSlots.value
    }

    fun openGreenChallengeDialog() {
        _showGreenChallengeDialog.value = true
    }

    fun closeGreenChallengeDialog() {
        _showGreenChallengeDialog.value = false
        _adStatusMessage.value = null
    }

    // Called when user watches a rewarded ad to unlock an extra card in the Green Challenge
    fun watchAdForGreenChallenge(activity: Activity, onCardSlotUnlocked: () -> Unit) {
        showRewardedAd(
            activity = activity,
            onRewardEarned = {
                val newSlots = _extraCardSlots.value + 1
                _extraCardSlots.value = newSlots
                prefs?.edit()?.putInt(KEY_EXTRA_CARD_SLOTS, newSlots)?.apply()
                _showGreenChallengeDialog.value = false
                _adStatusMessage.value = null
                onCardSlotUnlocked()
            },
            onDismissedOrFailed = { rewardEarned ->
                if (!rewardEarned) {
                    _adStatusMessage.value = "يجب مشاهدة الإعلان كاملاً لفتح البطاقة الإضافية."
                }
            }
        )
    }

    // Called when user watches a rewarded ad for the 1-hour session continuation
    fun watchAdForOneHourSession(activity: Activity) {
        showRewardedAd(
            activity = activity,
            onRewardEarned = {
                val remaining = _remainingHourAds.value - 1
                if (remaining <= 0) {
                    _remainingHourAds.value = 2
                    _sessionActiveSeconds.value = 0L
                    prefs?.edit()?.putLong(KEY_SESSION_ACTIVE_SECONDS, 0L)?.apply()
                    _showOneHourDialog.value = false
                    _adStatusMessage.value = null
                } else {
                    _remainingHourAds.value = remaining
                    _adStatusMessage.value = "أحسنت! بقي إعلان واحد فقط للمتابعة."
                }
            },
            onDismissedOrFailed = { rewardEarned ->
                if (!rewardEarned) {
                    _adStatusMessage.value = "يجب إكمال مشاهدة الإعلان للاستمرار."
                }
            }
        )
    }

    fun snoozeOneHourDialog(minutes: Int = 5) {
        _sessionActiveSeconds.value = ONE_HOUR_SECONDS - (minutes * 60L)
        _showOneHourDialog.value = false
    }

    /**
     * Shows an ad before executing a specific feature (Linking & Sync, Backup, Restore).
     * If user is in VIP Ad-Free mode, executes immediately without showing ads.
     */
    fun showAdForFeature(activity: Activity, featureName: String, onProceed: () -> Unit) {
        if (_isVipAdFree.value) {
            onProceed()
            return
        }

        showRewardedAd(
            activity = activity,
            onRewardEarned = {
                Log.d(TAG, "Ad reward completed for feature: $featureName")
            },
            onDismissedOrFailed = { _ ->
                onProceed()
            }
        )
    }

    fun setVipAdFree(enabled: Boolean) {
        _isVipAdFree.value = enabled
        prefs?.edit()?.putBoolean(KEY_VIP_AD_FREE, enabled)?.apply()
        if (enabled) {
            _showOneHourDialog.value = false
            _showGreenChallengeDialog.value = false
            _adStatusMessage.value = null
        }
    }
}
