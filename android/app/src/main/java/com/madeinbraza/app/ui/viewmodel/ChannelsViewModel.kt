package com.madeinbraza.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Channel
import com.madeinbraza.app.data.model.ChannelMember
import com.madeinbraza.app.data.model.ChannelMessage
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.ChannelRepository
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

data class ChannelsListUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val unreadCounts: Map<String, Int> = emptyMap()
)

data class ChannelChatUiState(
    val channel: Channel? = null,
    val messages: List<ChannelMessage> = emptyList(),
    val currentUserId: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null
)

data class ChannelMembersUiState(
    val channelId: String? = null,
    val channelName: String? = null,
    val members: List<ChannelMember> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _channelsState = MutableStateFlow(ChannelsListUiState())
    val channelsState: StateFlow<ChannelsListUiState> = _channelsState.asStateFlow()

    private val _chatState = MutableStateFlow(ChannelChatUiState())
    val chatState: StateFlow<ChannelChatUiState> = _chatState.asStateFlow()

    private val _membersState = MutableStateFlow(ChannelMembersUiState())
    val membersState: StateFlow<ChannelMembersUiState> = _membersState.asStateFlow()

    // Smart polling with exponential backoff for chat messages
    private val messagePoller: SmartPoller = SmartPollerFactory.forChat(viewModelScope)
    private val debouncer = Debouncer()
    private var currentChannelId: String? = null

    init {
        loadUserInfo()
        loadChannels()
        loadUnreadCounts()
    }

    private fun loadUserInfo() {
        // First try cached user to avoid API call
        authRepository.getCachedUser()?.let { user ->
            _chatState.update { it.copy(currentUserId = user.id) }
            return
        }

        // Fall back to API call if no cache
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    _chatState.update { it.copy(currentUserId = result.data.id) }
                }
                is Result.Error -> {
                    // Silently fail
                }
            }
        }
    }

    fun loadChannels() {
        viewModelScope.launch {
            _channelsState.update { it.copy(isLoading = true, error = null) }

            when (val result = channelRepository.getChannels()) {
                is Result.Success -> {
                    _channelsState.update { it.copy(isLoading = false, channels = result.data) }
                    loadUnreadCounts()
                }
                is Result.Error -> {
                    _channelsState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun loadUnreadCounts() {
        viewModelScope.launch {
            val channels = _channelsState.value.channels
            val counts = mutableMapOf<String, Int>()
            channels.forEach { channel ->
                counts[channel.id] = channelRepository.getUnreadCount(channel.id)
            }
            _channelsState.update { it.copy(unreadCounts = counts) }
        }
    }

    fun refreshChannels() {
        // Debounce refresh to prevent spam
        if (!debouncer.canExecute("channels_refresh", Debouncer.REFRESH_DEBOUNCE_MS)) {
            _channelsState.update { it.copy(isRefreshing = false) }
            return
        }

        debouncer.throttle(viewModelScope, "channels_refresh", Debouncer.REFRESH_DEBOUNCE_MS) {
            _channelsState.update { it.copy(isRefreshing = true, error = null) }

            when (val result = channelRepository.getChannels()) {
                is Result.Success -> {
                    _channelsState.update { it.copy(isRefreshing = false, channels = result.data) }
                    loadUnreadCounts()
                }
                is Result.Error -> {
                    _channelsState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun setupDefaultChannels() {
        viewModelScope.launch {
            _channelsState.update { it.copy(isLoading = true, error = null) }

            when (channelRepository.setupDefaultChannels()) {
                is Result.Success -> {
                    loadChannels()
                }
                is Result.Error -> {
                    _channelsState.update { it.copy(isLoading = false, error = "Erro ao criar canais") }
                }
            }
        }
    }

    fun openChannel(channel: Channel) {
        currentChannelId = channel.id
        _chatState.update { it.copy(channel = channel, messages = emptyList(), error = null) }
        loadMessages(channel.id)
        startPolling(channel.id)
        markChannelAsRead(channel.id)
    }

    private fun markChannelAsRead(channelId: String) {
        viewModelScope.launch {
            channelRepository.setLastReadTimestamp(channelId, System.currentTimeMillis())
            // Update the unread count to 0 for this channel
            _channelsState.update { state ->
                state.copy(unreadCounts = state.unreadCounts + (channelId to 0))
            }
        }
    }

    fun closeChannel() {
        messagePoller.stop()
        currentChannelId = null
        _chatState.update { ChannelChatUiState() }
        // Reload unread counts when returning to channel list
        loadUnreadCounts()
    }

    private fun loadMessages(channelId: String) {
        viewModelScope.launch {
            _chatState.update { it.copy(isLoading = true, error = null) }

            when (val result = channelRepository.getChannelMessages(channelId, limit = 50)) {
                is Result.Success -> {
                    _chatState.update { it.copy(isLoading = false, messages = result.data) }
                }
                is Result.Error -> {
                    _chatState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun refreshMessages() {
        val channelId = currentChannelId ?: return

        // Debounce refresh to prevent spam
        if (!debouncer.canExecute("messages_refresh_$channelId", Debouncer.REFRESH_DEBOUNCE_MS)) {
            _chatState.update { it.copy(isRefreshing = false) }
            return
        }

        debouncer.throttle(viewModelScope, "messages_refresh_$channelId", Debouncer.REFRESH_DEBOUNCE_MS) {
            _chatState.update { it.copy(isRefreshing = true, error = null) }

            when (val result = channelRepository.getChannelMessages(channelId, limit = 50)) {
                is Result.Success -> {
                    _chatState.update { it.copy(isRefreshing = false, messages = result.data) }
                    messagePoller.forceRefresh() // Reset polling interval after manual refresh
                }
                is Result.Error -> {
                    _chatState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    private fun startPolling(channelId: String) {
        messagePoller.stop()
        messagePoller.start(
            fetcher = {
                if (currentChannelId != channelId) return@start null
                when (val result = channelRepository.getChannelMessages(channelId, limit = 50)) {
                    is Result.Success -> result.data
                    is Result.Error -> null
                }
            },
            onData = { messages ->
                _chatState.update { it.copy(messages = messages) }
            }
        )
    }

    fun sendMessage(content: String) {
        val channelId = currentChannelId ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _chatState.update { it.copy(isSending = true, error = null) }

            when (val result = channelRepository.sendMessage(channelId, content.trim())) {
                is Result.Success -> {
                    _chatState.update { state ->
                        state.copy(
                            isSending = false,
                            messages = state.messages + result.data
                        )
                    }
                }
                is Result.Error -> {
                    _chatState.update { it.copy(isSending = false, error = result.message) }
                }
            }
        }
    }

    fun sendMediaMessage(fileUri: Uri, content: String? = null) {
        val channelId = currentChannelId ?: return

        viewModelScope.launch {
            _chatState.update { it.copy(isUploading = true, error = null) }

            when (val result = channelRepository.sendMediaMessage(channelId, fileUri, content?.trim())) {
                is Result.Success -> {
                    _chatState.update { state ->
                        state.copy(
                            isUploading = false,
                            messages = state.messages + result.data
                        )
                    }
                }
                is Result.Error -> {
                    _chatState.update { it.copy(isUploading = false, error = result.message) }
                }
            }
        }
    }

    fun loadChannelMembers(channel: Channel) {
        viewModelScope.launch {
            _membersState.update {
                it.copy(
                    channelId = channel.id,
                    channelName = channel.name,
                    isLoading = true,
                    error = null,
                    members = emptyList()
                )
            }

            when (val result = channelRepository.getChannelMembers(channel.id)) {
                is Result.Success -> {
                    _membersState.update { it.copy(isLoading = false, members = result.data) }
                }
                is Result.Error -> {
                    _membersState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun closeMembersSheet() {
        _membersState.update { ChannelMembersUiState() }
    }

    fun clearChannelsError() {
        _channelsState.update { it.copy(error = null) }
    }

    fun clearChatError() {
        _chatState.update { it.copy(error = null) }
    }

    fun clearMembersError() {
        _membersState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        messagePoller.stop()
        debouncer.cancelAll()
    }
}
