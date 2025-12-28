package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.Announcement
import com.madeinbraza.app.data.model.Role
import com.madeinbraza.app.data.model.User
import com.madeinbraza.app.data.repository.AnnouncementsRepository
import com.madeinbraza.app.data.repository.AuthRepository
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
    val isLoadingAnnouncements: Boolean = false,
    val isRefreshing: Boolean = false,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: String? = null,
    val error: String? = null
) {
    val isLeader: Boolean get() = user?.role == Role.LEADER
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val announcementsRepository: AnnouncementsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        loadAnnouncements()
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
            when (val result = announcementsRepository.getAnnouncements()) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        announcements = result.data,
                        isRefreshing = false
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isRefreshing = false,
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
