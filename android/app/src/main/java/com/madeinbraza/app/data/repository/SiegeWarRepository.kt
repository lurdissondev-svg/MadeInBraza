package com.madeinbraza.app.data.repository

import com.madeinbraza.app.data.api.BrazaApi
import com.madeinbraza.app.data.model.AvailableShare
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.model.SiegeWar
import com.madeinbraza.app.data.model.SubmitSWResponseRequest
import com.madeinbraza.app.data.model.SWResponsesResponse
import com.madeinbraza.app.data.model.SWResponseType
import com.madeinbraza.app.data.model.SWTag
import com.madeinbraza.app.data.model.SWUserResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiegeWarRepository @Inject constructor(
    private val api: BrazaApi,
    private val authRepository: AuthRepository
) {
    private suspend fun getToken(): String = "Bearer ${authRepository.getToken()}"

    suspend fun getCurrentSiegeWar(): Result<Pair<SiegeWar?, SWUserResponse?>> {
        return try {
            val response = api.getCurrentSiegeWar(getToken())
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.Success(Pair(body.siegeWar, body.userResponse))
            } else {
                Result.Error("Erro ao carregar Siege War")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Erro desconhecido")
        }
    }

    suspend fun createSiegeWar(): Result<SiegeWar> {
        return try {
            val response = api.createSiegeWar(getToken())
            if (response.isSuccessful) {
                Result.Success(response.body()!!.siegeWar)
            } else {
                Result.Error("Erro ao criar Siege War")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Erro desconhecido")
        }
    }

    suspend fun submitResponse(
        siegeWarId: String,
        responseType: SWResponseType,
        tag: SWTag? = null,
        gameId: String? = null,
        password: String? = null,
        sharedClass: PlayerClass? = null,
        pilotingForId: String? = null,
        preferredClass: PlayerClass? = null
    ): Result<SWUserResponse> {
        return try {
            val request = SubmitSWResponseRequest(
                responseType = responseType,
                tag = tag,
                gameId = gameId,
                password = password,
                sharedClass = sharedClass,
                pilotingForId = pilotingForId,
                preferredClass = preferredClass
            )
            val response = api.submitSWResponse(getToken(), siegeWarId, request)
            if (response.isSuccessful) {
                Result.Success(response.body()!!.response)
            } else {
                Result.Error("Erro ao enviar resposta")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Erro desconhecido")
        }
    }

    suspend fun getResponses(siegeWarId: String): Result<SWResponsesResponse> {
        return try {
            val response = api.getSWResponses(getToken(), siegeWarId)
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Erro ao carregar respostas")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Erro desconhecido")
        }
    }

    suspend fun getAvailableShares(siegeWarId: String): Result<List<AvailableShare>> {
        return try {
            val response = api.getAvailableShares(getToken(), siegeWarId)
            if (response.isSuccessful) {
                Result.Success(response.body()!!.availableShares)
            } else {
                Result.Error("Erro ao carregar contas compartilhadas")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Erro desconhecido")
        }
    }

    suspend fun closeSiegeWar(siegeWarId: String): Result<SiegeWar> {
        return try {
            val response = api.closeSiegeWar(getToken(), siegeWarId)
            if (response.isSuccessful) {
                Result.Success(response.body()!!.siegeWar)
            } else {
                Result.Error("Erro ao fechar Siege War")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Erro desconhecido")
        }
    }
}
