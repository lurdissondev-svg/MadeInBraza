package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Party
import com.madeinbraza.app.data.model.SlotRequest
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

data class PartiesUiState(
    val eventId: String = "",
    val eventTitle: String = "",
    val parties: List<Party> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val actionInProgress: String? = null,
    val currentUserId: String? = null,
    val isLeader: Boolean = false,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val partyToJoin: Party? = null,
    val isJoining: Boolean = false
)

@HiltViewModel
class PartiesViewModel @Inject constructor(
    private val partiesRepository: PartiesRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])
    private val eventTitle: String = savedStateHandle["eventTitle"] ?: ""

    private val _uiState = MutableStateFlow(PartiesUiState(eventId = eventId, eventTitle = eventTitle))
    val uiState: StateFlow<PartiesUiState> = _uiState.asStateFlow()

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

            when (val result = partiesRepository.getPartiesByEvent(eventId)) {
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

            when (val result = partiesRepository.getPartiesByEvent(eventId)) {
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

    fun createParty(name: String, description: String?, slots: List<SlotRequest>, creatorSlotClass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            when (val result = partiesRepository.createParty(eventId, name, description, slots, creatorSlotClass)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isCreating = false,
                            showCreateDialog = false,
                            parties = state.parties + result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isCreating = false, error = result.message) }
                }
            }
        }
    }

    fun showJoinDialog(party: Party) {
        _uiState.update { it.copy(partyToJoin = party) }
    }

    fun hideJoinDialog() {
        _uiState.update { it.copy(partyToJoin = null) }
    }

    fun joinParty(partyId: String, slotId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, actionInProgress = partyId) }

            when (val result = partiesRepository.joinParty(partyId, slotId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isJoining = false,
                            actionInProgress = null,
                            partyToJoin = null,
                            parties = state.parties.map { party ->
                                if (party.id == partyId) result.data else party
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            actionInProgress = null,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun leaveParty(partyId: String) {
        val currentUserId = _uiState.value.currentUserId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = partyId) }

            // Optimistic update: remove user from party slots
            val previousSlots = _uiState.value.parties.find { it.id == partyId }?.slots
            _uiState.update { state ->
                state.copy(
                    parties = state.parties.map { party ->
                        if (party.id == partyId) {
                            party.copy(
                                slots = party.slots.map { slot ->
                                    if (slot.filledBy?.id == currentUserId) {
                                        slot.copy(filledBy = null)
                                    } else slot
                                }
                            )
                        } else party
                    }
                )
            }

            when (val result = partiesRepository.leaveParty(partyId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(actionInProgress = null) }
                }
                is Result.Error -> {
                    // Rollback optimistic update
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = null,
                            error = result.message,
                            parties = state.parties.map { party ->
                                if (party.id == partyId && previousSlots != null) {
                                    party.copy(slots = previousSlots)
                                } else party
                            }
                        )
                    }
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
}
