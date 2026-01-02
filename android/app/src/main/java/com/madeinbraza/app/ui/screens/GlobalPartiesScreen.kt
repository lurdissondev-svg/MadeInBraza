package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.R
import com.madeinbraza.app.data.model.Party
import com.madeinbraza.app.data.model.PartySlot
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.data.model.SlotRequest
import com.madeinbraza.app.ui.viewmodel.GlobalPartiesViewModel

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

private val CLASS_ABBREVIATIONS = mapOf(
    PlayerClass.ASSASSIN to "ASS",
    PlayerClass.BRAWLER to "BS",
    PlayerClass.ATALANTA to "ATA",
    PlayerClass.PIKEMAN to "PIKE",
    PlayerClass.FIGHTER to "FIGHT",
    PlayerClass.MECHANIC to "MECH",
    PlayerClass.KNIGHT to "KNT",
    PlayerClass.PRIESTESS to "PRS",
    PlayerClass.SHAMAN to "SHA",
    PlayerClass.MAGE to "MAGE",
    PlayerClass.ARCHER to "ARC"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalPartiesScreen(
    viewModel: GlobalPartiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Create party dialog
    if (uiState.showCreateDialog) {
        CreateGlobalPartyDialog(
            isCreating = uiState.isCreating,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, description, slots -> viewModel.createParty(name, description, slots) }
        )
    }

    // Join party dialog
    uiState.partyToJoin?.let { party ->
        JoinPartyDialog(
            party = party,
            isJoining = uiState.actionInProgress == party.id,
            onDismiss = { viewModel.hideJoinDialog() },
            onJoin = { slotId -> viewModel.joinParty(party.id, slotId) }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_party))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                uiState.parties.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.no_parties),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.create_party_gather),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.parties) { party ->
                            GlobalPartyCard(
                                party = party,
                                currentUserId = uiState.currentUserId,
                                isLeader = uiState.isLeader,
                                isActionInProgress = uiState.actionInProgress == party.id,
                                onJoin = { viewModel.showJoinDialog(party) },
                                onLeave = { viewModel.leaveParty(party.id) },
                                onDelete = { viewModel.deleteParty(party.id) }
                            )
                        }
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun CreateGlobalPartyDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String?, List<SlotRequest>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var slotCounts by remember { mutableStateOf(PlayerClass.entries.associateWith { 0 }) }

    val totalSlots = slotCounts.values.sum()

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text(stringResource(R.string.create_party)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.party_name)) },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_optional)) },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vagas por Classe",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Total: $totalSlots",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Class slot selectors in a grid
                PlayerClass.entries.forEach { playerClass ->
                    val count = slotCounts[playerClass] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CLASS_DISPLAY_NAMES[playerClass] ?: playerClass.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (count > 0) {
                                        slotCounts = slotCounts + (playerClass to count - 1)
                                    }
                                },
                                enabled = !isCreating && count > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Diminuir", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    if (count < 6 && totalSlots < 6) {
                                        slotCounts = slotCounts + (playerClass to count + 1)
                                    }
                                },
                                enabled = !isCreating && count < 6 && totalSlots < 6,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val slots = slotCounts
                        .filter { it.value > 0 }
                        .map { SlotRequest(playerClass = it.key, count = it.value) }
                    val desc = description.trim().ifEmpty { null }
                    onCreate(name, desc, slots)
                },
                enabled = name.isNotBlank() && totalSlots in 2..6 && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.create))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()

@Composable
fun JoinPartyDialog(
    party: Party,
    isJoining: Boolean,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var selectedSlotId by remember { mutableStateOf<String?>(null) }

    // Group available slots by class
    val availableSlotsByClass = remember(party.slots) {
        party.slots
            .filter { it.filledBy == null }
            .groupBy { it.playerClass }
    }

    AlertDialog(
        onDismissRequest = { if (!isJoining) onDismiss() },
        title = {
            Column {
                Text("Entrar na Party")
                Text(
                    text = party.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Selecione a vaga que deseja ocupar:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                availableSlotsByClass.forEach { (playerClass, slots) ->
                    slots.forEachIndexed { index, slot ->
                        val isSelected = selectedSlotId == slot.id
                        Card(
                            onClick = { selectedSlotId = slot.id },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = CLASS_DISPLAY_NAMES[playerClass] ?: playerClass.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (slots.size > 1) {
                                    Text(
                                        text = "(${index + 1}/${slots.size})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (availableSlotsByClass.isEmpty()) {
                    Text(
                        text = "Não há vagas disponíveis nesta party.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedSlotId?.let { onJoin(it) } },
                enabled = selectedSlotId != null && !isJoining
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Entrar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isJoining
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun GlobalPartyCard(
    party: Party,
    currentUserId: String?,
    isLeader: Boolean,
    isActionInProgress: Boolean,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    val isMember = party.slots.any { it.filledBy?.id == currentUserId }
    val isCreator = party.createdBy.id == currentUserId
    val canDelete = isCreator || isLeader

    val closedText = stringResource(R.string.party_closed)
    val createdByText = stringResource(R.string.created_by_party, party.createdBy.nick)
    val deleteText = stringResource(R.string.delete)
    val leaveText = stringResource(R.string.leave)
    val joinText = stringResource(R.string.join)
    val fullText = stringResource(R.string.party_full)

    // Group slots by class
    val slotsByClass = remember(party.slots) {
        party.slots.groupBy { it.playerClass }
            .mapValues { (_, slots) ->
                val filled = slots.count { it.filledBy != null }
                val total = slots.size
                val members = slots.mapNotNull { it.filledBy }
                Triple(filled, total, members)
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (party.isClosed) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (party.isClosed) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = closedText,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Text(
                        text = createdByText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    party.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        enabled = !isActionInProgress
                    ) {
                        if (isActionInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = deleteText,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slots count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Vagas: ${party.filledSlots}/${party.totalSlots}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (party.isFull) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Slots by class
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(slotsByClass.toList()) { (playerClass, data) ->
                    val (filled, total, _) = data
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "${CLASS_ABBREVIATIONS[playerClass]} $filled/$total",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (filled == total)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Members list
            if (party.filledSlots > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val allMembers = slotsByClass.flatMap { (playerClass, data) ->
                        data.third.map { member -> member to playerClass }
                    }
                    items(allMembers) { (member, playerClass) ->
                        val isCurrentUser = member.id == currentUserId
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${member.nick} (${CLASS_ABBREVIATIONS[playerClass]})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isCurrentUser)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action button
            val buttonEnabled = !isActionInProgress && (isMember || !party.isClosed)
            val buttonText = when {
                isMember -> leaveText
                party.isClosed -> closedText
                party.isFull -> fullText
                else -> joinText
            }

            Button(
                onClick = { if (isMember) onLeave() else onJoin() },
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isMember) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(buttonText)
                }
            }
        }
    }
}
