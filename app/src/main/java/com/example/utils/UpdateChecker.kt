package com.example.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File

// --- Data Classes for GitHub Release API Response ---

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body") val body: String?,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubAsset>?
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)

// --- Retrofit API Service Interface ---

interface GitHubUpdateService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRelease
}

// --- Semantic Version Comparison and Checker Object ---

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    
    // Default Owner and Repo placeholders - the user can change these or pass them to the function
    const val DEFAULT_OWNER = "zaidbnihani" // Set your GitHub username here
    const val DEFAULT_REPO = "QuranTune"    // Set your GitHub repository name here

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val service by lazy {
        retrofit.create(GitHubUpdateService::class.java)
    }

    /**
     * Checks if there's a newer version on GitHub.
     * Returns the GitHubRelease if an update is found, or null otherwise (fails silently on errors).
     */
    suspend fun checkForUpdate(
        currentVersion: String,
        owner: String = DEFAULT_OWNER,
        repo: String = DEFAULT_REPO
    ): GitHubRelease? {
        if (owner == "OWNER" || repo == "REPO" || owner.isBlank() || repo.isBlank()) {
            Log.w(TAG, "Update check skipped: Repository owner or name is not configured.")
            return null
        }
        
        val currentClean = if (currentVersion.isBlank()) "0.0.0" else currentVersion

        return try {
            val latestRelease = service.getLatestRelease(owner, repo)
            val latestVersion = latestRelease.tagName
            
            if (isNewerVersion(currentClean, latestVersion)) {
                latestRelease
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates: ${e.localizedMessage}")
            null // Fail silently on network errors or parsing errors
        }
    }

    /**
     * Semantic Versioning comparison (major.minor.patch)
     * Returns true if latest version is newer than current version.
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentClean = current.trim().lowercase().removePrefix("v")
        val latestClean = latest.trim().lowercase().removePrefix("v")
        
        val currentParts = currentClean.split(".")
        val latestParts = latestClean.split(".")
        
        val maxParts = maxOf(currentParts.size, latestParts.size)
        
        for (i in 0 until maxParts) {
            val currentPartVal = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            val latestPartVal = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
            
            if (latestPartVal > currentPartVal) {
                return true
            } else if (currentPartVal > latestPartVal) {
                return false
            }
        }
        return false
    }
}

// --- Composable Dialog Component ---

@Composable
fun UpdateCheckerEffect(
    owner: String = UpdateChecker.DEFAULT_OWNER,
    repo: String = UpdateChecker.DEFAULT_REPO,
    currentVersion: String = BuildConfig.VERSION_NAME
) {
    val context = LocalContext.current
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(owner, repo, currentVersion) {
        val release = withContext(Dispatchers.IO) {
            UpdateChecker.checkForUpdate(currentVersion, owner, repo)
        }
        if (release != null) {
            latestRelease = release
            showDialog = true
        }
    }

    // BroadcastReceiver for download completion
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val action = intent.action
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                    val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriString = if (uriIndex != -1) cursor.getString(uriIndex) else null
                            if (uriString != null) {
                                val apkUri = Uri.parse(uriString)
                                val file = if (apkUri.scheme == "file") {
                                    File(apkUri.path ?: "")
                                } else {
                                    null
                                }
                                
                                if (file != null && file.exists()) {
                                    try {
                                        val providerUri = FileProvider.getUriForFile(
                                            ctx,
                                            "${ctx.packageName}.fileprovider",
                                            file
                                        )
                                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(providerUri, "application/vnd.android.package-archive")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        ctx.startActivity(installIntent)
                                    } catch (e: Exception) {
                                        Log.e("UpdateChecker", "Failed to install APK", e)
                                    }
                                }
                            }
                        }
                        cursor.close()
                    }
                    isDownloading = false
                }
            }
        }
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    if (showDialog && latestRelease != null) {
        val release = latestRelease!!
        // Find APK download URL in assets, otherwise fallback to release html url
        val downloadUrl = release.assets?.firstOrNull { 
            it.name.endsWith(".apk", ignoreCase = true) 
        }?.browserDownloadUrl ?: release.htmlUrl

        AlertDialog(
            onDismissRequest = { if (!isDownloading) showDialog = false },
            title = {
                Text(
                    text = "يتوفر تحديث جديد! (${release.tagName})",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    Text(
                        text = "الإصدار الحالي: $currentVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ملاحظات التحديث:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = release.body ?: "لا توجد تفاصيل متوفرة لهذا الإصدار.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "جاري تنزيل التحديث وتجهيز التثبيت...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val hasInstallPermission = context.packageManager.canRequestPackageInstalls()
                                if (!hasInstallPermission) {
                                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                    return@Button
                                }
                            }

                            isDownloading = true
                            try {
                                val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                    setTitle("QuranTune Update")
                                    setDescription("تنزيل التحديث الجديد الإصدار ${release.tagName}")
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
                                    setAllowedOverMetered(true)
                                    setAllowedOverRoaming(true)
                                }
                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                            } catch (e: Exception) {
                                Log.e("UpdateCheckerEffect", "Failed to start download", e)
                                isDownloading = false
                            }
                        }
                    ) {
                        Text("تحميل والتثبيت الآن")
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text("لاحقاً")
                    }
                }
            }
        )
    }
}
