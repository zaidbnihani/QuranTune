package com.example.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import kotlinx.coroutines.launch

object OfflineDownloader {
    fun download(context: Context, reciterId: String?, surahNumber: String) {
        val validSurah = surahNumber.toIntOrNull()
        if (validSurah == null) return
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        if (validSurah in 1..114) {
            val fileName = "quran_${reciterId.hashCode()}_${validSurah}.mp3"
            val localFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
            if (!localFile.exists()) {
                var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                val baseServer = if (server.endsWith("/")) server else "$server/"
                val formattedSurah = String.format(java.util.Locale.US, "%03d", validSurah)
                val url = "$baseServer$formattedSurah.mp3"
                
                enqueueDownload(context, downloadManager, url, fileName, "جاري تحميل السورة...")
            }
        } else if (validSurah == -1 || validSurah == -2) {
            val fileName = if (validSurah == -1) "athkar_sabah.mp3" else "athkar_masa.mp3"
            val localFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
            if (!localFile.exists()) {
                val url = if (validSurah == -1) "https://backup.qurango.net/radio/athkar_sabah" else "https://backup.qurango.net/radio/athkar_masa"
                enqueueDownload(context, downloadManager, url, fileName, "جاري تحميل الأذكار...")
            }
        } else if (validSurah == -3) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                var server = if (reciterId.isNullOrBlank()) "https://server11.mp3quran.net/a_jabr/" else reciterId
                if (!server.startsWith("http")) server = "https://server11.mp3quran.net/a_jabr/"
                val baseServer = if (server.endsWith("/")) server else "$server/"
                
                for (i in 78..114) {
                    val fileName = "quran_${reciterId.hashCode()}_${i}.mp3"
                    val localFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
                    if (!localFile.exists()) {
                        val formattedSurah = String.format(java.util.Locale.US, "%03d", i)
                        val url = "$baseServer$formattedSurah.mp3"
                        enqueueDownload(context, downloadManager, url, fileName, "جاري تحميل جزء عم...")
                        kotlinx.coroutines.delay(200) // Space out download requests slightly
                    }
                }
            }
        }
    }
    
    private fun enqueueDownload(context: Context, downloadManager: DownloadManager, url: String, fileName: String, title: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("تحميل للاستماع بدون إنترنت")
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
