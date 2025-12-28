package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Member
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.data.model.User
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.MembersRepository
import com.madeinbraza.app.data.repository.Result
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
    val error: String? = null,
    val banSuccess: Boolean = false,
    val promoteSuccess: Boolean = false,
    val demoteSuccess: Boolean = false
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

    init {
        loadCurrentUser()
        loadMembers()
    }

    private fun loadCurrentUser() {
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
        viewModelScope.launch {
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

            when (val result = membersRepository.banMember(memberId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isBanning = null, banSuccess = true) }
                    loadMembers() // Reload list after ban
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isBanning = null, error = result.message) }
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
                    loadMembers() // Reload list after promotion
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
