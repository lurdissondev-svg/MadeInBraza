package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val navigationTarget: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        viewModelScope.launch {
            val result = authRepository.checkSavedSession()

            when (result) {
                is Result.Success -> {
                    // Register FCM token for push notifications
                    authRepository.registerFcmToken()

                    val status = result.data.status.name
                    _uiState.update {
                        it.copy(
                            navigationTarget = if (status == "APPROVED") "home" else "waiting"
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(navigationTarget = "login") }
                }
            }
        }
    }
}
