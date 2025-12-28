package com.madeinbraza.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.CreateEventRequest
import com.madeinbraza.app.data.model.Event
import com.madeinbraza.app.data.model.PlayerClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EventsRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun getEvents(): Result<List<Event>> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.getEvents("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.events)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get events")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createEvent(
        title: String,
        description: String?,
        eventDate: String,
        maxParticipants: Int?,
        requiredClasses: List<PlayerClass>?
    ): Result<Event> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.createEvent(
                "Bearer $token",
                CreateEventRequest(
                    title = title,
                    description = description,
                    eventDate = eventDate,
                    maxParticipants = maxParticipants,
                    requiredClasses = requiredClasses?.takeIf { it.isNotEmpty() }
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.event)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to create event")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.deleteEvent("Bearer $token", eventId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to delete event")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun joinEvent(eventId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.joinEvent("Bearer $token", eventId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to join event")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun leaveEvent(eventId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Not authenticated")
        return try {
            val response = api.leaveEvent("Bearer $token", eventId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to leave event")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
