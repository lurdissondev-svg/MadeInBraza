package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
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
            onCreate = { name, description, slots, creatorSlotClass -> viewModel.createParty(name, description, slots, creatorSlotClass) }
        )
    }

    // Join party dialog
    uiState.partyToJoin?.let { party ->
        JoinPartyDialog(
            party = party,
            isJoining = uiState.actionInProgress == party.id,
            onDismiss = { viewModel.hideJoinDialog() },
            onJoin = { slotId, selectedClass -> viewModel.joinParty(party.id, slotId, selectedClass) }
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

// Slot key type: PlayerClass enum name or "FREE"
private const val FREE_SLOT = "FREE"

private fun getSlotDisplayName(slotKey: String): String {
    if (slotKey == FREE_SLOT) return "Livre"
    return try {
        CLASS_DISPLAY_NAMES[PlayerClass.valueOf(slotKey)] ?: slotKey
    } catch (e: IllegalArgumentException) {
        slotKey
    }
}

@Composable
fun CreateGlobalPartyDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String?, List<SlotRequest>, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    // Slot counts: PlayerClass.name -> count, plus "FREE" -> count
    var slotCounts by remember {
        mutableStateOf(
            PlayerClass.entries.associate { it.name to 0 } + (FREE_SLOT to 0)
        )
    }
    var creatorSlotClass by remember { mutableStateOf<String?>(null) }

    val totalSlots = slotCounts.values.sum()
    val availableSlotKeys = slotCounts.filter { it.value > 0 }.keys.toList()

    // All slot options (class names + FREE)
    val allSlotOptions = remember { PlayerClass.entries.map { it.name } + FREE_SLOT }

    // Reset creator class if not available anymore
    LaunchedEffect(availableSlotKeys) {
        if (creatorSlotClass != null && creatorSlotClass !in availableSlotKeys) {
            creatorSlotClass = null
        }
    }

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

                // Class slot selectors including FREE
                allSlotOptions.forEach { slotKey ->
                    val count = slotCounts[slotKey] ?: 0
                    val isFreeSlot = slotKey == FREE_SLOT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getSlotDisplayName(slotKey),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFreeSlot) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isFreeSlot) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (count > 0) {
                                        slotCounts = slotCounts + (slotKey to count - 1)
                                    }
                                },
                                enabled = !isCreating && count > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleMedium)
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
                                        slotCounts = slotCounts + (slotKey to count + 1)
                                    }
                                },
                                enabled = !isCreating && count < 6 && totalSlots < 6,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                Text(
                    text = "* \"Livre\" permite qualquer classe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Creator's class selection
                if (availableSlotKeys.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sua vaga na Party",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Escolha qual vaga você vai ocupar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    availableSlotKeys.forEach { slotKey ->
                        val isSelected = creatorSlotClass == slotKey
                        val isFreeSlot = slotKey == FREE_SLOT
                        Card(
                            onClick = { creatorSlotClass = slotKey },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else if (isFreeSlot)
                                    MaterialTheme.colorScheme.tertiaryContainer
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
                                    text = getSlotDisplayName(slotKey),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isFreeSlot) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isFreeSlot) FontWeight.Medium else FontWeight.Normal
                                )
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val slots = slotCounts
                        .filter { it.value > 0 }
                        .map { SlotRequest(playerClass = it.key, count = it.value) }
                    val desc = description.trim().ifEmpty { null }
                    onCreate(name, desc, slots, creatorSlotClass!!)
                },
                enabled = name.isNotBlank() && totalSlots in 2..6 && creatorSlotClass != null && !isCreating
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
    onJoin: (String, String?) -> Unit
) {
    var selectedSlotId by remember { mutableStateOf<String?>(null) }
    var selectedClass by remember { mutableStateOf<PlayerClass?>(null) }

    // Group available slots by class (null = FREE slot)
    val availableSlotsByClass = remember(party.slots) {
        party.slots
            .filter { it.filledBy == null }
            .groupBy { it.playerClass }
    }

    // Check if selected slot is FREE
    val selectedSlotIsFree = remember(selectedSlotId, party.slots) {
        selectedSlotId?.let { slotId ->
            party.slots.find { it.id == slotId }?.playerClass == null
        } ?: false
    }

    // Reset selected class when slot changes
    LaunchedEffect(selectedSlotId) {
        selectedClass = null
    }

    val canJoin = selectedSlotId != null && (!selectedSlotIsFree || selectedClass != null)

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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Selecione a vaga que deseja ocupar:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                availableSlotsByClass.forEach { (playerClass, slots) ->
                    val isFreeSlot = playerClass == null
                    val displayName = if (isFreeSlot) "Livre (escolha sua classe)" else CLASS_DISPLAY_NAMES[playerClass] ?: playerClass.name

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
                                else if (isFreeSlot)
                                    MaterialTheme.colorScheme.tertiaryContainer
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
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isFreeSlot) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isFreeSlot) FontWeight.Medium else FontWeight.Normal
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

                // Class selector for FREE slots
                if (selectedSlotIsFree) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Escolha qual classe você vai jogar:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.height(200.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(PlayerClass.entries.toList()) { pc ->
                                    val isClassSelected = selectedClass == pc
                                    Card(
                                        onClick = { selectedClass = pc },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isClassSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = CLASS_ABBREVIATIONS[pc] ?: pc.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isClassSelected)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
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
                onClick = { selectedSlotId?.let { onJoin(it, selectedClass?.name) } },
                enabled = canJoin && !isJoining
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

    // Group slots by class (null = FREE slot)
    val slotsByClass = remember(party.slots) {
        party.slots.groupBy { it.playerClass }
            .mapValues { (playerClass, slots) ->
                val filled = slots.count { it.filledBy != null }
                val total = slots.size
                // For FREE slots, include the filledAsClass for display
                val membersWithClass = slots.mapNotNull { slot ->
                    slot.filledBy?.let { member ->
                        // For FREE slots, use filledAsClass if available
                        val displayClass = if (playerClass == null && slot.filledAsClass != null) {
                            slot.filledAsClass
                        } else {
                            playerClass
                        }
                        Triple(member, playerClass, displayClass)
                    }
                }
                Triple(filled, total, membersWithClass)
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
                    val isFreeSlot = playerClass == null
                    val abbreviation = if (isFreeSlot) "LIVRE" else CLASS_ABBREVIATIONS[playerClass] ?: playerClass.name
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "$abbreviation $filled/$total",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (filled == total)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else if (isFreeSlot)
                                MaterialTheme.colorScheme.tertiaryContainer
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
                    val allMembers = slotsByClass.flatMap { (_, data) ->
                        data.third // List of Triple(member, originalClass, displayClass)
                    }
                    items(allMembers) { (member, originalClass, displayClass) ->
                        val isCurrentUser = member.id == currentUserId
                        val isFreeSlot = originalClass == null
                        // Show the actual class chosen (filledAsClass) or the slot's class
                        val abbreviation = if (displayClass != null) {
                            CLASS_ABBREVIATIONS[displayClass] ?: displayClass.name
                        } else {
                            "LIVRE"
                        }
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${member.nick} ($abbreviation)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isCurrentUser)
                                    MaterialTheme.colorScheme.primaryContainer
                                else if (isFreeSlot)
                                    MaterialTheme.colorScheme.tertiaryContainer
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
