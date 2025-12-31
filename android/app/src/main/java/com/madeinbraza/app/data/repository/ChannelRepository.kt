package com.madeinbraza.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.Channel
import com.madeinbraza.app.data.model.ChannelMember
import com.madeinbraza.app.data.model.ChannelMessage
import com.madeinbraza.app.data.model.SendChannelMessageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ChannelRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun getChannels(): Result<List<Channel>> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.getChannels("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get channels")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun setupDefaultChannels(): Result<Boolean> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.setupDefaultChannels("Bearer $token")
            if (response.isSuccessful) {
                Result.Success(true)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to setup channels")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getChannelMessages(
        channelId: String,
        limit: Int? = null,
        before: String? = null
    ): Result<List<ChannelMessage>> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.getChannelMessages("Bearer $token", channelId, limit, before)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get messages")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun sendMessage(channelId: String, content: String): Result<ChannelMessage> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.sendChannelMessage(
                "Bearer $token",
                channelId,
                SendChannelMessageRequest(content)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to send message")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getChannelMembers(channelId: String): Result<List<ChannelMember>> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.getChannelMembers("Bearer $token", channelId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.members)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get members")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun sendMediaMessage(
        channelId: String,
        fileUri: Uri,
        content: String? = null
    ): Result<ChannelMessage> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            withContext(Dispatchers.IO) {
                // Get file info from Uri
                val fileName = getFileName(fileUri) ?: "media_${System.currentTimeMillis()}"
                val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"

                // Copy Uri content to temp file
                val tempFile = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(fileUri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.Error("Failed to read file")

                // Create multipart request
                val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
                val contentPart = content?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.sendMediaMessage(
                    "Bearer $token",
                    channelId,
                    filePart,
                    contentPart
                )

                // Clean up temp file
                tempFile.delete()

                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error(response.errorBody()?.string() ?: "Failed to send media")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }

    // Unread messages tracking
    private fun lastReadKey(channelId: String) = longPreferencesKey("last_read_$channelId")

    suspend fun getLastReadTimestamp(channelId: String): Long {
        return dataStore.data.map { it[lastReadKey(channelId)] ?: 0L }.first()
    }

    suspend fun setLastReadTimestamp(channelId: String, timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[lastReadKey(channelId)] = timestamp
        }
    }

    suspend fun getUnreadCount(channelId: String): Int {
        val token = getToken() ?: return 0
        val lastRead = getLastReadTimestamp(channelId)

        return try {
            val response = api.getChannelMessages("Bearer $token", channelId, limit = 100, before = null)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!
                messages.count { parseTimestamp(it.createdAt) > lastRead }
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun parseTimestamp(isoDate: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoDate)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
