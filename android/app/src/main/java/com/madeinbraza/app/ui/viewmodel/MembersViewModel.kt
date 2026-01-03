package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Member
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.data.model.User
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.MembersRepository
import com.madeinbraza.app.data.repository.Result
import com.madeinbraza.app.util.Debouncer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembersUiState(
    val members: List<Member> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isBanning: String? = null, // ID of member being banned
    val isPromoting: String? = null, // ID of member being promoted
    val isDemoting: String? = null, // ID of member being demoted
    val isUpdatingRole: String? = null, // ID of member whose role is being updated
    val error: String? = null,
    val banSuccess: Boolean = false,
    val promoteSuccess: Boolean = false,
    val demoteSuccess: Boolean = false,
    val updateRoleSuccess: Boolean = false
) {
    val isLeader: Boolean get() = currentUser?.role == Role.LEADER
}

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    private val debouncer = Debouncer()

    init {
        loadCurrentUser()
        loadMembers()
    }

    private fun loadCurrentUser() {
        // First try cached user to avoid API call
        authRepository.getCachedUser()?.let { user ->
            _uiState.update { it.copy(currentUser = user) }
            return
        }

        // Fall back to API call if no cache
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    _uiState.update { it.copy(currentUser = result.data) }
                }
                is Result.Error -> {}
            }
        }
    }

    fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = membersRepository.getMembers()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, members = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun refresh() {
        // Debounce refresh to prevent spam
        if (!debouncer.canExecute("members_refresh", Debouncer.REFRESH_DEBOUNCE_MS)) {
            _uiState.update { it.copy(isRefreshing = false) }
            return
        }

        debouncer.throttle(viewModelScope, "members_refresh", Debouncer.REFRESH_DEBOUNCE_MS) {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            when (val result = membersRepository.getMembers()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, members = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun banMember(memberId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBanning = memberId, error = null) }

            // Optimistic update: remove member immediately
            val previousMembers = _uiState.value.members
            _uiState.update { state ->
                state.copy(members = state.members.filter { it.id != memberId })
            }

            when (val result = membersRepository.banMember(memberId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isBanning = null, banSuccess = true) }
                    // No need to reload - optimistic update already applied
                }
                is Result.Error -> {
                    // Rollback optimistic update
                    _uiState.update {
                        it.copy(isBanning = null, error = result.message, members = previousMembers)
                    }
                }
            }
        }
    }

    fun promoteMember(memberId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPromoting = memberId, error = null) }

            when (val result = membersRepository.promoteMember(memberId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isPromoting = null, promoteSuccess = true) }
                    // Need to reload to get updated role
                    loadMembers()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isPromoting = null, error = result.message) }
                }
            }
        }
    }

    fun clearBanSuccess() {
        _uiState.update { it.copy(banSuccess = false) }
    }

    fun clearPromoteSuccess() {
        _uiState.update { it.copy(promoteSuccess = false) }
    }

    fun demoteMember(memberId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDemoting = memberId, error = null) }

            when (val result = membersRepository.demoteMember(memberId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isDemoting = null, demoteSuccess = true) }
                    loadMembers() // Reload list after demotion
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isDemoting = null, error = result.message) }
                }
            }
        }
    }

    fun clearDemoteSuccess() {
        _uiState.update { it.copy(demoteSuccess = false) }
    }

    fun updateMemberRole(memberId: String, role: Role) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingRole = memberId, error = null) }

            when (val result = membersRepository.updateMemberRole(memberId, role)) {
                is Result.Success -> {
                    // Update local state with new role
                    val updatedMembers = _uiState.value.members.map { member ->
                        if (member.id == memberId) {
                            member.copy(role = result.data)
                        } else {
                            member
                        }
                    }
                    _uiState.update { it.copy(isUpdatingRole = null, updateRoleSuccess = true, members = updatedMembers) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isUpdatingRole = null, error = result.message) }
                }
            }
        }
    }

    fun clearUpdateRoleSuccess() {
        _uiState.update { it.copy(updateRoleSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
