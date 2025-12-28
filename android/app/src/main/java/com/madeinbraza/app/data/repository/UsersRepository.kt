package com.madeinbraza.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.PendingUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UsersRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun getPendingUsers(): Result<List<PendingUser>> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.getPendingUsers("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.users)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao carregar usuários pendentes"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun approveUser(userId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.approveUser("Bearer $token", userId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao aprovar usuário"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun banUser(userId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.banUser("Bearer $token", userId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao banir usuário"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun rejectUser(userId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.rejectUser("Bearer $token", userId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao rejeitar usuário"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = org.json.JSONObject(errorBody)
            json.optString("error").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
