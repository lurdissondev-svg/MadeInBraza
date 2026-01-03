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

data class LoginUiState(
    val nick: String = "",
    val password: String = "",
    val stayLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val userStatus: String = "",
    // Forgot password states
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordNick: String = "",
    val forgotPasswordLoading: Boolean = false,
    val forgotPasswordError: String? = null,
    val forgotPasswordSuccess: String? = null,
    val newPassword: String? = null,
    val useEmailRecovery: Boolean = true  // Default to email recovery
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateNick(nick: String) {
        _uiState.update { it.copy(nick = nick, error = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun updateStayLoggedIn(value: Boolean) {
        _uiState.update { it.copy(stayLoggedIn = value) }
    }

    fun login() {
        if (_uiState.value.nick.isBlank() || _uiState.value.password.isBlank()) {
            _uiState.update { it.copy(error = "Preencha todos os campos") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.login(_uiState.value.nick, _uiState.value.password, _uiState.value.stayLoggedIn)) {
                is Result.Success -> {
                    // Register FCM token for push notifications
                    authRepository.registerFcmToken()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true,
                            userStatus = result.data.user.status.name
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun showForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = true,
                forgotPasswordNick = it.nick,
                forgotPasswordError = null,
                forgotPasswordSuccess = null,
                newPassword = null,
                useEmailRecovery = true
            )
        }
    }

    fun hideForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = false,
                forgotPasswordNick = "",
                forgotPasswordError = null,
                forgotPasswordSuccess = null,
                newPassword = null,
                useEmailRecovery = true
            )
        }
    }

    fun updateForgotPasswordNick(nick: String) {
        _uiState.update { it.copy(forgotPasswordNick = nick, forgotPasswordError = null) }
    }

    fun toggleRecoveryMethod() {
        _uiState.update {
            it.copy(
                useEmailRecovery = !it.useEmailRecovery,
                forgotPasswordError = null,
                forgotPasswordSuccess = null,
                newPassword = null
            )
        }
    }

    fun requestPasswordReset() {
        if (_uiState.value.forgotPasswordNick.isBlank()) {
            _uiState.update { it.copy(forgotPasswordError = "Digite seu nick") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(forgotPasswordLoading = true, forgotPasswordError = null) }

            if (_uiState.value.useEmailRecovery) {
                // Email recovery: sends reset link to email
                when (val result = authRepository.requestPasswordReset(_uiState.value.forgotPasswordNick)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                forgotPasswordLoading = false,
                                forgotPasswordSuccess = result.data.message,
                                newPassword = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                forgotPasswordLoading = false,
                                forgotPasswordError = result.message
                            )
                        }
                    }
                }
            } else {
                // Legacy recovery: generates new password
                when (val result = authRepository.forgotPassword(_uiState.value.forgotPasswordNick)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                forgotPasswordLoading = false,
                                forgotPasswordSuccess = result.data.message,
                                newPassword = result.data.newPassword
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                forgotPasswordLoading = false,
                                forgotPasswordError = result.message
                            )
                        }
                    }
                }
            }
        }
    }
}
