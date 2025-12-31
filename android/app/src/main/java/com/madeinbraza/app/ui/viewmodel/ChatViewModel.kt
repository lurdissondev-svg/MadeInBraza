package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.ChatMessage
import com.madeinbraza.app.data.repository.ChatRepository
import com.madeinbraza.app.data.repository.Result
import com.madeinbraza.app.util.Debouncer
import com.madeinbraza.app.util.SmartPoller
import com.madeinbraza.app.util.SmartPollerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Smart polling with exponential backoff to reduce server load
    private val smartPoller: SmartPoller = SmartPollerFactory.forChat(viewModelScope)
    private val debouncer = Debouncer()

    init {
        loadMessages()
        startPolling()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = chatRepository.getMessages(limit = 50)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, messages = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun refresh() {
        // Debounce refresh to prevent spam
        if (!debouncer.canExecute("chat_refresh", Debouncer.REFRESH_DEBOUNCE_MS)) {
            _uiState.update { it.copy(isRefreshing = false) }
            return
        }

        debouncer.throttle(viewModelScope, "chat_refresh", Debouncer.REFRESH_DEBOUNCE_MS) {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            when (val result = chatRepository.getMessages(limit = 50)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, messages = result.data) }
                    smartPoller.forceRefresh() // Reset polling interval after manual refresh
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    private fun startPolling() {
        smartPoller.start(
            fetcher = {
                when (val result = chatRepository.getMessages(limit = 50)) {
                    is Result.Success -> result.data
                    is Result.Error -> null
                }
            },
            onData = { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        )
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }

            when (val result = chatRepository.sendMessage(content.trim())) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSending = false,
                            messages = state.messages + result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSending = false, error = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        smartPoller.stop()
        debouncer.cancelAll()
    }
}
