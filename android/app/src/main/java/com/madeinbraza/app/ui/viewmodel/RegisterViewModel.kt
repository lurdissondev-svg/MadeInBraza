package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val nick: String = "",
    val password: String = "",
    val selectedClass: PlayerClass? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val registerSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateNick(nick: String) {
        _uiState.update { it.copy(nick = nick, error = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun updateClass(playerClass: PlayerClass) {
        _uiState.update { it.copy(selectedClass = playerClass, error = null) }
    }

    fun register() {
        val state = _uiState.value

        if (state.nick.length < 3) {
            _uiState.update { it.copy(error = "Nick deve ter pelo menos 3 caracteres") }
            return
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "Senha deve ter pelo menos 6 caracteres") }
            return
        }

        if (state.selectedClass == null) {
            _uiState.update { it.copy(error = "Selecione uma classe") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.register(state.nick, state.password, state.selectedClass)) {
                is Result.Success -> {
                    // Register FCM token for push notifications
                    authRepository.registerFcmToken()

                    _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
