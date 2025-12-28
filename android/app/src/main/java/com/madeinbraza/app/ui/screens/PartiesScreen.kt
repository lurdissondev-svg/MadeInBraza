package com.madeinbraza.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.madeinbraza.app.R
import com.madeinbraza.app.data.model.Party
import com.madeinbraza.app.data.model.PlayerClass
import com.madeinbraza.app.ui.viewmodel.PartiesViewModel

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
fun PartiesScreen(
    onNavigateBack: () -> Unit,
    viewModel: PartiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Create party dialog
    if (uiState.showCreateDialog) {
        CreatePartyDialog(
            isCreating = uiState.isCreating,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, description, maxMembers -> viewModel.createParty(name, description, maxMembers) }
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
                            PartyCard(
                                party = party,
                                currentUserId = uiState.currentUserId,
                                isLeader = uiState.isLeader,
                                isActionInProgress = uiState.actionInProgress == party.id,
                                onJoin = { viewModel.joinParty(party.id) },
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
fun CreatePartyDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String?, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var maxMembers by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text(stringResource(R.string.create_party)) },
        text = {
            Column {
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
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = maxMembers,
                    onValueChange = { maxMembers = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.max_members)) },
                    singleLine = true,
                    enabled = !isCreating,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val max = maxMembers.toIntOrNull()
                    val desc = description.trim().ifEmpty { null }
                    onCreate(name, desc, max)
                },
                enabled = name.isNotBlank() && !isCreating
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
fun PartyCard(
    party: Party,
    currentUserId: String?,
    isLeader: Boolean,
    isActionInProgress: Boolean,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    val isMember = party.members.any { it.id == currentUserId }
    val isCreator = party.createdBy.id == currentUserId
    val canDelete = isCreator || isLeader

    val closedText = stringResource(R.string.party_closed)
    val createdByText = stringResource(R.string.created_by_party, party.createdBy.nick)
    val deleteText = stringResource(R.string.delete)
    val membersCountText = stringResource(R.string.party_members_count, party.members.size, party.maxMembers)
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

            // Members count
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
                    text = membersCountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (party.isFull) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Members list with class chips
            if (party.members.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(party.members) { member ->
                        val isCurrentUser = member.id == currentUserId
                        val className = member.playerClass?.let { CLASS_DISPLAY_NAMES[it] ?: it.name } ?: "?"
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${member.nick} ($className)",
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
