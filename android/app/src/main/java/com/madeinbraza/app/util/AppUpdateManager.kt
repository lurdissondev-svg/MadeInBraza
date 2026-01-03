package com.madeinbraza.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.BuildConfig
import com.madeinbraza.app.di.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdate(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val publishedAt: String
)

@Singleton
class AppUpdateManager @Inject constructor() {

    companion object {
        // Configure your GitHub repository here
        private const val GITHUB_USER = "lurdissondev-svg"
        private const val GITHUB_REPO = "MadeInBraza"
        private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest"

        // DataStore key for tracking downloaded version
        private val DOWNLOADED_VERSION_KEY = stringPreferencesKey("downloaded_version")
    }

    // Track the version that was started for download
    suspend fun markVersionAsDownloaded(context: Context, versionName: String) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOADED_VERSION_KEY] = versionName
        }
    }

    // Check if a version was already downloaded
    suspend fun wasVersionDownloaded(context: Context, versionName: String): Boolean {
        val downloadedVersion = context.dataStore.data.first()[DOWNLOADED_VERSION_KEY]
        return downloadedVersion == versionName
    }

    // Clear downloaded version tracking (call when update is installed or dismissed permanently)
    suspend fun clearDownloadedVersion(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(DOWNLOADED_VERSION_KEY)
        }
    }

    suspend fun checkForUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseRelease(response)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseRelease(json: String): AppUpdate? {
        return try {
            val jsonObject = org.json.JSONObject(json)
            val tagName = jsonObject.getString("tag_name")
            val body = jsonObject.optString("body", "")
            val publishedAt = jsonObject.optString("published_at", "")
            val assets = jsonObject.getJSONArray("assets")

            // Find APK asset
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (downloadUrl == null) return null

            // Parse version from tag (e.g., "v1.0.1" -> "1.0.1")
            val versionName = tagName.removePrefix("v").removePrefix("V")
            val versionCode = parseVersionCode(versionName)

            AppUpdate(
                versionName = versionName,
                versionCode = versionCode,
                releaseNotes = body,
                downloadUrl = downloadUrl,
                publishedAt = publishedAt
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseVersionCode(versionName: String): Int {
        // Convert "1.0.1" to version code like 10001
        return try {
            val parts = versionName.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            0
        }
    }

    fun isUpdateAvailable(update: AppUpdate): Boolean {
        val currentVersionName = BuildConfig.VERSION_NAME
        val currentVersionCode = parseVersionCode(currentVersionName)

        // First check: if version names are exactly equal, no update needed
        if (update.versionName == currentVersionName) {
            return false
        }

        // Second check: compare version codes
        // Only show update if remote version is strictly greater
        return update.versionCode > currentVersionCode
    }

    fun getCurrentVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    fun getCurrentVersionCode(): Int {
        return parseVersionCode(BuildConfig.VERSION_NAME)
    }

    fun downloadAndInstall(context: Context, update: AppUpdate) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val fileName = "MadeInBraza-${update.versionName}.apk"
        val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(destination, fileName)

        // Delete old file if exists
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setTitle("Made in Braza ${update.versionName}")
            .setDescription("Baixando atualização...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        // Register receiver to install after download
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    ctx.unregisterReceiver(this)
                    installApk(ctx, file)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}
