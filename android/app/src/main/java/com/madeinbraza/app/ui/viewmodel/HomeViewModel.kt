package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Announcement
import com.madeinbraza.app.data.model.Event
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.data.model.User
import com.madeinbraza.app.data.repository.AnnouncementsRepository
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

data class HomeUiState(
    val user: User? = null,
    val announcements: List<Announcement> = emptyList(),
    val events: List<Event> = emptyList(),
    val isLoadingAnnouncements: Boolean = false,
    val isLoadingEvents: Boolean = false,
    val isRefreshing: Boolean = false,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: String? = null,
    val joiningEventId: String? = null,
    val error: String? = null
) {
    val isLeader: Boolean get() = user?.role == Role.LEADER
    val upcomingEvents: List<Event> get() = events.take(3)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val announcementsRepository: AnnouncementsRepository,
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        loadAnnouncements()
        loadEvents()
    }

    private fun loadUser() {
        viewModelScope.launch {
            when (val result = authRepository.checkStatus()) {
                is Result.Success -> {
                    _uiState.update { it.copy(user = result.data) }
                }
                is Result.Error -> {}
            }
        }
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAnnouncements = true, error = null) }
            when (val result = announcementsRepository.getAnnouncements()) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        announcements = result.data,
                        isLoadingAnnouncements = false
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isLoadingAnnouncements = false,
                        error = result.message
                    ) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            // Load both announcements and events in parallel
            val announcementsResult = announcementsRepository.getAnnouncements()
            val eventsResult = eventsRepository.getEvents()

            _uiState.update { state ->
                state.copy(
                    announcements = if (announcementsResult is Result.Success) announcementsResult.data else state.announcements,
                    events = if (eventsResult is Result.Success) eventsResult.data else state.events,
                    isRefreshing = false,
                    error = when {
                        announcementsResult is Result.Error -> announcementsResult.message
                        eventsResult is Result.Error -> eventsResult.message
                        else -> null
                    }
                )
            }
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEvents = true) }
            when (val result = eventsRepository.getEvents()) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        events = result.data,
                        isLoadingEvents = false
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isLoadingEvents = false
                    ) }
                }
            }
        }
    }

    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(joiningEventId = eventId) }
            when (val result = eventsRepository.joinEvent(eventId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(joiningEventId = null) }
                    loadEvents()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        joiningEventId = null,
                        error = result.message
                    ) }
                }
            }
        }
    }

    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(joiningEventId = eventId) }
            when (val result = eventsRepository.leaveEvent(eventId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(joiningEventId = null) }
                    loadEvents()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        joiningEventId = null,
                        error = result.message
                    ) }
                }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createAnnouncement(title: String, content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            when (val result = announcementsRepository.createAnnouncement(title, content)) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        announcements = listOf(result.data) + it.announcements,
                        isCreating = false,
                        showCreateDialog = false
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isCreating = false,
                        error = result.message
                    ) }
                }
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = announcementId) }
            when (val result = announcementsRepository.deleteAnnouncement(announcementId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        announcements = it.announcements.filter { a -> a.id != announcementId },
                        isDeleting = null
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isDeleting = null,
                        error = result.message
                    ) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
