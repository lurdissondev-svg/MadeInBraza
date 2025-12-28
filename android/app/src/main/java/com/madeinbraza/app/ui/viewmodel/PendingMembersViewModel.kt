package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.PendingUser
import com.madeinbraza.app.data.repository.Result
import com.madeinbraza.app.data.repository.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingMembersUiState(
    val users: List<PendingUser> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val actionInProgress: String? = null
)

@HiltViewModel
class PendingMembersViewModel @Inject constructor(
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingMembersUiState())
    val uiState: StateFlow<PendingMembersUiState> = _uiState.asStateFlow()

    init {
        loadPendingUsers()
    }

    fun loadPendingUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = usersRepository.getPendingUsers()) {
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

            when (val result = usersRepository.getPendingUsers()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, users = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun approveUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = userId) }

            when (val result = usersRepository.approveUser(userId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = null,
                            users = state.users.filter { it.id != userId }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(actionInProgress = null, error = result.message) }
                }
            }
        }
    }

    fun rejectUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = userId) }

            when (val result = usersRepository.rejectUser(userId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = null,
                            users = state.users.filter { it.id != userId }
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
