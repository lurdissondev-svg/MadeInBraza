package com.madeinbraza.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.BuildConfig
import com.madeinbraza.app.di.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progress: Int) : UpdateDownloadState()
    object Installing : UpdateDownloadState()
    object Failed : UpdateDownloadState()
}

@Singleton
class AppUpdateManager @Inject constructor() {

    companion object {
        // Configure your GitHub repository here
        private const val GITHUB_USER = "lurdissondev-svg"
        private const val GITHUB_REPO = "MadeInBraza"
        private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest"

        // DataStore keys
        private val DOWNLOADED_VERSION_KEY = stringPreferencesKey("downloaded_version")
        private val LAST_VERSION_CODE_KEY = intPreferencesKey("last_version_code")
    }

    // Download state flow for UI
    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private var currentDownloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

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

    // Check if app was just installed or updated (for notification permission)
    suspend fun isFirstRunAfterInstallOrUpdate(context: Context): Boolean {
        val lastVersionCode = context.dataStore.data.first()[LAST_VERSION_CODE_KEY] ?: 0
        val currentVersionCode = BuildConfig.VERSION_CODE
        return lastVersionCode < currentVersionCode
    }

    // Mark current version as seen
    suspend fun markCurrentVersionAsSeen(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[LAST_VERSION_CODE_KEY] = BuildConfig.VERSION_CODE
        }
    }

    // Reset download state
    fun resetDownloadState() {
        _downloadState.value = UpdateDownloadState.Idle
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

        currentDownloadId = downloadManager.enqueue(request)
        _downloadState.value = UpdateDownloadState.Downloading(0)

        // Start tracking download progress
        trackDownloadProgress(context, downloadManager, currentDownloadId, file)

        // Register receiver to install after download
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == currentDownloadId) {
                    try {
                        ctx.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // Already unregistered
                    }
                    downloadReceiver = null

                    // Check if download was successful
                    val query = DownloadManager.Query().setFilterById(currentDownloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            _downloadState.value = UpdateDownloadState.Installing
                            installApk(ctx, file)
                        } else {
                            _downloadState.value = UpdateDownloadState.Failed
                        }
                    } else {
                        _downloadState.value = UpdateDownloadState.Failed
                    }
                    cursor.close()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    private fun trackDownloadProgress(
        context: Context,
        downloadManager: DownloadManager,
        downloadId: Long,
        file: File
    ) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = cursor.getInt(statusIndex)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                    val bytesTotal = cursor.getLong(bytesTotalIndex)

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            _downloadState.value = UpdateDownloadState.Downloading(100)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            _downloadState.value = UpdateDownloadState.Failed
                        }
                        DownloadManager.STATUS_RUNNING -> {
                            if (bytesTotal > 0) {
                                val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                                _downloadState.value = UpdateDownloadState.Downloading(progress)
                            }
                        }
                    }
                } else {
                    downloading = false
                }
                cursor.close()

                if (downloading) {
                    delay(500) // Check every 500ms
                }
            }
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
