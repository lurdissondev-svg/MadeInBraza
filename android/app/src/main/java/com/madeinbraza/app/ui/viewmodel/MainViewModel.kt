package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.madeinbraza.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        val cachedUser = authRepository.getCachedUser()
        _uiState.value = MainUiState(
            isLeader = cachedUser?.role?.name == "LEADER"
        )
    }
}
