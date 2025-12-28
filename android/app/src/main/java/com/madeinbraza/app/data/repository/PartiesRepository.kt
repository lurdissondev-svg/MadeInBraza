package com.madeinbraza.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.CreatePartyRequest
import com.madeinbraza.app.data.model.Party
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PartiesRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun getPartiesByEvent(eventId: String): Result<List<Party>> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.getPartiesByEvent("Bearer $token", eventId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.parties)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get parties")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createParty(eventId: String, name: String, maxMembers: Int?): Result<Party> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.createParty(
                "Bearer $token",
                eventId,
                CreatePartyRequest(name = name, maxMembers = maxMembers)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.party)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to create party")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteParty(partyId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.deleteParty("Bearer $token", partyId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to delete party")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun joinParty(partyId: String): Result<Boolean> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.joinParty("Bearer $token", partyId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.isClosed)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to join party")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun leaveParty(partyId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.leaveParty("Bearer $token", partyId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to leave party")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
