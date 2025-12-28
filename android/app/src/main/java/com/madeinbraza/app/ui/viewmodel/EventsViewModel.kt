package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Event
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.repository.AuthRepository
import com.madeinbraza.app.data.repository.EventsRepository
import com.madeinbraza.app.data.repository.Result
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

    init {
        loadUserInfo()
        loadEvents()
    }

    private fun loadUserInfo() {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = eventId) }

            when (val result = eventsRepository.joinEvent(eventId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(actionInProgress = null) }
                    loadEvents() // Refresh to get updated participant list
                }
                is Result.Error -> {
                    _uiState.update { it.copy(actionInProgress = null, error = result.message) }
                }
            }
        }
    }

    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = eventId) }

            when (val result = eventsRepository.leaveEvent(eventId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(actionInProgress = null) }
                    loadEvents() // Refresh to get updated participant list
                }
                is Result.Error -> {
                    _uiState.update { it.copy(actionInProgress = null, error = result.message) }
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
