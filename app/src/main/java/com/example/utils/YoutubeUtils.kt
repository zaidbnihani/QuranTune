package com.example.utils

import android.util.Log
import com.example.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YoutubeUtils {

    fun extractVideoId(url: String): String? {
        if (url.isBlank()) return null
        val patterns = listOf(
            "(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|\\S*?[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})",
            "(?:https?:\\/\\/)?(?:www\\.)?youtube\\.com\\/shorts\\/([a-zA-Z0-9_-]{11})"
        )
        for (pattern in patterns) {
            val regex = pattern.toRegex()
            val matchResult = regex.find(url)
            if (matchResult != null) {
                return matchResult.groupValues[1]
            }
        }
        return null
    }

    suspend fun fetchVideoTitle(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.YOUTUBE_API_KEY
            val apiUrl = "https://www.googleapis.com/youtube/v3/videos?id=$videoId&key=$apiKey&part=snippet"
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val items = json.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val snippet = items.getJSONObject(0).optJSONObject("snippet")
                    return@withContext snippet?.optString("title")
                }
            }
        } catch (e: Exception) {
            Log.e("YoutubeUtils", "Error fetching YouTube title", e)
        }
        null
    }

    suspend fun fetchAudioStreamUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        val cobaltInstances = listOf(
            "https://api.cobalt.tools/",
            "https://api.cobalt.tools/api/json",
            "https://cobalt.api.ryb.ovh/",
            "https://cobalt.api.ryb.ovh/api/json",
            "https://cobalt.shuttleapp.rs/",
            "https://cobalt.shuttleapp.rs/api/json"
        )
        
        var lastRawResponse: String? = null
        var lastEndpoint: String? = null
        
        for (apiEndpoint in cobaltInstances) {
            try {
                lastEndpoint = apiEndpoint
                val connection = URL(apiEndpoint).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                connection.doOutput = true
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
 
                val jsonBody = JSONObject().apply {
                    put("url", youtubeUrl)
                    put("isAudioOnly", true)
                    put("downloadMode", "audio")
                    put("audioFormat", "mp3")
                    put("alwaysProxy", true)
                }

                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray())
                    os.flush()
                }

                val isSuccess = connection.responseCode in 200..299
                val responseStream = if (isSuccess) connection.inputStream else connection.errorStream
                val responseText = responseStream?.bufferedReader()?.use { it.readText() } ?: ""
                lastRawResponse = responseText

                if (responseText.isNotBlank()) {
                    val json = JSONObject(responseText)
                    val status = json.optString("status")
                    if (status == "error") {
                        val errorObj = json.optJSONObject("error")
                        val errorText = errorObj?.optString("text") ?: json.optString("text") ?: json.optString("message") ?: "Unknown error"
                        Log.e("YoutubeUtils", "Cobalt instance $apiEndpoint returned error status: $errorText. Raw response: $responseText")
                    } else {
                        val streamUrl = json.optString("url")
                        if (!streamUrl.isNullOrBlank()) {
                            return@withContext streamUrl
                        }
                    }
                } else {
                    Log.e("YoutubeUtils", "Cobalt API returned empty response with code ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("YoutubeUtils", "Error with Cobalt instance $apiEndpoint", e)
                lastRawResponse = e.message ?: e.toString()
            }
        }
        
        Log.e("YoutubeUtils", "ALL Cobalt instances failed. Last tried endpoint: $lastEndpoint. Last raw response/error: $lastRawResponse")
        null
    }

    suspend fun downloadFile(streamUrl: String, destinationFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(streamUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()
            
            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext destinationFile.exists() && destinationFile.length() > 0
            } else {
                Log.e("YoutubeUtils", "Download failed with HTTP response code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("YoutubeUtils", "Error downloading YouTube audio stream", e)
        }
        false
    }
}
