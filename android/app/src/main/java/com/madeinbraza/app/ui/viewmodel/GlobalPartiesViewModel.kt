package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Party
import com.madeinbraza.app.data.model.SlotRequest
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.PartiesRepository
import com.madeinbraza.app.data.repository.Result
import com.madeinbraza.app.util.Debouncer
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
    val isCounselor: Boolean = false,
    val canSkipJoining: Boolean = false, // LEADER or COUNSELOR can create party without joining
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val partyToJoin: Party? = null,
    val isJoining: Boolean = false
)

@HiltViewModel
class GlobalPartiesViewModel @Inject constructor(
    private val partiesRepository: PartiesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalPartiesUiState())
    val uiState: StateFlow<GlobalPartiesUiState> = _uiState.asStateFlow()

    private val debouncer = Debouncer()

    init {
        loadUserInfo()
        loadParties()
    }

    private fun loadUserInfo() {
        // First try cached user to avoid API call
        authRepository.getCachedUser()?.let { user ->
            val isLeader = user.role.name == "LEADER"
            val isCounselor = user.role.name == "COUNSELOR"
            _uiState.update {
                it.copy(
                    currentUserId = user.id,
                    isLeader = isLeader,
                    isCounselor = isCounselor,
                    canSkipJoining = isLeader || isCounselor
                )
            }
            return
        }

        // Fall back to API call if no cache
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    val isLeader = result.data.role.name == "LEADER"
                    val isCounselor = result.data.role.name == "COUNSELOR"
                    _uiState.update {
                        it.copy(
                            currentUserId = result.data.id,
                            isLeader = isLeader,
                            isCounselor = isCounselor,
                            canSkipJoining = isLeader || isCounselor
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
        // Debounce refresh to prevent spam
        if (!debouncer.canExecute("parties_refresh", Debouncer.REFRESH_DEBOUNCE_MS)) {
            _uiState.update { it.copy(isRefreshing = false) }
            return
        }

        debouncer.throttle(viewModelScope, "parties_refresh", Debouncer.REFRESH_DEBOUNCE_MS) {
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

    fun createParty(name: String, description: String?, slots: List<SlotRequest>, creatorSlotClass: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            when (val result = partiesRepository.createGlobalParty(name, description, slots, creatorSlotClass)) {
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

    fun showJoinDialog(party: Party) {
        _uiState.update { it.copy(partyToJoin = party) }
    }

    fun hideJoinDialog() {
        _uiState.update { it.copy(partyToJoin = null) }
    }

    fun joinParty(partyId: String, slotId: String, selectedClass: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, actionInProgress = partyId) }

            when (val result = partiesRepository.joinParty(partyId, slotId, selectedClass)) {
                is Result.Success -> {
                    // Update party with the returned data from API
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
