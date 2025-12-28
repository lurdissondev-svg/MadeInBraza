package com.madeinbraza.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.repository.EventsRepository
import com.madeinbraza.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class CreateEventUiState(
    val title: String = "",
    val description: String = "",
    val selectedDateTime: LocalDateTime? = null,
    val maxParticipants: String = "",
    val selectedClasses: Set<PlayerClass> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val titleError: String? = null,
    val dateTimeError: String? = null,
    val maxParticipantsError: String? = null,
    val isSuccess: Boolean = false
) {
    val formattedDateTime: String
        get() = selectedDateTime?.let {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            it.format(formatter)
        } ?: "Selecione data e hora"

    val maxParticipantsInt: Int?
        get() = maxParticipants.toIntOrNull()
}

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventsRepository: EventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, titleError = null) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateDateTime(dateTime: LocalDateTime) {
        _uiState.update { it.copy(selectedDateTime = dateTime, dateTimeError = null) }
    }

    fun updateMaxParticipants(value: String) {
        // Only allow digits
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _uiState.update { it.copy(maxParticipants = value, maxParticipantsError = null) }
        }
    }

    fun toggleClass(playerClass: PlayerClass) {
        _uiState.update { state ->
            val newClasses = if (state.selectedClasses.contains(playerClass)) {
                state.selectedClasses - playerClass
            } else {
                state.selectedClasses + playerClass
            }
            state.copy(selectedClasses = newClasses)
        }
    }

    fun clearSelectedClasses() {
        _uiState.update { it.copy(selectedClasses = emptySet()) }
    }

    fun createEvent() {
        val state = _uiState.value
        var hasError = false

        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Titulo obrigatorio") }
            hasError = true
        }

        if (state.selectedDateTime == null) {
            _uiState.update { it.copy(dateTimeError = "Data e hora obrigatorios") }
            hasError = true
        }

        // Validate maxParticipants if provided
        if (state.maxParticipants.isNotEmpty()) {
            val max = state.maxParticipants.toIntOrNull()
            if (max == null || max < 1) {
                _uiState.update { it.copy(maxParticipantsError = "Numero invalido") }
                hasError = true
            }
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val isoDateTime = state.selectedDateTime!!
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            when (val result = eventsRepository.createEvent(
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                eventDate = isoDateTime,
                maxParticipants = state.maxParticipantsInt,
                requiredClasses = state.selectedClasses.toList().takeIf { it.isNotEmpty() }
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}
