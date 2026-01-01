package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.AvailableShare
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.data.model.SiegeWar
import com.madeinbraza.app.data.model.SWResponseItem
import com.madeinbraza.app.data.model.SWResponsesSummary
import com.madeinbraza.app.data.model.SWResponseType
import com.madeinbraza.app.data.model.SWResponseUser
import com.madeinbraza.app.data.model.SWTag
import com.madeinbraza.app.data.model.SWUserResponse
import com.madeinbraza.app.data.model.SiegeWarHistoryItem
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.Result
import com.madeinbraza.app.data.repository.SiegeWarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SiegeWarUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val siegeWar: SiegeWar? = null,
    val userResponse: SWUserResponse? = null,
    val isLeader: Boolean = false,
    // Leader panel data
    val responses: List<SWResponseItem> = emptyList(),
    val notResponded: List<SWResponseUser> = emptyList(),
    val availableShares: List<AvailableShare> = emptyList(),
    val summary: SWResponsesSummary? = null,
    // For submitting
    val showResponseDialog: Boolean = false,
    val selectedResponseType: SWResponseType? = null,
    // History
    val history: List<SiegeWarHistoryItem> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SiegeWarViewModel @Inject constructor(
    private val repository: SiegeWarRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SiegeWarUiState())
    val uiState: StateFlow<SiegeWarUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        loadSiegeWar()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLeader = result.data.role == Role.LEADER) }
                }
                is Result.Error -> {
                    // User not logged in, keep isLeader as false
                }
            }
        }
    }

    fun loadSiegeWar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.getCurrentSiegeWar()) {
                is Result.Success -> {
                    val (siegeWar, userResponse) = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            siegeWar = siegeWar,
                            userResponse = userResponse
                        )
                    }
                    // If leader and siege war exists, load responses
                    if (_uiState.value.isLeader && siegeWar != null) {
                        loadResponses(siegeWar.id)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadSiegeWar()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadResponses(siegeWarId: String) {
        viewModelScope.launch {
            when (val result = repository.getResponses(siegeWarId)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            responses = data.responses,
                            notResponded = data.notResponded,
                            availableShares = data.availableShares,
                            summary = data.summary
                        )
                    }
                }
                is Result.Error -> {
                    // Silently fail for responses loading
                }
            }
        }
    }

    fun loadAvailableShares() {
        val siegeWarId = _uiState.value.siegeWar?.id ?: return
        viewModelScope.launch {
            when (val result = repository.getAvailableShares(siegeWarId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(availableShares = result.data) }
                }
                is Result.Error -> {
                    // Silently fail
                }
            }
        }
    }

    fun showResponseDialog(type: SWResponseType) {
        _uiState.update { it.copy(showResponseDialog = true, selectedResponseType = type) }
    }

    fun hideResponseDialog() {
        _uiState.update { it.copy(showResponseDialog = false, selectedResponseType = null) }
    }

    fun submitResponse(
        responseType: SWResponseType,
        tag: SWTag? = null,
        gameId: String? = null,
        password: String? = null,
        sharedClass: PlayerClass? = null,
        pilotingForId: String? = null,
        preferredClass: PlayerClass? = null
    ) {
        val siegeWarId = _uiState.value.siegeWar?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            when (val result = repository.submitResponse(
                siegeWarId = siegeWarId,
                responseType = responseType,
                tag = tag,
                gameId = gameId,
                password = password,
                sharedClass = sharedClass,
                pilotingForId = pilotingForId,
                preferredClass = preferredClass
            )) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            userResponse = result.data,
                            showResponseDialog = false,
                            selectedResponseType = null
                        )
                    }
                    // Reload responses if leader
                    if (_uiState.value.isLeader) {
                        loadResponses(siegeWarId)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSubmitting = false, error = result.message)
                    }
                }
            }
        }
    }

    fun createSiegeWar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.createSiegeWar()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, siegeWar = result.data, userResponse = null)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun closeSiegeWar() {
        val siegeWarId = _uiState.value.siegeWar?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.closeSiegeWar(siegeWarId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, siegeWar = result.data)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }

            when (val result = repository.getHistory()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoadingHistory = false, history = result.data)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoadingHistory = false, error = result.message)
                    }
                }
            }
        }
    }
}
