package com.madeinbraza.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.ui.viewmodel.CreateEventViewModel
import java.time.LocalDateTime
import java.util.*

private val CLASS_DISPLAY_NAMES = mapOf(
    PlayerClass.ASSASSIN to "Assassino",
    PlayerClass.BRAWLER to "Lutador",
    PlayerClass.ATALANTA to "Atalanta",
    PlayerClass.PIKEMAN to "Lanceiro",
    PlayerClass.FIGHTER to "Guerreiro",
    PlayerClass.MECHANIC to "Mecanico",
    PlayerClass.KNIGHT to "Cavaleiro",
    PlayerClass.PRIESTESS to "Sacerdotisa",
    PlayerClass.SHAMAN to "Xama",
    PlayerClass.MAGE to "Mago",
    PlayerClass.ARCHER to "Arqueiro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit,
    onEventCreated: () -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onEventCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar Evento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Titulo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Descricao (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = uiState.formattedDateTime,
                onValueChange = {},
                label = { Text("Data e Hora") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance()
                        uiState.selectedDateTime?.let {
                            calendar.set(it.year, it.monthValue - 1, it.dayOfMonth, it.hour, it.minute)
                        }

                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val dateTime = LocalDateTime.of(year, month + 1, dayOfMonth, hour, minute)
                                        viewModel.updateDateTime(dateTime)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = "Selecionar data",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = uiState.dateTimeError != null,
                supportingText = uiState.dateTimeError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                )
            )

            OutlinedTextField(
                value = uiState.maxParticipants,
                onValueChange = { viewModel.updateMaxParticipants(it) },
                label = { Text("Vagas (opcional)") },
                placeholder = { Text("Sem limite") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.maxParticipantsError != null,
                supportingText = uiState.maxParticipantsError?.let { { Text(it) } }
            )

            // Classes section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Classes permitidas (opcional)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.selectedClasses.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearSelectedClasses() }) {
                            Text("Limpar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (uiState.selectedClasses.isEmpty()) {
                        "Todas as classes podem participar"
                    } else {
                        "${uiState.selectedClasses.size} classe(s) selecionada(s)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PlayerClass.entries) { playerClass ->
                        val isSelected = uiState.selectedClasses.contains(playerClass)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleClass(playerClass) },
                            label = { Text(CLASS_DISPLAY_NAMES[playerClass] ?: playerClass.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { viewModel.createEvent() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Criar Evento")
                }
            }
        }
    }
}
