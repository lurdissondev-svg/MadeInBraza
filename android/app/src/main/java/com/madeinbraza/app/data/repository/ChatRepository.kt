package com.madeinbraza.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.ChatMessage
import com.madeinbraza.app.data.model.SendMessageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun getMessages(limit: Int? = null, before: String? = null): Result<List<ChatMessage>> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.getMessages("Bearer $token", limit, before)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.messages)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get messages")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun sendMessage(content: String): Result<ChatMessage> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.sendMessage("Bearer $token", SendMessageRequest(content))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.message)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to send message")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
