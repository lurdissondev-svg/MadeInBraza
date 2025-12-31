package com.madeinbraza.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.AuthResponse
import com.madeinbraza.app.data.model.FcmTokenRequest
import com.madeinbraza.app.data.model.LoginRequest
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.model.RegisterRequest
import com.madeinbraza.app.data.model.StatusResponse
import com.madeinbraza.app.data.model.User
import com.madeinbraza.app.data.model.ChangePasswordRequest
import com.madeinbraza.app.data.model.ForgotPasswordRequest
import com.madeinbraza.app.data.model.ForgotPasswordResponse
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class AuthRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")
    private val stayLoggedInKey = stringPreferencesKey("stay_logged_in")

    suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    private suspend fun saveToken(token: String) {
        dataStore.edit { it[tokenKey] = token }
    }

    suspend fun clearToken() {
        dataStore.edit { it.remove(tokenKey) }
    }

    suspend fun logout() {
        dataStore.edit {
            it.remove(tokenKey)
            it.remove(stayLoggedInKey)
        }
    }

    suspend fun getStayLoggedIn(): Boolean {
        return dataStore.data.map { it[stayLoggedInKey] == "true" }.first()
    }

    suspend fun setStayLoggedIn(value: Boolean) {
        dataStore.edit { it[stayLoggedInKey] = if (value) "true" else "false" }
    }

    suspend fun register(nick: String, password: String, playerClass: PlayerClass): Result<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(nick, password, playerClass))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                saveToken(auth.token)
                Result.Success(auth)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Falha no registro"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun login(nick: String, password: String, stayLoggedIn: Boolean = false): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(nick, password))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                saveToken(auth.token)
                setStayLoggedIn(stayLoggedIn)
                Result.Success(auth)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Falha no login"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun checkStatus(): Result<User> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.checkStatus("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Falha ao verificar status"
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

    suspend fun checkSavedSession(): Result<User>? {
        val stayLoggedIn = getStayLoggedIn()
        if (!stayLoggedIn) return null

        val token = getToken() ?: return null
        return try {
            val response = api.checkStatus("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user)
            } else {
                // Token invalid, clear it
                clearToken()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun registerFcmToken(): Boolean {
        val authToken = getToken() ?: return false
        return try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val response = api.registerFcmToken("Bearer $authToken", FcmTokenRequest(fcmToken))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.changePassword(
                "Bearer $token",
                ChangePasswordRequest(currentPassword, newPassword)
            )
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Falha ao alterar senha"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun forgotPassword(nick: String): Result<ForgotPasswordResponse> {
        return try {
            val response = api.forgotPassword(ForgotPasswordRequest(nick))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "Falha ao recuperar senha"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }
}
