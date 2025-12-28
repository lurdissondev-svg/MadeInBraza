package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.BannedUser
import com.madeinbraza.app.data.repository.MembersRepository
import com.madeinbraza.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BannedUsersUiState(
    val users: List<BannedUser> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isUnbanning: String? = null,
    val error: String? = null,
    val unbanSuccess: Boolean = false
)

@HiltViewModel
class BannedUsersViewModel @Inject constructor(
    private val membersRepository: MembersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BannedUsersUiState())
    val uiState: StateFlow<BannedUsersUiState> = _uiState.asStateFlow()

    init {
        loadBannedUsers()
    }

    fun loadBannedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = membersRepository.getBannedUsers()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, users = result.data) }
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

            when (val result = membersRepository.getBannedUsers()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, users = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnbanning = userId, error = null) }

            when (val result = membersRepository.unbanMember(userId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isUnbanning = null, unbanSuccess = true) }
                    loadBannedUsers()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isUnbanning = null, error = result.message) }
                }
            }
        }
    }

    fun clearUnbanSuccess() {
        _uiState.update { it.copy(unbanSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
