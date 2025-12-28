package com.madeinbraza.app.data.repository

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
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
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
}
