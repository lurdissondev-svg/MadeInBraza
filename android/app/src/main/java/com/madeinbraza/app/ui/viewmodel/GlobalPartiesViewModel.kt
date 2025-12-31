package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Party
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.PartiesRepository
import com.madeinbraza.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlobalPartiesUiState(
    val parties: List<Party> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val actionInProgress: String? = null,
    val currentUserId: String? = null,
    val isLeader: Boolean = false,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val partyToEdit: Party? = null,
    val isEditing: Boolean = false
)

@HiltViewModel
class GlobalPartiesViewModel @Inject constructor(
    private val partiesRepository: PartiesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalPartiesUiState())
    val uiState: StateFlow<GlobalPartiesUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
        loadParties()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            currentUserId = result.data.id,
                            isLeader = result.data.role.name == "LEADER"
                        )
                    }
                }
                is Result.Error -> {
                    // Silently fail
                }
            }
        }
    }

    fun loadParties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = partiesRepository.getGlobalParties()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, parties = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            when (val result = partiesRepository.getGlobalParties()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, parties = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createParty(name: String, description: String?, maxMembers: Int?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            when (val result = partiesRepository.createGlobalParty(name, description, maxMembers)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isCreating = false,
                            showCreateDialog = false,
                            parties = listOf(result.data) + state.parties
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isCreating = false, error = result.message) }
                }
            }
        }
    }

    fun joinParty(partyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = partyId) }

            when (val result = partiesRepository.joinParty(partyId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(actionInProgress = null) }
                    loadParties()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(actionInProgress = null, error = result.message) }
                }
            }
        }
    }

    fun leaveParty(partyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = partyId) }

            when (val result = partiesRepository.leaveParty(partyId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(actionInProgress = null) }
                    loadParties()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(actionInProgress = null, error = result.message) }
                }
            }
        }
    }

    fun deleteParty(partyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = partyId) }

            when (val result = partiesRepository.deleteParty(partyId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = null,
                            parties = state.parties.filter { it.id != partyId }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(actionInProgress = null, error = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showEditDialog(party: Party) {
        _uiState.update { it.copy(partyToEdit = party) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(partyToEdit = null) }
    }

    fun updateParty(partyId: String, name: String, description: String?, maxMembers: Int?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEditing = true) }

            when (val result = partiesRepository.updateParty(partyId, name, description, maxMembers)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isEditing = false,
                            partyToEdit = null,
                            parties = state.parties.map { if (it.id == partyId) result.data else it }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isEditing = false, error = result.message) }
                }
            }
        }
    }
}
