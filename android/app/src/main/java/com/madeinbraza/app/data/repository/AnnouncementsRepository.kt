package com.madeinbraza.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.Announcement
import com.madeinbraza.app.data.model.CreateAnnouncementRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AnnouncementsRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun getAnnouncements(): Result<List<Announcement>> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.getAnnouncements("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.announcements)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get announcements")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createAnnouncement(title: String, content: String): Result<Announcement> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.createAnnouncement(
                "Bearer $token",
                CreateAnnouncementRequest(title = title, content = content)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.announcement)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to create announcement")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.deleteAnnouncement("Bearer $token", announcementId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to delete announcement")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
