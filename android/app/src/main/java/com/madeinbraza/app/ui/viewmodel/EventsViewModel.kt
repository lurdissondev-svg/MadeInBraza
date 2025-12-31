package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Event
import com.madeinbraza.app.data.model.EventParticipant
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.EventsRepository
import com.madeinbraza.app.data.repository.Result
import com.madeinbraza.app.util.Debouncer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val actionInProgress: String? = null,
    val currentUserId: String? = null,
    val currentUserClass: PlayerClass? = null,
    val isLeader: Boolean = false
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val debouncer = Debouncer()

    init {
        loadUserInfo()
        loadEvents()
    }

    private fun loadUserInfo() {
        // First try cached user to avoid API call
        authRepository.getCachedUser()?.let { user ->
            _uiState.update {
                it.copy(
                    currentUserId = user.id,
                    currentUserClass = user.playerClass,
                    isLeader = user.role.name == "LEADER"
                )
            }
            return
        }

        // Fall back to API call if no cache
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            currentUserId = result.data.id,
                            currentUserClass = result.data.playerClass,
                            isLeader = result.data.role.name == "LEADER"
                        )
                    }
                }
                is Result.Error -> {
                    // Silently fail
                }
            }
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = eventsRepository.getEvents()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, events = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun refresh() {
        // Debounce refresh to prevent spam
        if (!debouncer.canExecute("events_refresh", Debouncer.REFRESH_DEBOUNCE_MS)) {
            _uiState.update { it.copy(isRefreshing = false) }
            return
        }

        debouncer.throttle(viewModelScope, "events_refresh", Debouncer.REFRESH_DEBOUNCE_MS) {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            when (val result = eventsRepository.getEvents()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, events = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun joinEvent(eventId: String) {
        val currentState = _uiState.value
        val currentUserId = currentState.currentUserId ?: return
        val currentUserClass = currentState.currentUserClass ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = eventId) }

            // Optimistic update: add user to participants immediately
            val optimisticParticipant = EventParticipant(
                id = currentUserId,
                nick = "", // Will be filled on refresh
                playerClass = currentUserClass
            )
            _uiState.update { state ->
                state.copy(
                    events = state.events.map { event ->
                        if (event.id == eventId) {
                            event.copy(participants = event.participants + optimisticParticipant)
                        } else event
                    }
                )
            }

            when (val result = eventsRepository.joinEvent(eventId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(actionInProgress = null) }
                    // No need to reload - optimistic update already applied
                }
                is Result.Error -> {
                    // Rollback optimistic update
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = null,
                            error = result.message,
                            events = state.events.map { event ->
                                if (event.id == eventId) {
                                    event.copy(participants = event.participants.filter { it.id != currentUserId })
                                } else event
                            }
                        )
                    }
                }
            }
        }
    }

    fun leaveEvent(eventId: String) {
        val currentUserId = _uiState.value.currentUserId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = eventId) }

            // Optimistic update: remove user from participants immediately
            val previousParticipants = _uiState.value.events.find { it.id == eventId }?.participants
            _uiState.update { state ->
                state.copy(
                    events = state.events.map { event ->
                        if (event.id == eventId) {
                            event.copy(participants = event.participants.filter { it.id != currentUserId })
                        } else event
                    }
                )
            }

            when (val result = eventsRepository.leaveEvent(eventId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(actionInProgress = null) }
                    // No need to reload - optimistic update already applied
                }
                is Result.Error -> {
                    // Rollback optimistic update
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = null,
                            error = result.message,
                            events = state.events.map { event ->
                                if (event.id == eventId && previousParticipants != null) {
                                    event.copy(participants = previousParticipants)
                                } else event
                            }
                        )
                    }
                }
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = eventId) }

            when (val result = eventsRepository.deleteEvent(eventId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            actionInProgress = null,
                            events = state.events.filter { it.id != eventId }
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
