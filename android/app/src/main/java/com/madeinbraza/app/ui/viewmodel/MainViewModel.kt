package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLeader: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        // First try cached user for instant UI
        val cachedUser = authRepository.getCachedUser()
        if (cachedUser != null) {
            _uiState.value = MainUiState(
                isLeader = cachedUser.role.name == "LEADER"
            )
        }

        // Then fetch from API to ensure we have the latest data
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    _uiState.value = MainUiState(
                        isLeader = result.data.role.name == "LEADER"
                    )
                }
                is Result.Error -> {
                    // Keep cached value if API fails
                }
            }
        }
    }
}
