package com.madeinbraza.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.BannedUser
import com.madeinbraza.app.data.model.Member
import com.madeinbraza.app.data.model.MemberProfile
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.data.model.UpdateUserRoleRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MembersRepository @Inject constructor(
    private val api: BrazaApi,
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    private suspend fun getToken(): String? {
        return dataStore.data.map { it[tokenKey] }.first()
    }

    suspend fun getMembers(): Result<List<Member>> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.getMembers("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.members)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao carregar membros"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun banMember(memberId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.banUser("Bearer $token", memberId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao banir membro"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun promoteMember(memberId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.promoteUser("Bearer $token", memberId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao promover membro"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun demoteMember(memberId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.demoteUser("Bearer $token", memberId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao rebaixar membro"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun updateMemberRole(memberId: String, role: Role): Result<Role> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val request = UpdateUserRoleRequest(role = role.name)
            val response = api.updateUserRole("Bearer $token", memberId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user.role)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao alterar cargo"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun getBannedUsers(): Result<List<BannedUser>> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.getBannedUsers("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.users)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao carregar banidos"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun unbanMember(memberId: String): Result<Unit> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.unbanUser("Bearer $token", memberId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao desbanir membro"
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error("Erro de conexão. Verifique sua internet.")
        }
    }

    suspend fun getMemberProfile(memberId: String): Result<MemberProfile> {
        val token = getToken() ?: return Result.Error("Não autenticado")
        return try {
            val response = api.getMemberProfile("Bearer $token", memberId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.user)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string()) ?: "Falha ao carregar perfil"
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
