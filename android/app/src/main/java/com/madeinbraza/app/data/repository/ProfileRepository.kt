package com.madeinbraza.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.model.Profile
import com.madeinbraza.app.data.model.UpdateProfileRequest
import com.madeinbraza.app.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>,
    private val contentResolver: ContentResolver
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    private fun getMimeType(uri: Uri): String {
        return contentResolver.getType(uri) ?: "image/jpeg"
    }

    private fun getFileName(uri: Uri): String {
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(getMimeType(uri)) ?: "jpg"
        return "avatar.$extension"
    }

    suspend fun getProfile(): Result<Profile> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.getProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.profile)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get profile")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateProfile(nick: String?, playerClass: PlayerClass?): Result<User> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val request = UpdateProfileRequest(nick = nick, playerClass = playerClass)
            val response = api.updateProfile("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to update profile")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun uploadAvatar(uri: Uri): Result<String> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.Error("Could not read file")
            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = getMimeType(uri)
            val fileName = getFileName(uri)

            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("avatar", fileName, requestBody)

            val response = api.uploadAvatar("Bearer $token", part)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.avatarUrl)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to upload avatar")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteAvatar(): Result<Unit> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.deleteAvatar("Bearer $token")
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to delete avatar")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
