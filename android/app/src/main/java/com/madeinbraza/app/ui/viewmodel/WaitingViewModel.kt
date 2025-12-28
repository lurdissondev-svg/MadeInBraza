package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.UserStatus
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaitingUiState(
    val isApproved: Boolean = false
)

@HiltViewModel
class WaitingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaitingUiState())
    val uiState: StateFlow<WaitingUiState> = _uiState.asStateFlow()

    fun checkStatus() {
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    if (result.data.status == UserStatus.APPROVED) {
                        _uiState.update { it.copy(isApproved = true) }
                    }
                }
                is Result.Error -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearToken()
        }
    }
}
