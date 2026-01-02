package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import com.madeinbraza.app.ui.viewmodel.PartiesViewModel

private val CLASS_DISPLAY_NAMES = mapOf(
    PlayerClass.ASSASSIN to "Assassin",
    PlayerClass.BRAWLER to "Brawler",
    PlayerClass.ATALANTA to "Atalanta",
    PlayerClass.PIKEMAN to "Pikeman",
    PlayerClass.FIGHTER to "Fighter",
    PlayerClass.MECHANIC to "Mechanic",
    PlayerClass.KNIGHT to "Knight",
    PlayerClass.PRIESTESS to "Priestess",
    PlayerClass.SHAMAN to "Shaman",
    PlayerClass.MAGE to "Mage",
    PlayerClass.ARCHER to "Archer"
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
fun PartiesScreen(
    onNavigateBack: () -> Unit,
    viewModel: PartiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Create party dialog
    if (uiState.showCreateDialog) {
        CreateEventPartyDialog(
            isCreating = uiState.isCreating,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, description, slots -> viewModel.createParty(name, description, slots) }
        )
    }

    // Join party dialog
    uiState.partyToJoin?.let { party ->
        JoinEventPartyDialog(
            party = party,
            isJoining = uiState.isJoining,
            onDismiss = { viewModel.hideJoinDialog() },
            onJoin = { slotId -> viewModel.joinParty(party.id, slotId) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.parties))
                        if (uiState.eventTitle.isNotEmpty()) {
                            Text(
                                text = uiState.eventTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
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
                                text = stringResource(R.string.create_party_for_event),
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
                            EventPartyCard(
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
private fun CreateEventPartyDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String?, List<SlotRequest>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val slotCounts = remember { mutableStateMapOf<PlayerClass, Int>().apply {
        PlayerClass.entries.forEach { put(it, 0) }
    }}

    val totalSlots = slotCounts.values.sum()

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text(stringResource(R.string.create_party)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
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
                Text(
                    text = "Slots por Classe (Total: $totalSlots)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Class slot selection grid
                PlayerClass.entries.forEach { playerClass ->
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val current = slotCounts[playerClass] ?: 0
                                    if (current > 0) slotCounts[playerClass] = current - 1
                                },
                                enabled = !isCreating && (slotCounts[playerClass] ?: 0) > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = "${slotCounts[playerClass] ?: 0}",
                                modifier = Modifier
                                    .width(32.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    val current = slotCounts[playerClass] ?: 0
                                    if (current < 6 && totalSlots < 6) {
                                        slotCounts[playerClass] = current + 1
                                    }
                                },
                                enabled = !isCreating && (slotCounts[playerClass] ?: 0) < 6 && totalSlots < 6,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                if (totalSlots < 2 || totalSlots > 6) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (totalSlots < 2) "Mínimo de 2 slots" else "Máximo de 6 slots",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
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
private fun JoinEventPartyDialog(
    party: Party,
    isJoining: Boolean,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    // Group available slots by class
    val availableSlots = party.slots.filter { it.filledBy == null }
    val slotsByClass = availableSlots.groupBy { it.playerClass }

    AlertDialog(
        onDismissRequest = { if (!isJoining) onDismiss() },
        title = { Text("Entrar na Party") },
        text = {
            Column {
                Text(
                    text = "Escolha uma vaga disponível:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (availableSlots.isEmpty()) {
                    Text(
                        text = "Nenhuma vaga disponível",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        slotsByClass.forEach { (playerClass, slots) ->
                            val className = CLASS_DISPLAY_NAMES[playerClass] ?: playerClass.name
                            val slotCount = slots.size

                            OutlinedButton(
                                onClick = { onJoin(slots.first().id) },
                                enabled = !isJoining,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("$className ($slotCount ${if (slotCount == 1) "vaga" else "vagas"})")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
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
private fun EventPartyCard(
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

    // Group slots by class for display
    val slotsByClass = party.slots.groupBy { it.playerClass }

    val closedText = stringResource(R.string.party_closed)
    val createdByText = stringResource(R.string.created_by_party, party.createdBy.nick)
    val deleteText = stringResource(R.string.delete)
    val leaveText = stringResource(R.string.leave)
    val joinText = stringResource(R.string.join)
    val fullText = stringResource(R.string.party_full)

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
                    text = "${party.filledSlots}/${party.totalSlots} vagas preenchidas",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (party.isFull) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Slots by class display
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(slotsByClass.entries.toList()) { (playerClass, slots) ->
                    val filled = slots.count { it.filledBy != null }
                    val total = slots.size
                    val abbrev = CLASS_ABBREVIATIONS[playerClass] ?: playerClass.name.take(3)

                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "$abbrev: $filled/$total",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (filled == total)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Show filled slot members
            val filledSlots = party.slots.filter { it.filledBy != null }
            if (filledSlots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filledSlots) { slot ->
                        val isCurrentUser = slot.filledBy?.id == currentUserId
                        val abbrev = CLASS_ABBREVIATIONS[slot.playerClass] ?: slot.playerClass.name.take(3)
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${slot.filledBy?.nick} ($abbrev)",
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
            val hasAvailableSlots = party.slots.any { it.filledBy == null }
            val buttonEnabled = !isActionInProgress && (isMember || (!party.isClosed && hasAvailableSlots))
            val buttonText = when {
                isMember -> leaveText
                party.isClosed -> closedText
                !hasAvailableSlots -> fullText
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
