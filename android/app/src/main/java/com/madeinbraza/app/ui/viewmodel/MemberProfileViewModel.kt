package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.MemberProfile
import com.madeinbraza.app.data.repository.MembersRepository
import com.madeinbraza.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberProfileUiState(
    val profile: MemberProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MemberProfileViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val memberId: String = checkNotNull(savedStateHandle["memberId"])

    private val _uiState = MutableStateFlow(MemberProfileUiState())
    val uiState: StateFlow<MemberProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = membersRepository.getMemberProfile(memberId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, profile = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
